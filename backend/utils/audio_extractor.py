"""
ARCHIVO: utils/audio_extractor.py
PROPÓSITO: Buscar canciones en YouTube y extraer la URL directa de audio (streaming).
"""
import yt_dlp
import logging

logger = logging.getLogger(__name__)

def get_youtube_audio_url(query: str):
    """
    Busca una canción en YouTube y devuelve la URL directa de audio.
    USA: yt-dlp con búsqueda 'ytsearch1:'
    """
    ydl_opts = {
        'format': 'bestaudio[ext=m4a]/bestaudio/best',  # Priorizamos m4a (AAC) que Android ama
        'noplaylist': True,
        'quiet': True,
        'default_search': 'ytsearch1:',  # Busca el primer resultado
        'geo_bypass': True,
        'socket_timeout': 10,
    }

    try:
        logger.info(f"🎧 Buscando audio para: {query}")
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            # Extraemos info sin descargar nada (download=False)
            info = ydl.extract_info(query, download=False)

            if 'entries' in info:
                # Si es una búsqueda, cogemos el primer resultado
                video_data = info['entries'][0]
            else:
                video_data = info

            audio_url = video_data.get('url')
            title = video_data.get('title')
            duration = video_data.get('duration')
            thumbnail = video_data.get('thumbnail')

            logger.info(f"✅ Audio encontrado: {title}")
            
            return {
                "title": title,
                "stream_url": audio_url,
                "duration": duration,
                "thumbnail": thumbnail,
                "source": "YouTube (Backend Proxy)"
            }

    except Exception as e:
        logger.error(f"❌ Error extrayendo audio con yt-dlp: {e}")
        return None