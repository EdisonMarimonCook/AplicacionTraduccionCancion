"""
ROUTER: Canciones
PROPÓSITO: Búsqueda unificada y Listas MVP
"""

import logging
from typing import List, Optional
from fastapi import APIRouter, Depends, Query

from models import User
from routers.auth import get_current_user
from utils.spotify import search_songs_spotify, get_grammy_songs

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/songs", tags=["Songs"])

@router.get("/search", response_model=List[dict])
async def search_songs(
    query: str, 
    current_user: User = Depends(get_current_user)
):
    """
    🔍 Búsqueda UNIFICADA.
    Usa Spotify para encontrar la canción (con carátula y audio).
    Luego el frontend usará Title + Artist para pedir la letra.
    """
    if not query:
        return []
    return search_songs_spotify(query)

@router.get("/top-grammy", response_model=List[dict])
async def get_top_grammy(
    lang: str = "en",
    current_user: User = Depends(get_current_user)
):
    """
    🏆 Devuelve listas curadas (Grammys/Hits) para el MVP.
    Mucho mejor que un Top 10 genérico.
    """
    return get_grammy_songs(lang)