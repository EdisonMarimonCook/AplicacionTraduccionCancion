"""
ROUTER: Letras de Canciones
PROPÓSITO: Endpoints para obtener y analizar letras de canciones
ENDPOINTS:
  - GET /api/v1/lyrics/{song_id}           → Obtener letra completa
  - GET /api/v1/lyrics/{song_id}/verses    → Obtener versículos
  - GET /api/v1/lyrics/{song_id}/lines     → Obtener líneas individuales
  - POST /api/v1/lyrics/analyze            → Analizar y resaltar por nivel

USA:
  - utils/genius_client.py → Extraer letras de Genius
  - OpenAI → Análisis IA de palabras por nivel
"""

from fastapi import APIRouter, HTTPException, Depends, status
from typing import Optional
import logging

from models import User
from routers.auth import get_current_user
from utils.genius_client import (
    get_song_lyrics,
    get_song_verses,
    get_line_lyrics,
    is_genius_configured
)
from cache import get_cached_songs
from services.openai_client import analyze_lyrics

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/lyrics", tags=["Lyrics"])

# ===============================================================================
# FUNCIONES AUXILIARES
# ===============================================================================

def get_song_from_cache(song_id: str) -> Optional[dict]:
    """
    🔍 BUSCA UNA CANCIÓN EN EL CACHE DE SPOTIFY
    
    LÓGICA:
    1. Obtiene todas las canciones del cache
    2. Busca por ID
    3. Retorna la canción o None
    
    ¿POR QUÉ?
    └─ Necesitamos el título y artista EXACTOS para buscar en Genius
    └─ El cache tiene esa información
    
    PARÁMETRO:
    - song_id: ID de la canción (ej: "spotify-123")
    
    RETORNA:
    - dict con información de la canción o None
    """
    
    cached_songs = get_cached_songs()
    
    for song in cached_songs:
        if song.get("id") == song_id or song.get("spotify_id") == song_id:
            return song
    
    return None

# ===============================================================================
# ENDPOINTS - RUTAS ESPECÍFICAS PRIMERO
# ===============================================================================

@router.get("/", response_model=dict)
async def get_lyrics(
    title: str,
    artist: str,
    current_user: User = Depends(get_current_user)
):
    """Obtiene letra por título + artista"""
    
    try:
        logger.info(f"📥 Buscando letra: {title} por {artist}")
        
        lyrics_data = await get_song_lyrics(title, artist)
        
        if not lyrics_data or not isinstance(lyrics_data, dict):
            logger.warning(f"⚠️  Letra no encontrada: {title} - {artist}")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Lyrics not found for {title} by {artist}"
            )
        
        logger.info(f"✅ Letra encontrada: {lyrics_data.get('line_count', 0)} líneas")
        
        return {
            "title": lyrics_data.get("title"),
            "artist": lyrics_data.get("artist"),
            "lyrics": lyrics_data.get("lyrics"),
            "line_count": lyrics_data.get("line_count"),
            "url": lyrics_data.get("url"),
            "language": lyrics_data.get("language", "en"), 
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

@router.get("/search", response_model=dict)
async def search_lyrics_suggestions(
    q: str,
    limit: int = 5,
    current_user: User = Depends(get_current_user)
):
    """Busca sugerencias de canciones"""
    
    try:
        logger.info(f"🔍 Buscando sugerencias en Genius: {q}")
        
        from utils.genius_client import search_genius_songs
        
        # Buscar en Genius
        results = await search_genius_songs(q, limit)
        
        if not results:
            logger.warning(f"⚠️  Sin resultados para: {q}")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"No results found for '{q}'"
            )
        
        logger.info(f"✅ Se encontraron {len(results)} sugerencias")
        
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
            detail="Error searching suggestions"
        )

# ===============================================================================

