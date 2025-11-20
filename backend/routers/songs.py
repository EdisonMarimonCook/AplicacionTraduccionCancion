"""
ROUTER: Canciones
PROPÓSITO: Endpoints para listar y obtener información de canciones
"""

import logging
from typing import List, Optional
from fastapi import APIRouter, HTTPException, Depends, status

from models import User
from routers.auth import get_current_user
from cache import get_cached_songs_by_language

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/songs", tags=["Songs"])

# =============================================================================
# ENDPOINTS - RUTAS ESPECÍFICAS PRIMERO
# ===============================================================================

@router.get("/search/genius", response_model=dict)
async def search_genius_lyrics(
    title: str,
    artist: Optional[str] = None,
    current_user: User = Depends(get_current_user)
):
    """
    🎵 BUSCA LETRA EN GENIUS (cualquier canción del mundo)
    
    PARÁMETROS:
    - title: Título de la canción (REQUERIDO)
    - artist: Artista (opcional, pero recomendado)
    """
    
    try:
        logger.info(f"🎵 Buscando en Genius: {title} - {artist}")
        
        from utils.genius_client import get_song_lyrics
        
        # Buscar en Genius
        lyrics_data = await get_song_lyrics(title, artist or "")
        
        if not lyrics_data or not isinstance(lyrics_data, dict):
            logger.warning(f"⚠️  Letra no encontrada: {title}")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Lyrics not found for '{title}' by {artist or 'any artist'}"
            )
        
        logger.info(f"✅ Letra encontrada en Genius")
        
        return {
            "title": lyrics_data.get("title"),
            "artist": lyrics_data.get("artist"),
            "lyrics": lyrics_data.get("lyrics"),
            "line_count": lyrics_data.get("line_count"),
            "url": lyrics_data.get("url"),
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error buscando en Genius: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error fetching lyrics from Genius"
        )

# ===============================================================================

@router.get("/", response_model=List[dict])
async def get_all_songs(
    language: Optional[str] = None,
    current_user: User = Depends(get_current_user)
):
    """
    📚 Obtiene TODAS las canciones disponibles
    """
    
    try:
        logger.info(f"📥 Obteniendo canciones para usuario: {current_user.email}")
        
        if language:
            songs = get_cached_songs_by_language(language)
            logger.info(f"📥 Canciones filtradas por idioma '{language}': {len(songs)} encontradas")
        else:
            from cache import get_cached_songs
            songs = get_cached_songs()
            logger.info(f"📥 Todas las canciones: {len(songs)} encontradas")
        
        simplified_songs = []
        
        for song in songs:
            try:
                simplified = {
                    "id": song.get("id") or song.get("uri", "").split(":")[-1] or "unknown",
                    "name": song.get("name", "Unknown"),
                    "artist": song.get("artist", "Unknown"),
                    "preview_url": song.get("preview_url"),
                    "image_url": song.get("image_url") or (
                        song.get("album", {}).get("images", [{}])[0].get("url") if isinstance(song.get("album"), dict) else None
                    ),
                    "language": song.get("language", "en")  # Por ahora default "en"
                }
                simplified_songs.append(simplified)
            
            except Exception as e:
                logger.warning(f"⚠️  Error simplificando canción: {str(e)}")
                continue
        
        logger.info(f"✅ Se retornaron {len(simplified_songs)} canciones")
        
        return simplified_songs
    
    except Exception as e:
        logger.error(f"❌ Error obteniendo canciones: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error fetching songs"
        )

# ===============================================================================

@router.get("/{song_id}", response_model=dict)
async def get_song_details(
    song_id: str,
    current_user: User = Depends(get_current_user)
):
    """
    🎵 Obtiene detalles ESPECÍFICOS de una canción
    """
    
    try:
        logger.info(f"📥 Buscando canción: {song_id}")
        
        from cache import get_song_from_cache
        song = get_song_from_cache(song_id)
        
        if not song:
            logger.warning(f"⚠️  Canción no encontrada: {song_id}")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Song not found"
            )
        
        return {
            "id": song.get("id") or song_id,
            "name": song.get("name", "Unknown"),
            "artist": song.get("artist", "Unknown"),
            "preview_url": song.get("preview_url"),
            "language": song.get("language", "en"),
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error obteniendo detalles: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error fetching song details"
        )

