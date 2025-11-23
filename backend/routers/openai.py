from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from services.openai_client import analyze_lyrics_vocab, translate_word
from schemas import AnalyzeRequest, TranslateWordRequest

router = APIRouter(prefix="/api/v1/openai", tags=["OpenAI"])

@router.post("/analyze-lyrics")
async def analyze(req: AnalyzeRequest):
    if not req.lyrics.strip():
        raise HTTPException(status_code=400, detail="lyrics required")
    items = await analyze_lyrics_vocab(req.lyrics, req.level)
    if len(items) == 1 and isinstance(items[0], dict) and items[0].get("error"):
        #Si el modelo nos devuelve error
        raise HTTPException(status_code=502, detail={"message": "model_parse_error", "raw": items[0].get("raw")})
    return {"items": items}

@router.post("/translate-word")
async def translate_word_endpoint(req: TranslateWordRequest):
    if not req.word.strip():
        raise HTTPException(status_code=400, detail="word required")
    res = await translate_word(req.word.strip(), req.source_lang, req.target_lang)
    if res.get("error"):
        raise HTTPException(status_code=502, detail={"message": "model_parse_error", "raw": res.get("raw")})
    return {"result": res}