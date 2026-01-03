"""
ARCHIVO: routers/languages.py
PROPÓSITO: Gestión de idiomas de aprendizaje del usuario (Fase 2.5)
ENDPOINTS:
  - POST /api/v1/users/languages - Añadir nuevo idioma
  - PUT /api/v1/users/languages/reorder - Reordenar idiomas (primero = primary_language)
  - PUT /api/v1/users/languages/{code}/toggle - Activar/desactivar idioma (soft delete)
  - PUT /api/v1/users/languages/{code}/daily-goal - Actualizar meta diaria
"""

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from typing import List
from models import User
from routers.auth import get_current_user
from database import db
from services.level_mapper import LEVEL_SYSTEMS
import logging

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/users/languages", tags=["Languages"])

# ===============================================================================
# SCHEMAS
# ===============================================================================

class AddLanguageRequest(BaseModel):
    language: str
    level: str

class ReorderLanguagesRequest(BaseModel):
    language_order: List[str]  # Lista de códigos de idioma en el nuevo orden

class UpdateDailyGoalRequest(BaseModel):
    daily_goal: int

class UpdateLevelRequest(BaseModel):
    level: str

# ===============================================================================
# ENDPOINTS
# ===============================================================================

@router.post("")
async def add_language(
    request: AddLanguageRequest,
    current_user: User = Depends(get_current_user)
):
    """
    ➕ Añadir nuevo idioma a learning_languages
    - Valida que el idioma esté soportado
    - Valida que el usuario no lo tenga ya
    - Añade con valores por defecto Fase 2.5
    """
    try:
        # Validar idioma soportado
        if request.language not in LEVEL_SYSTEMS:
            raise HTTPException(
                status_code=400, 
                detail=f"Idioma '{request.language}' no soportado"
            )
        
        # Verificar que no exista ya
        existing_languages = [
            lang.language for lang in current_user.learning_languages
        ] if current_user.learning_languages else []
        
        if request.language in existing_languages:
            raise HTTPException(
                status_code=400,
                detail=f"Ya estás aprendiendo {request.language}"
            )
        
        # Crear nuevo idioma con defaults Fase 2.5
        new_language = {
            "language": request.language,
            "level": request.level,
            "started_at": None,
            "last_tested": None,
            "daily_goal": 10,
            "reviews_pending": 0,
            "is_active": True,
            "words_learned": 0
        }
        
        # Añadir a la lista
        await db.db["users"].update_one(
            {"_id": current_user.id},
            {"$push": {"learning_languages": new_language}}
        )
        
        logger.info(f"✅ Usuario {current_user.username} añadió idioma {request.language}")
        
        return {"message": f"Idioma {request.language} añadido correctamente"}
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error añadiendo idioma: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.put("/reorder")
async def reorder_languages(
    request: ReorderLanguagesRequest,
    current_user: User = Depends(get_current_user)
):
    """
    🔄 Reordenar idiomas (el primero se convierte en primary_language)
    - Valida que todos los códigos existan en el usuario
    - Actualiza el orden en learning_languages
    - Establece el primero como primary_language
    """
    try:
        if not request.language_order or len(request.language_order) == 0:
            raise HTTPException(status_code=400, detail="Debe enviar al menos un idioma")
        
        # Obtener idiomas actuales
        current_languages = current_user.learning_languages if current_user.learning_languages else []
        current_codes = [lang.language for lang in current_languages]
        
        # Validar que todos los códigos enviados existan
        for code in request.language_order:
            if code not in current_codes:
                raise HTTPException(
                    status_code=400,
                    detail=f"Idioma '{code}' no encontrado en tus idiomas"
                )
        
        # Reordenar
        lang_dict = {lang.language: lang for lang in current_languages}
        reordered = []
        
        for code in request.language_order:
            lang = lang_dict[code]
            lang_data = lang.model_dump() if hasattr(lang, 'model_dump') else dict(lang)
            reordered.append(lang_data)
        
        # Establecer primary_language como el primero
        new_primary = request.language_order[0]
        
        await db.db["users"].update_one(
            {"_id": current_user.id},
            {
                "$set": {
                    "learning_languages": reordered,
                    "primary_language": new_primary
                }
            }
        )
        
        logger.info(f"✅ Usuario {current_user.username} reordenó idiomas. Nuevo primary: {new_primary}")
        
        return {
            "message": "Idiomas reordenados correctamente",
            "primary_language": new_primary
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error reordenando idiomas: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.put("/{code}/toggle")
async def toggle_language_active(
    code: str,
    current_user: User = Depends(get_current_user)
):
    """
    🔘 Activar/Desactivar idioma (soft delete)
    - Cambia is_active de True ↔ False
    - Si es el primary_language y se desactiva, asigna otro como primario
    """
    try:
        # Buscar idioma
        language_index = None
        current_languages = current_user.learning_languages if current_user.learning_languages else []
        
        for idx, lang in enumerate(current_languages):
            if lang.language == code:
                language_index = idx
                break
        
        if language_index is None:
            raise HTTPException(status_code=404, detail=f"Idioma '{code}' no encontrado")
        
        # Toggle is_active
        lang = current_languages[language_index]
        lang_data = lang.model_dump() if hasattr(lang, 'model_dump') else dict(lang)
        new_state = not lang_data.get("is_active", True)
        lang_data["is_active"] = new_state
        
        # Actualizar en DB
        await db.db["users"].update_one(
            {"_id": current_user.id},
            {"$set": {f"learning_languages.{language_index}.is_active": new_state}}
        )
        
        # Si desactivamos el primary_language, asignar otro
        updates = {}
        if not new_state and current_user.primary_language == code:
            # Buscar el primer idioma activo que no sea este
            new_primary = None
            for lang in current_languages:
                lang_dict = lang.model_dump() if hasattr(lang, 'model_dump') else dict(lang)
                if lang_dict.get("language") != code and lang_dict.get("is_active", True):
                    new_primary = lang_dict.get("language")
                    break
            
            if new_primary:
                updates["primary_language"] = new_primary
                await db.db["users"].update_one(
                    {"_id": current_user.id},
                    {"$set": updates}
                )
                logger.info(f"⚠️ Primary language cambiado de {code} a {new_primary}")
        
        action = "activado" if new_state else "desactivado"
        logger.info(f"✅ Usuario {current_user.username} {action} idioma {code}")
        
        response = {"message": f"Idioma {code} {action} correctamente"}
        if updates:
            response["new_primary_language"] = updates["primary_language"]
        
        return response
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error toggle idioma: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.put("/{code}/daily-goal")
async def update_daily_goal(
    code: str,
    request: UpdateDailyGoalRequest,
    current_user: User = Depends(get_current_user)
):
    """
    🎯 Actualizar meta diaria de palabras para un idioma
    - Valida que daily_goal esté entre 1 y 100
    """
    try:
        if request.daily_goal < 1 or request.daily_goal > 100:
            raise HTTPException(
                status_code=400,
                detail="Meta diaria debe estar entre 1 y 100 palabras"
            )
        
        # Buscar idioma
        language_index = None
        current_languages = current_user.learning_languages if current_user.learning_languages else []
        
        for idx, lang in enumerate(current_languages):
            if lang.language == code:
                language_index = idx
                break
        
        if language_index is None:
            raise HTTPException(status_code=404, detail=f"Idioma '{code}' no encontrado")
        
        # Actualizar daily_goal
        await db.db["users"].update_one(
            {"_id": current_user.id},
            {"$set": {f"learning_languages.{language_index}.daily_goal": request.daily_goal}}
        )
        
        logger.info(f"✅ Usuario {current_user.username} cambió meta de {code} a {request.daily_goal}")
        
        return {
            "message": f"Meta diaria actualizada a {request.daily_goal} palabras/día",
            "daily_goal": request.daily_goal
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error actualizando meta: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.put("/{code}/level")
async def update_level(
    code: str,
    request: UpdateLevelRequest,
    current_user: User = Depends(get_current_user)
):
    """
    📊 Actualizar nivel de un idioma
    - Valida que el nivel sea válido para el sistema del idioma
    """
    try:
        # Validar que el idioma esté en LEVEL_SYSTEMS
        if code not in LEVEL_SYSTEMS:
            raise HTTPException(status_code=400, detail=f"Idioma '{code}' no soportado")
        
        # Validar que el nivel sea válido
        level_data = LEVEL_SYSTEMS[code]
        valid_levels = level_data["levels"]
        if request.level not in valid_levels:
            raise HTTPException(
                status_code=400, 
                detail=f"Nivel inválido. Niveles válidos para {code}: {', '.join(valid_levels)}"
            )
        
        # Buscar idioma
        language_index = None
        current_languages = current_user.learning_languages if current_user.learning_languages else []
        
        for idx, lang in enumerate(current_languages):
            if lang.language == code:
                language_index = idx
                break
        
        if language_index is None:
            raise HTTPException(status_code=404, detail=f"Idioma '{code}' no encontrado en tu perfil")
        
        # Actualizar level en DB
        await db.db["users"].update_one(
            {"_id": current_user.id},
            {"$set": {f"learning_languages.{language_index}.level": request.level}}
        )
        
        logger.info(f"✅ Usuario {current_user.username} cambió nivel de {code} a {request.level}")
        return {"message": f"Nivel actualizado a {request.level}"}
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error actualizando nivel: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
