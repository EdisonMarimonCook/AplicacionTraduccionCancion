"""
ROUTER: Audio Streaming
Endpoint para obtener enlaces directos de reproducción.
"""
from fastapi import APIRouter, HTTPException, Query
from utils.audio_extractor import get_youtube_audio_url
from .schemas import AudioStreamResponse  # 👈 IMPORTANTE: Importamos el schema

router = APIRouter(prefix="/api/v1/audio", tags=["Audio Stream"])

# 👇 AÑADIMOS 'response_model' AQUÍ
@router.get("/stream", response_model=AudioStreamResponse)
async def get_audio_stream(
    q: str = Query(..., description="Consulta de búsqueda (ej. 'Numb Linkin Park')")
):
    """
    🔍 Recibe 'Artista - Canción' y devuelve la URL directa para ExoPlayer.
    """
    if not q:
        raise HTTPException(status_code=400, detail="Falta el parámetro de búsqueda (q)")

    result = get_youtube_audio_url(q)
    
    if not result:
        raise HTTPException(status_code=404, detail="No se encontró audio para esta búsqueda")
    
    # FastAPI se encarga de validar que 'result' coincida con 'AudioStreamResponse'
    return result