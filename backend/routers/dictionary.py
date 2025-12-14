"""
ARCHIVO: routers/dictionary.py
PROPÓSITO: Endpoints del diccionario personal
"""

import logging
from fastapi import APIRouter, Depends, HTTPException, status
from datetime import datetime

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
    """
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
        
        # ✅ 5. ACTUALIZAR ACTIVIDAD DEL USUARIO (PARA RACHA)
        await db.update_user_activity(current_user.id)
        
        # 🔥 AÑADIR ESTA LÍNEA AL FINAL (antes del return)
        await db.update_user_streak(str(current_user.id))
        
        # 6. Responder
        new_entry["id"] = entry_id
        return new_entry
        
    except Exception as e:
        logger.error(f"Error guardando palabra: {e}")
        raise HTTPException(status_code=500, detail="Error saving to dictionary")

@router.get("/list", response_model=list[DictionaryEntryResponse])
async def list_dictionary(
    current_user: User = Depends(get_current_user),
    type: str = None # Filtro opcional para carpetas
):
    """Obtiene todo el diccionario"""
    items = await db.get_user_dictionary(current_user.id)
    
    # Filtrado básico en memoria si el DB driver no lo hizo
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