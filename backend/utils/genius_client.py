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

logger = logging.getLogger(__name__)

# ✅ OBTENER EL TOKEN DE SETTINGS
GENIUS_ACCESS_TOKEN = settings.GENIUS_API_TOKEN

# Crear cliente de Genius
genius = lg.Genius(
    access_token=GENIUS_ACCESS_TOKEN,
    skip_non_songs=True,
    excluded_terms=["(Remix)", "(Cover)"]
)

# ===============================================================================
# FUNCIONES PRINCIPALES
# ===============================================================================

async def get_song_lyrics(song_title: str, artist_name: str) -> Optional[Dict]:
    """
    🎵 EXTRAE LA LETRA DE UNA CANCIÓN
    
    LÓGICA:
    1. Recibe título y artista
    2. Busca en Genius API
    3. Si existe, retorna la letra
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
        
        result = {
            "title": song.title,
            "artist": song.artist,
            "url": song.url,
            "lyrics": raw_lyrics,
            "lines": lines,
            "line_count": len(lines)
        }
        
        logger.info(f"✅ Letra encontrada: {song.title} ({len(lines)} líneas)")
        
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
                    "lines": []
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
        
        # ✅ USAR EL TOKEN DEFINIDO ARRIBA
        response = requests.get(
            "https://api.genius.com/search",
            params={"q": query},
            headers={"Authorization": f"Bearer {GENIUS_ACCESS_TOKEN}"},
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