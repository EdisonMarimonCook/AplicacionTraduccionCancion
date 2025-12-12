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
from datetime import datetime, timedelta
from fastapi import APIRouter, Depends
from models import User
from routers.auth import get_current_user
from database import db

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/user", tags=["Progress"])

@router.get("/progress")
async def get_user_progress(current_user: User = Depends(get_current_user)):
    """
    📊 Devuelve estadísticas calculadas en tiempo real.
    """
    try:
        # 1. Obtener todas las palabras del usuario
        items = await db.get_user_dictionary(current_user.id)
        total_words = len(items)
        
        # 2. Obtener datos del usuario (para racha)
        user_data = await db.get_user_by_email(current_user.email)
        
        # 3. Calcular racha
        current_streak = user_data.get("current_streak", 0)
        last_activity = user_data.get("last_activity_date")
        
        # Si no hay actividad registrada, la racha es 0
        if not last_activity:
            current_streak = 0
        else:
            # Convertir a datetime si viene como string de Mongo
            if isinstance(last_activity, str):
                last_activity = datetime.fromisoformat(last_activity.replace('Z', '+00:00'))
            
            # Calcular días desde última actividad
            today = datetime.utcnow().date()
            last_date = last_activity.date() if isinstance(last_activity, datetime) else last_activity
            
            days_diff = (today - last_date).days
            
            # Si la última actividad fue hoy, mantener racha
            # Si fue ayer, la racha ya se incrementó al guardar palabra
            # Si pasaron más de 1 día, mostrar 0 (se resetea al guardar siguiente palabra)
            if days_diff > 1:
                current_streak = 0
        
        # 4. Estadísticas por idioma
        stats_by_lang = {
            "en": {
                "words_learned": total_words,
                "estimated_level": "B1"
            }
        }

        return {
            "total_words_learned": total_words,
            "current_streak": current_streak,
            "longest_streak": user_data.get("longest_streak", 0),
            "stats_by_language": stats_by_lang,
            "status": "success"
        }
    except Exception as e:
        logger.error(f"❌ Error calculando progreso: {e}")
        return {
            "total_words_learned": 0,
            "current_streak": 0,
            "longest_streak": 0,
            "stats_by_language": {},
            "status": "error"
        }