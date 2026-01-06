"""
MÓDULO: Cliente de Letras Híbrido (LRCLIB + Genius Fallback)
PROPÓSITO: Obtener letras de forma robusta evitando bloqueos de Cloudflare.
ESTRATEGIA:
1. Intentar API abierta LRCLIB (Sin bloqueo, rápido).
2. Si falla, intentar Genius con curl_cffi (Impersonate Browser).
"""

import logging
from typing import Optional, List, Dict
import re
import requests  # Para LRCLIB (API amigable)
from curl_cffi import requests as cffi_requests  # 🚀 EL ARMA SECRETA ANTI-CLOUDFLARE
from bs4 import BeautifulSoup
from langdetect import detect, LangDetectException
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
from config import settings

logger = logging.getLogger(__name__)

# Token de Genius (Solo se usa para la búsqueda inicial en la API oficial)
GENIUS_ACCESS_TOKEN = settings.GENIUS_API_TOKEN

# ===============================================================================
# 🛠️ UTILIDADES
# ===============================================================================

def normalize_search_query(text: str) -> str:
    """
    Normaliza nombres de canciones/artistas para mejorar búsquedas.
    Elimina: "feat.", "Remix", "Remake Ver.", "(Official)", etc.
    """
    # Lista de patrones a eliminar
    patterns = [
        r'\s*-\s*Remake Ver\.?',
        r'\s*-\s*Remaster',
        r'\s*\(Remix\)',
        r'\s*\(Official.*?\)',
        r'\s*\(Lyric.*?\)',
        r'\s*\(Audio\)',
        r'\s*feat\..*',
        r'\s*ft\..*',
        r'\s*&.*',  # Colaboraciones después de &
    ]
    
    result = text
    for pattern in patterns:
        result = re.sub(pattern, '', result, flags=re.IGNORECASE)
    
    # Limpiar espacios extras
    result = ' '.join(result.split())
    return result.strip()

def validate_lyrics_match(lyrics_data: Dict, spotify_metadata: Optional[Dict] = None) -> Dict:
    """
    🔍 VALIDACIÓN AVANZADA: Verifica que la letra corresponda a la canción correcta.
    Compara: duración (si tiene timestamps), título, artista, álbum.
    Retorna: lyrics_data con campo 'confidence' (high/medium/low)
    """
    confidence = "high"  # Por defecto confiamos
    warnings = []
    
    # Si no tenemos metadata de Spotify, no podemos validar
    if not spotify_metadata:
        lyrics_data["confidence"] = "medium"
        lyrics_data["validation_warnings"] = ["No Spotify metadata available for validation"]
        return lyrics_data
    
    # 1. Validar duración (si existen synced_lyrics con timestamps)
    synced = lyrics_data.get("synced_lyrics")
    spotify_duration_ms = spotify_metadata.get("duration_ms")
    
    if synced and spotify_duration_ms:
        # Extraer último timestamp de las letras sincronizadas
        # Formato: "[00:23.45] Letra..." -> extraer 23.45 segundos
        last_timestamp = 0
        for line in synced.split("\n"):
            match = re.search(r'\[(\d{2}):(\d{2}\.\d{2})\]', line)
            if match:
                minutes, seconds = match.groups()
                timestamp_ms = (int(minutes) * 60 + float(seconds)) * 1000
                last_timestamp = max(last_timestamp, timestamp_ms)
        
        # Comparar: si difiere más de 30 segundos, es sospechoso
        duration_diff = abs(last_timestamp - spotify_duration_ms)
        if duration_diff > 30000:  # 30 segundos
            confidence = "low"
            warnings.append(f"Duration mismatch: lyrics end at {last_timestamp/1000:.1f}s, song is {spotify_duration_ms/1000:.1f}s")
            logger.warning(f"⚠️ Duración sospechosa: letra termina en {last_timestamp/1000:.1f}s pero canción dura {spotify_duration_ms/1000:.1f}s")
    
    # 2. Validar título (similitud básica)
    lyrics_title = lyrics_data.get("title", "").lower()
    spotify_title = spotify_metadata.get("title", "").lower()
    
    # Normalizar ambos para comparar
    lyrics_title_norm = normalize_search_query(lyrics_title)
    spotify_title_norm = normalize_search_query(spotify_title)
    
    # Si no hay palabras en común, es sospechoso
    lyrics_words = set(lyrics_title_norm.split())
    spotify_words = set(spotify_title_norm.split())
    common_words = lyrics_words & spotify_words
    
    if not common_words and confidence == "high":
        confidence = "medium"
        warnings.append(f"Title mismatch: '{lyrics_title}' vs '{spotify_title}'")
        logger.warning(f"⚠️ Títulos sin palabras en común: '{lyrics_title}' vs '{spotify_title}'")
    
    # 3. Validar artista (al menos una palabra en común)
    lyrics_artist = lyrics_data.get("artist", "").lower()
    spotify_artist = spotify_metadata.get("artist", "").lower()
    
    artist_words_lyrics = set(lyrics_artist.split())
    artist_words_spotify = set(spotify_artist.split())
    common_artists = artist_words_lyrics & artist_words_spotify
    
    if not common_artists and confidence != "low":
        confidence = "medium"
        warnings.append(f"Artist mismatch: '{lyrics_artist}' vs '{spotify_artist}'")
        logger.warning(f"⚠️ Artistas sin palabras en común: '{lyrics_artist}' vs '{spotify_artist}'")
    
    # 4. Validar álbum (si está disponible)
    spotify_album = spotify_metadata.get("album")
    if spotify_album:
        # LRCLib puede tener info de álbum en algunos casos
        lyrics_album = lyrics_data.get("albumName")
        if lyrics_album and lyrics_album.lower() not in spotify_album.lower():
            warnings.append(f"Album mismatch: '{lyrics_album}' vs '{spotify_album}'")
    
    # Añadir resultados de validación
    lyrics_data["confidence"] = confidence
    lyrics_data["validation_warnings"] = warnings
    
    if confidence == "low":
        logger.error(f"❌ BAJA CONFIANZA en letra encontrada: {warnings}")
    elif confidence == "medium":
        logger.warning(f"⚠️ CONFIANZA MEDIA en letra: {warnings}")
    else:
        logger.info(f"✅ Alta confianza en letra encontrada")
    
    return lyrics_data

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

