"""
ROUTER: Usuarios
Endpoints: Perfil, actualización de datos, cambio de contraseña
USA: auth.py (JWT), crud.py (BD)
"""

import logging
from fastapi import APIRouter, HTTPException, Depends, status
from email_validator import validate_email, EmailNotValidError

from models import User
from routers.schemas import (
    UserProfileUpdate,
    PasswordChangeRequest,
    EmailChangeRequest,
    UserProfileResponse,
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

# ===============================================================================
# CONFIGURACIÓN
# ===============================================================================

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/users", tags=["Users"])

# ===============================================================================
# ENDPOINTS
# ===============================================================================

@router.get("/profile", response_model=UserProfileResponse)
async def get_profile(current_user: User = Depends(get_current_user)):
    """
    👤 Obtener perfil del usuario actual
    
    Returns:
        Datos completos del usuario
    """
    logger.info(f"✅ Perfil solicitado: {current_user.email}")
    
    return UserProfileResponse(
        id=current_user.id,
        email=current_user.email,
        username=current_user.username,
        full_name=current_user.full_name,
        native_language=current_user.native_language,
        learning_languages=current_user.learning_languages,
        created_at=current_user.created_at,
        updated_at=current_user.updated_at,
        is_active=current_user.is_active
    )


@router.put("/profile", response_model=UserProfileResponse)
async def update_profile(
    profile_update: UserProfileUpdate,
    current_user: User = Depends(get_current_user)
):
    """
    ✏️ Actualizar perfil del usuario
    
    Permite cambiar:
    - Username (verificar que no esté cogido)
    - Nombre completo
    - Idioma nativo
    - Idiomas de aprendizaje
    
    Request:
    {
        "username": "newusername",
        "full_name": "Nuevo Nombre",
        "native_language": "es",
        "learning_languages": [
            {"language": "en", "level": "B1"}
        ]
    }
    """
    
    try:
        # Validar username si se intenta cambiar
        if profile_update.username and profile_update.username != current_user.username:
            if await username_exists(profile_update.username, exclude_email=current_user.email):
                logger.warning(f"⚠️  Username ya existe: {profile_update.username}")
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"Username '{profile_update.username}' is already taken"
                )
        
        # Actualizar en BD
        updated_user = await update_user_profile(
            email=current_user.email,
            username=profile_update.username,
            full_name=profile_update.full_name,
            native_language=profile_update.native_language,
            learning_languages=profile_update.learning_languages
        )
        
        if not updated_user:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error updating profile"
            )
        
        logger.info(f"✅ Perfil actualizado: {current_user.email}")
        
        return UserProfileResponse(
            id=updated_user.get("_id") or updated_user.get("id"),
            email=updated_user["email"],
            username=updated_user["username"],
            full_name=updated_user.get("full_name"),
            native_language=updated_user.get("native_language", "en"),
            learning_languages=[
                LearningLanguage(
                    language=lang.get("language"),
                    level=lang.get("level"),
                    started_at=lang.get("started_at"),
                    last_tested=lang.get("last_tested")
                )
                for lang in updated_user.get("learning_languages", [])
            ],
            created_at=updated_user.get("created_at"),
            updated_at=updated_user.get("updated_at"),
            is_active=updated_user.get("is_active", True)
        )
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error actualizando perfil: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error updating profile"
        )


@router.post("/change-password")
async def change_password(
    password_change: PasswordChangeRequest,
    current_user: User = Depends(get_current_user)
):
    """
    🔐 Cambiar contraseña del usuario
    
    Request:
    {
        "current_password": "mipasswordactual",
        "new_password": "mipasswordnuevo",
        "confirm_password": "mipasswordnuevo"
    }
    
    Validaciones:
    - Contraseña actual debe ser correcta
    - Nueva contraseña debe tener 8+ caracteres
    - Confirmación debe coincidir
    """
    
    try:
        # Obtener usuario para verificar contraseña actual
        user_dict = await get_user_by_email(current_user.email)
        
        if not user_dict:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User not found"
            )
        
        # Verificar que contraseña actual es correcta
        if not verify_password(password_change.current_password, user_dict.get("password_hash", "")):
            logger.warning(f"⚠️  Intento de cambio de contraseña fallido (contraseña incorrecta): {current_user.email}")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Current password is incorrect"
            )
        
        # Verificar que nueva contraseña y confirmación coinciden
        if password_change.new_password != password_change.confirm_password:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="New passwords do not match"
            )
        
        # Verificar que no usa la misma contraseña
        if verify_password(password_change.new_password, user_dict.get("password_hash", "")):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="New password must be different from current password"
            )
        
        # Hashear nueva contraseña y actualizar
        new_password_hash = hash_password(password_change.new_password)
        success = await change_user_password(current_user.email, new_password_hash)
        
        if not success:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error updating password"
            )
        
        logger.info(f"✅ Contraseña actualizada: {current_user.email}")
        
        return {
            "message": "Password changed successfully",
            "email": current_user.email
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error cambiando contraseña: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error changing password"
        )


@router.post("/change-email")
async def change_email(
    email_change: EmailChangeRequest,
    current_user: User = Depends(get_current_user)
):
    """
    📧 Cambiar email del usuario
    
    Request:
    {
        "new_email": "newemail@example.com",
        "password": "micontraseña"
    }
    
    Validaciones:
    - Email nuevo debe ser válido
    - Email nuevo no debe estar registrado
    - Contraseña debe ser correcta
    - En BD real: Enviar email de confirmación
    
    NOTA: De momento con MOCK DB, cambio es inmediato
    Con MongoDB: Se enviará email de confirmación
    """
    
    try:
        # Validar email nuevo
        try:
            validate_email(email_change.new_email)
        except EmailNotValidError as e:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Invalid email format: {str(e)}"
            )
        
        # Verificar que email nuevo no esté registrado
        if await email_exists(email_change.new_email):
            logger.warning(f"⚠️  Email ya está registrado: {email_change.new_email}")
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Email is already registered"
            )
        
        # Verificar que contraseña es correcta
        user_dict = await get_user_by_email(current_user.email)
        
        if not user_dict:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User not found"
            )
        
        if not verify_password(email_change.password, user_dict.get("password_hash", "")):
            logger.warning(f"⚠️  Intento de cambio de email con contraseña incorrecta: {current_user.email}")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Password is incorrect"
            )
        
        # Cambiar email
        success = await change_user_email(current_user.email, email_change.new_email)
        
        if not success:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Error changing email"
            )
        
        logger.info(f"✅ Email actualizado: {current_user.email} → {email_change.new_email}")
        
        return {
            "message": "Email changed successfully",
            "old_email": current_user.email,
            "new_email": email_change.new_email,
            "note": "Please log in again with your new email"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error cambiando email: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error changing email"
        )