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

# ================================================================
# USUARIOS - VALIDACIONES
# ================================================================

async def username_exists(username: str, exclude_email: str = None) -> bool:
    """Verificar si username está disponible"""
    return await db.username_exists(username, exclude_email=exclude_email)

async def email_exists(email: str) -> bool:
    """Verificar si email está disponible"""
    return await db.email_exists(email)

# ================================================================
# USUARIOS - ACTUALIZACIÓN
# ================================================================

async def update_user_profile(
    email: str,
    username: str = None,
    full_name: str = None,
    native_language: str = None,
    learning_languages: list = None
) -> dict:
    """
    Actualizar perfil del usuario
    
    Args:
        email: Email del usuario
        username: Nuevo username (opcional)
        full_name: Nuevo nombre completo (opcional)
        native_language: Nuevo idioma nativo (opcional)
        learning_languages: Nuevos idiomas de aprendizaje (opcional)
    
    Returns:
        Usuario actualizado o None
    """
    update_data = {}
    
    if username is not None:
        update_data["username"] = username
    
    if full_name is not None:
        update_data["full_name"] = full_name
    
    if native_language is not None:
        update_data["native_language"] = native_language
    
    if learning_languages is not None:
        update_data["learning_languages"] = [
            {
                "language": lang.language,
                "level": lang.level,
                "started_at": lang.started_at.isoformat(),
                "last_tested": lang.last_tested.isoformat() if lang.last_tested else None
            }
            for lang in learning_languages
        ]
    
    return await db.update_user(email, update_data)

async def change_user_password(email: str, new_password_hash: str) -> bool:
    """Cambiar contraseña del usuario"""
    user = await db.update_user(email, {"password_hash": new_password_hash})
    return user is not None

async def change_user_email(old_email: str, new_email: str) -> bool:
    """Cambiar email del usuario"""
    return await db.change_email(old_email, new_email)