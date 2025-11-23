"""
ROUTER: Letras de Canciones
PROPÓSITO: Endpoints para obtener, analizar y fragmentar letras.

ENDPOINTS:
- GET /                    → Letra plana
- GET /search              → Buscar en Genius
- GET /analyze             → Analizar con colores (IA real)
- GET /with-fragments      → Para karaoke/sincronización
"""

from fastapi import APIRouter, HTTPException, Depends, status
from typing import Optional
import logging

from models import User
from routers.auth import get_current_user
from utils.genius_client import (
    get_song_lyrics,
    is_genius_configured,
    search_genius_songs
)
from services.openai_client import highlight_by_level

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/lyrics", tags=["Lyrics"])

# ===============================================================================
# ENDPOINT 1: OBTENER LETRA PLANA
# ===============================================================================

@router.get("/", response_model=dict)
async def get_lyrics(
    title: str,
    artist: str,
    current_user: User = Depends(get_current_user)
):
    """
    📄 Obtiene letra plana (sin procesar)
    
    Args:
        title: Título de la canción
        artist: Artista
    
    Returns:
        Letra completa con metadatos
    """
    try:
        logger.info(f"📥 Buscando letra: {title} por {artist}")
        
        lyrics_data = await get_song_lyrics(title, artist)
        
        if not lyrics_data or not isinstance(lyrics_data, dict):
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Lyrics not found for {title} by {artist}"
            )
        
        return {
            "title": lyrics_data.get("title"),
            "artist": lyrics_data.get("artist"),
            "lyrics": lyrics_data.get("lyrics"),
            "line_count": lyrics_data.get("line_count"),
            "url": lyrics_data.get("url"),
            "language": lyrics_data.get("language", "en"),
            "image_url": lyrics_data.get("image_url"),
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error obteniendo letra: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error fetching lyrics"
        )

# ===============================================================================
# ENDPOINT 2: BUSCAR SUGERENCIAS
# ===============================================================================

@router.get("/search", response_model=dict)
async def search_lyrics_suggestions(
    q: str,
    limit: int = 5,
    current_user: User = Depends(get_current_user)
):
    """
    🔍 Busca sugerencias de canciones en Genius
    
    Args:
        q: Query de búsqueda
        limit: Límite de resultados
    
    Returns:
        Lista de canciones encontradas
    """
    try:
        logger.info(f"🔍 Buscando sugerencias: {q}")
        
        results = await search_genius_songs(q, limit)
        
        if not results:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"No results found for '{q}'"
            )
        
        return {
            "query": q,
            "total_results": len(results),
            "results": results,
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error en búsqueda: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error searching"
        )

# ===============================================================================
# ENDPOINT 3: ANALIZAR CON IA (PRINCIPAL)
# ===============================================================================

@router.get("/analyze", response_model=dict)
async def analyze_lyrics_endpoint(
    title: str,
    artist: str,
    user_level: str = "B1",
    current_user: User = Depends(get_current_user)
):
    """
    🤖 Analiza letra y resalta palabras por NIVEL con IA (OpenAI real)
    
    FLUJO:
    1. Obtiene letra de Genius
    2. OpenAI resalta palabras según nivel del usuario
    3. Asigna colores: verde (fácil), naranja (medio), rojo (difícil)
    4. Proporciona traducciones y explicaciones
    
    Args:
        title: Título de la canción
        artist: Artista
        user_level: Nivel del usuario (A1-C2)
        current_user: Usuario autenticado
    
    Returns:
        Letra con palabras resaltadas y colores
    
    Example:
        GET /analyze?title=Blinding%20Lights&artist=The%20Weeknd&user_level=B1
    """
    try:
        logger.info(f"🤖 Analizando: {title} por {artist} (nivel {user_level})")
        
        # 1. Obtener letra
        lyrics_data = await get_song_lyrics(title, artist)
        
        if not lyrics_data or not lyrics_data.get("lyrics"):
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Lyrics not found for {title}"
            )
        
        raw_lyrics = lyrics_data.get("lyrics")
        
        # 2. Usar OpenAI para resaltar
        logger.info(f"🔄 Procesando con IA...")
        
        analysis_result = highlight_by_level(
            lyrics=raw_lyrics,
            user_level=user_level,
            language="en"
        )
        
        logger.info(f"✅ Análisis completado")
        
        return {
            "title": lyrics_data.get("title"),
            "artist": lyrics_data.get("artist"),
            "user_level": user_level,
            "language": "en",
            "image_url": lyrics_data.get("image_url"),
            "highlighted_words": analysis_result.get("highlighted_words", []),
            "words_by_level": analysis_result.get("words_by_level", {}),
            "total_highlighted": len(analysis_result.get("highlighted_words", [])),
            "suggestions": analysis_result.get("suggestions", []),
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error analizando: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error analyzing lyrics: {str(e)}"
        )

# ===============================================================================
# ENDPOINT 4: FRAGMENTOS PARA KARAOKE/SINCRONIZACIÓN
# ===============================================================================

@router.get("/with-fragments", response_model=dict)
async def get_lyrics_with_fragments(
    title: str,
    artist: str,
    current_user: User = Depends(get_current_user)
):
    """
    🎤 Obtiene letra fragmentada por líneas (para karaoke/sincronización)
    
    FLUJO:
    1. Obtiene letra completa
    2. Divide en líneas
    3. Genera timestamps simulados (3s por línea)
    4. Listo para sincronización con audio
    
    ¿POR QUÉ?
    └─ Para reproducir línea por línea
    └─ Para sincronizar con audio de Spotify
    └─ Para karaoke/highlighting mientras se reproduce
    
    Args:
        title: Título
        artist: Artista
        current_user: Usuario autenticado
    
    Returns:
        Letra con fragmentos y tiempos
    
    NOTE:
        Los tiempos son simulados (3s por línea).
        Con Spotify API real, se usarían tiempos exactos del audio.
    
    Example:
        GET /with-fragments?title=Blinding%20Lights&artist=The%20Weeknd
    """
    try:
        logger.info(f"🎤 Obteniendo fragmentos: {title}")
        
        lyrics_data = await get_song_lyrics(title, artist)
        
        if not lyrics_data or not lyrics_data.get("lyrics"):
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Lyrics not found for {title}"
            )
        
        raw_lyrics = lyrics_data.get("lyrics")
        lines = [l.strip() for l in raw_lyrics.split('\n') if l.strip()]
        
        # Generar fragmentos con timestamps simulados
        fragments = []
        current_time_ms = 0
        duration_per_line_ms = 3000  # 3 segundos por línea (simulado)
        
        for idx, line in enumerate(lines):
            fragments.append({
                "id": idx,
                "text": line,
                "start_ms": current_time_ms,
                "end_ms": current_time_ms + duration_per_line_ms,
                "duration_ms": duration_per_line_ms,
                "line_number": idx + 1
            })
            current_time_ms += duration_per_line_ms
        
        logger.info(f"✅ Se crearon {len(fragments)} fragmentos")
        
        return {
            "title": lyrics_data.get("title"),
            "artist": lyrics_data.get("artist"),
            "language": "en",
            "image_url": lyrics_data.get("image_url"),
            "preview_url": lyrics_data.get("preview_url"),  # De Spotify
            "duration_ms": current_time_ms,
            "total_fragments": len(fragments),
            "fragments": fragments,
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error obteniendo fragmentos: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error fetching fragments"
        )