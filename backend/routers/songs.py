"""
ROUTER: Canciones
PROPÓSITO: Endpoints para listar y obtener información de canciones
INTEGRACIONES: Spotify API (para enriquecer datos)

⚠️  IMPORTANTE: Las rutas están ordenadas por especificidad
    1. Sin parámetros (/search/genius, /top10, /)
    2. Con parámetros específicos (/top10/{language}, /{song_id}/preview)
    3. Con parámetros genéricos (/{song_id})
"""

import logging
from typing import List, Optional
from fastapi import APIRouter, HTTPException, Depends, status

from models import User
from routers.auth import get_current_user
from cache import top10_cache  # ✅ AGREGAR ESTA IMPORTACIÓN
from utils.spotify import enrich_songs_batch

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/songs", tags=["Songs"])

# ===============================================================================
# 1️⃣ RUTAS SIN PARÁMETROS (ESPECÍFICAS - VAN PRIMERO)
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

@router.get("/search/spotify", response_model=dict)
async def search_spotify_songs(
    query: str,
    limit: int = 10,
    current_user: User = Depends(get_current_user)
):
    """
    🎵 BUSCA CANCIONES EN SPOTIFY
    
    PARÁMETROS:
    - query: Búsqueda (ej: "The Weeknd Blinding Lights")
    - limit: Máximo de resultados (default: 10, máximo: 50)
    
    RETORNA:
    Lista de canciones con ID de Spotify + metadata
    
    EJEMPLO:
    GET /api/v1/songs/search/spotify?query=The%20Weeknd&limit=5
    """
    
    try:
        logger.info(f"🔍 Buscando en Spotify: {query} (limit: {limit})")
        
        # Validar query
        if not query or len(query.strip()) < 2:
            logger.warning("⚠️  Query muy corta")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Query must be at least 2 characters"
            )
        
        # Validar limit
        if limit < 1 or limit > 50:
            logger.warning(f"⚠️  Limit inválido: {limit}, usando 10")
            limit = 10
        
        from utils.spotify import search_on_spotify
        
        # Buscar en Spotify
        results = search_on_spotify(query, search_type="track", limit=limit)
        
        if not results or len(results) == 0:
            logger.warning(f"⚠️  No encontrado en Spotify: {query}")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"No songs found for '{query}' on Spotify"
            )
        
        logger.info(f"✅ Encontradas {len(results)} canciones en Spotify")
        
        # Enriquecer resultados y agregar rank
        enriched_results = []
        for idx, song in enumerate(results, 1):
            enriched_results.append({
                "rank": idx,
                "id": song.get("id"),
                "name": song.get("name"),
                "artist": song.get("artist"),
                "preview_url": song.get("preview_url"),
                "image_url": song.get("image_url"),
                "popularity": song.get("popularity"),
                "duration_ms": song.get("duration_ms"),
                "spotify_url": song.get("spotify_url"),
                "has_preview": song.get("has_preview", False),
            })
        
        return {
            "query": query,
            "total_results": len(enriched_results),
            "results": enriched_results,
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error buscando en Spotify: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error fetching songs from Spotify"
        )

# ===============================================================================

