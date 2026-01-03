"""
ROUTER: Autenticación JWT (Versión Multi-idioma con Verificación Email)
"""
import logging
import random
from datetime import timedelta, datetime
from fastapi import APIRouter, HTTPException, Depends, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials  
from email_validator import validate_email, EmailNotValidError
from pydantic import BaseModel, EmailStr

from config import settings
from models import UserCreate, UserLogin, User, Token, LearningLanguage
from auth import (
    hash_password, verify_password, create_access_token,
    create_refresh_token, verify_refresh_token, verify_token
)
from crud import create_user, get_user_by_email, user_exists, username_exists
from services.email_service import send_verification_code, send_password_reset_code

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/auth", tags=["Authentication"])
security = HTTPBearer()

# ===============================================================================
# MODELOS ADICIONALES
# ===============================================================================

class VerifyRequest(BaseModel):
    email: EmailStr
    code: str

class ForgotPasswordRequest(BaseModel):
    email: EmailStr

class ResetPasswordRequest(BaseModel):
    email: EmailStr
    code: str
    new_password: str

# ===============================================================================
# FUNCIONES AUXILIARES
# ===============================================================================

def generate_pin() -> str:
    """Genera un PIN de 4 dígitos"""
    return str(random.randint(1000, 9999))

# ===============================================================================
# DEPENDENCY
# ===============================================================================

async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)) -> User:
    token = credentials.credentials
    token_data = verify_token(token)
    if not token_data: raise HTTPException(status_code=401, detail="Invalid token")
    
    email = token_data.get("sub") or token_data.get("email")
    if not email: raise HTTPException(status_code=401, detail="Invalid token payload")

    user_dict = await get_user_by_email(email)
    if not user_dict: raise HTTPException(status_code=401, detail="User not found")
    
    if "_id" in user_dict: user_dict["id"] = str(user_dict["_id"])
    return User(**user_dict)

# ===============================================================================
# ENDPOINTS
# ===============================================================================

@router.post("/register", response_model=dict)
async def register(user_data: UserCreate):
    """
    🔐 Registro con verificación de email
    """
    try:
        validate_email(user_data.email)
    except EmailNotValidError as e:
        raise HTTPException(status_code=400, detail=f"Invalid email: {str(e)}")
    
    if await user_exists(user_data.email):
        raise HTTPException(status_code=400, detail="Email already registered")
    
    # ✨ FASE 2.5: Validación username duplicado
    if await username_exists(user_data.username):
        raise HTTPException(status_code=400, detail="Username already taken")
    
    # Validar que venga al menos un idioma
    if not user_data.learning_languages:
        raise HTTPException(status_code=400, detail="Must select at least one language")
    
    # Generar código de verificación
    verification_code = generate_pin()
    
    # ✨ FASE 2.5: Detectar idioma principal (el primero de la lista)
    primary_lang = user_data.learning_languages[0].language if user_data.learning_languages else "en"
    
    # Procesar la lista para la BD con campos Fase 2.5
    processed_languages = []
    for lang in user_data.learning_languages:
        processed_languages.append({
            "language": lang.language,
            "level": lang.level,
            "started_at": datetime.now(), 
            "last_tested": None,
            # FASE 2.5: Campos nuevos
            "daily_goal": 10,
            "reviews_pending": 0,
            "is_active": True,
            "words_learned": 0
        })
    
    user_dict = {
        "email": user_data.email,
        "username": user_data.username,
        "full_name": user_data.full_name,
        "password_hash": hash_password(user_data.password),
        "is_active": True,
        "native_language": user_data.native_language,
        "learning_languages": processed_languages,
        "created_at": datetime.now(),
        # ✨ FASE 2.5: Campos nuevos
        "primary_language": primary_lang,
        "total_xp": 0,
        "burnout_limit": 50,
        "current_streak": 0,
        # Campos de verificación
        "is_verified": False,
        "verification_code": verification_code
    }
    
    await create_user(user_dict)
    logger.info(f"✅ Usuario registrado (pendiente verificación): {user_data.email}")
    
    # Enviar email de verificación
    try:
        await send_verification_code(user_data.email, verification_code)
        logger.info(f"📧 Email de verificación enviado a: {user_data.email}")
    except Exception as e:
        logger.error(f"❌ Error enviando email: {e}")
        # No bloqueamos el registro si falla el email
    
    return {
        "message": "Usuario registrado. Revisa tu correo para verificar la cuenta.",
        "email": user_data.email,
        "username": user_data.username,
        "verification_required": True
    }

