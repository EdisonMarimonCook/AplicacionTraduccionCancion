from fastapi import APIRouter, HTTPException, Depends
from typing import Optional
import logging
import asyncio
from datetime import datetime, timedelta

from models import User
from routers.auth import get_current_user
from routers.schemas import LyricsResponse
from utils.genius_client import get_song_lyrics
from utils.spotify import enrich_single_song 
from utils.audio_extractor import get_youtube_audio_url
from database import db  # 🔥 Para caché de letras

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/lyrics", tags=["Lyrics"])

@router.get("/", response_model=LyricsResponse)
async def get_lyrics(
    title: str,
    artist: str,
    current_user: User = Depends(get_current_user)
):
    """
    📄 Obtiene letra, metadatos y busca enlaces de audio (Preview + Full).
    🔥 OPTIMIZADO: Usa caché de MongoDB (TTL 30 días) para canciones populares.
    NOTA: Si no se encuentra letra, retorna respuesta parcial con audio solamente.
    """
    try:
        logger.info(f"📄 Procesando canción: {title} - {artist}")
        
        # 1. Buscar metadata en Spotify/iTunes PRIMERO (para validación Y caché)
        song_meta = await enrich_single_song(title, artist)
        spotify_id = song_meta.get("id")  # ID único de Spotify
        
        # 🔥 2. BUSCAR EN CACHÉ (si tenemos spotify_id)
        cached_lyrics = None
        if spotify_id:
            try:
                cached_lyrics = await db.db["lyrics_cache"].find_one({"spotify_id": spotify_id})
                if cached_lyrics:
                    logger.info(f"⚡ CACHÉ HIT: Letra encontrada en caché para {title}")
                    
                    # Retornar desde caché (mucho más rápido)
                    return {
                        "title": cached_lyrics.get("title"),
                        "artist": cached_lyrics.get("artist"),
                        "lyrics": cached_lyrics.get("lyrics"),
                        "image_url": cached_lyrics.get("image_url") or song_meta.get("image_url"),
                        "genius_url": cached_lyrics.get("genius_url"),
                        "preview_url": song_meta.get("preview_url"),
                        "confidence": cached_lyrics.get("confidence", "medium"),
                        "validation_warnings": cached_lyrics.get("validation_warnings")
                    }
                else:
                    logger.info(f"🔍 CACHÉ MISS: Buscando letra en fuentes externas...")
            except Exception as e:
                logger.warning(f"⚠️ Error consultando caché: {e}")
        
        # 3. NO EN CACHÉ → Obtener Letras con timeout máximo de 20s + metadata para validación
        try:
            lyrics_data = await asyncio.wait_for(
                get_song_lyrics(title, artist, spotify_metadata=song_meta),
                timeout=20.0  # 20 segundos máximo total
            )
        except asyncio.TimeoutError:
            logger.warning(f"⏱️ Timeout buscando letra (>20s): {title} - {artist}")
            lyrics_data = None

        # 4. Construir Respuesta (tolerante a falta de letra)
        if not lyrics_data:
            # ⚠️ NO HAY LETRA - Retornar solo metadatos y audio
            logger.warning(f"⚠️ Letra no encontrada para: {title} - {artist}")
            return {
                "title": title,
                "artist": artist,
                "lyrics": "",  # Vacío, el frontend mostrará mensaje
                "image_url": song_meta.get("image_url"),
                "genius_url": None,
                "preview_url": song_meta.get("preview_url")
            }
        
        # ✅ HAY LETRA - Respuesta completa con validación
        confidence = lyrics_data.get("confidence", "medium")
        warnings = lyrics_data.get("validation_warnings", [])
        
        # Log si hay baja confianza
        if confidence == "low":
            logger.error(f"❌ ADVERTENCIA: Letra con baja confianza para '{title}' - {artist}: {warnings}")
        
        # 🔥 5. GUARDAR EN CACHÉ (si tenemos spotify_id y confianza >= medium)
        if spotify_id and confidence in ["high", "medium"]:
            try:
                cache_entry = {
                    "spotify_id": spotify_id,
                    "title": lyrics_data.get("title"),
                    "artist": lyrics_data.get("artist"),
                    "lyrics": lyrics_data.get("lyrics"),
                    "image_url": lyrics_data.get("image_url"),
                    "genius_url": lyrics_data.get("url"),
                    "confidence": confidence,
                    "validation_warnings": warnings,
                    "cached_at": datetime.utcnow(),
                    "expires_at": datetime.utcnow() + timedelta(days=30)  # TTL 30 días
                }
                
                # Upsert (insertar o actualizar)
                await db.db["lyrics_cache"].update_one(
                    {"spotify_id": spotify_id},
                    {"$set": cache_entry},
                    upsert=True
                )
                logger.info(f"💾 Letra guardada en caché (TTL 30 días): {title}")
            except Exception as e:
                logger.warning(f"⚠️ Error guardando en caché: {e}")
        
        return {
            "title": lyrics_data.get("title"),
            "artist": lyrics_data.get("artist"),
            "lyrics": lyrics_data.get("lyrics"),
            # Imagen: Prioridad Genius > Spotify
            "image_url": lyrics_data.get("image_url") or song_meta.get("image_url"),
            "genius_url": lyrics_data.get("url"),
            # 🎵 SOLO PREVIEW
            "preview_url": song_meta.get("preview_url"),
            # 🔍 Metadata de validación (para debugging en frontend)
            "confidence": confidence,
            "validation_warnings": warnings if warnings else None
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error getting lyrics/audio: {e}")
        raise HTTPException(status_code=500, detail="Internal server error")