from fastapi import APIRouter, HTTPException, Depends, status
from services.openai_client import analyze_lyrics, highlight_by_level, identify_hiphop_terms
from routers.schemas import (
    AnalyzeRequest,
    HighlightWordsRequest,
    HighlightWordsResponse,
    WordHighlight,
    HipHopTermRequest,
    HipHopTermResponse,
    HipHopTerm
)
from routers.auth import get_current_user  
from routers.schemas import AnalyzeLyricsRequest
from models import User
from fastapi.logger import logger

router = APIRouter(prefix="/api/v1/openai", tags=["OpenAI"])

# ===============================================================================
# ENDPOINT 1: Analizar letra completa (EXISTENTE - MANTENER)
# ===============================================================================

@router.post("/analyze-lyrics", response_model=dict)
async def analyze_lyrics_openai(
    request: AnalyzeLyricsRequest,
    current_user: User = Depends(get_current_user)  # ✅ Usuario autenticado
):
    """Analiza letra con IA."""
    try:
        # ✅ SACAMOS native_lang del usuario
        native_lang = getattr(current_user, 'native_language', 'es')
        
        logger.info(f"🤖 Analizando OpenAI: {request.title} (idioma usuario: {native_lang})")
        
        result = await highlight_by_level(
            lyrics=request.lyrics,
            user_level=request.user_level,
            native_lang=native_lang  # ← Del usuario
        )
        
        return {
            "title": request.title,
            "artist": request.artist,
            "user_level": request.user_level,
            "user_native_language": native_lang,
            "language": result.get("detected_language", "en"),
            "highlighted_words": result.get("words", []),
            "expressions": result.get("expressions", []),
            "suggestions": result.get("suggestions", []),
            "status": "success"
        }
    
    except Exception as e:
        logger.error(f"❌ Error: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


# ===============================================================================
# ENDPOINT 2: Resaltar palabras por nivel (NUEVO - PRINCIPAL)
# ===============================================================================

@router.post("/highlight-words", response_model=HighlightWordsResponse)
async def highlight_words(
    request: HighlightWordsRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Resalta palabras difíciles en la letra según el nivel del usuario.
    
    Características:
    - Identifica palabras por nivel de dificultad
    - Asigna colores: verde (A1-A2), naranja (B1-B2), rojo (C1-C2)
    - Proporciona explicaciones y contexto
    - Detecta términos de HipHop
    
    Args:
        lyrics: Letra completa de la canción
        user_level: Nivel del usuario (A1-C2)
        language: Idioma de la letra (en, es, fr, etc)
    
    Returns:
        - highlighted_words: Lista de palabras resaltadas con colores
        - total_words: Total de palabras resaltadas
        - words_by_level: Conteo de palabras por nivel
        - suggestions: Sugerencias de estudio
    
    Example:
        {
            "lyrics": "When the beat drops, everyone jumps...",
            "user_level": "B1",
            "language": "en"
        }
    """
    
    if not request.lyrics.strip():
        raise HTTPException(status_code=400, detail="lyrics required")
    
    try:
        result = highlight_by_level(
            lyrics=request.lyrics,
            user_level=request.user_level,
            language=request.language
        )
        
        # Convertir resultado a HighlightWordsResponse
        highlighted_words = [
            WordHighlight(**word) for word in result.get("highlighted_words", [])
        ]
        
        return HighlightWordsResponse(
            highlighted_words=highlighted_words,
            total_words=len(highlighted_words),
            words_by_level=result.get("words_by_level", {}),
            suggestions=result.get("suggestions", [])
        )
        
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error highlighting words: {str(e)}"
        )


# ===============================================================================
# ENDPOINT 3: Identificar términos de HipHop (NUEVO)
# ===============================================================================

@router.post("/hiphop-terms", response_model=HipHopTermResponse)
async def identify_terms(
    request: HipHopTermRequest,
    current_user: dict = Depends(get_current_user)
):
    """
    Identifica y explica términos y jerga típica de HipHop en la letra.
    
    Características:
    - Detecta slang y expresiones de HipHop
    - Explica significado en contexto
    - Proporciona referencias culturales
    - Ayuda a entender la cultura del HipHop
    
    Args:
        lyrics: Letra completa de la canción
        language: Idioma de la letra (en, es, fr, etc)
    
    Returns:
        - terms: Lista de términos de HipHop encontrados
        - total_terms: Total de términos identificados
    
    Example:
        {
            "lyrics": "Check my flow, I'm dropping bars...",
            "language": "en"
        }
    
    Respuesta:
        {
            "terms": [
                {
                    "term": "flow",
                    "meaning": "Manera de rimar y cantar",
                    "context": "Check my flow = Escucha mi forma de cantar",
                    "cultural_reference": "Elemento clave del HipHop"
                }
            ],
            "total_terms": 2
        }
    """
    
    if not request.lyrics.strip():
        raise HTTPException(status_code=400, detail="lyrics required")
    
    try:
        result = identify_hiphop_terms(
            lyrics=request.lyrics,
            language=request.language
        )
        
        # Convertir resultado a HipHopTermResponse
        terms = [
            HipHopTerm(**term) for term in result.get("terms", [])
        ]
        
        return HipHopTermResponse(
            terms=terms,
            total_terms=len(terms)
        )
        
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error identifying HipHop terms: {str(e)}"
        )