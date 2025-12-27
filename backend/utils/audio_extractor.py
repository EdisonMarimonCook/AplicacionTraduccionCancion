"""
ARCHIVO: utils/audio_extractor.py
PROPÓSITO: Extracción de audio con TODAS las estrategias posibles
ORDEN: Piped → DuckDuckGo + yt-dlp → Invidious validado
"""
import logging
import requests
import yt_dlp
from typing import Optional, Dict
import random

logger = logging.getLogger(__name__)

# =========================================================================
# INSTANCIAS PÚBLICAS (Ordenadas por confiabilidad según LibreTube)
# =========================================================================

# Piped (Lo que usa LibreTube - MÁS CONFIABLE)
PIPED_INSTANCES = [
    "https://pipedapi.kavin.rocks",
    "https://pipedapi-libre.kavin.rocks",
    "https://api-piped.mha.fi",
    "https://pipedapi.adminforge.de",
    "https://api.piped.privacydev.net"
]

# Invidious (Con validación previa)
INVIDIOUS_INSTANCES = [
    "https://inv.nadeko.net",
    "https://yewtu.be",
    "https://invidious.fdn.fr",
    "https://inv.riverside.rocks"
]

# User-Agents rotativos
USER_AGENTS = [
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36',
    'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36'
]


def get_youtube_audio_url(query: str) -> Optional[Dict]:
    """
    🎯 ESTRATEGIA DEFINITIVA DE 3 NIVELES
    1. Piped API (LibreTube way)
    2. DuckDuckGo + yt-dlp (Gemini suggestion)
    3. Invidious validado
    """
    
    # NIVEL 1: PIPED (Más confiable)
    logger.info(f"🔍 Iniciando búsqueda: '{query}'")
    audio_data = try_piped(query)
    if audio_data:
        return audio_data
    
    # NIVEL 2: DUCKDUCKGO + YT-DLP
    audio_data = try_duckduckgo_ytdlp(query)
    if audio_data:
        return audio_data
    
    # NIVEL 3: INVIDIOUS VALIDADO
    audio_data = try_invidious_validated(query)
    if audio_data:
        return audio_data
    
    logger.error(f"❌ TODAS las estrategias fallaron para: {query}")
    return None


# =========================================================================
# NIVEL 1: PIPED API (LibreTube Strategy)
# =========================================================================

def try_piped(query: str) -> Optional[Dict]:
    """
    Usa la API de Piped como LibreTube
    Docs: https://docs.piped.video/docs/api-documentation/
    """
    for instance in PIPED_INSTANCES:
        try:
            logger.info(f"🔍 Piped [{instance}]: Buscando '{query}'")
            
            # PASO 1: Buscar video
            search_url = f"{instance}/search"
            params = {
                "q": query,
                "filter": "all"  # music_songs, all, videos
            }
            
            headers = {
                "User-Agent": random.choice(USER_AGENTS)
            }
            
            search_response = requests.get(
                search_url,
                params=params,
                headers=headers,
                timeout=8
            )
            
            if search_response.status_code != 200:
                logger.warning(f"⚠️ [{instance}] Search failed: {search_response.status_code}")
                continue
            
            results = search_response.json().get("items", [])
            if not results:
                logger.warning(f"⚠️ [{instance}] Sin resultados")
                continue
            
            # Filtrar solo videos (no playlists/channels)
            videos = [r for r in results if r.get("type") == "stream"]
            if not videos:
                logger.warning(f"⚠️ [{instance}] Sin videos en resultados")
                continue
            
            video_id = videos[0].get("url", "").replace("/watch?v=", "")
            video_title = videos[0].get("title", query)
            
            # PASO 2: Obtener streams del video
            streams_url = f"{instance}/streams/{video_id}"
            streams_response = requests.get(
                streams_url,
                headers=headers,
                timeout=8
            )
            
            if streams_response.status_code != 200:
                logger.warning(f"⚠️ [{instance}] Streams failed")
                continue
            
            video_data = streams_response.json()
            
            # PASO 3: Extraer mejor audio stream
            audio_streams = video_data.get("audioStreams", [])
            if not audio_streams:
                logger.warning(f"⚠️ [{instance}] Sin audio streams")
                continue
            
            # Ordenar por bitrate (mejor calidad primero)
            audio_streams.sort(key=lambda x: x.get("bitrate", 0), reverse=True)
            best_audio = audio_streams[0]
            
            logger.info(f"✅ Piped [{instance}]: {video_title}")
            
            return {
                "stream_url": best_audio["url"],
                "title": video_title,
                "duration": video_data.get("duration"),
                "thumbnail": video_data.get("thumbnailUrl"),
                "source": f"YouTube (Piped)"
            }
        
        except requests.Timeout:
            logger.warning(f"⚠️ [{instance}] Timeout")
            continue
        except Exception as e:
            logger.warning(f"⚠️ [{instance}] Error: {str(e)}")
            continue
    
    logger.error("❌ Todas las instancias de Piped fallaron")
    return None


# =========================================================================
# NIVEL 2: DUCKDUCKGO + YT-DLP (Gemini Strategy)
# =========================================================================

