import requests
import logging
# 👇 Importamos la nueva librería de búsqueda
from youtubesearchpython import VideosSearch 

logger = logging.getLogger(__name__)

# Lista de instancias de Cobalt (si una falla, probamos la siguiente)
COBALT_INSTANCES = [
    "https://api.cobalt.tools",
    "https://co.wuk.sh",
    "https://cobalt.xyx.host"
]

def get_audio_url(query: str):
    """
    1. Busca el video en YouTube para obtener el enlace real.
    2. Usa Cobalt para extraer el MP3 de ese enlace.
    """
    try:
        logger.info(f"🎧 Buscando link de YouTube para: '{query}'...")

        # 1. BUSCAR EL VIDEO (Obtener URL real)
        videos_search = VideosSearch(query, limit=1)
        results = videos_search.result()

        if not results or not results['result']:
            logger.warning("⚠️ No se encontraron videos en YouTube.")
            return None

        # Obtenemos el enlace directo (ej: https://youtube.com/watch?v=...)
        youtube_link = results['result'][0]['link']
        logger.info(f"✅ Video encontrado: {youtube_link}")

        # 2. PEDIR AUDIO A COBALT (Probando instancias)
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json"
        }

        payload = {
            "url": youtube_link,  # Le damos el link exacto
            "aFormat": "mp3",
            "isAudioOnly": True,
        }

        for instance in COBALT_INSTANCES:
            try:
                api_url = f"{instance}/api/json"
                logger.info(f"🚀 Probando Cobalt en: {instance}")
                
                response = requests.post(api_url, json=payload, headers=headers, timeout=10)
                
                if response.status_code == 200:
                    data = response.json()
                    if "url" in data:
                        audio_url = data["url"]
                        logger.info("✨ ¡Audio extraído con éxito!")
                        return audio_url
                else:
                    logger.warning(f"⚠️ Falló instancia {instance} (Status: {response.status_code})")
            
            except Exception as e:
                logger.error(f"⚠️ Error conectando con {instance}: {e}")
                continue # Prueba la siguiente

        logger.error("❌ Todas las instancias de Cobalt fallaron.")
        return None

    except Exception as e:
        logger.error(f"❌ Error general en audio_extractor: {e}")
        return None