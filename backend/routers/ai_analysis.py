"""
ROUTER: AI Analysis
PROPÓSITO: Endpoints de inteligencia artificial (Gemini)
"""

import logging
from fastapi import APIRouter, Depends, HTTPException, status
from models import User
from routers.auth import get_current_user
from routers.schemas import (
    AnalyzeLyricsRequest, 
    HighlightWordsResponse
)
from services.gemini_client import highlight_by_level, identify_hiphop_terms
from utils.genius_client import detect_language_from_text # ✅ Necesario para la detección

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/ai", tags=["AI Analysis"])

# ===============================================================================
# ENDPOINT 1: ANALIZAR LETRA (Highlight Contextual + Nivel Inteligente)
# ===============================================================================
@router.post("/analyze", response_model=HighlightWordsResponse)
async def analyze_lyrics_endpoint(
    request: AnalyzeLyricsRequest,
    current_user: User = Depends(get_current_user)
):
    """
    🧠 Analiza la letra usando Gemini 2.0 Flash.
    
    LÓGICA INTELIGENTE:
    1. Detecta el idioma de la letra.
    2. Busca si el usuario está aprendiendo ese idioma.
    3. Si lo está, USA SU NIVEL REAL de la base de datos.
    4. Si no, usa el nivel por defecto del request (B1).
    """
    try:
        # 1. Detectar idioma de la canción (ej: 'en')
        song_lang = detect_language_from_text(request.lyrics)
        
        # 2. Obtener idioma nativo del usuario (para las explicaciones)
        native_lang = getattr(current_user, 'native_language', 'es')
        
        # 3. 🧠 LÓGICA SMART: Buscar nivel real del usuario para este idioma
        real_level = request.user_level # Valor por defecto (o el que venga del JSON)
        
        if current_user.learning_languages:
            # Buscamos si el usuario estudia el idioma de la canción
            for lang_profile in current_user.learning_languages:
                if lang_profile.language == song_lang:
                    real_level = lang_profile.level
                    logger.info(f"🎓 Nivel usuario detectado para {song_lang}: {real_level}")
                    break
        
        logger.info(f"🧠 AI Analyzing: '{request.title}' ({song_lang}) | Nivel: {real_level} | Explicación: {native_lang}")
        
        # 4. Llamar a Gemini con el nivel calculado
        result = await highlight_by_level(
            lyrics=request.lyrics,
            user_level=real_level, # ✅ Usamos el nivel de la BD
            native_lang=native_lang
        )
        
        return result

    except Exception as e:
        logger.error(f"❌ Error en análisis AI: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE, 
            detail="AI Service temporarily unavailable"
        )

# ===============================================================================
# ENDPOINT 2: HIPHOP TERMS (Slang)
# ===============================================================================
@router.post("/hiphop-terms", response_model=dict)
async def identify_hiphop_endpoint(
    request: AnalyzeLyricsRequest,
    current_user: User = Depends(get_current_user)
):
    """
    🎤 Identifica slang y referencias culturales.
    """
    try:
        native_lang = getattr(current_user, 'native_language', 'es')
        
        result = await identify_hiphop_terms(
            lyrics=request.lyrics,
            native_lang=native_lang
        )
        return result

    except Exception as e:
        logger.error(f"❌ Error en HipHop terms: {str(e)}")
        raise HTTPException(status_code=503, detail="AI Service unavailable")