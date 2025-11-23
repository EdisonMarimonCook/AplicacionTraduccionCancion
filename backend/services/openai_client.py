"""
MÓDULO: Cliente IA para análisis de letras
USANDO: Google Gemini (gratis, sin error 429)
FALLBACK: MOCK si Gemini no funciona
PLAN B: Ollama/Llama2 si Gemini tiene problemas
"""

import logging
from typing import Dict, Optional
from config import settings

logger = logging.getLogger(__name__)

# ================================================================================
# IMPORTAR GEMINI (NUEVO PAQUETE)
# ================================================================================

try:
    import google.genai as genai  # ← NUEVO (en lugar de google.generativeai)
    GEMINI_AVAILABLE = True
    logger.info("✅ Gemini (google-genai) importado correctamente")
except ImportError:
    GEMINI_AVAILABLE = False
    logger.warning("⚠️ Gemini no instalado - usar: pip install google-genai")

# ================================================================================
# INICIALIZAR GEMINI
# ================================================================================

_gemini_model = None

def get_gemini_model():
    """
    Obtiene modelo Gemini (singleton)
    Si falla, intenta MOCK como fallback
    """
    global _gemini_model
    
    if _gemini_model is not None:
        return _gemini_model
    
    if not GEMINI_AVAILABLE:
        logger.error("❌ Gemini no disponible - usando MOCK")
        return None
    
    try:
        # Configurar API key
        if not settings.GEMINI_API_KEY or settings.GEMINI_API_KEY == "tu_gemini_api_key_aqui":
            logger.error("❌ GEMINI_API_KEY no configurada en .env")
            return None
        
        genai.configure(api_key=settings.GEMINI_API_KEY)
        
        # Usar modelo recomendado por Google
        _gemini_model = genai.GenerativeModel('gemini-pro')
        
        logger.info("✅ Gemini modelo 'gemini-pro' inicializado")
        return _gemini_model
        
    except Exception as e:
        logger.error(f"❌ Error inicializando Gemini: {str(e)}")
        return None


# ================================================================================
# MOCK DATA - Fallback si Gemini falla
# ================================================================================

HIPHOP_SLANG_DB = {
    "drops": {
        "meaning": "Momento donde cae la beat",
        "level": "B1",
        "explanation": "En HipHop, cuando baja la música principal con fuerza",
        "example": "When the beat drops, everyone jumps"
    },
    "flow": {
        "meaning": "Manera de rimar y cantar",
        "level": "B1",
        "explanation": "Estilo y ritmo único de cada rapero",
        "example": "Your flow is incredible"
    },
    "bars": {
        "meaning": "Líneas o versos de rap",
        "level": "B1",
        "explanation": "Cada verso o sección del rap",
        "example": "These bars are fire"
    },
    "beat": {
        "meaning": "La música instrumental",
        "level": "A2",
        "explanation": "La base musical sobre la que rapean",
        "example": "The beat is sick"
    },
    "hype": {
        "meaning": "Entusiasmo, emoción",
        "level": "A2",
        "explanation": "Cuando algo emociona o entusiasma mucho",
        "example": "The hype is real"
    },
    "vibe": {
        "meaning": "Sensación, atmósfera",
        "level": "B1",
        "explanation": "El sentimiento o emoción que transmite",
        "example": "I love the vibe of this song"
    },
    "freestyle": {
        "meaning": "Rapear sin preparación previa",
        "level": "B2",
        "explanation": "Improvisar rimas sobre la marcha",
        "example": "He can freestyle really well"
    },
    "cypher": {
        "meaning": "Círculo donde rapean turnándose",
        "level": "B2",
        "explanation": "Tradición HipHop donde artistas rapean en ronda",
        "example": "Let's start a cypher"
    },
    "diss": {
        "meaning": "Insultar o criticar verbalmente",
        "level": "B1",
        "explanation": "Atacar verbalmente a otro artista",
        "example": "He dissed his rival in a song"
    },
    "beef": {
        "meaning": "Conflicto o rivalidad entre artistas",
        "level": "B1",
        "explanation": "Disputa entre dos raperos",
        "example": "There's beef between them"
    }
}

CEFR_LEVELS = {
    "A1": {"difficulty": 1, "color": "#008000", "description": "Principiante"},
    "A2": {"difficulty": 2, "color": "#008000", "description": "Elemental"},
    "B1": {"difficulty": 3, "color": "#FFA500", "description": "Intermedio"},
    "B2": {"difficulty": 4, "color": "#FFA500", "description": "Intermedio Alto"},
    "C1": {"difficulty": 5, "color": "#FF0000", "description": "Avanzado"},
    "C2": {"difficulty": 6, "color": "#FF0000", "description": "Maestría"}
}

