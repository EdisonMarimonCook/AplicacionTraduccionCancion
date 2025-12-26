"""
ARCHIVO: utils/audio_extractor.py
PROPÓSITO: Extraer audio de YouTube con múltiples fuentes
ESTRATEGIA:
  1. Invidious (5 instancias públicas)
  2. Cobalt (fallback)
  3. None (frontend usará preview_url de iTunes)
"""
import logging
import requests
from typing import Optional, Dict

logger = logging.getLogger(__name__)

# Instancias públicas de Invidious (ordenadas por confiabilidad)
INVIDIOUS_INSTANCES = [
    "https://inv.nadeko.net",
    "https://yewtu.be",
    "https://vid.puffyan.us",
    "https://invidious.privacyredirect.com",
    "https://invidious.snopyta.org"
]

# Instancias de Cobalt (fallback)
COBALT_INSTANCES = [
    "https://api.cobalt.tools",
    "https://co.wuk.sh",
    "https://cobalt.xyx.host"
]


def get_youtube_audio_url(query: str) -> Optional[Dict]:
    """
    Estrategia de extracción en 3 niveles:
    1. Invidious (APIs públicas de YouTube)
    2. Cobalt (fallback si Invidious falla)
    3. None (el frontend usará preview_url)
    """
    
    # NIVEL 1: INVIDIOUS
    audio_data = try_invidious(query)
    if audio_data:
        return audio_data
    
    # NIVEL 2: COBALT
    audio_data = try_cobalt(query)
    if audio_data:
        return audio_data
    
    # NIVEL 3: Sin audio
    logger.warning(f"⚠️ No se encontró audio completo para: {query}")
    return None


def try_invidious(query: str) -> Optional[Dict]:
    """
    Busca en múltiples instancias de Invidious hasta encontrar una que funcione
    """
    for instance in INVIDIOUS_INSTANCES:
        try:
            logger.info(f"🔍 Invidious [{instance}]: Buscando '{query}'")
            
            # PASO 1: Buscar el video
            search_url = f"{instance}/api/v1/search"
            search_params = {
                "q": query,
                "type": "video",
                "sort_by": "relevance"
            }
            
            search_response = requests.get(
                search_url,
                params=search_params,
                timeout=8,
                headers={"User-Agent": "MusicTransIAtor/1.0"}
            )
            
            if search_response.status_code != 200:
                logger.warning(f"⚠️ [{instance}] Search failed: {search_response.status_code}")
                continue
            
            results = search_response.json()
            if not results or len(results) == 0:
                logger.warning(f"⚠️ [{instance}] No results")
                continue
            
            video_id = results[0]["videoId"]
            video_title = results[0]["title"]
            
            # PASO 2: Obtener streams de audio
            video_url = f"{instance}/api/v1/videos/{video_id}"
            video_response = requests.get(
                video_url,
                timeout=8,
                headers={"User-Agent": "MusicTransIAtor/1.0"}
            )
            
            if video_response.status_code != 200:
                logger.warning(f"⚠️ [{instance}] Video fetch failed")
                continue
            
            video_data = video_response.json()
            
            # PASO 3: Extraer mejor stream de audio
            adaptive_formats = video_data.get("adaptiveFormats", [])
            audio_streams = [
                stream for stream in adaptive_formats
                if stream.get("type", "").startswith("audio")
            ]
            
            if not audio_streams:
                logger.warning(f"⚠️ [{instance}] No audio streams")
                continue
            
            # Ordenar por bitrate (mejor calidad primero)
            audio_streams.sort(key=lambda x: x.get("bitrate", 0), reverse=True)
            best_audio = audio_streams[0]
            
            logger.info(f"✅ Invidious [{instance}]: {video_title}")
            
            return {
                "stream_url": best_audio["url"],
                "title": video_title,
                "duration": video_data.get("lengthSeconds"),
                "thumbnail": video_data.get("videoThumbnails", [{}])[0].get("url"),
                "source": f"YouTube (Invidious)"
            }
        
        except requests.Timeout:
            logger.warning(f"⚠️ [{instance}] Timeout")
            continue
        except Exception as e:
            logger.warning(f"⚠️ [{instance}] Error: {str(e)}")
            continue
    
    logger.error("❌ Todas las instancias de Invidious fallaron")
    return None


def try_cobalt(query: str) -> Optional[Dict]:
    """
    Intenta extraer audio con Cobalt API
    Primero busca el video en YouTube, luego extrae con Cobalt
    """
    try:
        logger.info(f"🎧 Cobalt Extractor: Buscando '{query}'...")
        
        # Primero necesitamos el link de YouTube
        # Usamos la API de búsqueda de YouTube (simplificada)
        youtube_search_url = f"https://www.youtube.com/results?search_query={query.replace(' ', '+')}"
        
        for instance in COBALT_INSTANCES:
            try:
                api_url = f"{instance}/api/json"
                
                payload = {
                    "url": youtube_search_url,
                    "aFormat": "mp3",
                    "isAudioOnly": True
                }
                
                headers = {
                    "Accept": "application/json",
                    "Content-Type": "application/json",
                    "User-Agent": "MusicTransIAtor/1.0"
                }
                
                response = requests.post(
                    api_url, 
                    json=payload, 
                    headers=headers, 
                    timeout=15
                )
                
                if response.status_code == 200:
                    data = response.json()
                    
                    if "url" in data:
                        logger.info(f"✅ Cobalt [{instance}]: Audio extraído")
                        return {
                            "stream_url": data["url"],
                            "title": query,
                            "duration": None,
                            "thumbnail": None,
                            "source": "YouTube (Cobalt)"
                        }
                    elif "status" in data and data["status"] == "error":
                        logger.warning(f"⚠️ Cobalt error: {data.get('text')}")
                else:
                    logger.warning(f"⚠️ Cobalt [{instance}]: {response.status_code}")
            
            except Exception as e:
                logger.error(f"⚠️ Cobalt [{instance}] error: {e}")
                continue
        
        logger.error("❌ Todas las instancias de Cobalt fallaron")
        return None
        
    except Exception as e:
        logger.error(f"❌ Error general Cobalt: {e}")
        return None