"""
MÓDULO: Cliente Gemini IA (MusicTransIAtor v3.0 MVP)
PROPÓSITO: Análisis contextual, multilingüe y soporte nativo para el usuario.
"""

import asyncio
import logging
import json
from typing import Dict, Optional
from google.genai import Client
from google.genai import types 
from config import settings

logger = logging.getLogger(__name__)

# ===== CONFIGURACIÓN =====
# Usamos gemini-2.0-flash: Balance perfecto entre velocidad, coste y razonamiento contextual
MODEL_NAME = "gemini-2.0-flash" 

_client: Optional[Client] = None

def get_gemini_client() -> Client:
    """Obtiene cliente Gemini (singleton)."""
    global _client
    if _client is None:
        _client = Client(api_key=settings.GEMINI_API_KEY)
        logger.info(f"✅ Cliente Gemini inicializado. Modelo: {MODEL_NAME}")
    return _client

# ===============================================================================
# UTILIDAD: Generador Robusto con Retry y JSON Nativo
# ===============================================================================

async def _generate_content_with_retry(
    prompt: str, 
    require_json: bool = False,
    max_retries: int = 3
) -> str:
    """
    Envía prompt a Gemini con lógica de reintento, espera exponencial y soporte JSON.
    """
    client = get_gemini_client()
    
    # Configuración: Forzamos JSON si es necesario
    config = types.GenerateContentConfig(
        response_mime_type="application/json" if require_json else "text/plain",
        temperature=0.2 if require_json else 0.7
    )

    base_delay = 1.0 # Segundos de espera inicial

    for attempt in range(max_retries):
        try:
            # ✅ Llamada asíncrona nativa
            response = await client.aio.models.generate_content(
                model=MODEL_NAME,
                contents=prompt,
                config=config
            )
            return response.text

        except Exception as e:
            error_str = str(e).lower()
            # Manejo de Rate Limits (429) o sobrecarga
            if "429" in error_str or "resource exhausted" in error_str or "quota" in error_str:
                if attempt < max_retries - 1:
                    wait_time = base_delay * (2 ** attempt) # Backoff: 1s, 2s, 4s...
                    logger.warning(f"⚠️ Gemini Rate Limit. Reintentando en {wait_time}s...")
                    await asyncio.sleep(wait_time)
                    continue
            
            logger.error(f"❌ Error crítico Gemini (Intento {attempt+1}): {str(e)}")
            raise e 

# ===============================================================================
# FUNCIÓN PRINCIPAL: Resaltar palabras (JSON Contextual + Nativo)
# ===============================================================================

async def highlight_by_level(
    lyrics: str, 
    user_level: str, 
    native_lang: str = "es"
) -> Dict:
    """
    Extrae palabras y expresiones basándose en el CONTEXTO de la canción.
    Las explicaciones se dan en el idioma nativo del usuario.
    """
    try:
        logger.info(f"🎨 Resaltando (Contextual) para nivel {user_level} en {native_lang}")
        
        prompt = (
            f"Eres un analista lingüístico experto en música.\n"
            f"Tu alumno tiene nivel {user_level} (CEFR) y habla '{native_lang}'.\n\n"
            f"INSTRUCCIONES CRÍTICAS:\n"
            f"1. Analiza la letra y detecta el idioma origen.\n"
            f"2. Extrae palabras ('words') y expresiones ('expressions') para este nivel.\n"
            f"3. ⚠️ REGLA DE ORO (CONTEXTO): No uses definiciones de diccionario genéricas.\n"
            f"   - La 'translation' y 'explanation' deben reflejar el significado EXACTO en esta canción.\n"
            f"   - Si es slang o metáfora, explica ese sentido figurado.\n"
            f"4. IDIOMA DE SALIDA: 'translation' y 'explanation' deben estar en '{native_lang}'.\n"
            f"5. DICCIONARIO: Marca 'recommended': true si la palabra es muy útil para aprender.\n\n"
            f"LETRA:\n{lyrics}\n\n"
            f"Responde SOLO JSON con esta estructura exacta:\n"
            f"{{\n"
            f"  \"detected_language\": \"código (en, es, fr, etc)\",\n"
            f"  \"words\": [\n"
            f"    {{\n"
            f"      \"word\": \"término original\",\n"
            f"      \"type\": \"noun/verb/adj\",\n"
            f"      \"translation\": \"traducción en {native_lang}\",\n"
            f"      \"explanation\": \"explicación contextual en {native_lang}\",\n"
            f"      \"difficulty\": \"{user_level}\",\n"
            f"      \"color\": \"orange\",\n"
            f"      \"recommended\": true\n"
            f"    }}\n"
            f"  ],\n"
            f"  \"expressions\": [\n"
            f"    {{\n"
            f"      \"expression\": \"frase completa\",\n"
            f"      \"type\": \"idiom/phrasal/slang\",\n"
            f"      \"translation\": \"traducción en {native_lang}\",\n"
            f"      \"explanation\": \"significado figurado en {native_lang}\",\n"
            f"      \"difficulty\": \"{user_level}\",\n"
            f"      \"color\": \"orange\",\n"
            f"      \"recommended\": true\n"
            f"    }}\n"
            f"  ],\n"
            f"  \"suggestions\": [\"Consejo breve en {native_lang}\"]\n"
            f"}}"
        )
        
        json_str = await _generate_content_with_retry(prompt, require_json=True)
        return json.loads(json_str)
            
    except Exception as e:
        logger.error(f"❌ Fallback highlight: {e}")
        return {
            "detected_language": "unknown",
            "words": [], 
            "expressions": [], 
            "suggestions": ["Service momentarily unavailable."]
        }

# ===============================================================================
# FUNCIÓN SECUNDARIA: Identificar HipHop Terms
# ===============================================================================

async def identify_hiphop_terms(lyrics: str, native_lang: str = "es") -> Dict:
    """
    Identifica slang de HipHop y lo explica en el idioma del usuario.
    """
    try:
        logger.info(f"🎤 Identificando Slang HipHop (Explicación en {native_lang})")
        
        prompt = (
            f"Analiza esta letra buscando jerga (slang), referencias culturales de HipHop o doble sentido.\n"
            f"Explica los significados en '{native_lang}'.\n"
            f"LETRA:\n{lyrics}\n\n"
            f"Responde SOLO JSON:\n"
            f"{{\n"
            f"  \"language_detected\": \"code\",\n"
            f"  \"terms\": [\n"
            f"    {{\n"
            f"      \"term\": \"palabra/frase\",\n"
            f"      \"meaning\": \"significado en {native_lang}\",\n"
            f"      \"context\": \"ejemplo o matiz de uso en {native_lang}\",\n"
            f"      \"cultural_reference\": \"referencia (si aplica) explicada en {native_lang}\"\n"
            f"    }}\n"
            f"  ],\n"
            f"  \"total_terms\": int\n"
            f"}}"
        )
        
        json_str = await _generate_content_with_retry(prompt, require_json=True)
        return json.loads(json_str)

    except Exception as e:
        logger.error(f"❌ Fallback HipHop: {e}")
        return {"language_detected": "unknown", "terms": [], "total_terms": 0}