# ================================================================================
# FUNCIONES PRINCIPALES - GEMINI CON FALLBACK A MOCK
# ================================================================================

async def highlight_by_level(lyrics: str, user_level: str, language: str = "en") -> Dict:
    """
    📊 RESALTA PALABRAS SEGÚN NIVEL DEL USUARIO
    
    INTENTA:
    1. Gemini (IA real)
    2. MOCK (fallback si Gemini falla)
    """
    
    try:
        logger.info(f"📊 Analizando letra - Nivel: {user_level}, Gemini: {GEMINI_AVAILABLE}")
        
        # Intentar con Gemini
        model = get_gemini_model()
        if model:
            try:
                prompt = f"""
Analiza esta letra de canción y resalta palabras según el nivel CEFR del usuario: {user_level}

LETRA:
{lyrics}

TAREAS:
1. Identifica palabras difíciles para nivel {user_level}
2. Identifica jerga/slang de HipHop
3. Clasifica cada palabra por nivel CEFR (A1, A2, B1, B2, C1, C2)
4. Proporciona explicaciones breves en español

RESPONDE EN JSON:
{{
    "highlighted_words": [
        {{
            "word": "palabra",
            "level": "B1",
            "color": "#FFA500",
            "translation": "traducción",
            "explanation": "explicación breve",
            "is_hiphop_term": true/false
        }}
    ],
    "words_by_level": {{"A1": 0, "A2": 0, "B1": 2, "B2": 0, "C1": 0, "C2": 0}},
    "suggestions": ["sugerencia 1", "sugerencia 2"]
}}
"""
                
                response = model.generate_content(prompt)
                
                # Intentar parsear JSON de la respuesta
                import json
                import re
                
                # Extraer JSON de la respuesta
                json_match = re.search(r'\{.*\}', response.text, re.DOTALL)
                if json_match:
                    result = json.loads(json_match.group())
                    logger.info("✅ Análisis Gemini completado")
                    result["source"] = "GEMINI"
                    return result
                else:
                    logger.warning("⚠️ No se pudo parsear respuesta Gemini - usando MOCK")
                    
            except Exception as e:
                logger.warning(f"⚠️ Error con Gemini: {str(e)} - usando MOCK")
        
        # Fallback a MOCK
        logger.info("📦 Usando análisis MOCK")
        return _mock_highlight_by_level(lyrics, user_level)
        
    except Exception as e:
        logger.error(f"❌ Error en highlight_by_level: {str(e)}")
        return {
            "highlighted_words": [],
            "words_by_level": {},
            "suggestions": ["Error en análisis"],
            "error": str(e),
            "source": "ERROR"
        }


async def identify_hiphop_terms(lyrics: str, language: str = "en") -> Dict:
    """
    🎤 IDENTIFICA JERGA HIPHOP EN LA LETRA
    
    INTENTA:
    1. Gemini (IA real)
    2. MOCK (fallback si Gemini falla)
    """
    
    try:
        logger.info("🎤 Identificando términos HipHop")
        
        # Intentar con Gemini
        model = get_gemini_model()
        if model:
            try:
                prompt = f"""
Identifica términos de jerga/slang de HipHop en esta letra:

LETRA:
{lyrics}

Para cada término encontrado proporciona:
- Término original
- Significado en español
- Contexto donde aparece
- Referencia cultural

RESPONDE EN JSON:
{{
    "terms": [
        {{
            "term": "palabra",
            "meaning": "significado en español",
            "context": "contexto",
            "cultural_reference": "referencia cultural",
            "example": "ejemplo de uso"
        }}
    ],
    "total_terms": 0
}}
"""
                
                response = model.generate_content(prompt)
                
                # Intentar parsear JSON
                import json
                import re
                
                json_match = re.search(r'\{.*\}', response.text, re.DOTALL)
                if json_match:
                    result = json.loads(json_match.group())
                    logger.info(f"✅ Se encontraron {result.get('total_terms', 0)} términos con Gemini")
                    result["source"] = "GEMINI"
                    return result
                else:
                    logger.warning("⚠️ No se pudo parsear respuesta Gemini - usando MOCK")
                    
            except Exception as e:
                logger.warning(f"⚠️ Error con Gemini: {str(e)} - usando MOCK")
        
        # Fallback a MOCK
        logger.info("📦 Usando identificación MOCK")
        return _mock_identify_hiphop_terms(lyrics)
        
    except Exception as e:
        logger.error(f"❌ Error en identify_hiphop_terms: {str(e)}")
        return {
            "terms": [],
            "total_terms": 0,
            "error": str(e),
            "source": "ERROR"
        }


