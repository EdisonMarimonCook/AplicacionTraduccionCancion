"""
ROUTER: Audio Streaming & TTS
Endpoints para obtener enlaces de reproducción y pronunciación TTS.
"""
from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import StreamingResponse
from utils.audio_extractor import get_youtube_audio_url
from .schemas import AudioStreamResponse
from gtts import gTTS
import io
import logging

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/audio", tags=["Audio Stream"])

# Mapeo de códigos de idioma (ISO 639-1)
LANGUAGE_MAP = {
    "en": "en",
    "es": "es",
    "fr": "fr",
    "de": "de",
    "pt": "pt",
    "it": "it",
    "ja": "ja",
    "ko": "ko",
    "zh": "zh-CN",  # Mandarín simplificado
    "zh-TW": "zh-TW"  # Chino tradicional
}

@router.get("/stream", response_model=AudioStreamResponse)
async def get_audio_stream(
    q: str = Query(..., description="Consulta de búsqueda (ej. 'Numb Linkin Park')")
):
    """
    🔍 Recibe 'Artista - Canción' y devuelve la URL directa para ExoPlayer.
    """
    if not q:
        raise HTTPException(status_code=400, detail="Falta el parámetro de búsqueda (q)")

    result = get_youtube_audio_url(q)
    
    if not result:
        raise HTTPException(status_code=404, detail="No se encontró audio para esta búsqueda")
    
    return result


@router.get("/tts", summary="Generar audio TTS")
async def generate_tts(
    text: str = Query(..., description="Palabra o expresión a pronunciar"),
    language: str = Query(default="en", description="Código de idioma (en, es, ja, ko, zh...)"),
    slow: bool = Query(default=False, description="Si True, pronunciación más lenta")
):
    """
    🔊 Genera audio de pronunciación usando Google Text-to-Speech.
    
    **Ejemplo**: 
    ```
    GET /api/v1/audio/tts?text=こんにちは&language=ja
    GET /api/v1/audio/tts?text=beautiful&language=en&slow=true
    ```
    """
    try:
        # Validar que el texto no esté vacío
        if not text or text.strip() == "":
            raise HTTPException(status_code=400, detail="El parámetro 'text' no puede estar vacío")
        
        # Mapear el idioma
        lang_code = LANGUAGE_MAP.get(language, "en")
        
        # Generar TTS
        logger.info(f"🔊 Generando TTS: text='{text}', lang={lang_code}, slow={slow}")
        tts = gTTS(text=text, lang=lang_code, slow=slow)
        
        # Guardar en buffer de memoria
        audio_buffer = io.BytesIO()
        tts.write_to_fp(audio_buffer)
        audio_buffer.seek(0)
        
        # Filename seguro (solo ASCII)
        safe_filename = "tts_audio.mp3"
        
        # Devolver como stream de audio
        return StreamingResponse(
            audio_buffer,
            media_type="audio/mpeg",
            headers={
                "Content-Disposition": f'inline; filename="{safe_filename}"',
                "Cache-Control": "public, max-age=86400"  # Cache de 24 horas
            }
        )
        
    except Exception as e:
        logger.error(f"❌ Error generando TTS: {e}")
        raise HTTPException(
            status_code=500,
            detail=f"Error al generar audio TTS: {str(e)}"
        )