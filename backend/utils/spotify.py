"""
Cliente de Spotify para enriquecer datos de canciones + Fallback iTunes + Grammys
"""

import logging
import re
import requests
from typing import Optional, Dict, List
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials
from config import settings

logger = logging.getLogger(__name__)

# ===============================================================================
# UTILIDADES
# ===============================================================================

def normalize_title(text: str) -> str:
    """Normaliza títulos para mejorar búsquedas (elimina ruido)"""
    patterns = [
        r'\s*-\s*Anime Size',
        r'\s*-\s*TV Size',
        r'\s*-\s*Remake Ver\.?',
        r'\s*\(Official.*?\)',
        r'\s*\(Lyric.*?\)',
        r'\s*\(Audio\)',
    ]
    result = text
    for pattern in patterns:
        result = re.sub(pattern, '', result, flags=re.IGNORECASE)
    return ' '.join(result.split()).strip()

# ===============================================================================
# CONFIGURACIÓN: Listas MVP (Grammys / Hits)
# ===============================================================================
GRAMMY_SEARCHES = {
    "en": ["Billie Eilish", "Kendrick Lamar", "Chappell Roan", "Sabrina Carpenter", "Taylor Swift", "Beyoncé"],
    "es": ["Latin Grammy 2024", "Bad Bunny", "Karol G", "Rosalia", "Exitos España"],
    "fr": ["Top France", "Stromae", "Indila", "Aya Nakamura"],
    "de": ["Top Germany", "Apache 207"],
    "pt": ["Top Brasil", "Anitta"],
    "it": ["Top Italy", "Maneskin"],
    # 🔥 MEJORADO: Búsquedas 100% en idioma nativo con market forzado
    "ja": ["YOASOBI アイドル", "Ado うっせぇわ", "米津玄師 Lemon", "Official髭男dism", "LiSA 紅蓮華", "millennium parade"],  # 100% J-Pop
    "ko": ["뉴진스 Ditto", "아이유 Love wins all", "세븐틴", "BTS Dynamite", "에스파", "르세라핌"],  # K-Pop con hangul
    "zh": ["周杰伦 晴天", "邓紫棋 光年之外", "五月天 温柔", "林俊杰 江南", "王心凌 爱你", "田馥甄"]  # Mandopop clásico
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
        logger.error(f"❌ Error inicializando Spotify: {e}")
        return None

# ===============================================================================
# ITUNES RESCUE STRATEGY 🚑 (Para cuando Spotify no da audio)
# ===============================================================================

def get_itunes_preview(query: str) -> Optional[str]:
    """
    Busca el audio preview en iTunes API (es pública y gratis).
    """
    try:
        url = "https://itunes.apple.com/search"
        params = {
            "term": query,
            "media": "music",
            "entity": "song",
            "limit": 1
        }
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        }
        response = requests.get(url, params=params, headers=headers, timeout=5)
        if response.status_code == 200:
            data = response.json()
            if data["resultCount"] > 0:
                preview = data["results"][0].get("previewUrl")
                if preview:
                    # logger.info(f"✅ Audio rescatado de iTunes para: {query}")
                    return preview
        return None
    except Exception:
        return None

# ===============================================================================
# FUNCIONES PRINCIPALES
# ===============================================================================

