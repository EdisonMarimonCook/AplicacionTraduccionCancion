"""
ARCHIVO: routers/progress.py
PROPÓSITO: Endpoints de progreso y estadísticas
RUTAS: /progress/stats, /progress/by-language
USUARIO: Frontend/Mobile
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
        # Usamos str(current_user.id) para asegurar compatibilidad con Mongo
        items = await db.get_user_dictionary(str(current_user.id))
        
        # Aseguramos que items sea una lista
        if items is None:
            items = []
            
        total_words = len(items)
        
        # 2. Obtener datos del usuario (para racha y learning_languages)
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
                try:
                    last_activity = datetime.fromisoformat(last_activity.replace('Z', '+00:00'))
                except ValueError:
                    # Fallback si el formato es extraño
                    last_activity = datetime.utcnow()
            
            # Calcular días desde última actividad
            today = datetime.utcnow().date()
            last_date = last_activity.date() if isinstance(last_activity, datetime) else last_activity
            
            days_diff = (today - last_date).days
            
            # Si pasaron más de 1 día, resetear racha (solo visual, BD lo maneja al actualizar)
            if days_diff > 1:
                current_streak = 0
        
        # 4. Obtener learning_languages del usuario
        learning_languages = user_data.get("learning_languages", [])
        
        # Formato para el frontend
        learning_languages_formatted = []
        for lang_data in learning_languages:
            # Si es dict
            if isinstance(lang_data, dict):
                learning_languages_formatted.append({
                    "language": lang_data.get("language", "en"),
                    "level": lang_data.get("level", "A1"),
                    # Por ahora asignamos todas las palabras al inglés o primer idioma
                    "words_count": total_words if lang_data.get("language") == "en" else 0
                })

        return {
            "total_words_learned": total_words,
            "current_streak": current_streak,
            "longest_streak": user_data.get("longest_streak", 0),
            "learning_languages": learning_languages_formatted,
            "status": "success"
        }
    except Exception as e:
        logger.error(f"❌ Error calculando progreso: {e}")
        return {
            "total_words_learned": 0,
            "current_streak": 0,
            "longest_streak": 0,
            "learning_languages": [],
            "status": "error"
        }