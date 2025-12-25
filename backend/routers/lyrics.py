"""
ROUTER: Letras de Canciones
"""

from fastapi import APIRouter, HTTPException, Depends
from typing import Optional
import logging

from models import User
from routers.auth import get_current_user
from utils.genius_client import get_song_lyrics
from utils.spotify import enrich_single_song 
from utils.lyrics_fragmenter import fragment_lyrics

# ✅ IMPORTAMOS GEMINI
from services.gemini_client import highlight_by_level

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/lyrics", tags=["Lyrics"])

@router.get("/", response_model=dict)
async def get_lyrics(
    title: str,
    artist: str,
    current_user: User = Depends(get_current_user)
):
    """
    📄 Obtiene letra plana + Audio Preview (iTunes/Spotify)
    """
    try:
        logger.info(f"📄 Buscando letra: {title} - {artist}")
        
        # 🚨 CORRECCIÓN AQUÍ: Faltaba el 'await'
        lyrics_data = await get_song_lyrics(title, artist)
        
        if not lyrics_data:
            raise HTTPException(status_code=404, detail="Lyrics not found")

        # 2. Intentar conseguir el AUDIO (Spotify o iTunes)
        song_meta = await enrich_single_song(title, artist)
        
        return {
            "title": lyrics_data.get("title"),
            "artist": lyrics_data.get("artist"),
            "lyrics": lyrics_data.get("lyrics"),
            # Priorizamos la imagen de Spotify/iTunes si Genius no tiene o es de baja calidad
            "image_url": lyrics_data.get("image_url") or song_meta.get("image_url"),
            "preview_url": song_meta.get("preview_url"), # ✅ AUDIO DE ITUNES
            "genius_url": lyrics_data.get("url")
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error getting lyrics: {e}")
        raise HTTPException(status_code=500, detail="Internal server error")

@router.get("/with-fragments", response_model=dict)
async def get_lyrics_fragments(
    title: str,
    artist: str,
    current_user: User = Depends(get_current_user)
):
    """Retorna letra fragmentada para karaoke"""
    try:
        # 🚨 CORRECCIÓN AQUÍ TAMBIÉN: Añadir 'await'
        lyrics_data = await get_song_lyrics(title, artist)
        
        if not lyrics_data:
            raise HTTPException(status_code=404, detail="Lyrics not found")
            
        # Fragmentamos
        fragments = fragment_lyrics(lyrics_data.get("lyrics", ""))
        
        # Audio
        song_meta = await enrich_single_song(title, artist)

        return {
            "title": lyrics_data.get("title"),
            "artist": lyrics_data.get("artist"),
            "fragments": fragments,
            "preview_url": song_meta.get("preview_url"),
            "image_url": lyrics_data.get("image_url") or song_meta.get("image_url")
        }
    except Exception as e:
        logger.error(f"❌ Error fragmenting: {e}")
        raise HTTPException(status_code=500, detail="Error processing fragments")