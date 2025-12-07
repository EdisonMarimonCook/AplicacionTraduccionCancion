"""
ARCHIVO: routers/progress.py
PROPÓSITO: Endpoints de progreso y estadísticas
RUTAS: /progress/stats, /progress/by-language
USUARIO: Frontend/Mobile

"""
"""
ROUTER: Progreso
PROPÓSITO: Calcular estadísticas para el perfil del usuario.
"""

import logging
from fastapi import APIRouter, Depends
from models import User
from routers.auth import get_current_user
from database import db

logger = logging.getLogger(__name__)

# ⚠️ OJO: Prefijo singular 'user' para coincidir con Android (@GET /api/v1/user/progress)
router = APIRouter(prefix="/api/v1/user", tags=["Progress"])

@router.get("/progress")
async def get_user_progress(current_user: User = Depends(get_current_user)):
    """
    📊 Devuelve estadísticas calculadas en tiempo real.
    """
    try:
        # 1. Obtener todas las palabras del usuario
        # (Usamos la función cruda de DB para no filtrar)
        items = await db.get_user_dictionary(current_user.id)
        
        total_words = len(items)
        
        # 2. Calcular racha (Mock para MVP o lógica real si tienes fechas)
        # Aquí ponemos un valor fijo o calculado simple
        current_streak = 1 
        
        # 3. Estadísticas por idioma (MVP: Asumimos 'en')
        # Si tuvieras 'source_lang' en el diccionario, agruparíamos aquí.
        stats_by_lang = {
            "en": {
                "words_learned": total_words,
                "estimated_level": "B1" # Placeholder: Podrías calcularlo según la dificultad media
            }
        }

        return {
            "total_words_learned": total_words,
            "current_streak": current_streak,
            "stats_by_language": stats_by_lang,
            "status": "success"
        }
    except Exception as e:
        logger.error(f"Error calculando progreso: {e}")
        return {
            "total_words_learned": 0,
            "current_streak": 0,
            "stats_by_language": {},
            "status": "error"
        }