"""
MÓDULO: Cliente de Letras Híbrido (LRCLIB + Genius Fallback)
PROPÓSITO: Obtener letras de forma robusta evitando bloqueos de Cloudflare.
ESTRATEGIA:
1. Intentar API abierta LRCLIB (Sin bloqueo, rápido).
2. Si falla, intentar Genius con curl_cffi (Impersonate Browser).
"""

import logging
from typing import Optional, List, Dict
import requests  # Para LRCLIB (API amigable)
from curl_cffi import requests as cffi_requests  # 🚀 EL ARMA SECRETA ANTI-CLOUDFLARE
from bs4 import BeautifulSoup
from langdetect import detect, LangDetectException
from config import settings

logger = logging.getLogger(__name__)

# Token de Genius (Solo se usa para la búsqueda inicial en la API oficial)
GENIUS_ACCESS_TOKEN = settings.GENIUS_API_TOKEN

# ===============================================================================
# 🛠️ UTILIDADES
# ===============================================================================

def detect_language_from_text(text: str, min_length: int = 50) -> str:
    """Detecta idioma del texto, default 'en' si falla o es muy corto"""
    try:
        if not text or len(text) < min_length: 
            return "en"
        # Limpiamos headers tipo [Chorus] para no confundir al detector
        clean_lines = [line for line in text.split("\n") if not line.strip().startswith("[")]
        clean_text = "\n".join(clean_lines)[:500] 
        
        if not clean_text.strip():
            return "en"
            
        return detect(clean_text)
    except Exception:
        return "en"

# ===============================================================================
# 🚀 ESTRATEGIA 1: LRCLIB (Prioridad Alta)
# ===============================================================================

def get_lyrics_lrclib(title: str, artist: str) -> Optional[Dict]:
    """
    Busca letras en LRCLIB.net
    Ventajas: Gratis, Open Source, Sin Cloudflare, Muy rápido.
    """
    try:
        url = "https://lrclib.net/api/get"
        params = {
            "artist_name": artist,
            "track_name": title
        }
        
        # Usamos requests normal porque esta API es amigable
        response = requests.get(url, params=params, timeout=8)
        
        if response.status_code == 404:
            return None
            
        response.raise_for_status()
        data = response.json()
        
        plain_lyrics = data.get("plainLyrics")
        if not plain_lyrics:
            return None

        # Detectar idioma
        lang = detect_language_from_text(plain_lyrics)
        lines = [line.strip() for line in plain_lyrics.split("\n") if line.strip()]

        logger.info(f"✅ [LRCLIB] Letra encontrada: {title} ({len(lines)} líneas)")
        
        return {
            "source": "LRCLIB",
            "title": data.get("trackName", title),
            "artist": data.get("artistName", artist),
            "url": None, 
            "lyrics": plain_lyrics,
            "lines": lines,
            "line_count": len(lines),
            "language": lang,
            "synced_lyrics": data.get("syncedLyrics") # 🎁 Guardado para futuro karaoke
        }

    except Exception as e:
        logger.warning(f"⚠️ [LRCLIB] No encontrado o error: {e}")
        return None

# ===============================================================================
# 🛡️ ESTRATEGIA 2: Genius con Stealth Mode (Fallback)
# ===============================================================================

def get_lyrics_genius_advanced(title: str, artist: str) -> Optional[Dict]:
    """
    Intenta rascar Genius simulando ser Chrome 120 (curl_cffi).
    Esto salta la pantalla 'Verify you are human'.
    """
    try:
        # 1. Buscar la URL en la API oficial (esto no suele tener bloqueo fuerte)
        search_url = "https://api.genius.com/search"
        headers = {"Authorization": f"Bearer {GENIUS_ACCESS_TOKEN}"}
        
        resp = requests.get(search_url, params={"q": f"{title} {artist}"}, headers=headers, timeout=5)
        
        if resp.status_code != 200:
            return None
            
        hits = resp.json().get("response", {}).get("hits", [])
        if not hits:
            return None
            
        hit = hits[0]["result"]
        song_url = hit["url"]
        
        logger.info(f"🕵️ [Genius] URL hallada, iniciando extracción stealth: {song_url}")

        # 2. Descargar HTML simulando navegador real (Bypass Cloudflare)
        response = cffi_requests.get(
            song_url, 
            impersonate="chrome120",  # 👈 Aquí ocurre la magia
            timeout=15
        )

        if response.status_code != 200:
            logger.error(f"❌ [Genius] Bloqueo persistente (Status {response.status_code})")
            return None

        # 3. Parsear HTML
        soup = BeautifulSoup(response.content, "html.parser")
        
        # Genius usa contenedores con data-lyrics-container="true"
        lyrics_containers = soup.find_all("div", attrs={"data-lyrics-container": "true"})
        
        text = ""
        if lyrics_containers:
            for container in lyrics_containers:
                for br in container.find_all("br"):
                    br.replace_with("\n")
                text += container.get_text() + "\n"
        else:
            # Fallback a selectores antiguos
            lyrics_div = soup.find("div", class_="lyrics")
            if lyrics_div:
                text = lyrics_div.get_text()
            else:
                logger.warning("⚠️ [Genius] HTML descargado pero estructura desconocida")
                return None

        cleaned_text = text.strip()
        lines = [line.strip() for line in cleaned_text.split("\n") if line.strip()]
        lang = detect_language_from_text(cleaned_text)

        logger.info(f"✅ [Genius] Scraping exitoso: {len(lines)} líneas")

        return {
            "source": "Genius",
            "title": hit["title"],
            "artist": hit["primary_artist"]["name"],
            "url": song_url,
            "lyrics": cleaned_text,
            "lines": lines,
            "line_count": len(lines),
            "language": lang
        }

    except Exception as e:
        logger.error(f"❌ [Genius] Error scraping avanzado: {e}")
        return None

# ===============================================================================
# 🚦 ENTRY POINT (Función Principal)
# ===============================================================================

async def get_song_lyrics(song_title: str, artist_name: str) -> Optional[Dict]:
    """
    Orquestador: Intenta LRCLIB primero, luego Genius.
    """
    logger.info(f"🎵 Buscando letra: '{song_title}' - {artist_name}")

    # Prioridad 1: LRCLIB
    result = get_lyrics_lrclib(song_title, artist_name)
    if result:
        return result
        
    # Prioridad 2: Genius Stealth
    logger.warning("⚠️ LRCLIB falló. Activando protocolo Genius Stealth...")
    result = get_lyrics_genius_advanced(song_title, artist_name)
    
    return result

# Mantener compatibilidad si se llama desde otro lado
async def search_genius_songs(query: str, limit: int = 5) -> List[dict]:
    # Esta función usa la API oficial para BUSCAR (no ver letras), suele funcionar bien con requests normal
    if not GENIUS_ACCESS_TOKEN: return []
    try:
        headers = {"Authorization": f"Bearer {GENIUS_ACCESS_TOKEN}"}
        res = requests.get("https://api.genius.com/search", params={"q": query}, headers=headers)
        res.raise_for_status()
        hits = res.json()["response"]["hits"]
        return [{
            "title": h["result"]["title"], 
            "artist": h["result"]["primary_artist"]["name"],
            "url": h["result"]["url"],
            "image_url": h["result"]["song_art_image_url"]
        } for h in hits[:limit]]
    except:
        return []

# ===============================================================================
# 🛠️ HELPERS (Añadir al final del archivo)
# ===============================================================================

def is_genius_configured() -> bool:
    """Verifica si el token de Genius está presente"""
    return bool(GENIUS_ACCESS_TOKEN)