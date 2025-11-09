"""
MÓDULO: CRUD (Create, Read, Update, Delete)
Operaciones de base de datos centralizadas
"""

import logging
from datetime import datetime
from typing import Optional, List
from database import get_database

logger = logging.getLogger(__name__)

# ===============================================================================
# USUARIOS
# ===============================================================================

async def create_user(user_data: dict) -> dict:
    """
    Crea un nuevo usuario en BD
    
    Args:
        user_data: Datos del usuario
    
    Returns:
        dict: Usuario creado
    """
    db = get_database()
    users_collection = db.get("users", [])
    
    new_user = {
        "id": str(datetime.now().timestamp()),
        **user_data,
        "created_at": datetime.now().isoformat(),
        "updated_at": datetime.now().isoformat()
    }
    
    users_collection.append(new_user)
    db["users"] = users_collection
    
    logger.info(f"✅ Usuario creado: {user_data.get('email')}")
    return new_user

async def get_user_by_email(email: str) -> Optional[dict]:
    """Busca usuario por email"""
    db = get_database()
    users_collection = db.get("users", [])
    return next((u for u in users_collection if u.get("email") == email), None)

async def get_user_by_id(user_id: str) -> Optional[dict]:
    """Busca usuario por ID"""
    db = get_database()
    users_collection = db.get("users", [])
    return next((u for u in users_collection if u.get("id") == user_id), None)

async def user_exists(email: str) -> bool:
    """Verifica si un usuario existe"""
    user = await get_user_by_email(email)
    return user is not None

# ===============================================================================
# DICCIONARIO
# ===============================================================================

async def add_word_to_dictionary(user_id: str, word_data: dict) -> dict:
    """Añade palabra al diccionario personal del usuario"""
    db = get_database()
    dictionary = db.get("dictionary", [])
    
    new_entry = {
        "id": str(datetime.now().timestamp()),
        "user_id": user_id,
        **word_data,
        "created_at": datetime.now().isoformat()
    }
    
    dictionary.append(new_entry)
    db["dictionary"] = dictionary
    
    logger.info(f"✅ Palabra guardada: {word_data.get('word')} para usuario {user_id}")
    return new_entry

async def get_user_dictionary(user_id: str) -> List[dict]:
    """Obtiene diccionario completo del usuario"""
    db = get_database()
    dictionary = db.get("dictionary", [])
    return [d for d in dictionary if d.get("user_id") == user_id]

async def get_word_from_dictionary(user_id: str, word: str) -> Optional[dict]:
    """Busca una palabra en diccionario del usuario"""
    db = get_database()
    dictionary = db.get("dictionary", [])
    return next((d for d in dictionary if d.get("user_id") == user_id and d.get("word").lower() == word.lower()), None)

# ===============================================================================
# SESIONES
# ===============================================================================

async def create_song_session(user_id: str, song_id: str) -> dict:
    """Crea una sesión de estudio con una canción"""
    db = get_database()
    sessions = db.get("song_sessions", [])
    
    new_session = {
        "id": str(datetime.now().timestamp()),
        "user_id": user_id,
        "song_id": song_id,
        "started_at": datetime.now().isoformat(),
        "words_learned": 0,
        "score": 0
    }
    
    sessions.append(new_session)
    db["song_sessions"] = sessions
    
    return new_session