@retry(
    stop=stop_after_attempt(3),  # 3 intentos para casos difíciles
    wait=wait_exponential(multiplier=1, min=1, max=3),  # Max 3s de espera entre reintentos
    retry=retry_if_exception_type((requests.exceptions.SSLError, requests.exceptions.ConnectionError)),
    reraise=True
)
def _lrclib_request(url: str, params: dict) -> requests.Response:
    """Helper con retry automático para errores SSL/conexión temporales"""
    return requests.get(url, params=params, timeout=10)  # 🔥 Aumentado a 10s

def get_lyrics_lrclib(title: str, artist: str) -> Optional[Dict]:
    """
    Busca letras en LRCLIB.net con normalización y retry automático.
    Máximo 3 variaciones para manejar artistas múltiples.
    Ventajas: Gratis, Open Source, Sin Cloudflare, Muy rápido.
    """
    # 🔥 3 variaciones para manejar casos como "Bowling For Soup" vs "Jaret Reddick"
    variations = [
        (title, artist),  # Original (siempre primero)
        (normalize_search_query(title), normalize_search_query(artist)),  # Normalizados
        (normalize_search_query(title), artist.split(',')[0].split('&')[0].strip()),  # Solo primer artista
    ]
    
    for idx, (attempt_title, attempt_artist) in enumerate(variations, 1):
        try:
            url = "https://lrclib.net/api/get"
            params = {
                "artist_name": attempt_artist,
                "track_name": attempt_title
            }
            
            logger.info(f"🔍 [LRCLIB {idx}/3] '{attempt_title}' - {attempt_artist}")
            
            # Usar helper con retry automático (2 intentos max, 10s timeout)
            response = _lrclib_request(url, params)
            
            if response.status_code == 404:
                continue  # Probar siguiente variación
                
            response.raise_for_status()
            data = response.json()
            
            plain_lyrics = data.get("plainLyrics")
            if not plain_lyrics:
                continue

            # Detectar idioma
            lang = detect_language_from_text(plain_lyrics)
            lines = [line.strip() for line in plain_lyrics.split("\n") if line.strip()]

            logger.info(f"✅ [LRCLIB] Letra encontrada: {attempt_title} ({len(lines)} líneas)")
            
            return {
                "source": "LRCLIB",
                "title": data.get("trackName", title),
                "artist": data.get("artistName", artist),
                "url": None, 
                "lyrics": plain_lyrics,
                "lines": lines,
                "line_count": len(lines),
                "language": lang,
                "synced_lyrics": data.get("syncedLyrics"),
                "albumName": data.get("albumName"),  # 🔍 Metadata para validación
                "duration": data.get("duration"),  # Duración en segundos
                "instrumental": data.get("instrumental", False)
            }

        except (requests.exceptions.SSLError, requests.exceptions.ConnectionError) as e:
            # Si después de 2 reintentos sigue fallando, probar siguiente variación
            logger.warning(f"⚠️ [LRCLIB] Error de conexión (reintentado 2 veces): {type(e).__name__}")
            continue
        except Exception as e:
            logger.warning(f"⚠️ [LRCLIB] Error en búsqueda: {type(e).__name__}")
            continue
    
    # Si ninguna variación funcionó
    logger.warning(f"⚠️ [LRCLIB] No se encontró letra después de {len(variations)} intentos")
    return None

