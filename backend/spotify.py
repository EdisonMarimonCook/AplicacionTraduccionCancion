"""
MÓDULO SPOTIFY - Integración con la API de Spotify
Este módulo se encarga de obtener canciones populares de Spotify según el idioma.
"""

import base64
import requests
from typing import List, Dict
import logging
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials

# ✨ CAMBIO 1: Importar de config.py
from config import settings

# Configurar logger
logger = logging.getLogger(__name__)

# ✨ CAMBIO 2: Usar settings en lugar de os.getenv()
CLIENT_ID = settings.SPOTIFY_CLIENT_ID
CLIENT_SECRET = settings.SPOTIFY_CLIENT_SECRET

# ==============================================================================
# CONFIGURACIÓN: Mapeo de idiomas a códigos de mercado
# ==============================================================================
MARKET_MAP = {
    "en": "US",
    "es": "ES",
    "fr": "FR",
    "de": "DE",
    "it": "IT",
    "pt": "PT",
    "jp": "JP"
}

# ==============================================================================
# CONFIGURACIÓN: Consultas de búsqueda por idioma
# ==============================================================================
SEARCH_QUERIES = {
    "en": ["top 50 usa", "trending now", "viral hits"],
    "es": ["éxitos españa", "canciones populares español", "top latino"],
    "fr": ["hits france", "chansons populaires", "top france"],
    "de": ["deutsche hits", "top deutschland", "deutsche pop"],
    "it": ["canzoni italiane", "hit italia", "pop italiano"],
    "jp": ["アニメ オープニング", "日本のヒット曲", "トレンド", "バイラルヒット"]
}

# OBJETO de Spotify (autenticación)
sp = spotipy.Spotify(
    auth_manager=SpotifyClientCredentials(
        client_id=CLIENT_ID,
        client_secret=CLIENT_SECRET
    )
)

# ==============================================================================
# FUNCIONES
# ==============================================================================

def get_access_token() -> str:
    """
    Obtiene un token de acceso de Spotify usando Client ID y Secret.
    """
    if not CLIENT_ID or not CLIENT_SECRET:
        raise Exception("Faltan credenciales de Spotify en .env")

    url = "https://accounts.spotify.com/api/token"
    auth_header = base64.b64encode(f"{CLIENT_ID}:{CLIENT_SECRET}".encode()).decode()
    headers = {"Authorization": f"Basic {auth_header}"}
    data = {"grant_type": "client_credentials"}

    resp = requests.post(url, headers=headers, data=data)
    
    if resp.status_code != 200:
        raise Exception(f"Error obteniendo token: {resp.text}")

    return resp.json()["access_token"]

def get_top10_playlist(language: str = "en") -> List[Dict[str, str]]:
    """
    Obtiene las 10 canciones más populares de un idioma específico.
    """
    search_queries = SEARCH_QUERIES.get(language, SEARCH_QUERIES["en"])
    market = MARKET_MAP.get(language, "US")

    logger.info(f"Buscando canciones populares para idioma: {language}")

    try:
        token = get_access_token()
    except Exception as e:
        logger.error(f"No se pudo obtener token: {e}")
        return get_default_songs()

    for q in search_queries:
        try:
            logger.info(f"Intentando buscar: {q}")
            
            url = "https://api.spotify.com/v1/search"
            headers = {"Authorization": f"Bearer {token}"}
            params = {
                "q": q,
                "type": "track",
                "limit": 10,
                "market": market
            }

            resp = requests.get(url, headers=headers, params=params, timeout=5)
            resp.raise_for_status()
            
            data = resp.json()
            tracks = data.get("tracks", {}).get("items", [])

            if tracks and len(tracks) > 0:
                logger.info(f"Éxito obteniendo {len(tracks)} canciones para '{q}'")
                return process_tracks(tracks, from_search=True)
            else:
                logger.warning(f"No se encontraron canciones para '{q}'")
                continue

        except requests.exceptions.Timeout:
            logger.error(f"Timeout buscando '{q}'")
            continue
        except requests.exceptions.ConnectionError:
            logger.error(f"Error de conexión buscando '{q}'")
            continue
        except requests.HTTPError as e:
            if e.response.status_code == 401:
                logger.warning("Token caducado, intentando refrescar...")
                try:
                    token = get_access_token()
                except Exception as refresh_error:
                    logger.error(f"No se pudo refrescar token: {refresh_error}")
                    return get_default_songs()
                continue
            logger.error(f"HTTPError {e.response.status_code} buscando '{q}'")
        except Exception as e:
            logger.error(f"Error inesperado buscando '{q}': {str(e)}")

    logger.warning("No se pudieron obtener canciones, usando lista por defecto")
    return get_default_songs()

