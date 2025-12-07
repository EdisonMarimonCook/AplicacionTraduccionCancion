"""
MÓDULO: Cliente Gemini IA (MusicTransIAtor v3.1)
PROPÓSITO: Análisis contextual con ejemplos para Flashcards y clasificación.
"""

import logging
import json
from typing import Optional
from google.genai import Client
from config import settings
from routers.schemas import HighlightWordsResponse # Asegúrate de que este import funcione

logger = logging.getLogger(__name__)

# ===== CONFIGURACIÓN =====
MODEL_NAME = "gemini-2.5-flash-lite" 

_client: Optional[Client] = None

def get_gemini_client() -> Client:
    global _client
    if _client is None:
        _client = Client(api_key=settings.GEMINI_API_KEY)
        logger.info(f"✅ Cliente Gemini inicializado. Modelo: {MODEL_NAME}")
    return _client

async def highlight_by_level(lyrics: str, user_level: str = "B1", native_lang: str = "es") -> HighlightWordsResponse:
    """
    Analiza la letra y extrae vocabulario con EJEMPLOS y CONTEXTO.
    """
    client = get_gemini_client()
    
    # Mapeo de idiomas para el prompt
    lang_map = {"es": "Spanish", "en": "English", "fr": "French", "de": "German", "ja": "Japanese"}
    native_lang_name = lang_map.get(native_lang, "Spanish")

    prompt = f"""
    Act as an expert language tutor. The user has a CEFR level of {user_level}.
    Native Language of user: {native_lang_name}.
    
    Analyze these song lyrics:
    "{lyrics}"

    Identify:
    1. Single words suitable for {user_level} level (vocabulary building).
    2. Idioms, slang, or phrasal verbs (Expressions).

    CRITICAL OUTPUT RULES:
    - Contextual Translation: Translate based on the specific meaning in these lyrics.
    - Explanation: Explain WHY it means that in this context (in {native_lang_name}).
    - Example: Provide a very short usage sentence (can be from lyrics or new) for Flashcards.
    - Recommended: Mark 'true' if it's a key term for {user_level}.
    - Type: For words use 'noun', 'verb', 'adj'. For expressions use 'expression'.

    Respond STRICTLY with this JSON structure (no markdown):
    {{
      "detected_language": "en",
      "words": [
        {{
          "word": "term",
          "type": "noun/verb/adj",
          "translation": "traducción contextual",
          "explanation": "explicación breve",
          "example": "frase corta de ejemplo", 
          "difficulty": "{user_level}",
          "color": "orange",
          "recommended": true
        }}
      ],
      "expressions": [
        {{
          "expression": "idiom or phrase",
          "type": "expression",
          "translation": "traducción",
          "explanation": "significado",
          "example": "ejemplo de uso",
          "difficulty": "B2",
          "color": "orange",
          "recommended": true
        }}
      ],
      "suggestions": ["song recommendation"]
    }}
    """

    try:
        response = client.models.generate_content(
            model=MODEL_NAME,
            contents=prompt,
            config={
                "response_mime_type": "application/json"
            }
        )
        
        # Parseamos y validamos con Pydantic
        data = json.loads(response.text)
        return HighlightWordsResponse(**data)

    except Exception as e:
        logger.error(f"❌ Error en Gemini: {str(e)}")
        # Fallback de emergencia
        return HighlightWordsResponse(
            detected_language="en", words=[], expressions=[], suggestions=[]
        )

async def identify_hiphop_terms(lyrics: str, native_lang: str = "es") -> dict:
    # (Puedes dejar tu función antigua aquí si la usas para otra cosa, 
    # pero highlight_by_level ya cubre expresiones)
    return {}