# ===============================================================================
# 🛡️ ESTRATEGIA 2: Genius API para obtener metadata exacta
# ===============================================================================

def get_genius_metadata(title: str, artist: str) -> Optional[Dict]:
    """
    Busca la canción en Genius API y retorna metadata exacta (título/artista oficiales).
    No hace scraping, solo consulta la API.
    Valida que el resultado sea relevante antes de retornarlo.
    """
    try:
        search_url = "https://api.genius.com/search"
        headers = {"Authorization": f"Bearer {GENIUS_ACCESS_TOKEN}"}
        
        resp = requests.get(search_url, params={"q": f"{title} {artist}"}, headers=headers, timeout=10)
        
        if resp.status_code != 200:
            return None
            
        hits = resp.json().get("response", {}).get("hits", [])
        if not hits:
            return None
            
        hit = hits[0]["result"]
        genius_title = hit["title"]
        genius_artist = hit["primary_artist"]["name"]
        
        # 🔥 Validar relevancia: al menos una palabra del título original debe coincidir
        title_words = set(title.lower().split())
        genius_title_words = set(genius_title.lower().split())
        
        if not title_words & genius_title_words:  # Sin intersección
            logger.warning(f"⚠️ [Genius API] Resultado irrelevante: '{genius_title}' vs '{title}'")
            return None
        
        logger.info(f"✅ [Genius API] Metadata encontrada: {genius_title} - {genius_artist}")
        
        return {
            "title": genius_title,
            "artist": genius_artist,
            "url": hit["url"]
        }
    except Exception as e:
        logger.warning(f"⚠️ [Genius API] Error obteniendo metadata: {e}")
        return None

# ===============================================================================
# 🛡️ ESTRATEGIA 3: Genius Scraping con Stealth Mode (Último recurso)
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
        
        resp = requests.get(search_url, params={"q": f"{title} {artist}"}, headers=headers, timeout=10)
        
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
            timeout=10  # Timeout reducido
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

async def get_song_lyrics(song_title: str, artist_name: str, spotify_metadata: Optional[Dict] = None) -> Optional[Dict]:
    """
    Orquestador optimizado con 3 niveles + validación:
    1. LRClib con datos originales
    2. Genius API (metadata) → Reintentar LRClib con datos exactos
    3. Genius Scraping (último recurso)
    4. Validación de match usando metadata de Spotify
    """
    logger.info(f"🎵 Buscando letra: '{song_title}' - {artist_name}")

    # Nivel 1: LRClib con datos originales
    result = get_lyrics_lrclib(song_title, artist_name)
    if result:
        # Validar con metadata de Spotify si está disponible
        return validate_lyrics_match(result, spotify_metadata)
    
    # Nivel 2: Obtener metadata exacta de Genius y reintentar LRClib
    logger.warning("⚠️ LRClib falló. Obteniendo metadata de Genius para reintento...")
    genius_meta = get_genius_metadata(song_title, artist_name)
    
    if genius_meta:
        # Reintentar LRClib con título/artista exactos de Genius
        logger.info(f"🔄 Reintentando LRClib con datos de Genius: {genius_meta['title']} - {genius_meta['artist']}")
        result = get_lyrics_lrclib(genius_meta['title'], genius_meta['artist'])
        if result:
            return validate_lyrics_match(result, spotify_metadata)
    
    # Nivel 3: Scraping de Genius (último recurso)
    logger.warning("⚠️ Última opción: Activando protocolo Genius Stealth...")
    result = get_lyrics_genius_advanced(song_title, artist_name)
    
    if result:
        return validate_lyrics_match(result, spotify_metadata)
    
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