def process_tracks(tracks: List[Dict], from_search: bool = False) -> List[Dict[str, str]]:
    """
    Procesa la lista de tracks devueltos por Spotify.
    """
    top10 = []
    
    for idx, item in enumerate(tracks):
        try:
            track = item if from_search else item.get("track")
            
            if not track:
                logger.warning(f"Track {idx} es inválido o None")
                continue

            artists = track.get("artists", [])
            artist_name = artists[0]["name"] if artists and len(artists) > 0 else "Unknown"

            top10.append({
                "rank": idx + 1,
                "name": track.get("name", "Unknown"),
                "artist": artist_name,
                "preview_url": track.get("preview_url"),
                "spotify_url": track.get("external_urls", {}).get("spotify", "")
            })
            
        except Exception as e:
            logger.error(f"Error procesando track {idx}: {str(e)}")
            continue
    
    return top10

def get_default_songs() -> List[Dict[str, str]]:
    """
    Retorna canciones por defecto si falla la API de Spotify.
    """
    return [
        {
            "rank": 1,
            "name": "Blinding Lights",
            "artist": "The Weeknd",
            "preview_url": "",
            "spotify_url": ""
        },
        {
            "rank": 2,
            "name": "As It Was",
            "artist": "Harry Styles",
            "preview_url": "",
            "spotify_url": ""
        },
        {
            "rank": 3,
            "name": "Heat Waves",
            "artist": "Glass Animals",
            "preview_url": "",
            "spotify_url": ""
        }
    ]

def get_market_code(language: str) -> str:
    """
    Devuelve el código de market para un idioma.
    """
    return MARKET_MAP.get(language, "US")

def search_songs(query: str, language: str = "en", limit: int = 10) -> List[Dict[str, str]]:
    """
    Busca canciones en Spotify por título o artista.
    """
    try:
        token = get_access_token()
        url = "https://api.spotify.com/v1/search"
        headers = {"Authorization": f"Bearer {token}"}
        
        market = get_market_code(language)
        params = {
            "q": query,
            "type": "track",
            "limit": min(limit, 50),
            "market": market
        }
        
        resp = requests.get(url, headers=headers, params=params, timeout=5)
        resp.raise_for_status()
        
        data = resp.json()
        tracks = data.get("tracks", {}).get("items", [])
        
        if not tracks:
            logger.warning(f"No se encontraron canciones para: '{query}'")
            return []
        
        logger.info(f"Encontradas {len(tracks)} canciones para: '{query}'")
        return process_search_results(tracks)
        
    except requests.exceptions.Timeout:
        logger.error(f"Timeout buscando: '{query}'")
        return []
    except requests.HTTPError as e:
        if e.response.status_code == 401:
            logger.warning("Token caducado, refrescando...")
        logger.error(f"Error HTTP buscando '{query}': {e}")
        return []
    except Exception as e:
        logger.error(f"Error buscando '{query}': {str(e)}")
        return []

def process_search_results(tracks: List[Dict]) -> List[Dict[str, str]]:
    """
    Procesa los resultados de búsqueda de Spotify.
    """
    results = []
    
    for track in tracks:
        try:
            artists = track.get("artists", [])
            artist_name = artists[0]["name"] if artists else "Unknown"
            
            results.append({
                "id": track.get("id", ""),
                "name": track.get("name", "Unknown"),
                "artist": artist_name,
                "album": track.get("album", {}).get("name", "Unknown"),
                "preview_url": track.get("preview_url"),
                "spotify_url": track.get("external_urls", {}).get("spotify", ""),
                "duration_ms": track.get("duration_ms", 0)
            })
        except Exception as e:
            logger.error(f"Error procesando resultado de búsqueda: {e}")
            continue
    
    return results