@router.post("/verify")
async def verify_account(request: VerifyRequest):
    """
    ✅ Verificar cuenta con código de email
    """
    user_dict = await get_user_by_email(request.email)
    
    if not user_dict:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    
    if user_dict.get("is_verified"):
        return {"message": "La cuenta ya estaba verificada", "verified": True}
    
    # Comprobar código
    if user_dict.get("verification_code") == request.code:
        # Actualizar usuario (necesitas implementar esta función en crud.py)
        from database import db
        await db.update_user(request.email, {
            "is_verified": True,
            "verification_code": None
        })
        
        logger.info(f"✅ Cuenta verificada: {request.email}")
        return {"message": "Cuenta verificada con éxito ✅", "verified": True}
    else:
        raise HTTPException(status_code=400, detail="Código incorrecto ❌")

@router.post("/login", response_model=Token)
async def login(credentials: UserLogin):
    """
    🔐 Login (requiere cuenta verificada)
    """
    user_dict = await get_user_by_email(credentials.email)
    
    if not user_dict or not verify_password(credentials.password, user_dict.get("password_hash", "")):
        raise HTTPException(status_code=401, detail="Invalid email or password")
    
    # Verificar si la cuenta está verificada
    if not user_dict.get("is_verified", False):
        raise HTTPException(
            status_code=403, 
            detail="Cuenta no verificada. Revisa tu email."
        )
    
    access_token = create_access_token(data={"sub": user_dict["email"]}, expires_delta=timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES))
    refresh_token = create_refresh_token(data={"sub": user_dict["email"]}, expires_delta=timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS))
    
    # Mapear respuesta
    langs = user_dict.get("learning_languages", [])
    mapped_langs = []
    for l in langs:
        start = l.get("started_at")
        if isinstance(start, datetime): start = start.isoformat()
        
        mapped_langs.append(LearningLanguage(
            language=l.get("language"),
            level=l.get("level"),
            started_at=str(start) if start else None,
            last_tested=None
        ))

    logger.info(f"✅ Login exitoso: {credentials.email}")

    return Token(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer",
        expires_in=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        user_id=str(user_dict.get("_id") or user_dict.get("id")),
        email=credentials.email,
        username=user_dict.get("username"),
        learning_languages=mapped_langs
    )

@router.post("/forgot-password")
async def forgot_password(request: ForgotPasswordRequest):
    """
    📧 Solicitar código de recuperación de contraseña
    """
    user_dict = await get_user_by_email(request.email)
    
    # Por seguridad, siempre devolvemos OK aunque no exista
    if not user_dict:
        return {"message": "Si el correo existe, se ha enviado un código."}
    
    # Generar código
    reset_code = generate_pin()
    
    # Guardar en BD
    from database import db
    await db.update_user(request.email, {"reset_code": reset_code})
    
    # Enviar email
    try:
        await send_password_reset_code(request.email, reset_code)
        logger.info(f"📧 Código de recuperación enviado a: {request.email}")
    except Exception as e:
        logger.error(f"❌ Error enviando email de recuperación: {e}")
    
    return {"message": "Código enviado a tu correo 📧"}

@router.post("/reset-password")
async def reset_password(request: ResetPasswordRequest):
    """
    🔑 Cambiar contraseña con código de recuperación
    """
    user_dict = await get_user_by_email(request.email)
    
    if not user_dict:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    
    # Verificar código
    if user_dict.get("reset_code") == request.code:
        # Cambiar contraseña
        new_hashed_password = hash_password(request.new_password)
        
        from database import db
        await db.update_user(request.email, {
            "password_hash": new_hashed_password,
            "reset_code": None
        })
        
        logger.info(f"🔑 Contraseña cambiada: {request.email}")
        return {"message": "Contraseña actualizada correctamente 🔑"}
    else:
        raise HTTPException(status_code=400, detail="Código inválido o expirado")


@router.post("/refresh", response_model=dict)
async def refresh_access_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    """
    Refresca el Access Token Y TAMBIÉN el Refresh Token (Rotación).
    Esto hace que la sesión sea infinita mientras el usuario use la app.
    """
    token = credentials.credentials
    # 1. Verificar el refresh token actual
    token_data = verify_refresh_token(token)
    if not token_data: 
        raise HTTPException(status_code=401, detail="Refresh token inválido o expirado")
    email = token_data.get("email") or token_data.get("sub")
    # 2. Crear NUEVO Access Token (vida corta)
    new_access_token = create_access_token(
        data={"sub": email}, 
        expires_delta=timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    )
    # 3. Crear NUEVO Refresh Token (Reiniciamos el contador de 30 días)
    new_refresh_token = create_refresh_token(
        data={"sub": email}, 
        expires_delta=timedelta(days=30) 
    )
    # 4. Devolver AMBOS
    return {
        "access_token": new_access_token, 
        "refresh_token": new_refresh_token, # <--- IMPORTANTE: Enviamos el nuevo pase
        "token_type": "bearer", 
        "expires_in": settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60, 
        "status": "success"
    }

@router.get("/me", response_model=User)
async def get_current_user_info(current_user: User = Depends(get_current_user)):
    return current_user