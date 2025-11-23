from typing import Optional, List, Dict
import asyncio
import json
from functools import lru_cache
from openai import OpenAI
from config import settings

@lru_cache(maxsize=1)
def get_openai_client() -> OpenAI:
    # Singleton para el cliente OpenAI
    return OpenAI(api_key=settings.OPENAI_API_KEY)

def _extract_text(resp) -> str:
    # Adaptar según la SDK: intentamos varias rutas posibles
    text = getattr(resp, "output_text", None)
    if text:
        return text
    try:
        # resp.choices[0].message.content (forma del chat)
        return resp.choices[0].message.content
    except Exception:
        try:
            return str(resp)
        except Exception:
            return ""

async def analyze_lyrics_vocab(lyrics: str, level: str, max_items: int = 15) -> List[Dict]:
    """
    Extrae palabras/frases útiles del fragmento `lyrics` según `level` y devuelve
    una lista JSON con objetos: { text, type, translation, note, example }.
    """
    client = get_openai_client()
    prompt = (
        "Eres un extractor de vocabulario para estudiantes de inglés.\n\n"
        f"Dado este fragmento y el nivel {level}, selecciona hasta {max_items} palabras o "
        "frases útiles para aprender. Para cada ítem devuelve un objeto JSON con los campos:\n"
        "text (la palabra o frase en inglés), type (word o phrase), translation (español), "
        "note (una frase sobre por qué es útil), example (ejemplo corto en inglés).\n\n"
        "Devuelve SOLO un array JSON válido (ej: [ { ... }, ... ]) sin explicaciones adicionales.\n\n"
        f"FRAGMENT:\n{lyrics}\n\nRespuesta:"
    )

    def sync_call():
        resp = client.responses.create(model="gpt-4o-mini", input=prompt, max_tokens=800, temperature=0.2)
        return _extract_text(resp)

    # Hilo separado para no bloquear FastAPI
    raw = await asyncio.to_thread(sync_call)

    # Intentar parsear JSON; si no se puede, devolver información de error para debug
    try:
        parsed = json.loads(raw)
        if isinstance(parsed, list):
            return parsed
        if isinstance(parsed, dict) and "items" in parsed and isinstance(parsed["items"], list):
            return parsed["items"]
        # intentar extraer bloque JSON si viene texto extra
        start = raw.find("[")
        end = raw.rfind("]") + 1
        if start != -1 and end != -1:
            maybe = raw[start:end]
            parsed2 = json.loads(maybe)
            if isinstance(parsed2, list):
                return parsed2
        raise ValueError("Formato JSON inesperado")
    except Exception:
        return [{"error": "failed_to_parse_model_output", "raw": raw}]

async def translate_word(word: str, source_lang: str = "en", target_lang: str = "es") -> Dict:
    """
    Traduce una sola palabra y devuelve { translation, pos, short_note, examples: [ {en, es}, ... ] }
    """
    client = get_openai_client()
    prompt = (
        f"Traduce la palabra '{word}' del {source_lang} al {target_lang}.\n\n"
        "Devuelve SOLO un objeto JSON con campos: translation (string), pos (part of speech), "
        "short_note (1-2 frases sobre uso o matices), examples (array con 1-3 objetos {en, es}).\n\nRespuesta:"
    )

    def sync_call():
        resp = client.responses.create(model="gpt-4o-mini", input=prompt, max_tokens=250, temperature=0.0)
        return _extract_text(resp)

    raw = await asyncio.to_thread(sync_call)

    try:
        parsed = json.loads(raw)
        if isinstance(parsed, dict):
            return parsed
        # intentar extraer objeto JSON en caso de texto adicional
        start = raw.find("{")
        end = raw.rfind("}") + 1
        if start != -1 and end != -1:
            maybe = raw[start:end]
            parsed2 = json.loads(maybe)
            if isinstance(parsed2, dict):
                return parsed2
        raise ValueError("Formato JSON inesperado")
    except Exception:
        return {"error": "failed_to_parse_model_output", "raw": raw}