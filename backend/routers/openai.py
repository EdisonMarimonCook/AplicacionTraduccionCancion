from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from openai_client import analyze_lyrics

router = APIRouter(prefix="/api/v1/openai", tags=["OpenAI"])

class AnalyzeRequest(BaseModel):
    lyrics: str
    level: str

@router.post("/analyze-lyrics")
async def analyze(req: AnalyzeRequest):
    if not req.lyrics.strip():
        raise HTTPException(status_code=400, detail="lyrics required")
    analysis = await analyze_lyrics(req.lyrics, req.level)
    return {"analysis": analysis}