import requests
import logging
import urllib.parse

logger = logging.getLogger(__name__)

def get_youtube_audio_url(query: str):
    """
    Busca una canción en YouTube y extrae la URL directa de audio usando Cobalt API.
    Ventaja: Evita bloqueos de IP de Google y no requiere cookies.
    """
    try:
        logger.info(f"🎧 Cobalt Extractor: Buscando '{query}'...")

        # 1. Construimos la URL de búsqueda de YouTube
        # Cobalt es listo: si le das una búsqueda, coge el primer video.
        encoded_query = urllib.parse.quote(query)
        youtube_search_url = f"https://www.youtube.com/results?search_query={encoded_query}"

        # 2. Configuración del Payload para Cobalt
        payload = {
            "url": youtube_search_url,
            "vCodec": "h264",
            "vQuality": "720",
            "aFormat": "mp3",      # Pedimos MP3 para máxima compatibilidad
            "isAudioOnly": True    # Solo queremos el stream de audio
        }

        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "User-Agent": "MusicTranslatorBot/1.0"
        }

        # 3. Llamada a la API (Instancia pública principal)
        response = requests.post(
            "https://api.cobalt.tools/api/json",
            json=payload,
            headers=headers,
            timeout=15 
        )

        if response.status_code == 200:
            data = response.json()
            
            # Cobalt devuelve la url directa en 'url'
            stream_url = data.get("url")
            
            # Verificamos que sea un stream válido
            if data.get("status") in ["stream", "redirect"] and stream_url:
                logger.info(f"✅ Audio encontrado vía Cobalt")
                return {
                    "title": query, # Cobalt a veces no devuelve título en búsqueda, usamos la query
                    "stream_url": stream_url,
                    "duration": None, # Cobalt búsqueda no siempre da duración
                    "thumbnail": None,
                    "source": "YouTube (Cobalt API)"
                }
            elif data.get("status") == "picker":
                # Si devuelve una lista, cogemos el primero
                if data.get("picker") and len(data["picker"]) > 0:
                    stream_url = data["picker"][0].get("url")
                    logger.info(f"✅ Audio seleccionado del picker Cobalt")
                    return {
                        "title": query,
                        "stream_url": stream_url,
                        "source": "YouTube (Cobalt API)"
                    }
            
            logger.warning(f"⚠️ Cobalt no devolvió stream directo: {data.get('text')}")
            return None

        else:
            logger.error(f"❌ Error Cobalt API: {response.status_code}")
            return None

    except Exception as e:
        logger.error(f"❌ Error crítico en extractor Cobalt: {e}")
        return None