@router.get("/{song_id}/verses", response_model=dict)
async def get_song_verses_endpoint(
    song_id: str,
    current_user: User = Depends(get_current_user)
):
    """
    📖 Obtiene los VERSÍCULOS (secciones) de una canción
    
    FLUJO:
    1. Usuario solicita versículos de una canción
    2. Buscamos canción en cache
    3. Extrae letra y divide en secciones (Verso 1, Coro, Verso 2, etc)
    4. Retorna cada sección por separado
    
    ¿POR QUÉ?
    └─ Así el usuario puede estudiar un versículo a la vez
    └─ Y reproducir solo ese fragmento de la canción
    
    PARÁMETROS:
    - song_id: ID de la canción
    - current_user: Usuario autenticado
    
    RETORNA:
    - dict con:
        * "song_id": ID de la canción
        * "title": Título
        * "artist": Artista
        * "verses": Lista de versículos con:
            - "type": Tipo (Verse, Chorus, Bridge, etc)
            - "number": Número de sección
            - "text": Texto completo
            - "lines": Lista de líneas
        * "total_verses": Total de secciones
    
    EJEMPLO de request:
    GET /api/v1/lyrics/spotify-123/verses
    """
    
    try:
        if not is_genius_configured():
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Lyrics service not available"
            )
        
        logger.info(f"📖 Buscando versículos de: {song_id}")
        
        # Obtener todas las canciones y buscar por ID
        all_songs = get_cached_songs()
        song_data = None
        
        for song in all_songs:
            if song.get("id") == song_id or song.get("spotify_id") == song_id:
                song_data = song
                break
        
        if not song_data:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Song {song_id} not found"
            )
        
        song_title = song_data.get("name")
        artist_name = song_data.get("artist")
        
        verses = await get_song_verses(song_title, artist_name)
        
        if not verses:
            logger.warning(f"⚠️  No se pudieron extraer versículos")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Could not extract verses"
            )
        
        logger.info(f"✅ Se extrajeron {len(verses)} versículos")
        
        return {
            "song_id": song_id,
            "title": song_data.get("name"),
            "artist": song_data.get("artist"),
            "verses": verses,
            "total_verses": len(verses),
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error obteniendo versículos: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error fetching verses"
        )

# ===============================================================================

@router.get("/{song_id}/lines", response_model=dict)
async def get_song_lines_endpoint(
    song_id: str,
    current_user: User = Depends(get_current_user)
):
    """
    📝 Obtiene las LÍNEAS individuales de una canción
    
    FLUJO:
    1. Usuario solicita líneas de una canción
    2. Extrae letra y divide en líneas individuales
    3. Retorna cada línea numerada
    
    ¿POR QUÉ?
    └─ Para análisis línea por línea
    └─ Para resaltar palabras de una línea específica
    └─ Para reproducir el audio de esa línea exacta
    
    PARÁMETROS:
    - song_id: ID de la canción
    - current_user: Usuario autenticado
    
    RETORNA:
    - dict con:
        * "song_id": ID
        * "title": Título
        * "artist": Artista
        * "lines": Lista de líneas con índice
        * "total_lines": Total de líneas
        * "preview_url": URL de preview
    
    EJEMPLO de request:
    GET /api/v1/lyrics/spotify-123/lines
    
    EJEMPLO de respuesta:
    {
      "song_id": "spotify-123",
      "title": "Blinding Lights",
      "artist": "The Weeknd",
      "lines": [
        {"index": 0, "text": "I can't sleep until I feel your touch"},
        {"index": 1, "text": "And realize you're out of reach"},
        ...
      },
      "total_lines": 45
    }
    """
    
    try:
        if not is_genius_configured():
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Lyrics service not available"
            )
        
        logger.info(f"📝 Obteniendo líneas de: {song_id}")
        
        # Obtener todas las canciones
        all_songs = get_cached_songs()
        song_data = None
        
        for song in all_songs:
            if song.get("id") == song_id or song.get("spotify_id") == song_id:
                song_data = song
                break
        
        if not song_data:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Song {song_id} not found"
            )
        
        song_title = song_data.get("name")
        artist_name = song_data.get("artist")
        
        lines = await get_line_lyrics(song_title, artist_name)
        
        if not lines:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Could not extract lines"
            )
        
        formatted_lines = [
            {"index": i, "text": line}
            for i, line in enumerate(lines)
        ]
        
        logger.info(f"✅ Se obtuvieron {len(lines)} líneas")
        
        return {
            "song_id": song_id,
            "title": song_data.get("name"),
            "artist": song_data.get("artist"),
            "lines": formatted_lines,
            "total_lines": len(lines),
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error obteniendo líneas: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error fetching lines"
        )