@router.get("/top10", response_model=dict)
async def get_top10_all_languages(
    current_user: User = Depends(get_current_user)
):
    """
    🏆 TOP 10 de TODAS las canciones populares POR IDIOMA
    
    RETORNA:
    {
        "total_languages": 7,
        "data": {
            "en": [10 canciones],
            "es": [10 canciones],
            "fr": [10 canciones],
            ...
        }
    }
    """
    try:
        logger.info(f"🏆 Obteniendo Top 10 para todos los idiomas")
        
        if not top10_cache:
            logger.warning("⚠️  Cache de Top 10 vacío")
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Top 10 cache not available yet"
            )
        
        simplified_cache = {}
        for lang, songs in top10_cache.items():
            logger.info(f"🔄 Enriqueciendo Top 10 para {lang}...")
            enriched_songs = enrich_songs_batch(songs)
            
            # ✅ ORDENAR POR POPULARIDAD
            enriched_songs = sorted(
                enriched_songs,
                key=lambda x: x.get("popularity", 0),
                reverse=True
            )
            
            simplified_cache[lang] = []
            for idx, song in enumerate(enriched_songs, 1):
                try:
                    simplified = {
                        "rank": idx,
                        "id": song.get("id") or song.get("uri", "").split(":")[-1],
                        "name": song.get("name", "Unknown"),
                        "artist": song.get("artist", "Unknown"),
                        "preview_url": song.get("preview_url"),  # ✅ ARREGLADO
                        "image_url": song.get("image_url"),
                        "duration_ms": song.get("duration_ms"),
                        "popularity": song.get("popularity", 0),
                        "spotify_url": song.get("spotify_url"),
                        "has_preview": song.get("has_preview", False),
                        "language": lang
                    }
                    simplified_cache[lang].append(simplified)
                except Exception as e:
                    logger.warning(f"⚠️  Error procesando canción: {e}")
                    continue
        
        logger.info(f"✅ Top 10 completo: {len(simplified_cache)} idiomas")
        
        return {
            "total_languages": len(simplified_cache),
            "data": simplified_cache,
            "sorted_by": "popularity_desc",
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error fetching Top 10"
        )

# ===============================================================================
# 2️⃣ RUTAS CON PARÁMETROS ESPECÍFICOS (VAN SEGUNDO)
# ===============================================================================

