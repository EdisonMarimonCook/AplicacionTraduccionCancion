"""
CRUD - Operaciones de Base de Datos
Usa MOCK DB si no hay MongoDB, usa BD real cuando esté lista
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

# ================================================================
# DICCIONARIO
# ================================================================

async def create_dictionary_entry(entry_data: dict) -> str:
    """Crear palabra en diccionario"""
    return await db.create_dictionary_entry(entry_data)

async def get_user_dictionary(user_id: str, language: str = None) -> list:
    """Obtener diccionario del usuario"""
    return await db.get_user_dictionary(user_id, language)

async def delete_dictionary_entry(entry_id: str) -> bool:
    """Eliminar palabra del diccionario"""
    return await db.delete_dictionary_entry(entry_id)

# ================================================================
# SESIONES
# ================================================================

async def create_session(session_data: dict) -> str:
    """Crear sesión de estudio"""
    return await db.create_session(session_data)

async def get_user_sessions(user_id: str) -> list:
    """Obtener sesiones del usuario"""
    return await db.get_user_sessions(user_id)

# ================================================================
# PROGRESO
# ================================================================

async def get_user_progress(user_id: str) -> dict:
    """Obtener progreso del usuario"""
    return await db.get_user_progress(user_id)

async def update_user_progress(user_id: str, progress_data: dict) -> bool:
    """Actualizar progreso del usuario"""
    return await db.update_user_progress(user_id, progress_data)