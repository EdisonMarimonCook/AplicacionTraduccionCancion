"""
ROUTER: Usuarios
Endpoints: Perfil, actualización de datos, cambio de contraseña, SUBIDA DE AVATAR
USA: auth.py (JWT), crud.py (BD)
"""

import logging
import os
import shutil
from bson import ObjectId
from fastapi import APIRouter, HTTPException, Depends, status, File, UploadFile, Request
from email_validator import validate_email, EmailNotValidError 

from models import User
from database import db 
from routers.schemas import (
    UserProfileUpdate,
    UserProfileResponse,  # 🔥 CAMBIAR: UserProfile → UserProfileResponse
    PasswordChangeRequest,
    EmailChangeRequest,
    LoginRequest,
    AvatarUpdateResponse,
    LearningLanguage  # 🔥 AÑADIR: Este es el nombre correcto (no SchemaLearningLanguage)
)
from routers.auth import get_current_user
from auth import verify_password, hash_password
from crud import (
    username_exists,
    email_exists,
    get_user_by_email,
    update_user_profile,
    change_user_password,
    change_user_email
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/users", tags=["Users"])

@router.get("/profile", response_model=UserProfileResponse)
async def get_user_profile(current_user: User = Depends(get_current_user)):
    try:
        reviews_count = await db.count_user_flashcards_reviews(str(current_user.id))
        words_count = await db.db["dictionary_entries"].count_documents({
            "user_id": str(current_user.id)
        })
        streak = getattr(current_user, 'current_streak', 0)
        
        learning_languages = []
        if hasattr(current_user, 'learning_languages') and current_user.learning_languages:
            for lang in current_user.learning_languages:
                lang_dict = lang.model_dump() if hasattr(lang, 'model_dump') else dict(lang)
                
                if 'started_at' in lang_dict and lang_dict['started_at']:
                    if hasattr(lang_dict['started_at'], 'isoformat'):
                        lang_dict['started_at'] = lang_dict['started_at'].isoformat()
                
                if 'last_tested' in lang_dict and lang_dict['last_tested']:
                    if hasattr(lang_dict['last_tested'], 'isoformat'):
                        lang_dict['last_tested'] = lang_dict['last_tested'].isoformat()
                
                learning_languages.append(lang_dict)
        
        logger.info(f"📊 Stats: Palabras={words_count}, Racha={streak}, Repasos={reviews_count}")
        
        return UserProfileResponse(
            id=str(current_user.id),
            username=current_user.username,
            email=current_user.email,
            full_name=getattr(current_user, 'full_name', None),
            native_language=getattr(current_user, 'native_language', 'es'),
            avatar_url=current_user.avatar_url,
            learning_languages=learning_languages,
            created_at=current_user.created_at,
            is_active=current_user.is_active,
            streak=streak,
            reviews_count=reviews_count,
            words_count=words_count
        )
    except Exception as e:
        logger.error(f"❌ Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/upload-avatar", response_model=AvatarUpdateResponse)
async def upload_avatar(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user)
):
    try:
        logger.info(f"📸 Iniciando upload de avatar para user_id={current_user.id}")
        logger.info(f"📸 Archivo recibido: {file.filename}, tipo: {file.content_type}")
        
        # 🔥 OBTENER AVATAR ANTERIOR PARA BORRARLO
        user_data = await db.db["users"].find_one({"_id": ObjectId(str(current_user.id))})
        old_avatar_url = user_data.get("avatar_url") if user_data else None
        
        # Ruta absoluta a la carpeta static/avatars
        current_file_dir = os.path.dirname(os.path.abspath(__file__))  # backend/routers
        backend_dir = os.path.dirname(current_file_dir)  # backend
        static_dir = os.path.join(backend_dir, "static", "avatars")
        
        # Asegurar que exista
        os.makedirs(static_dir, exist_ok=True)
        
        # 🔥 BORRAR AVATAR ANTERIOR SI EXISTE
        if old_avatar_url and old_avatar_url.startswith("/static/avatars/"):
            old_filename = old_avatar_url.split("/")[-1]
            old_file_path = os.path.join(static_dir, old_filename)
            if os.path.exists(old_file_path):
                os.remove(old_file_path)
                logger.info(f"🗑️ Avatar anterior eliminado: {old_filename}")
        
        # Nombre único con timestamp
        import time
        timestamp = int(time.time())
        file_extension = file.filename.split(".")[-1] if "." in file.filename else "jpg"
        filename = f"avatar_{current_user.id}_{timestamp}.{file_extension}"
        file_path = os.path.join(static_dir, filename)
        
        logger.info(f"📸 Guardando en: {file_path}")
        
        # Guardar archivo
        await file.seek(0)
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
        
        # Verificar tamaño
        file_size = os.path.getsize(file_path)
        logger.info(f"✅ Archivo guardado correctamente ({file_size} bytes)")
        logger.info(f"📁 Carpeta static: {static_dir}")
        
        # Actualizar en base de datos
        avatar_url = f"/static/avatars/{filename}"
        result = await db.db["users"].update_one(
            {"_id": ObjectId(str(current_user.id))},
            {"$set": {"avatar_url": avatar_url}}
        )
        
        logger.info(f"📸 BD actualizada: matched={result.matched_count}, modified={result.modified_count}")
        logger.info(f"🔗 URL guardada en BD: {avatar_url}")
        
        return AvatarUpdateResponse(
            url=avatar_url,
            message="Avatar actualizado"
        )
        
    except Exception as e:
        logger.error(f"❌ Error subiendo avatar: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))

@router.put("/profile", response_model=UserProfileResponse)
async def update_profile(
    profile_update: UserProfileUpdate,
    current_user: User = Depends(get_current_user)
):
    try:
        if profile_update.username and profile_update.username != current_user.username:
            if await username_exists(profile_update.username, exclude_email=current_user.email):
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"Username '{profile_update.username}' is already taken"
                )
        
        updated_user = await update_user_profile(
            email=current_user.email,
            username=profile_update.username,
            full_name=profile_update.full_name,
            native_language=profile_update.native_language,
            learning_languages=profile_update.learning_languages
        )
        
        if not updated_user:
            raise HTTPException(status_code=500, detail="Error updating profile")
        
        # Reconstrucción manual rápida para evitar errores de validación
        return UserProfileResponse(
            id=str(updated_user["_id"]),
            email=updated_user["email"],
            username=updated_user["username"],
            full_name=updated_user.get("full_name"),
            native_language=updated_user.get("native_language", "en"),
            avatar_url=updated_user.get("avatar_url"),
            learning_languages=[], # Simplificado para respuesta
            created_at=updated_user.get("created_at"),
            is_active=True
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/change-password")
async def change_password(
    password_change: PasswordChangeRequest,
    current_user: User = Depends(get_current_user)
):
    try:
        user_dict = await get_user_by_email(current_user.email)
        if not user_dict: raise HTTPException(status_code=401, detail="User not found")
        if not verify_password(password_change.current_password, user_dict.get("password_hash", "")):
            raise HTTPException(status_code=401, detail="Current password is incorrect")
        if password_change.new_password != password_change.confirm_password:
            raise HTTPException(status_code=400, detail="New passwords do not match")
        
        new_password_hash = hash_password(password_change.new_password)
        success = await change_user_password(current_user.email, new_password_hash)
        if not success: raise HTTPException(status_code=500, detail="Error updating password")
        return {"message": "Password changed successfully", "email": current_user.email}
    except Exception:
        raise HTTPException(status_code=500, detail="Error changing password")

@router.post("/change-email")
async def change_email(
    email_change: EmailChangeRequest,
    current_user: User = Depends(get_current_user)
):
    try:
        try: validate_email(email_change.new_email)
        except EmailNotValidError as e: raise HTTPException(status_code=400, detail=f"Invalid email: {str(e)}")
        if await email_exists(email_change.new_email): raise HTTPException(status_code=400, detail="Email is already registered")
        user_dict = await get_user_by_email(current_user.email)
        if not verify_password(email_change.password, user_dict.get("password_hash", "")):
            raise HTTPException(status_code=401, detail="Password is incorrect")
        success = await change_user_email(current_user.email, email_change.new_email)
        return {"message": "Email changed successfully", "old_email": current_user.email, "new_email": email_change.new_email}
    except Exception:
        raise HTTPException(status_code=500, detail="Error changing email")

@router.get("/me", response_model=UserProfileResponse)
async def get_current_user_profile(current_user: User = Depends(get_current_user)):
    try:
        # 🔥 CALCULAR REPASOS DINÁMICAMENTE
        reviews_count = await db.count_user_flashcards_reviews(str(current_user.id))
        
        # Racha actual
        streak = getattr(current_user, 'current_streak', 0)
        
        logger.info(f"📊 Usuario {current_user.username}: Racha={streak}, Repasos={reviews_count}")
        
        return UserProfileResponse(
            id=str(current_user.id),
            username=current_user.username,
            email=current_user.email,
            full_name=getattr(current_user, 'full_name', None),
            native_language=getattr(current_user, 'native_language', 'es'),
            avatar_url=current_user.avatar_url,
            learning_languages=getattr(current_user, 'learning_languages', []),
            created_at=current_user.created_at,
            is_active=current_user.is_active,
            streak=streak,
            reviews_count=reviews_count
        )
    except Exception as e:
        logger.error(f"❌ Error obteniendo perfil: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))