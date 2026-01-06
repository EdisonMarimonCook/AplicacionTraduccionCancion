"""
MÓDULO: Cliente Gemini IA (MusicTransIAtor v3.1)
PROPÓSITO: Análisis contextual con ejemplos para Flashcards y clasificación.
"""

import logging
import json
import asyncio
from typing import Optional
from google.genai import Client
from config import settings
from routers.schemas import HighlightWordsResponse # Asegúrate de que este import funcione
from fastapi import HTTPException, status

logger = logging.getLogger(__name__)

# ===== CONFIGURACIÓN =====
MODEL_NAME = "gemini-2.5-flash-lite"
GEMINI_TIMEOUT = 120  # 🔥 Timeout para canciones largas (2 minutos) 

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
    Usa timeout de 120s para canciones largas.
    """
    client = get_gemini_client()
    
    # Mapeo de idiomas para el prompt
    lang_map = {
        "es": "Spanish", "en": "English", "fr": "French", "de": "German", 
        "it": "Italian", "pt": "Portuguese",
        "ja": "Japanese", "zh": "Chinese", "ko": "Korean"
    }
    native_lang_name = lang_map.get(native_lang, "Spanish")

    prompt = f"""
    Act as an expert language tutor. The user has a CEFR level of {user_level}.
    Native Language of user: {native_lang_name}.
    
    Analyze these song lyrics:
    "{lyrics}"

    Identify:
    1. Words or vocabulary units suitable for {user_level} level (vocabulary building).
    2. Idioms, slang, or phrasal verbs (Expressions).

    CRITICAL TOKENIZATION RULES:
    - **COMPOUND WORDS:** If you select a compound word (e.g., "bloodstains", "sunflower"), YOU MUST RETURN THE FULL WORD. 
    - **DO NOT SPLIT:** If the text says "bloodstains", return "bloodstains". DO NOT return "stains".
    - **EXACT MATCH:** The extracted word must exist EXACTLY as written in the lyrics.
    - **NON-SPACED LANGUAGES (Japanese/Chinese/Korean):** Extract meaningful vocabulary units even if they appear connected. For example:
      * Japanese: Extract individual kanji compounds (漢字), words in hiragana/katakana that form semantic units.
      * Chinese: Extract 词 (words) which may be 1-4 characters forming a semantic unit.
      * Korean: Extract words separated by spacing, but recognize compound words.

    CRITICAL OUTPUT RULES:
    - Contextual Translation: Translate based on the specific meaning in these lyrics.
    - Explanation: Explain WHY it means that in this context (in {native_lang_name}).
    - Example: Provide a very short usage sentence.
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
        # 🔥 Wrapper async para la llamada síncrona de Gemini con timeout
        async def _generate_content():
            return await asyncio.to_thread(
                client.models.generate_content,
                model=MODEL_NAME,
                contents=prompt,
                config={"response_mime_type": "application/json"}
            )
        
        # Aplicar timeout de 120 segundos para canciones largas
        response = await asyncio.wait_for(_generate_content(), timeout=GEMINI_TIMEOUT)
        
        # Parseamos JSON crudo
        data = json.loads(response.text)
        
        # 🔥 LIMPIEZA: Gemini a veces pone "word" en expressions
        if "expressions" in data:
            cleaned_expressions = []
            for expr in data["expressions"]:
                if "word" in expr and "expression" not in expr:
                    expr["expression"] = expr.pop("word")  # Mover word → expression
                cleaned_expressions.append(expr)
            data["expressions"] = cleaned_expressions
        
        # 🔥 VALIDACIÓN: Asegurar que todas las palabras tengan campos requeridos
        if "words" in data:
            cleaned_words = []
            for word in data["words"]:
                # Campos obligatorios con valores por defecto
                if "word" not in word or not word["word"]:
                    logger.warning(f"⚠️ Palabra sin 'word' field, saltando: {word}")
                    continue
                
                word.setdefault("translation", f"[Sin traducción para: {word['word']}]")
                word.setdefault("type", "noun")
                word.setdefault("explanation", "")
                word.setdefault("example", "")
                word.setdefault("difficulty", user_level)
                word.setdefault("recommended", False)
                
                # Asignar colores correctos
                if word.get("recommended"):
                    word["color"] = "green"  # ✅ Palabras recomendadas en VERDE
                else:
                    word["color"] = "orange"  # Palabras normales en naranja
                
                cleaned_words.append(word)
            data["words"] = cleaned_words
        
        # 🔥 VALIDACIÓN: Asegurar que todas las expresiones tengan campos requeridos
        if "expressions" in data:
            cleaned_expressions = []
            for expr in data["expressions"]:
                # Campos obligatorios con valores por defecto
                if "expression" not in expr or not expr["expression"]:
                    logger.warning(f"⚠️ Expresión sin 'expression' field, saltando: {expr}")
                    continue
                
                expr.setdefault("translation", f"[Sin traducción para: {expr['expression']}]")
                expr.setdefault("type", "expression")
                expr.setdefault("explanation", "")
                expr.setdefault("example", "")
                expr.setdefault("difficulty", "B2")
                expr.setdefault("recommended", False)
                expr["color"] = "red"  # 🔥 Expresiones SIEMPRE en ROJO
                
                cleaned_expressions.append(expr)
            data["expressions"] = cleaned_expressions
        
        # Asegurar que existan las listas (aunque estén vacías)
        data.setdefault("words", [])
        data.setdefault("expressions", [])
        data.setdefault("suggestions", [])
        data.setdefault("detected_language", "en")
        
        # Validamos con Pydantic
        return HighlightWordsResponse(**data)

    except asyncio.TimeoutError:
        logger.error(f"❌ Timeout de Gemini ({GEMINI_TIMEOUT}s) - Canción demasiado larga")
        raise HTTPException(
            status_code=status.HTTP_504_GATEWAY_TIMEOUT,
            detail=f"La canción es demasiado larga. Gemini tardó más de {GEMINI_TIMEOUT}s en responder."
        )
    except json.JSONDecodeError as e:
        logger.error(f"❌ Gemini envió JSON inválido: {str(e)}")
        logger.debug(f"Respuesta cruda: {response.text[:500]}")
    except Exception as e:
        logger.error(f"❌ Error en Gemini: {str(e)}")
        
        # 🔥 Fallback con datos de DEMO para testing cuando Gemini falla
        logger.warning("⚠️ Usando datos de demo - Gemini no disponible")
        return HighlightWordsResponse(
            detected_language="en", 
            words=[
                {
                    "word": "demo",
                    "type": "noun",
                    "translation": "demostración",
                    "explanation": "Palabra de prueba mientras Gemini está sobrecargado",
                    "example": "This is a demo word",
                    "difficulty": user_level,
                    "color": "orange",
                    "recommended": True
                },
                {
                    "word": "testing",
                    "type": "verb",
                    "translation": "probando",
                    "explanation": "Otra palabra de demo para verificar colores",
                    "example": "We are testing the app",
                    "difficulty": user_level,
                    "color": "green",
                    "recommended": False
                }
            ], 
            expressions=[
                {
                    "expression": "work in progress",
                    "type": "expression",
                    "translation": "trabajo en progreso",
                    "explanation": "Expresión de demo mientras esperamos a Gemini",
                    "example": "This feature is work in progress",
                    "difficulty": "B2",
                    "color": "red",
                    "recommended": True
                }
            ], 
            suggestions=["Demo mode - Gemini temporarily unavailable"]
        )

async def identify_hiphop_terms(lyrics: str, native_lang: str = "es") -> dict:
    # (Puedes dejar tu función antigua aquí si la usas para otra cosa, 
    # pero highlight_by_level ya cubre expresiones)
    return {}

# Verificación de configuración
if not settings.GEMINI_API_KEY:
    logger.error("❌ GEMINI_API_KEY no está configurada en .env")
    raise ValueError("GEMINI_API_KEY no configurada. Revisa tu archivo .env")