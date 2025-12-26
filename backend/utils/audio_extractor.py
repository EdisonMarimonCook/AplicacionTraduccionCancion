import logging
import requests
import yt_dlp

logger = logging.getLogger(__name__)

# Lista de instancias de Cobalt
COBALT_INSTANCES = [
    "https://api.cobalt.tools",
    "https://co.wuk.sh",
    "https://cobalt.xyx.host"
]

def get_youtube_audio_url(query: str):
    """
    1. Usa yt-dlp para BUSCAR el video (obtiene el link).
    2. Usa Cobalt para extraer el MP3 de ese link.
    """
    try:
        # Truco: Si el artista es muy largo o raro (como bandas sonoras), 
        # a veces ayuda simplificar la búsqueda, pero por ahora confiaremos en yt-dlp sin 'extract_flat'
        logger.info(f"🎧 Buscando link de YouTube con yt-dlp para: '{query}'...")

        # Configuramos yt-dlp para ser un poco más paciente y encontrar mejor la data
        ydl_opts = {
            'default_search': 'ytsearch1:', 
            'quiet': True,
            'no_warnings': True,
            # 'extract_flat': True,  <-- QUITAMOS ESTO. Hacía que fallara en búsquedas complejas.
            'noplaylist': True,      # Aseguramos que no traiga listas de reproducción
        }

        youtube_link = None

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            try:
                info = ydl.extract_info(query, download=False)
            except Exception as e:
                logger.error(f"⚠️ Error interno de yt-dlp al buscar: {e}")
                return None
            
            # Verificamos si encontró algo (la estructura cambia sin extract_flat)
            if 'entries' in info:
                # Es una búsqueda, tomamos el primero
                if len(info['entries']) > 0:
                    video_data = info['entries'][0]
                    youtube_link = video_data.get('webpage_url') or video_data.get('url')
            elif 'webpage_url' in info:
                # A veces devuelve el video directo si la búsqueda fue muy exacta
                youtube_link = info['webpage_url']

            if youtube_link:
                logger.info(f"✅ Video encontrado: {youtube_link}")
            else:
                logger.warning("⚠️ yt-dlp no encontró resultados (lista vacía).")
                return None

        # 2. PEDIR AUDIO A COBALT
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json"
        }

        payload = {
            "url": youtube_link,
            "aFormat": "mp3",
            "isAudioOnly": True,
        }

        for instance in COBALT_INSTANCES:
            try:
                api_url = f"{instance}/api/json"
                logger.info(f"🚀 Probando Cobalt en: {instance}")
                
                response = requests.post(api_url, json=payload, headers=headers, timeout=20) # Subimos timeout a 20s
                
                if response.status_code == 200:
                    data = response.json()
                    if "url" in data:
                        audio_url = data["url"]
                        logger.info("✨ ¡Audio extraído con éxito!")
                        return audio_url
                    elif "status" in data and data["status"] == "error":
                         logger.warning(f"⚠️ Cobalt devolvió error: {data.get('text')}")
                else:
                    logger.warning(f"⚠️ Falló instancia {instance} (Status: {response.status_code})")
            
            except Exception as e:
                logger.error(f"⚠️ Error conectando con {instance}: {e}")
                continue 

        logger.error("❌ Todas las instancias de Cobalt fallaron o no devolvieron URL.")
        return None

    except Exception as e:
        logger.error(f"❌ Error general en audio_extractor: {e}")
        return None