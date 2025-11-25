"""
Cliente de Spotify para enriquecer datos de canciones
Obtiene: preview_url, image_url, duration, popularity
"""

import logging
from typing import Optional, Dict, List
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials

# ✅ CAMBIAR: Usar settings en lugar de os.getenv()
from config import settings

logger = logging.getLogger(__name__)

# ===============================================================================
# SPOTIFY CLIENT
# ===============================================================================

def get_spotify_client() -> spotipy.Spotify:
    """
    Obtiene cliente autenticado de Spotify
    Requiere SPOTIFY_CLIENT_ID y SPOTIFY_CLIENT_SECRET en .env
    """
    try:
        # ✅ CAMBIAR: Usar settings en lugar de os.getenv()
        client_id = settings.SPOTIFY_CLIENT_ID
        client_secret = settings.SPOTIFY_CLIENT_SECRET
        
        if not client_id or not client_secret:
            logger.error("❌ Falta SPOTIFY_CLIENT_ID o SPOTIFY_CLIENT_SECRET en settings")
            return None
        
        logger.info(f"🔐 Conectando a Spotify con Client ID: {client_id[:10]}...")
        
        credentials_manager = SpotifyClientCredentials(
            client_id=client_id,
            client_secret=client_secret
        )
        
        sp = spotipy.Spotify(client_credentials_manager=credentials_manager)
        logger.info("✅ Cliente Spotify autenticado correctamente")
        return sp
    
    except Exception as e:
        logger.error(f"❌ Error autenticando Spotify: {str(e)}")
        return None

# ===============================================================================
# ENRIQUECER CANCIÓN INDIVIDUAL
# ===============================================================================

def enrich_song_with_spotify(song_id: str) -> Optional[Dict]:
    """
    Enriquece datos de una canción individual
    
    Args:
        song_id: ID de Spotify de la canción
    
    Returns:
        Diccionario con datos enriquecidos o None
    """
    try:
        if not song_id:
            logger.warning(f"⚠️  song_id vacío")
            return None
        
        sp = get_spotify_client()
        if not sp:
            logger.warning("⚠️  No se pudo obtener cliente Spotify")
            return None
        
        logger.info(f"🔄 Enriqueciendo canción: {song_id}")
        
        track = sp.track(song_id)
        
        if not track:
            logger.warning(f"⚠️  Track no encontrado o null: {song_id}")
            return None
        
        image_url = None
        if track.get("album") and track["album"].get("images"):
            image_url = track["album"]["images"][0].get("url")
        
        enriched = {
            "id": track.get("id"),
            "name": track.get("name"),
            "artist": track.get("artists", [{}])[0].get("name", "Unknown"),
            "preview_url": track.get("preview_url"),
            "image_url": image_url,
            "duration_ms": track.get("duration_ms"),
            "popularity": track.get("popularity"),
            "external_url": track.get("external_urls", {}).get("spotify"),
        }
        
        logger.info(f"✅ Canción enriquecida: {enriched.get('name')} - Preview: {enriched.get('preview_url') is not None}")
        return enriched
    
    except spotipy.exceptions.SpotifyException as e:
        logger.warning(f"⚠️  Error Spotify para {song_id}: {str(e)} (Code: {e.http_status})")
        return None
    except Exception as e:
        logger.error(f"❌ Error enriqueciendo canción {song_id}: {str(e)}")
        return None

# ===============================================================================
# ENRIQUECER MÚLTIPLES CANCIONES
# ===============================================================================

def enrich_songs_batch(songs: List[Dict]) -> List[Dict]:
    """
    Enriquece múltiples canciones de una vez
    
    Args:
        songs: Lista de canciones con al menos "id" y "name"
    
    Returns:
        Lista de canciones enriquecidas (mantiene datos originales si falla)
    """
    try:
        sp = get_spotify_client()
        if not sp:
            logger.warning("⚠️  No se pudo conectar a Spotify, devolviendo datos originales")
            return songs
        
        logger.info(f"🔄 Enriqueciendo batch de {len(songs)} canciones")
        
        enriched_songs = []
        success_count = 0
        fail_count = 0
        
        for idx, song in enumerate(songs):
            try:
                song_id = song.get("id")
                song_name = song.get("name", "Unknown")
                
                if not song_id:
                    logger.warning(f"⚠️  [{idx+1}] Canción sin ID: {song_name}")
                    enriched_songs.append(song)
                    fail_count += 1
                    continue
                
                logger.debug(f"[{idx+1}/{len(songs)}] Enriqueciendo: {song_name} ({song_id})")
                
                track = sp.track(song_id)
                
                if not track:
                    logger.warning(f"⚠️  [{idx+1}] Track no encontrado: {song_id}")
                    enriched_songs.append(song)
                    fail_count += 1
                    continue
                
                image_url = None
                if track.get("album") and track["album"].get("images"):
                    image_url = track["album"]["images"][0].get("url")
                
                enriched = {
                    **song,
                    "preview_url": track.get("preview_url"),
                    "image_url": image_url or song.get("image_url"),
                    "duration_ms": track.get("duration_ms"),
                    "popularity": track.get("popularity"),
                    "spotify_url": track.get("external_urls", {}).get("spotify"),
                    "has_preview": track.get("preview_url") is not None,  # True/False
                }
                
                enriched_songs.append(enriched)
                success_count += 1
                
                logger.debug(f"✅ [{idx+1}] Enriquecida: {song_name} - Preview: {track.get('preview_url') is not None}")
            
            except spotipy.exceptions.SpotifyException as e:
                logger.warning(f"⚠️  [{idx+1}] Error Spotify para {song.get('name')}: {str(e)}")
                enriched_songs.append(song)
                fail_count += 1
            
            except Exception as e:
                logger.warning(f"⚠️  [{idx+1}] Error enriqueciendo {song.get('name')}: {str(e)}")
                enriched_songs.append(song)
                fail_count += 1
        
        logger.info(f"✅ Batch completo: {success_count} exitosas, {fail_count} fallos")
        return enriched_songs
    
    except Exception as e:
        logger.error(f"❌ Error en batch: {str(e)}")
        return songs