# ===============================================================================

@router.post("/{song_id}/analyze", response_model=dict)
async def analyze_song_lyrics_endpoint(
    song_id: str,
    current_user: User = Depends(get_current_user)
):
    """
    🤖 Analiza letra según nivel de usuario con IA
    
    FLUJO:
    1. Obtiene canción del cache
    2. Extrae letra completa de Genius
    3. OpenAI analiza según nivel del usuario principal
    4. Retorna explicaciones, vocabulario, ejemplos
    
    ¿POR QUÉ?
    └─ Proporciona aprendizaje personalizado por nivel
    └─ Explica vocabulario útil para ese nivel específico
    └─ Genera ejemplos contextualizados
    
    PARÁMETROS:
    - song_id: ID de la canción
    - current_user: Usuario autenticado (trae su nivel de idioma)
    
    RETORNA:
    - dict con:
        * "song_id": ID de la canción
        * "title": Título
        * "artist": Artista
        * "user_level": Nivel del usuario
        * "analysis": Análisis de OpenAI
        * "status": "success"
    
    EJEMPLO de request:
    POST /api/v1/lyrics/spotify-123/analyze
    
    EJEMPLO de respuesta:
    {
      "song_id": "spotify-123",
      "title": "Blinding Lights",
      "artist": "The Weeknd",
      "user_level": "B1",
      "analysis": "Para tu nivel B1, palabras clave: 'touch' (tocar), 'reach' (alcanzar)...",
      "status": "success"
    }
    """
    
    try:
        if not is_genius_configured():
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Lyrics service not available"
            )
        
        logger.info(f"🤖 Analizando canción: {song_id} para usuario: {current_user.email}")
        
        # ===== 1. Obtener canción del cache =====
        song_data = get_song_from_cache(song_id)
        
        if not song_data:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Song {song_id} not found"
            )
        
        song_title = song_data.get("name")
        artist_name = song_data.get("artist")
        
        # ===== 2. Obtener letra completa =====
        lyrics_data = await get_song_lyrics(song_title, artist_name)
        
        if not lyrics_data or not lyrics_data.get("lyrics"):
            logger.warning(f"⚠️  No se pudo obtener letra para: {song_title}")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Could not fetch lyrics for {song_title}"
            )
        
        full_lyrics = lyrics_data.get("lyrics")
        
        # ===== 3. Obtener nivel del usuario =====
        # Asumiendo que current_user.learning_languages es una lista con dicts que tienen "level"
        user_level = "A1"  # Por defecto
        
        if hasattr(current_user, "learning_languages") and current_user.learning_languages:
            # El primer idioma es el principal
            user_level = current_user.learning_languages[0].get("level", "A1")
        
        logger.info(f"👤 Nivel del usuario: {user_level}")
        
        # ===== 4. Analizar con OpenAI =====
        logger.info(f"🔄 Enviando a OpenAI para análisis...")
        
        analysis = await analyze_lyrics(full_lyrics, user_level)
        
        logger.info(f"✅ Análisis completado")
        
        return {
            "song_id": song_id,
            "title": song_data.get("name"),
            "artist": song_data.get("artist"),
            "user_level": user_level,
            "analysis": analysis,
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error analizando letra: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error analyzing lyrics: {str(e)}"
        )