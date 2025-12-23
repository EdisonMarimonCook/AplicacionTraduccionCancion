"""
MÓDULO: Cliente de Genius API
PROPÓSITO: Extraer letras de canciones desde Genius
USA: lyricsgenius (librería oficial)
"""

import logging
from typing import Optional, List, Dict
import lyricsgenius as lg
from config import settings
import requests
from langdetect import detect, LangDetectException  # 🆕 AGREGAR

logger = logging.getLogger(__name__)

# ✅ OBTENER EL TOKEN DE SETTINGS
GENIUS_ACCESS_TOKEN = settings.GENIUS_API_TOKEN

# 🛡️ Headers para evitar bloqueo de Cloudflare (Chrome 124 - versión real y reciente)
BROWSER_HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.9,es;q=0.8",
    "Accept-Encoding": "gzip, deflate, br, zstd",
    "DNT": "1",
    "Connection": "keep-alive",
    "Upgrade-Insecure-Requests": "1",
    "Sec-Fetch-Dest": "document",
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "none",
    "Sec-Fetch-User": "?1",
    "Sec-CH-UA": '"Chromium";v="124", "Google Chrome";v="124", "Not-A.Brand";v="99"',
    "Sec-CH-UA-Mobile": "?0",
    "Sec-CH-UA-Platform": '"Windows"',
    "Cache-Control": "max-age=0"
}

# Crear cliente de Genius con headers de navegador
genius = lg.Genius(
    access_token=GENIUS_ACCESS_TOKEN,
    skip_non_songs=True,
    excluded_terms=["(Remix)", "(Cover)"],
    timeout=15,
    retries=3
)

# Aplicar headers personalizados a la sesión de requests
genius._session.headers.update(BROWSER_HEADERS)

# ===============================================================================
# 🆕 FUNCIÓN: Detectar idioma
# ===============================================================================

def detect_language_from_text(text: str, min_length: int = 100) -> str:
    """
    🌍 DETECTA EL IDIOMA DE UN TEXTO USANDO LANGDETECT
    
    PARÁMETROS:
    - text: Texto a analizar
    - min_length: Mínimo de caracteres para detectar (default: 100)
    
    RETORNA:
    - Código idioma ISO 639-1 (en, es, fr, de, etc.)
    
    SOPORTA: 55+ idiomas
    """
    
    try:
        # Validar longitud mínima
        if len(text) < min_length:
            logger.warning(f"⚠️  Texto muy corto ({len(text)} chars), usando default 'en'")
            return "en"
        
        # Limpiar texto (remover metadata de Genius)
        cleaned_text = text.replace("[", "").replace("]", "").strip()
        
        if len(cleaned_text) < min_length:
            logger.warning(f"⚠️  Texto limpio muy corto, usando default 'en'")
            return "en"
        
        # Usar solo primeras líneas (más eficiente)
        lines = cleaned_text.split("\n")
        sample = "\n".join(lines[:30])
        
        # Detectar idioma
        detected = detect(sample)
        logger.info(f"✅ Idioma detectado: {detected}")
        
        return detected
    
    except LangDetectException:
        logger.warning(f"⚠️  No se pudo detectar idioma con langdetect, usando default 'en'")
        return "en"
    except Exception as e:
        logger.error(f"❌ Error detectando idioma: {str(e)}")
        return "en"

# ===============================================================================
# FUNCIONES PRINCIPALES
# ===============================================================================

async def get_song_lyrics(song_title: str, artist_name: str) -> Optional[Dict]:
    """
    🎵 EXTRAE LA LETRA DE UNA CANCIÓN
    
    LÓGICA:
    1. Recibe título y artista
    2. Busca en Genius API
    3. Si existe, retorna la letra + idioma detectado
    4. Si no existe, retorna None
    """
    
    try:
        logger.info(f"🔍 Buscando letra en Genius: '{song_title}' - {artist_name}")
        
        song = genius.search_song(
            title=song_title,
            artist=artist_name
        )
        
        if song is None:
            logger.warning(f"⚠️  Letra no encontrada: {song_title} - {artist_name}")
            return None
        
        raw_lyrics = song.lyrics
        lines = [line.strip() for line in raw_lyrics.split("\n") if line.strip()]
        
        # 🆕 DETECTAR IDIOMA
        detected_language = detect_language_from_text(raw_lyrics)
        
        result = {
            "title": song.title,
            "artist": song.artist,
            "url": song.url,
            "lyrics": raw_lyrics,
            "lines": lines,
            "line_count": len(lines),
            "language": detected_language  # 🆕 AGREGAR IDIOMA
        }
        
        logger.info(f"✅ Letra encontrada: {song.title} ({len(lines)} líneas, idioma: {detected_language})")
        
        return result
    
    except Exception as e:
        logger.error(f"❌ Error extrayendo letra de Genius: {str(e)}")
        return None