@router.get("/top10/{language}", response_model=dict)
async def get_top10_by_language(
    language: str,
    current_user: User = Depends(get_current_user)
):
    """
    🏆 TOP 10 de canciones en un idioma ESPECÍFICO
    (Ordenadas por popularidad: Mayor → Menor)
    """
    try:
        logger.info(f"🏆 Obteniendo Top 10 para: {language}")
        
        if language not in top10_cache:
            logger.warning(f"⚠️  Idioma no disponible: {language}")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Language '{language}' not found"
            )
        
        songs = top10_cache[language]
        
        if not songs:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail=f"No songs available for language '{language}'"
            )
        
        logger.info(f"🔄 Enriqueciendo {len(songs)} canciones con datos de Spotify...")
        enriched_songs = enrich_songs_batch(songs)
        
        # ✅ ORDENAR POR POPULARIDAD (Mayor → Menor)
        enriched_songs = sorted(
            enriched_songs,
            key=lambda x: x.get("popularity", 0),
            reverse=True
        )
        
        simplified_songs = []
        for idx, song in enumerate(enriched_songs, 1):
            try:
                simplified = {
                    "rank": idx,
                    "id": song.get("id") or song.get("uri", "").split(":")[-1],
                    "name": song.get("name", "Unknown"),
                    "artist": song.get("artist", "Unknown"),
                    "preview_url": song.get("preview_url"),
                    "image_url": song.get("image_url"),
                    "duration_ms": song.get("duration_ms"),
                    "popularity": song.get("popularity", 0),
                    "spotify_url": song.get("spotify_url"),
                    "has_preview": song.get("has_preview", False),
                    "language": language
                }
                simplified_songs.append(simplified)
            except Exception as e:
                logger.warning(f"⚠️  Error procesando canción: {e}")
                continue
        
        logger.info(f"✅ Top 10 retornado: {len(simplified_songs)} canciones")
        
        return {
            "language": language,
            "total": len(simplified_songs),
            "songs": simplified_songs,
            "sorted_by": "popularity_desc",
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

@router.get("/trending/{language}", response_model=dict)
async def get_trending_songs(
    language: str,
    limit: int = 10,
    current_user: User = Depends(get_current_user)
):
    """
    🔥 CANCIONES EN TENDENCIA (DINÁMICO)
    
    Obtiene canciones trending EN ESTE MOMENTO para un idioma.
    Busca dinámicamente en Spotify (no está en cache).
    
    PARÁMETROS:
    - language: Código idioma (en, es, fr, de, it, pt, ja, ar, zh, ru)
    - limit: Máximo de resultados (default: 10, máximo: 50)
    
    EJEMPLO:
    GET /api/v1/songs/trending/es?limit=5
    """
    
    try:
        logger.info(f"🔥 Obteniendo tendencias para: {language} (limit: {limit})")
        
        # Mapeo de búsquedas por idioma
        trending_queries = {
            "en": "trending English music popular hits",
            "es": "canciones españolas populares tendencia 2024",
            "fr": "musique française tendance populaire",
            "de": "deutsche Musik trending beliebt",
            "it": "musica italiana tendenza popolare",
            "pt": "música portuguesa tendência popular",
            "ja": "流行日本語曲人気",
            "ar": "أغاني عربية شهيرة",
            "zh": "流行中文歌曲",
            "ru": "популярная русская музыка"
        }
        
        # Validar idioma
        if language not in trending_queries:
            logger.warning(f"⚠️  Idioma no soportado: {language}")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Language '{language}' not supported. Supported: {list(trending_queries.keys())}"
            )
        
        # Validar limit
        if limit < 1 or limit > 50:
            logger.warning(f"⚠️  Limit inválido: {limit}, usando 10")
            limit = 10
        
        query = trending_queries[language]
        
        from utils.spotify import search_on_spotify
        
        logger.info(f"🔍 Buscando trending con query: {query}")
        
        # Buscar dinámicamente en Spotify
        results = search_on_spotify(query, search_type="track", limit=limit)
        
        if not results or len(results) == 0:
            logger.warning(f"⚠️  No se encontraron trending para {language}, usando fallback")
            # Usar top10 como fallback
            results = top10_cache.get(language, [])
            
            if not results:
                raise HTTPException(
                    status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                    detail=f"Could not fetch trending for {language}"
                )
        
        # Ordenar por popularidad (descendente)
        enriched = sorted(
            results,
            key=lambda x: x.get("popularity", 0),
            reverse=True
        )
        
        # Agregar rank y simplificar
        simplified = []
        for idx, song in enumerate(enriched, 1):
            simplified.append({
                "rank": idx,
                "id": song.get("id"),
                "name": song.get("name"),
                "artist": song.get("artist"),
                "preview_url": song.get("preview_url"),
                "image_url": song.get("image_url"),
                "duration_ms": song.get("duration_ms"),
                "popularity": song.get("popularity"),
                "spotify_url": song.get("spotify_url"),
                "has_preview": song.get("has_preview", False),
                "language": language
            })
        
        logger.info(f"✅ Tendencias obtenidas: {len(simplified)} canciones para {language}")
        
        return {
            "language": language,
            "type": "trending",
            "total": len(simplified),
            "songs": simplified,
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error obteniendo tendencias: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error fetching trending songs for {language}"
        )

# ===============================================================================

