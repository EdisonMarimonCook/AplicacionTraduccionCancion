"""
ARCHIVO: routers/dictionary.py
PROPÓSITO: Endpoints del diccionario personal
"""

import logging
from fastapi import APIRouter, Depends, HTTPException, status
from datetime import datetime, timezone

from models import User
from routers.auth import get_current_user
from database import db # Usamos el import genérico que decide si es Mock o Mongo
from routers.schemas import DictionaryEntryCreate, DictionaryEntryResponse

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/dictionary", tags=["Dictionary"])

@router.post("/add", response_model=DictionaryEntryResponse)
async def add_word(
    entry: DictionaryEntryCreate, 
    current_user: User = Depends(get_current_user)
):
    """
    Guarda una palabra O expresión en el diccionario.
    Soporta carpetas (type) y ejemplos (flashcards).
    
    🔥 VALIDACIONES ANTI-BURNOUT:
    - daily_goal: Advierte si supera la meta diaria del idioma
    - burnout_cap: Bloquea si tiene >50 repasos pendientes
    """
    # 🔥 VALIDACIÓN 1: Burnout Cap (reviews_pending > 50)
    # Contar flashcards pendientes del idioma
    from bson import ObjectId
    user_id = ObjectId(current_user.id) if isinstance(current_user.id, str) else current_user.id
    
    # Obtener idioma de la palabra que se quiere añadir
    word_language = entry.language
    
    # Contar palabras con flashcards pendientes de ese idioma
    all_words = await db.db["dictionary_entries"].find({
        "user_id": str(current_user.id),
        "language": word_language,
        "example": {"$exists": True, "$ne": None}
    }).to_list(None)
    
    reviews_pending = 0
    for word in all_words:
        word_id = str(word.get("_id"))
        srs_data = await db.db["flashcards_srs"].find_one({"word_id": word_id})
        if srs_data and srs_data.get("next_review_date") <= datetime.utcnow():
            reviews_pending += 1
    
    BURNOUT_LIMIT = 50
    if reviews_pending > BURNOUT_LIMIT:
        raise HTTPException(
            status_code=400,
            detail=f"Tienes {reviews_pending} flashcards pendientes. Repasa antes de añadir más."
        )
    
    # 🔥 VALIDACIÓN 2: Daily Goal
    # Contar palabras añadidas HOY en este idioma
    today_start = datetime.utcnow().replace(hour=0, minute=0, second=0, microsecond=0)
    words_added_today = await db.db["dictionary_entries"].count_documents({
        "user_id": str(current_user.id),
        "language": word_language,
        "created_at": {"$gte": today_start}
    })
    
    # Obtener daily_goal del idioma
    daily_goal = 10  # Default
    if hasattr(current_user, 'learning_languages') and current_user.learning_languages:
        for lang in current_user.learning_languages:
            lang_dict = lang.model_dump() if hasattr(lang, 'model_dump') else dict(lang)
            if lang_dict.get("language") == word_language:
                daily_goal = lang_dict.get("daily_goal", 10)
                break
    
    # Si alcanzó la meta, añadir advertencia en respuesta (pero permitir continuar)
    warning = None
    if words_added_today >= daily_goal:
        warning = f"Has alcanzado tu meta diaria ({daily_goal} palabras). ¿Seguro que quieres continuar?"
        logger.warning(f"⚠️ Usuario {current_user.username} superó daily_goal: {words_added_today}/{daily_goal}")
    
    # 1. Convertir Pydantic a Dict
    new_entry = entry.dict()
    
    # 2. Añadir metadatos del sistema
    new_entry.update({
        "user_id": current_user.id,
        "created_at": datetime.utcnow(),
        "last_reviewed": datetime.utcnow(),
        "times_reviewed": 0,
        "is_learned": False,
        
        # 3. Asegurar campos clave para tu App
        "type": entry.type,          # 'word' vs 'expression' (Carpetas)
        "example": entry.example,    # Flashcards
        "is_recommended": entry.is_recommended # Estrellita ⭐
    })

    try:
        # 4. Guardar en Base de Datos (Mongo o Mock)
        entry_id = await db.create_dictionary_entry(new_entry)
        
        # 🔥 5. SI TIENE EJEMPLO, CREAR FLASHCARD SRS INMEDIATAMENTE
        if entry.example:
            srs_data = {
                "word_id": entry_id,
                "easiness_factor": 2.5,
                "interval": 0,
                "repetitions": 0,
                "next_review_date": datetime.now(timezone.utc),  # Disponible HOY
                "last_reviewed": None,
                "times_reviewed": 0,
                "times_correct": 0,
                "times_incorrect": 0
            }
            await db.create_flashcard_srs_data(srs_data)
            logger.info(f"✅ Flashcard SRS creada para word_id={entry_id}")
        
        # ✅ 6. ACTUALIZAR RACHA DEL USUARIO
        await db.update_user_streak(str(current_user.id))
        
        # 7. Responder (con warning si superó daily_goal)
        new_entry["id"] = entry_id
        if warning:
            new_entry["warning"] = warning
        return new_entry
        
    except Exception as e:
        logger.error(f"Error guardando palabra: {e}")
        raise HTTPException(status_code=500, detail="Error saving to dictionary")

@router.get("/list", response_model=list[DictionaryEntryResponse])
async def list_dictionary(
    current_user: User = Depends(get_current_user),
    language: str = None,  # 🔥 Filtro por idioma (None = primary_language)
    type: str = None       # Filtro por tipo (word/expression)
):
    """
    Obtiene diccionario filtrado
    ✨ FASE 2.5: Filtra por idioma activo + tipo
    - language=None → usar primary_language
    - type=None → devolver todo (palabras + expresiones)
    """
    items = await db.get_user_dictionary(current_user.id)
    
    # ✨ SOFT DELETE: Filtrar idiomas inactivos
    active_language_codes = []
    if hasattr(current_user, 'learning_languages') and current_user.learning_languages:
        for lang in current_user.learning_languages:
            lang_dict = lang.model_dump() if hasattr(lang, 'model_dump') else dict(lang)
            if lang_dict.get("is_active", True):
                active_language_codes.append(lang_dict.get("language"))
    
    # Filtrar por idiomas activos
    if active_language_codes:
        items = [i for i in items if i.get("language") in active_language_codes]
    
    # 🔥 Filtrar por idioma específico (o primary_language si no se especifica)
    if language is None:
        # Usar primary_language por defecto
        language = current_user.primary_language
    
    items = [i for i in items if i.get("language") == language]
    
    # Filtrar por tipo si se especifica
    if type:
        items = [i for i in items if i.get("type") == type]
        
    return items

@router.delete("/delete/{item_id}")
async def delete_word(
    item_id: str,
    current_user: User = Depends(get_current_user)
):
    success = await db.delete_dictionary_entry(item_id)
    if not success:
        raise HTTPException(status_code=404, detail="Word not found")
    return {"status": "deleted"}