# ===============================================================================

async def get_song_verses(song_title: str, artist_name: str) -> Optional[List[Dict]]:
    """
    📖 EXTRAE LOS VERSÍCULOS (SECCIONES) DE UNA CANCIÓN
    """
    
    try:
        song_data = await get_song_lyrics(song_title, artist_name)
        
        if not song_data:
            return None
        
        lyrics = song_data["lyrics"]
        sections = []
        
        current_section = None
        
        for line in song_data["lines"]:
            if line.startswith("[") and line.endswith("]"):
                if current_section:
                    sections.append(current_section)
                
                header = line.strip("[]")
                parts = header.split()
                
                section_type = parts[0]
                section_number = parts[1] if len(parts) > 1 else "1"
                
                current_section = {
                    "type": section_type,
                    "number": section_number,
                    "text": "",
                    "lines": [],
                    "language": song_data.get("language", "en") 
                }
            
            else:
                if current_section:
                    current_section["lines"].append(line)
        
        if current_section:
            current_section["text"] = "\n".join(current_section["lines"])
            sections.append(current_section)
        
        logger.info(f"✅ Se extrajeron {len(sections)} secciones")
        
        return sections
    
    except Exception as e:
        logger.error(f"❌ Error extrayendo versículos: {str(e)}")
        return None

# ===============================================================================

async def get_line_lyrics(song_title: str, artist_name: str) -> Optional[List[str]]:
    """
    📝 RETORNA SOLO LAS LÍNEAS (sin headers de secciones)
    """
    
    try:
        song_data = await get_song_lyrics(song_title, artist_name)
        
        if not song_data:
            return None
        
        lines = [
            line for line in song_data["lines"]
            if not (line.startswith("[") and line.endswith("]"))
        ]
        
        logger.info(f"✅ Se obtuvieron {len(lines)} líneas de letra")
        
        return lines
    
    except Exception as e:
        logger.error(f"❌ Error obteniendo líneas: {str(e)}")
        return None

# ===============================================================================

async def search_genius_songs(query: str, limit: int = 5) -> List[dict]:
    """
    🔍 Busca canciones en Genius y devuelve MÚLTIPLES RESULTADOS
    """
    
    if not is_genius_configured():
        logger.warning("⚠️  Genius no está configurado")
        return []
    
    try:
        logger.info(f"🔍 Buscando en Genius: {query}")
        
        # ✅ USAR EL TOKEN DEFINIDO ARRIBA CON HEADERS DE NAVEGADOR
        headers = {
            "Authorization": f"Bearer {GENIUS_ACCESS_TOKEN}",
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Accept": "application/json"
        }
        response = requests.get(
            "https://api.genius.com/search",
            params={"q": query},
            headers=headers,
            timeout=10
        )
        response.raise_for_status()
        
        data = response.json()
        hits = data.get("response", {}).get("hits", [])
        
        if not hits:
            logger.warning(f"⚠️  Sin resultados en Genius para: {query}")
            return []
        
        # Formatear resultados
        results = []
        for hit in hits[:limit]:
            song = hit.get("result", {})
            
            result_item = {
                "title": song.get("title", "Unknown"),
                "artist": song.get("primary_artist", {}).get("name", "Unknown"),
                "url": song.get("url", ""),
                "image_url": song.get("song_art_image_url", ""),
                "genius_id": song.get("id")
            }
            results.append(result_item)
        
        logger.info(f"✅ Se encontraron {len(results)} canciones en Genius")
        
        return results
    
    except requests.exceptions.RequestException as e:
        logger.error(f"❌ Error conectando con Genius: {str(e)}")
        return []
    except Exception as e:
        logger.error(f"❌ Error procesando resultados: {str(e)}")
        return []

# ===============================================================================

def is_genius_configured() -> bool:
    """
    ✅ VERIFICA SI GENIUS ESTÁ CONFIGURADO
    """
    
    token = getattr(settings, "GENIUS_API_TOKEN", None)
    
    if not token or token == "your-genius-api-token":
        logger.warning("⚠️  GENIUS_API_TOKEN no configurado. Lyrics no disponibles.")
        return False
    
    logger.info("✅ Genius API configurado correctamente")
    return True