# ===============================================================================
# BUSCAR EN SPOTIFY
# ===============================================================================

def search_on_spotify(
    query: str,
    search_type: str = "track",
    limit: int = 10
) -> Optional[List[Dict]]:
    """
    🎵 Busca en Spotify
    
    PARÁMETROS:
    - query: Lo que buscas (ej: "The Weeknd")
    - search_type: Tipo de búsqueda (track, artist, playlist)
    - limit: Máximo de resultados
    """
    try:
        sp = get_spotify_client()
        if not sp:
            logger.error("❌ No se pudo obtener cliente de Spotify")
            return None
        
        logger.info(f"🔍 Buscando en Spotify: {query} (type: {search_type}, limit: {limit})")
        
        results = sp.search(q=query, type=search_type, limit=limit)
        
        if search_type == "track":
            tracks = results.get("tracks", {}).get("items", [])
            
            if not tracks:
                logger.warning(f"⚠️  No se encontraron resultados para: {query}")
                return []
            
            formatted = []
            
            for track in tracks:
                # Obtener imagen del álbum
                image_url = None
                if track.get("album") and track["album"].get("images"):
                    image_url = track["album"]["images"][0].get("url")
                
                # ✅ NUEVO: Obtener URL de Spotify
                spotify_url = track.get("external_urls", {}).get("spotify", "")
                
                # ✅ NUEVO: Obtener duración
                duration_ms = track.get("duration_ms", 0)
                
                # ✅ NUEVO: Indicador de preview
                preview_url = track.get("preview_url")
                has_preview = preview_url is not None
                
                formatted.append({
                    "id": track.get("id"),
                    "name": track.get("name"),
                    "artist": track.get("artists", [{}])[0].get("name", "Unknown"),
                    "preview_url": preview_url,
                    "image_url": image_url,
                    "popularity": track.get("popularity", 0),
                    "spotify_url": spotify_url,              # ✅ NUEVO
                    "duration_ms": duration_ms,             # ✅ NUEVO
                    "has_preview": has_preview              # ✅ NUEVO
                })
            
            logger.info(f"✅ Encontrados {len(formatted)} resultados en Spotify")
            return formatted
        
        logger.warning(f"⚠️  Tipo de búsqueda no soportado: {search_type}")
        return None
    
    except Exception as e:
        logger.error(f"❌ Error buscando en Spotify: {str(e)}")
        return None

# ===============================================================================
# DEBUG - TEST DE CONEXIÓN
# ===============================================================================

def test_spotify_connection() -> bool:
    """
    Prueba que la conexión a Spotify funciona
    """
    try:
        sp = get_spotify_client()
        if not sp:
            logger.error("❌ No se pudo autenticar con Spotify")
            return False
        
        # Probar con una canción conocida
        track = sp.track("11dFghVXANMlKmJXsNCQvb")  # "Blinding Lights" - The Weeknd
        
        if track and track.get("name"):
            logger.info(f"✅ Conexión a Spotify OK - Track: {track.get('name')}")
            return True
        else:
            logger.warning("⚠️  Conexión a Spotify fallida - No se obtuvo track")
            return False
    
    except Exception as e:
        logger.error(f"❌ Error probando conexión a Spotify: {str(e)}")
        return False

# ===============================================================================
# BUSCAR UNA CANCIÓN EN SPOTIFY
# ===============================================================================

async def search_song_spotify(title: str, artist: str) -> Optional[Dict]:
    """
    🎵 Busca UNA canción en Spotify y retorna su metadata + imagen
    
    PARÁMETROS:
    - title: Título de la canción
    - artist: Artista
    
    RETORNA:
    {
        "id": "...",
        "name": "...",
        "artist": "...",
        "image_url": "...",  ← ESTO ES LO QUE NECESITAS
        "preview_url": "...",
        "popularity": 85,
        "spotify_url": "..."
    }
    """
    try:
        if not title:
            logger.warning("⚠️  Title vacío")
            return None
        
        # Construir query
        query = f"{title}"
        if artist:
            query = f"{title} {artist}"
        
        logger.info(f"🔍 Buscando en Spotify: {query}")
        
        # Buscar en Spotify
        results = search_on_spotify(query, search_type="track", limit=1)
        
        if not results or len(results) == 0:
            logger.warning(f"⚠️  No encontrado en Spotify: {query}")
            return None
        
        # Retornar el primer resultado (mejor match)
        song = results[0]
        logger.info(f"✅ Encontrado en Spotify: {song.get('name')} - Image: {song.get('image_url') is not None}")
        
        return song
    
    except Exception as e:
        logger.error(f"❌ Error buscando en Spotify: {str(e)}")
        return None