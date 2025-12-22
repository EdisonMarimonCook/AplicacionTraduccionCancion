"""
ROUTER: Usuarios
Endpoints: Perfil, actualización de datos, cambio de contraseña, SUBIDA DE AVATAR
USA: auth.py (JWT), crud.py (BD)
"""

import logging
from bson import ObjectId
from fastapi import APIRouter, HTTPException, Depends, status, File, UploadFile, Request
from email_validator import validate_email, EmailNotValidError 

from models import User
from database import db  # 🔥 Aquí está la instancia de MongoDatabase
from routers.schemas import (
    UserProfileUpdate,
    UserProfileResponse,
    PasswordChangeRequest,
    EmailChangeRequest,
    LoginRequest,
    AvatarUpdateResponse,
    LearningLanguage
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
from services.cloudinary_service import upload_avatar, delete_avatar

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

@router.post("/upload-avatar")
async def upload_user_avatar(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user)
):
    try:
        # Validar tipo de archivo
        if not file.content_type or not file.content_type.startswith("image/"):
            raise HTTPException(
                status_code=400, 
                detail="El archivo debe ser una imagen (JPG, PNG, WebP)"
            )
        
        # Leer bytes del archivo
        file_bytes = await file.read()
        
        # Validar tamaño (máximo 5MB)
        max_size = 5 * 1024 * 1024
        if len(file_bytes) > max_size:
            raise HTTPException(
                status_code=400, 
                detail="La imagen no puede superar 5MB"
            )
        
        user_id = str(current_user.id)
        
        # Subir a Cloudinary
        avatar_url = upload_avatar(file_bytes, user_id)
        
        # 🔥 USAR db (instancia de MongoDatabase) en lugar de importar la función
        await db.update_user_avatar(user_id, avatar_url)
        
        logger.info(f"✅ Avatar actualizado para usuario {current_user.email}")
        
        return {
            "message": "Avatar actualizado correctamente",
            "avatar_url": avatar_url
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error al subir avatar: {e}")
        raise HTTPException(
            status_code=500, 
            detail=f"Error al subir imagen: {str(e)}"
        )

@router.delete("/delete-avatar")
async def delete_user_avatar(current_user: User = Depends(get_current_user)):
    try:
        user_id = str(current_user.id)
        
        # Eliminar de Cloudinary
        delete_avatar(user_id)
        
        # Poner avatar por defecto
        default_avatar = "https://res.cloudinary.com/demo/image/upload/v1/avatar.png"
        
        # 🔥 USAR db en lugar de importar la función
        await db.update_user_avatar(user_id, default_avatar)
        
        logger.info(f"🗑️ Avatar eliminado para usuario {current_user.email}")
        
        return {
            "message": "Avatar eliminado correctamente",
            "avatar_url": default_avatar
        }
        
    except Exception as e:
        logger.error(f"❌ Error al eliminar avatar: {e}")
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