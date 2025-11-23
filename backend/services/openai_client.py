from typing import Optional, Dict
import asyncio
import json
import logging
from openai import OpenAI
from config import settings

logger = logging.getLogger(__name__)

_client: Optional[OpenAI] = None

def get_client() -> OpenAI:
    global _client
    if _client is None:
        _client = OpenAI(api_key=settings.OPENAI_API_KEY)
    return _client

async def analyze_lyrics(lyrics: str, level: str) -> str:
    """
    Analiza letra y proporciona explicaciones adaptadas al nivel.
    """
    client = get_client()
    
    prompt = (
        f"Eres un asistente que adapta explicaciones al nivel {level}. "
        "Explica vocabulario útil y ejemplos para este fragmento de letra:\n\n"
        f"{lyrics}"
    )

    try:
        response = client.chat.completions.create(  # ✅ CORRECTO
            model="gpt-3.5-turbo",
            messages=[{"role": "user", "content": prompt}],
            max_tokens=800,
            temperature=0.7
        )
        
        return response.choices[0].message.content
    
    except Exception as e:
        logger.error(f"❌ Error en analyze_lyrics: {str(e)}")
        raise


def highlight_by_level(lyrics: str, user_level: str, language: str = "en") -> Dict:
    """
    Resalta palabras en la letra según el nivel del usuario.
    Devuelve palabras con colores, explicaciones y contexto.
    
    Args:
        lyrics: Texto completo de la letra
        user_level: Nivel del usuario (A1, A2, B1, B2, C1, C2)
        language: Idioma del texto (en, es, fr, etc)
    
    Returns:
        {
            "highlighted_words": [
                {
                    "word": "drops",
                    "level": "B1",
                    "color": "#FFA500",
                    "translation": "caídas/momentos clave",
                    "explanation": "En HipHop, momento donde cae la beat principal",
                    "example_in_context": "When the beat drops..."
                }
            ],
            "words_by_level": {"A1": 2, "B1": 5, "C1": 1},
            "suggestions": ["Enfócate en B1 words"]
        }
    """
    
    try:
        prompt = f"""Analiza esta letra de canción en {language} para un estudiante de nivel {user_level}.

LETRA:
{lyrics}

INSTRUCCIONES:
1. Identifica SOLO palabras difíciles para un estudiante de nivel {user_level}
2. Para CADA palabra, determina su nivel real (A1, A2, B1, B2, C1, C2)
3. Asigna COLOR según dificultad relativa al nivel del usuario:
   - Si es FÁCIL para él (A1-A2): COLOR VERDE (#008000)
   - Si es MEDIO (B1-B2): COLOR NARANJA (#FFA500)
   - Si es DIFÍCIL (C1-C2): COLOR ROJO (#FF0000)
4. Proporciona TRADUCCIÓN, EXPLICACIÓN BREVE y EJEMPLO EN CONTEXTO

IMPORTANTE:
- Si la palabra es HipHop slang, menciónalo en la explicación
- Las palabras muy fáciles para su nivel NO incluir
- Responder SOLO con JSON válido, sin markdown, sin explicaciones extra

FORMATO JSON REQUERIDO:
{{
    "highlighted_words": [
        {{
            "word": "drops",
            "level": "B1",
            "color": "#FFA500",
            "translation": "caídas/momentos clave",
            "explanation": "En HipHop, momento donde cae la beat principal",
            "example_in_context": "When the beat drops, everyone jumps"
        }}
    ],
    "words_by_level": {{"A1": 0, "A2": 0, "B1": 5, "B2": 2, "C1": 1, "C2": 0}},
    "suggestions": ["Aprende jerga de HipHop", "Enfócate en B1 words"]
}}
"""
        
        response = get_client().chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7,
            max_tokens=2000
        )
        
        response_text = response.choices[0].message.content.strip()
        
        # Limpiar markdown si viene así
        if response_text.startswith("```"):
            response_text = response_text.split("```")[1]
            if response_text.startswith("json"):
                response_text = response_text[4:]
        
        # Parse JSON
        result = json.loads(response_text)
        return result
        
    except json.JSONDecodeError as e:
        logger.error(f"JSON decode error in highlight_by_level: {e}")
        return {
            "highlighted_words": [],
            "words_by_level": {},
            "error": "No se pudo procesar la respuesta de OpenAI",
            "suggestions": []
        }
    except Exception as e:
        logger.error(f"Error in highlight_by_level: {str(e)}")
        raise


def identify_hiphop_terms(lyrics: str, language: str = "en") -> Dict:
    """
    Identifica términos y jerga de HipHop en la letra.
    
    Args:
        lyrics: Texto completo de la letra
        language: Idioma del texto
    
    Returns:
        {
            "terms": [
                {
                    "term": "flow",
                    "meaning": "Manera de rimar y cantar",
                    "context": "Tu flow es increíble",
                    "cultural_reference": "Elemento clave del HipHop"
                }
            ],
            "total_terms": 5
        }
    """
    
    try:
        prompt = f"""Analiza esta letra de HipHop en {language} e identifica TODOS los términos, jerga y slang típicos de HipHop.

LETRA:
{lyrics}

INSTRUCCIONES:
1. Busca palabras o frases que son jerga/slang de HipHop
2. Para CADA término, proporciona:
   - Significado en términos simples
   - Contexto (cómo se usa en la canción)
   - Referencia cultural (por qué es importante en HipHop)
3. Responder SOLO con JSON válido, sin markdown

FORMATO JSON:
{{
    "terms": [
        {{
            "term": "flow",
            "meaning": "Manera de rimar y pronunciar el rap",
            "context": "Tu flow es increíble = Tu forma de rapear es increíble",
            "cultural_reference": "Elemento fundamental del HipHop, marca el estilo de cada artista"
        }}
    ],
    "total_terms": 5
}}
"""
        
        response = get_client().chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.7,
            max_tokens=1500
        )
        
        response_text = response.choices[0].message.content.strip()
        
        # Limpiar markdown
        if response_text.startswith("```"):
            response_text = response_text.split("```")[1]
            if response_text.startswith("json"):
                response_text = response_text[4:]
        
        result = json.loads(response_text)
        return result
        
    except json.JSONDecodeError as e:
        logger.error(f"JSON decode error in identify_hiphop_terms: {e}")
        return {"terms": [], "total_terms": 0, "error": str(e)}
    except Exception as e:
        logger.error(f"Error in identify_hiphop_terms: {str(e)}")
        raise