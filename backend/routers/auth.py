"""
ROUTER: Autenticación JWT
Endpoints: login, registro, verificación de token
USA: auth.py (funciones auxiliares), crud.py (BD)
"""

import logging
from datetime import timedelta
from fastapi import APIRouter, HTTPException, Depends, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials  
from email_validator import validate_email, EmailNotValidError

from config import settings
from models import UserCreate, UserLogin, User, Token
from auth import hash_password, verify_password, create_access_token, verify_token
from crud import create_user, get_user_by_email, user_exists

# ===============================================================================
# CONFIGURACIÓN
# ===============================================================================

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/auth", tags=["Authentication"])
security = HTTPBearer()

# ===============================================================================
# DEPENDENCY
# ===============================================================================

async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)) -> User:
    """Dependency para obtener usuario actual del token"""
    token = credentials.credentials  # ← CAMBIAR: credentials.credentials
    
    # Verificar token
    token_data = verify_token(token)
    if not token_data:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    # Obtener usuario de BD
    user_dict = await get_user_by_email(token_data["email"])
    
    if not user_dict:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    return User(**user_dict)

# ===============================================================================
# ENDPOINTS
# ===============================================================================

@router.post("/register", response_model=dict)
async def register(user_data: UserCreate):
    """
    🔐 Registro de nuevo usuario
    
    - Valida email
    - Verifica que no exista
    - Encripta contraseña
    - Guarda en BD
    """
    try:
        validate_email(user_data.email)
    except EmailNotValidError as e:
        logger.warning(f"⚠️  Email inválido: {user_data.email}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid email: {str(e)}"
        )
    
    # Verificar si usuario ya existe
    if await user_exists(user_data.email):
        logger.warning(f"⚠️  Usuario ya existe: {user_data.email}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email already registered"
        )
    
    # Crear usuario
    user_dict = {
        "email": user_data.email,
        "username": user_data.username,
        "full_name": user_data.full_name,
        "password_hash": hash_password(user_data.password),
        "is_active": True,
        "language_level": "A1",
        "native_language": "en",
        "learning_language": "es"
    }
    
    await create_user(user_dict)
    logger.info(f"✅ Nuevo usuario registrado: {user_data.email}")
    
    return {
        "message": "User registered successfully",
        "email": user_data.email,
        "username": user_data.username
    }

@router.post("/login", response_model=Token)
async def login(credentials: UserLogin):
    """
    🔐 Login de usuario y generación de token JWT
    """
    user_dict = await get_user_by_email(credentials.email)
    
    if not user_dict:
        logger.warning(f"⚠️  Login fallido - usuario no encontrado: {credentials.email}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password"
        )
    
    if not verify_password(credentials.password, user_dict.get("password_hash", "")):
        logger.warning(f"⚠️  Login fallido - contraseña incorrecta: {credentials.email}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password"
        )
    
    # Crear token
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": user_dict["email"]},
        expires_delta=access_token_expires
    )
    
    logger.info(f"✅ Login exitoso: {credentials.email}")
    
    return Token(
        access_token=access_token,
        token_type="bearer",
        expires_in=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60
    )

@router.get("/me", response_model=User)
async def get_current_user_info(current_user: User = Depends(get_current_user)):
    """
    👤 Obtiene información del usuario actual autenticado
    """
    logger.info(f"✅ Información del usuario solicitada: {current_user.email}")
    return current_user

@router.post("/verify-token")
async def verify_token_endpoint(credentials: HTTPAuthorizationCredentials = Depends(security)):
    """
    ✅ Verifica si un token es válido
    """
    token_data = verify_token(credentials.credentials)  # ← CAMBIAR: credentials.credentials
    
    if not token_data:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token"
        )
    
    logger.info(f"✅ Token válido para: {token_data['email']}")
    
    return {
        "valid": True,
        "email": token_data["email"]
    }