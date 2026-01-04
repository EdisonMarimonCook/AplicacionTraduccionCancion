from fastapi import APIRouter, HTTPException, Depends
from typing import Optional
import logging
import asyncio

from models import User
from routers.auth import get_current_user
from routers.schemas import LyricsResponse
from utils.genius_client import get_song_lyrics
from utils.spotify import enrich_single_song 
from utils.audio_extractor import get_youtube_audio_url

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
    NOTA: Si no se encuentra letra, retorna respuesta parcial con audio solamente.
    """
    try:
        logger.info(f"📄 Procesando canción: {title} - {artist}")
        
        # 1. Obtener Letras con timeout máximo de 20s (no eternizar espera)
        try:
            lyrics_data = await asyncio.wait_for(
                get_song_lyrics(title, artist),
                timeout=20.0  # 20 segundos máximo total
            )
        except asyncio.TimeoutError:
            logger.warning(f"⏱️ Timeout buscando letra (>20s): {title} - {artist}")
            lyrics_data = None
        
        # 2. Buscar Preview en iTunes/Spotify (Rápido)
        song_meta = await enrich_single_song(title, artist)

        # 3. Construir Respuesta (tolerante a falta de letra)
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
        
        # ✅ HAY LETRA - Respuesta completa
        return {
            "title": lyrics_data.get("title"),
            "artist": lyrics_data.get("artist"),
            "lyrics": lyrics_data.get("lyrics"),
            # Imagen: Prioridad Genius > Spotify
            "image_url": lyrics_data.get("image_url") or song_meta.get("image_url"),
            "genius_url": lyrics_data.get("url"),
            # 🎵 SOLO PREVIEW
            "preview_url": song_meta.get("preview_url")
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error getting lyrics/audio: {e}")
        raise HTTPException(status_code=500, detail="Internal server error")