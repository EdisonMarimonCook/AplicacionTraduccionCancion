"""
CRUD - Operaciones de Base de Datos
Abstrae la lógica de base de datos (sea Mock o Mongo)
"""

import logging
from database import db

logger = logging.getLogger(__name__)

# ================================================================
# USUARIOS
# ================================================================

async def create_user(user_data: dict) -> str:
    """Crear usuario"""
    return await db.create_user(user_data)

async def get_user_by_email(email: str) -> dict:
    """Obtener usuario por email"""
    return await db.get_user_by_email(email)

async def user_exists(email: str) -> bool:
    """Verificar si usuario existe"""
    return await db.user_exists(email)

async def get_user_by_id(user_id: str) -> dict:
    """Obtener usuario por ID"""
    return await db.get_user_by_id(user_id)

async def username_exists(username: str, exclude_email: str = None) -> bool:
    """
    Verificar si username está en uso.
    """
    user = await db.users_collection.find_one({"username": username})
    
    if not user:
        return False
    
    # Si existe pero es del usuario actual (exclude_email), no cuenta como "existente"
    if exclude_email and user.get("email") == exclude_email:
        return False
    
    return True

async def email_exists(email: str) -> bool:
    """Verificar si email está disponible"""
    user = await db.users_collection.find_one({"email": email})
    return user is not None

async def change_user_email(current_email: str, new_email: str):
    """Cambiar email"""

    result = await db.users_collection.update_one(
        {"email": current_email},
        {"$set": {"email": new_email}}
    )
    return result.modified_count > 0

async def update_user_profile(email: str, update_data: dict):
    """Actualizar datos del perfil"""

    result = await db.users_collection.update_one(
        {"email": email},
        {"$set": update_data}
    )
    return result.modified_count > 0

async def change_user_password(email: str, new_hashed_password: str):
    """Cambiar contraseña"""
   
    result = await db.users_collection.update_one(
        {"email": email},
        {"$set": {"hashed_password": new_hashed_password}}
    )
    return result.modified_count > 0

# ================================================================
# DICCIONARIO
# ================================================================

async def create_dictionary_entry(entry_data: dict) -> str:
    """Crear palabra en diccionario"""
    return await db.create_dictionary_entry(entry_data)

async def get_user_dictionary(user_id: str, language: str = None) -> list:
    """Obtener diccionario crudo (Legacy)"""
    return await db.get_user_dictionary(user_id, language)

# 🔥 ESTA ES LA FUNCIÓN QUE FALTABA Y QUE ARREGLA EL ERROR
async def list_user_dictionary(
    user_id: str, 
    q: str = None,
    language: str = None,
    level_system: str = None,
    difficulty_level: str = None,
    is_hiphop_term: bool = None,
    type: str = None, # Filtro de carpetas
    page: int = 1,
    page_size: int = 50
) -> list:
    """
    Obtener diccionario con filtros y paginación.
    """
    # 1. Obtener todo de la BD
    items = await db.get_user_dictionary(user_id, language)
    
    # 2. Filtrado en memoria (Python) para el MVP
    if q:
        q = q.lower()
        items = [i for i in items if q in i.get("word", "").lower() or q in i.get("translation", "").lower()]
    
    if type:
        items = [i for i in items if i.get("type") == type]

    if is_hiphop_term is not None:
        items = [i for i in items if i.get("is_hiphop_term") == is_hiphop_term]

    # Implementar paginación simple
    start = (page - 1) * page_size
    end = start + page_size
    
    return items[start:end]

async def delete_dictionary_entry(user_id: str, entry_id: str) -> bool:
    """Borrar entrada"""
    return await db.delete_dictionary_entry(entry_id)