# ===============================================================================

@router.get("/{song_id}/preview", response_model=dict)
async def get_song_preview(
    song_id: str,
    current_user: User = Depends(get_current_user)
):
    """
    🎧 Obtiene URL del PREVIEW (primeros 30 segundos) de una canción
    """
    
    try:
        logger.info(f"🎧 Obteniendo preview de: {song_id}")
        
        from cache import get_song_from_cache
        song = get_song_from_cache(song_id)
        
        if not song or not song.get("preview_url"):
            logger.warning(f"⚠️  Preview no disponible para: {song_id}")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Preview not available for this song"
            )
        
        return {
            "preview_url": song.get("preview_url"),
            "duration_ms": 30000,
            "message": "Preview URL (30 seconds)"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error obteniendo preview: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error fetching preview"
        )

# ===============================================================================
# TOP 10 - CANCIONES POPULARES POR IDIOMA
# ===============================================================================

@router.get("/top10", response_model=dict)
async def get_top10_all_languages(
    current_user: User = Depends(get_current_user)
):
    """
    🏆 TOP 10 de TODAS las canciones populares
    
    RETORNA:
    {
        "en": [10 canciones],
        "es": [10 canciones],
        "fr": [10 canciones],
        ...
    }
    """
    try:
        logger.info(f"🏆 Obteniendo Top 10 para todos los idiomas")
        
        from cache import top10_cache
        
        if not top10_cache:
            logger.warning("⚠️  Cache de Top 10 vacío")
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Top 10 cache not available yet"
            )
        
        # Simplificar cada canción
        simplified_cache = {}
        for lang, songs in top10_cache.items():
            simplified_cache[lang] = []
            for song in songs:
                try:
                    simplified = {
                        "id": song.get("id") or song.get("uri", "").split(":")[-1],
                        "name": song.get("name", "Unknown"),
                        "artist": song.get("artist", "Unknown"),
                        "preview_url": song.get("preview_url"),
                        "image_url": song.get("image_url"),
                        "language": lang
                    }
                    simplified_cache[lang].append(simplified)
                except Exception as e:
                    logger.warning(f"⚠️  Error procesando canción: {e}")
                    continue
        
        logger.info(f"✅ Top 10 retornado para {len(simplified_cache)} idiomas")
        
        return {
            "total_languages": len(simplified_cache),
            "data": simplified_cache,
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error obteniendo Top 10: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error fetching Top 10"
        )

# ===============================================================================

@router.get("/top10/{language}", response_model=dict)
async def get_top10_by_language(
    language: str,
    current_user: User = Depends(get_current_user)
):
    """
    🏆 TOP 10 de canciones en un idioma ESPECÍFICO
    
    PARÁMETROS:
    - language: Código de idioma (jp, es, fr, de, it, pt, en)
    
    EJEMPLO:
    GET /api/v1/songs/top10/jp
    
    RETORNA:
    {
        "language": "jp",
        "total": 10,
        "songs": [...]
    }
    """
    try:
        logger.info(f"🏆 Obteniendo Top 10 para: {language}")
        
        from cache import top10_cache
        
        if language not in top10_cache:
            logger.warning(f"⚠️  Idioma no disponible: {language}")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Language '{language}' not found. Supported: en, es, fr, de, it, pt, jp"
            )
        
        songs = top10_cache[language]
        
        if not songs:
            logger.warning(f"⚠️  No hay canciones para {language}")
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail=f"No songs available for language '{language}' yet"
            )
        
        # Simplificar canciones
        simplified_songs = []
        for song in songs:
            try:
                simplified = {
                    "id": song.get("id") or song.get("uri", "").split(":")[-1],
                    "name": song.get("name", "Unknown"),
                    "artist": song.get("artist", "Unknown"),
                    "preview_url": song.get("preview_url"),
                    "image_url": song.get("image_url"),
                    "language": language
                }
                simplified_songs.append(simplified)
            except Exception as e:
                logger.warning(f"⚠️  Error procesando canción: {e}")
                continue
        
        logger.info(f"✅ Top 10 retornado para {language}: {len(simplified_songs)} canciones")
        
        return {
            "language": language,
            "total": len(simplified_songs),
            "songs": simplified_songs,
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error obteniendo Top 10 para {language}: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error fetching Top 10 for language '{language}'"
        )