def search_songs_spotify(query: str, limit: int = 10, market: str = None) -> List[Dict]:
    """Busca en Spotify y rellena huecos con iTunes si hace falta"""
    sp = get_spotify_client()
    if not sp: 
        logger.warning("⚠️ Spotify no configurado")
        return []

    try:
        # 🔥 MEJORADO: Detectar idioma asiático y añadir market específico
        search_params = {"q": query, "limit": limit, "type": "track"}
        
        # Detectar si la query contiene caracteres asiáticos
        import re
        if re.search(r'[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF]', query):  # Japonés/Chino
            if re.search(r'[\u3040-\u309F\u30A0-\u30FF]', query):  # Hiragana/Katakana
                logger.info(f"🎌 Búsqueda japonesa, forzando market=JP")
                search_params["market"] = "JP"
            elif re.search(r'[\u4E00-\u9FFF]', query):  # Kanji/Hanzi
                logger.info(f"🇨🇳 Búsqueda china, forzando market=TW")
                search_params["market"] = "TW"
        elif re.search(r'[\uAC00-\uD7AF]', query):  # Coreano (Hangul)
            logger.info(f"🇰🇷 Búsqueda coreana, forzando market=KR")
            search_params["market"] = "KR"
        elif market:
            search_params["market"] = market
        
        results = sp.search(**search_params)
        items = results['tracks']['items']
        tracks = []
        
        for item in items:
            # Lógica de Audio: Si Spotify da null, probamos iTunes
            preview_url = item.get('preview_url')
            
            # 🔥 ESTRATEGIA RESCATE SOLO SI ES EL PRIMER RESULTADO (Para no saturar)
            if not preview_url and items.index(item) == 0:
                search_term = f"{item['name']} {item['artists'][0]['name']}"
                preview_url = get_itunes_preview(search_term)

            track = {
                "id": item['id'],
                "name": item['name'],
                "title": item['name'], # Alias para compatibilidad
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
    """Obtiene mix de canciones populares/nominadas CON MARKET ESPECÍFICO"""
    queries = GRAMMY_SEARCHES.get(lang, ["Top Hits"])
    all_tracks = []
    
    # 🔥 Mapeo de idioma a market de Spotify
    MARKET_MAP = {
        "ja": "JP",
        "ko": "KR",
        "zh": "TW",  # Taiwan para Mandopop
        "es": "ES",
        "en": "US",
        "fr": "FR",
        "de": "DE",
        "pt": "BR",
        "it": "IT"
    }
    
    market = MARKET_MAP.get(lang, None)
    
    # Buscamos un poco de cada query para tener variedad
    for q in queries:
        tracks = search_songs_spotify(q, limit=3, market=market)
        all_tracks.extend(tracks)
    
    # Eliminamos duplicados por ID
    unique_tracks = {t['id']: t for t in all_tracks}.values()
    return list(unique_tracks)

async def enrich_single_song(title: str, artist: str) -> Dict:
    """
    Busca metadatos detallados + audio con validación de relevancia.
    Intenta con título original y luego normalizado.
    """
    # Intentar primero con título original
    search_query = f"{title} {artist}"
    tracks = search_songs_spotify(search_query, limit=5)
    
    # Si no hay resultados, probar con título normalizado
    if not tracks:
        normalized_title = normalize_title(title)
        if normalized_title != title:
            logger.info(f"🔄 Reintentando con título normalizado: {normalized_title}")
            search_query = f"{normalized_title} {artist}"
            tracks = search_songs_spotify(search_query, limit=5)
    
    if not tracks:
        return {}
    
    # 🔥 Validar relevancia: al menos una palabra del título debe coincidir
    title_words = set(normalize_title(title).lower().split())
    
    for track in tracks:
        track_title_words = set(track['name'].lower().split())
        
        # Si hay intersección de palabras, es un resultado válido
        if title_words & track_title_words:
            logger.info(f"✅ Audio encontrado: {track['name']} - {track['artist']}")
            return track
    
    # Si ninguno coincide, retornar el primero con advertencia
    logger.warning(f"⚠️ Sin coincidencias exactas, usando primer resultado: {tracks[0]['name']}")
    return tracks[0]

# 🔥 ESTA ES LA FUNCIÓN QUE FALTABA Y DABA ERROR EN MAIN.PY
def get_top_tracks_by_language(lang: str) -> List[Dict]:
    """Alias para que main.py pueda actualizar la cache"""
    return get_grammy_songs(lang)

def is_grammy_nominee(song_title: str, artist: str, lang: str = "en") -> bool:
    """
    🏆 Detecta si una canción es nominada a Grammy/Top Hit
    
    Compara el título y artista con las listas de GRAMMY_SEARCHES.
    Retorna True si la canción está en la lista curada.
    """
    try:
        # Obtener lista de hits para el idioma
        grammy_tracks = get_grammy_songs(lang)
        
        # Normalizar para comparación
        song_title_lower = song_title.lower().strip()
        artist_lower = artist.lower().strip()
        
        # Buscar coincidencia
        for track in grammy_tracks:
            track_title_lower = track.get('name', '').lower().strip()
            track_artist_lower = track.get('artist', '').lower().strip()
            
            # Coincidencia si título Y artista matchean
            if song_title_lower in track_title_lower or track_title_lower in song_title_lower:
                if artist_lower in track_artist_lower or track_artist_lower in artist_lower:
                    return True
        
        return False
    except Exception as e:
        logger.error(f"❌ Error verificando Grammy: {e}")
        return False