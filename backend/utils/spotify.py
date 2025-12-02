"""
Cliente de Spotify para enriquecer datos de canciones + Fallback iTunes + Grammys
"""

import logging
import requests
from typing import Optional, Dict, List
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials
from config import settings

logger = logging.getLogger(__name__)

# ===============================================================================
# CONFIGURACIÓN: Listas MVP (Grammys / Hits)
# ===============================================================================
GRAMMY_SEARCHES = {
    "en": ["Billie Eilish", "Kendrick Lamar", "Chappell Roan", "Sabrina Carpenter", "Taylor Swift","Beyoncé"],
    "es": ["Latin Grammy 2024", "Bad Bunny", "Karol G", "Rosalia", "Exitos España"],
    "fr": ["Top France", "Stromae", "Indila", "Aya Nakamura"],
    "de": ["Top Germany", "Apache 207"],
    "pt": ["Top Brasil", "Anitta"],
    "it": ["Top Italy", "Maneskin"],
    "jp": ["Top Japan", "J-Pop Hits"]
}

# ===============================================================================
# SPOTIFY CLIENT
# ===============================================================================

def get_spotify_client() -> Optional[spotipy.Spotify]:
    try:
        if not settings.SPOTIFY_CLIENT_ID or not settings.SPOTIFY_CLIENT_SECRET:
            return None
        
        credentials_manager = SpotifyClientCredentials(
            client_id=settings.SPOTIFY_CLIENT_ID,
            client_secret=settings.SPOTIFY_CLIENT_SECRET
        )
        return spotipy.Spotify(client_credentials_manager=credentials_manager)
    except Exception as e:
        logger.error(f"❌ Error iniciando Spotify: {e}")
        return None

# ===============================================================================
# ITUNES FALLBACK (Audio Preview)
# ===============================================================================

def get_audio_preview_from_itunes(term: str) -> Optional[str]:
    """
    Busca en iTunes si Spotify no da preview.
    API pública y gratuita.
    """
    try:
        # Buscamos 'track' de tipo 'music'
        url = "https://itunes.apple.com/search"
        params = {
            "term": term,
            "media": "music",
            "entity": "song",
            "limit": 1
        }
        response = requests.get(url, params=params, timeout=3)
        
        if response.status_code == 200:
            data = response.json()
            if data["resultCount"] > 0:
                preview = data["results"][0].get("previewUrl")
                if preview:
                    logger.info("🍏 Audio recuperado desde iTunes Fallback")
                    return preview
    except Exception as e:
        logger.warning(f"⚠️ iTunes Fallback falló: {e}")
    return None

# ===============================================================================
# FUNCIONES DE BÚSQUEDA
# ===============================================================================

def search_songs_spotify(query: str, limit: int = 10) -> List[Dict]:
    """Busca canciones y rellena audio con iTunes si hace falta"""
    sp = get_spotify_client()
    if not sp: return []
    
    try:
        results = sp.search(q=query, limit=limit, type='track')
        tracks = []
        
        for item in results['tracks']['items']:
            preview_url = item.get('preview_url')
            
            # Formatear track
            track = {
                "id": item['id'],
                "name": item['name'],
                "artist": item['artists'][0]['name'],
                "image_url": item['album']['images'][0]['url'] if item['album']['images'] else None,
                "preview_url": preview_url,
                "spotify_url": item['external_urls']['spotify'],
                "album": item['album']['name']
            }
            tracks.append(track)
            
        return tracks
    except Exception as e:
        logger.error(f"❌ Error búsqueda Spotify: {e}")
        return []

def get_grammy_songs(lang: str = "en") -> List[Dict]:
    """Obtiene mix de canciones populares/nominadas"""
    queries = GRAMMY_SEARCHES.get(lang, ["Top Hits"])
    all_tracks = []
    
    # Buscamos un poco de cada query para tener variedad
    for q in queries:
        tracks = search_songs_spotify(q, limit=3)
        all_tracks.extend(tracks)
    
    # Eliminamos duplicados por ID
    unique_tracks = {t['id']: t for t in all_tracks}.values()
    return list(unique_tracks)

async def enrich_single_song(title: str, artist: str) -> Dict:
    """Busca metadatos detallados + intento fuerte de audio"""
    search_query = f"{title} {artist}"
    tracks = search_songs_spotify(search_query, limit=1)
    
    if not tracks:
        return {}
    
    song = tracks[0]
    
    # Si Spotify no dio audio, intentamos iTunes ahora
    if not song.get("preview_url"):
        song["preview_url"] = get_audio_preview_from_itunes(search_query)
        
    return song