@router.get("/{song_id}/preview", response_model=dict)
async def get_song_preview(
    song_id: str,
    current_user: User = Depends(get_current_user)
):
    """
    🎧 Obtiene URL del PREVIEW (primeros 30 segundos) de una canción
    
    BUSCA EN:
    1. Cache (si existe)
    2. Spotify directamente (si no está en cache)
    """
    
    try:
        logger.info(f"🎧 Obteniendo preview de: {song_id}")
        
        from cache import get_song_from_cache
        from utils.spotify import get_spotify_client
        
        # Paso 1: Intentar obtener del cache
        song = get_song_from_cache(song_id)
        
        # Paso 2: Si no está en cache, buscar en Spotify directamente
        if not song:
            logger.info(f"🔍 No en cache, buscando en Spotify: {song_id}")
            
            sp = get_spotify_client()
            if sp:
                try:
                    track = sp.track(song_id)
                    
                    # Enriquecer datos de Spotify
                    song = {
                        "id": track.get("id"),
                        "name": track.get("name"),
                        "artist": track.get("artists", [{}])[0].get("name", "Unknown"),
                        "preview_url": track.get("preview_url"),
                        "image_url": None,
                        "popularity": track.get("popularity", 0),
                        "duration_ms": track.get("duration_ms"),
                        "spotify_url": track.get("external_urls", {}).get("spotify"),
                        "has_preview": track.get("preview_url") is not None
                    }
                    
                    # Obtener imagen del álbum
                    if track.get("album") and track["album"].get("images"):
                        song["image_url"] = track["album"]["images"][0].get("url")
                    
                    logger.info(f"✅ Encontrado en Spotify: {song.get('name')}")
                
                except Exception as e:
                    logger.warning(f"⚠️  No encontrado en Spotify: {e}")
                    song = None
        
        # Paso 3: Verificar si tenemos la canción
        if not song:
            logger.warning(f"⚠️  Canción no encontrada: {song_id}")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Song '{song_id}' not found"
            )
        
        # Paso 4: Verificar si tiene preview
        if not song.get("preview_url"):
            logger.warning(f"⚠️  Preview no disponible para: {song_id}")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Preview not available for this song"
            )
        
        return {
            "song_id": song_id,
            "song_name": song.get("name"),
            "artist": song.get("artist"),
            "preview_url": song.get("preview_url"),
            "duration_ms": 30000,
            "message": "Preview URL (30 seconds from Spotify)",
            "status": "success"
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

@router.get("/{song_id}", response_model=dict)
async def get_song_details(
    song_id: str,
    current_user: User = Depends(get_current_user)
):
    """
    🎵 Obtiene detalles ESPECÍFICOS de una canción
    
    BUSCA EN:
    1. Cache (si existe)
    2. Spotify directamente (si no está en cache)
    """
    
    try:
        logger.info(f"📥 Buscando canción: {song_id}")
        
        from cache import get_song_from_cache
        from utils.spotify import get_spotify_client
        
        # Paso 1: Intentar obtener del cache
        song = get_song_from_cache(song_id)
        
        # Paso 2: Si no está en cache, buscar en Spotify directamente
        if not song:
            logger.info(f"🔍 No en cache, buscando en Spotify: {song_id}")
            
            sp = get_spotify_client()
            if sp:
                try:
                    track = sp.track(song_id)
                    
                    # Enriquecer datos de Spotify
                    song = {
                        "id": track.get("id"),
                        "name": track.get("name"),
                        "artist": track.get("artists", [{}])[0].get("name", "Unknown"),
                        "preview_url": track.get("preview_url"),
                        "image_url": None,
                        "popularity": track.get("popularity", 0),
                        "duration_ms": track.get("duration_ms"),
                        "spotify_url": track.get("external_urls", {}).get("spotify"),
                        "has_preview": track.get("preview_url") is not None,
                        "language": "en"  # Por defecto, podría deterse del idioma del usuario
                    }
                    
                    # Obtener imagen del álbum
                    if track.get("album") and track["album"].get("images"):
                        song["image_url"] = track["album"]["images"][0].get("url")
                    
                    logger.info(f"✅ Encontrado en Spotify: {song.get('name')}")
                
                except Exception as e:
                    logger.warning(f"⚠️  No encontrado en Spotify: {e}")
                    song = None
        
        # Paso 3: Verificar si tenemos la canción
        if not song:
            logger.warning(f"⚠️  Canción no encontrada: {song_id}")
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Song '{song_id}' not found"
            )
        
        return {
            "id": song.get("id") or song_id,
            "name": song.get("name", "Unknown"),
            "artist": song.get("artist", "Unknown"),
            "preview_url": song.get("preview_url"),
            "image_url": song.get("image_url"),
            "duration_ms": song.get("duration_ms"),
            "popularity": song.get("popularity"),
            "spotify_url": song.get("spotify_url"),
            "has_preview": song.get("has_preview", False),
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