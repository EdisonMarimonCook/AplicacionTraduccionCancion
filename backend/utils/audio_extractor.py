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
    1. Usa yt-dlp para BUSCAR el video (solo obtiene el link, no descarga nada).
    2. Usa Cobalt para extraer el MP3 de ese link.
    """
    try:
        logger.info(f"🎧 Buscando link de YouTube con yt-dlp para: '{query}'...")

        # 1. BUSCAR EL VIDEO CON YT-DLP (Modo ultra-rápido 'extract_flat')
        ydl_opts = {
            'default_search': 'ytsearch1:', # Busca el primer resultado
            'quiet': True,                  # No imprimir basura en la consola
            'no_warnings': True,
            'extract_flat': True,           # ¡CLAVE! Solo saca datos, no descarga video
        }

        youtube_link = None

        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(query, download=False)
            
            # Verificamos si encontró algo
            if 'entries' in info and len(info['entries']) > 0:
                video_data = info['entries'][0]
                # A veces yt-dlp devuelve la URL completa, a veces solo el ID
                if 'url' in video_data:
                    youtube_link = video_data['url']
                elif 'id' in video_data:
                    youtube_link = f"https://www.youtube.com/watch?v={video_data['id']}"
                
                logger.info(f"✅ Video encontrado: {youtube_link}")
            else:
                logger.warning("⚠️ yt-dlp no encontró resultados.")
                return None

        # 2. PEDIR AUDIO A COBALT (Probando instancias)
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
                
                response = requests.post(api_url, json=payload, headers=headers, timeout=15)
                
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
                continue 

        logger.error("❌ Todas las instancias de Cobalt fallaron.")
        return None

    except Exception as e:
        logger.error(f"❌ Error general en audio_extractor: {e}")
        return None