async def analyze_lyrics(lyrics: str, level: str) -> str:
    """
    📖 ANALIZA LETRA Y PROPORCIONA EXPLICACIONES
    
    INTENTA:
    1. Gemini (IA real)
    2. MOCK (fallback si Gemini falla)
    """
    
    try:
        logger.info(f"📖 Analizando letra - Nivel: {level}")
        
        # Intentar con Gemini
        model = get_gemini_model()
        if model:
            try:
                prompt = f"""
Analiza esta letra de canción para un estudiante de inglés nivel {level} (CEFR):

LETRA:
{lyrics}

Proporciona:
1. Explicación general del tema
2. Palabras clave y su significado
3. Expresiones idiomáticas identificadas
4. Contexto cultural/histórico
5. Consejos para aprender inglés con esta canción

Responde en español, de forma clara y educativa.
"""
                
                response = model.generate_content(prompt)
                
                analysis = response.text
                logger.info("✅ Análisis Gemini completado")
                return f"{analysis}\n\n[Fuente: Gemini AI]"
                
            except Exception as e:
                logger.warning(f"⚠️ Error con Gemini: {str(e)} - usando MOCK")
        
        # Fallback a MOCK
        logger.info("📦 Usando análisis MOCK")
        return _mock_analyze_lyrics(lyrics, level)
        
    except Exception as e:
        logger.error(f"❌ Error en analyze_lyrics: {str(e)}")
        return f"Error: {str(e)}"


# ================================================================================
# FUNCIONES MOCK - Fallback
# ================================================================================

def _mock_highlight_by_level(lyrics: str, user_level: str) -> Dict:
    """MOCK: Resalta palabras"""
    highlighted_words = []
    words_by_level = {k: 0 for k in ["A1", "A2", "B1", "B2", "C1", "C2"]}
    
    words = [w.strip(',.!?;:"').lower() for w in lyrics.split()]
    
    for word in set(words):
        if word in HIPHOP_SLANG_DB:
            term = HIPHOP_SLANG_DB[word]
            level = term.get("level", "B1")
            
            highlighted_words.append({
                "word": word,
                "level": level,
                "color": CEFR_LEVELS[level]["color"],
                "translation": term.get("meaning", ""),
                "explanation": f"[HipHop] {term.get('explanation', '')}",
                "example_in_context": term.get("example", ""),
                "is_hiphop_term": True
            })
            
            words_by_level[level] += 1
    
    return {
        "highlighted_words": highlighted_words,
        "words_by_level": words_by_level,
        "suggestions": ["Basado en análisis MOCK"],
        "source": "MOCK_AI"
    }


def _mock_identify_hiphop_terms(lyrics: str) -> Dict:
    """MOCK: Identifica términos HipHop"""
    terms = []
    words = [w.strip(',.!?;:"').lower() for w in lyrics.split()]
    
    for word in set(words):
        if word in HIPHOP_SLANG_DB:
            term_data = HIPHOP_SLANG_DB[word]
            terms.append({
                "term": word,
                "meaning": term_data.get("meaning", ""),
                "context": f"Aparece en la canción",
                "cultural_reference": f"Término típico de HipHop",
                "example": term_data.get("example", "")
            })
    
    return {
        "terms": terms,
        "total_terms": len(terms),
        "source": "MOCK_AI"
    }


def _mock_analyze_lyrics(lyrics: str, level: str) -> str:
    """MOCK: Analiza letra"""
    return f"""
ANÁLISIS DE LETRA - Nivel {level}

Este fragmento de letra contiene vocabulario variado para tu nivel.

PALABRAS CLAVE IDENTIFICADAS:
- Se han detectado términos de HipHop
- El idioma es accesible para nivel {level}

RECOMENDACIÓN DE ESTUDIO:
1. Aprende los términos clave de HipHop
2. Escucha la canción varias veces
3. Lee la letra mientras escuchas

[Fuente: MOCK AI - Análisis simulado]
"""


# ================================================================================
# FUNCIONES AUXILIARES
# ================================================================================

def is_gemini_available() -> bool:
    """Indica si Gemini está disponible"""
    model = get_gemini_model()
    return model is not None

def get_ai_provider() -> str:
    """Retorna qué IA está en uso"""
    if is_gemini_available():
        return "GEMINI"
    else:
        return "MOCK_AI"