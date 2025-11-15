from typing import Optional
import asyncio
from openai import OpenAI
from config import settings

_client: Optional[OpenAI] = None

def get_client() -> OpenAI:
    global _client
    if _client is None:
        _client = OpenAI(api_key=settings.OPENAI_API_KEY)
    return _client

async def analyze_lyrics(lyrics: str, level: str) -> str:
    client = get_client()
    prompt = (
        f"Eres un asistente que adapta explicaciones al nivel {level}. "
        "Explica vocabulario útil y ejemplos para este fragmento de letra:\n\n"
        f"{lyrics}"
    )

    def sync_call():
        resp = client.responses.create(
            model="gpt-4o-mini",
            input=prompt,
            max_tokens=800
        )
        # Intentar obtener texto de salida de forma segura
        try:
            return getattr(resp, "output_text", None) or str(resp)
        except Exception:
            return str(resp)

    return await asyncio.to_thread(sync_call)