def try_duckduckgo_ytdlp(query: str) -> Optional[Dict]:
    """
    Usa DuckDuckGo para buscar (menos restrictivo que Google)
    y yt-dlp para extraer el audio
    """
    try:
        logger.info(f"🦆 DuckDuckGo: Buscando '{query}'")
        
        # PASO 1: Buscar con DuckDuckGo (endpoint no oficial)
        ddg_url = "https://api.duckduckgo.com/"
        params = {
            "q": f"{query} site:youtube.com",
            "format": "json",
            "no_html": 1
        }
        
        ddg_response = requests.get(
            ddg_url,
            params=params,
            timeout=5,
            headers={"User-Agent": random.choice(USER_AGENTS)}
        )
        
        if ddg_response.status_code != 200:
            logger.warning("⚠️ DuckDuckGo search failed")
            return None
        
        ddg_data = ddg_response.json()
        
        # Buscar primer resultado de YouTube
        youtube_url = None
        for result in ddg_data.get("Results", []):
            url = result.get("FirstURL", "")
            if "youtube.com/watch" in url or "youtu.be/" in url:
                youtube_url = url
                break
        
        # Fallback: RelatedTopics
        if not youtube_url:
            for topic in ddg_data.get("RelatedTopics", []):
                if isinstance(topic, dict):
                    url = topic.get("FirstURL", "")
                    if "youtube.com/watch" in url or "youtu.be/" in url:
                        youtube_url = url
                        break
        
        if not youtube_url:
            logger.warning("⚠️ DuckDuckGo: Sin resultados de YouTube")
            return None
        
        logger.info(f"✅ DuckDuckGo encontró: {youtube_url}")
        
        # PASO 2: Extraer audio con yt-dlp
        ydl_opts = {
            'format': 'bestaudio[ext=m4a]/bestaudio/best',
            'quiet': True,
            'no_warnings': True,
            'extract_flat': False,
            'socket_timeout': 10,
            'http_headers': {
                'User-Agent': random.choice(USER_AGENTS),
                'Accept-Language': 'en-US,en;q=0.9',
            }
        }
        
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(youtube_url, download=False)
            
            audio_url = info.get('url')
            if not audio_url:
                logger.warning("⚠️ yt-dlp: Sin URL de audio")
                return None
            
            logger.info(f"✅ DuckDuckGo + yt-dlp: {info.get('title')}")
            
            return {
                "stream_url": audio_url,
                "title": info.get('title'),
                "duration": info.get('duration'),
                "thumbnail": info.get('thumbnail'),
                "source": "YouTube (DuckDuckGo + yt-dlp)"
            }
    
    except Exception as e:
        logger.error(f"❌ DuckDuckGo + yt-dlp error: {str(e)}")
        return None


# =========================================================================
# NIVEL 3: INVIDIOUS VALIDADO (Con Health Check)
# =========================================================================

def try_invidious_validated(query: str) -> Optional[Dict]:
    """
    Valida primero qué instancias de Invidious están activas
    """
    # Validar instancias activas
    active_instances = []
    for instance in INVIDIOUS_INSTANCES:
        try:
            health_url = f"{instance}/api/v1/stats"
            health_response = requests.get(health_url, timeout=3)
            if health_response.status_code == 200:
                active_instances.append(instance)
                logger.info(f"✅ Invidious [{instance}] activa")
        except:
            logger.warning(f"⚠️ Invidious [{instance}] caída")
            continue
    
    if not active_instances:
        logger.error("❌ Ninguna instancia de Invidious activa")
        return None
    
    # Intentar con instancias activas
    for instance in active_instances:
        try:
            logger.info(f"🔍 Invidious [{instance}]: Buscando '{query}'")
            
            # Buscar
            search_url = f"{instance}/api/v1/search"
            search_response = requests.get(
                search_url,
                params={"q": query, "type": "video"},
                headers={"User-Agent": random.choice(USER_AGENTS)},
                timeout=8
            )
            
            if search_response.status_code != 200:
                continue
            
            results = search_response.json()
            if not results:
                continue
            
            video_id = results[0]["videoId"]
            
            # Obtener streams
            video_url = f"{instance}/api/v1/videos/{video_id}"
            video_response = requests.get(video_url, timeout=8)
            
            if video_response.status_code != 200:
                continue
            
            video_data = video_response.json()
            audio_streams = [s for s in video_data.get("adaptiveFormats", []) 
                           if s.get("type", "").startswith("audio")]
            
            if not audio_streams:
                continue
            
            best_audio = max(audio_streams, key=lambda x: x.get("bitrate", 0))
            
            logger.info(f"✅ Invidious [{instance}]: {video_data.get('title')}")
            
            return {
                "stream_url": best_audio["url"],
                "title": video_data.get("title"),
                "duration": video_data.get("lengthSeconds"),
                "thumbnail": video_data.get("videoThumbnails", [{}])[0].get("url"),
                "source": "YouTube (Invidious)"
            }
        
        except Exception as e:
            logger.warning(f"⚠️ [{instance}] Error: {str(e)}")
            continue
    
    logger.error("❌ Todas las instancias validadas de Invidious fallaron")
    return None