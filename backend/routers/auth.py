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
from models import UserCreate, UserLogin, User, Token, LearningLanguage
from auth import (
    hash_password,
    verify_password,
    create_access_token,
    create_refresh_token,     
    verify_refresh_token,       
    verify_token
)
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
    token = credentials.credentials
    
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
    🔐 Registro de nuevo usuario CON IDIOMAS
    
    Request:
    {
      "email": "user@example.com",
      "username": "user123",
      "password": "password123",
      "native_language": "es",          ✅ NUEVO
      "learning_languages": [
        {
          "language": "es",
          "level": "A1"
        },
        {
          "language": "en",
          "level": "B1"
        }
      ]
    }
    
    - Valida email
    - Verifica que no exista
    - Encripta contraseña
    - Guarda learning_languages
    - Guarda native_language
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
    
    # Validar que haya al menos un idioma
    if not user_data.learning_languages or len(user_data.learning_languages) == 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Must select at least one language"
        )
    
    # Crear usuario 🆕 CON learning_languages y native_language
    user_dict = {
        "email": user_data.email,
        "username": user_data.username,
        "full_name": user_data.full_name,
        "password_hash": hash_password(user_data.password),
        "is_active": True,
        "native_language": user_data.native_language,    # ✅ CAMBIAR (era "en")
        "learning_languages": [
            {
                "language": lang.language,
                "level": lang.level,
                "started_at": lang.started_at.isoformat(),
                "last_tested": None
            }
            for lang in user_data.learning_languages
        ]
    }
    
    await create_user(user_dict)
    logger.info(f"✅ Nuevo usuario registrado: {user_data.email}")
    logger.info(f"📚 Idiomas: {[f'{l.language}({l.level})' for l in user_data.learning_languages]}")
    logger.info(f"🌍 Idioma nativo: {user_data.native_language}")
    
    return {
        "message": "User registered successfully",
        "email": user_data.email,
        "username": user_data.username,
        "native_language": user_data.native_language,
        "learning_languages": [
            {"language": l.language, "level": l.level}
            for l in user_data.learning_languages
        ]
    }

@router.post("/login", response_model=Token)
async def login(credentials: UserLogin):
    """
    🔐 Login de usuario y generación de token JWT
    
    AHORA RETORNA: 
    - access_token (15 min)
    - refresh_token (7 días)    ✅ NUEVO
    - learning_languages
    - user_id
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
    
    # Crear access_token (15 minutos)
    access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": user_dict["email"]},
        expires_delta=access_token_expires
    )
    
    # ✅ AGREGAR: Crear refresh_token (7 días)
    refresh_token = create_refresh_token(
        data={"sub": user_dict["email"]},
        expires_delta=timedelta(days=7)
    )
    
    # Preparar learning_languages para response
    learning_languages = [
        LearningLanguage(
            language=lang.get("language"),
            level=lang.get("level"),
            started_at=lang.get("started_at"),
            last_tested=lang.get("last_tested")
        )
        for lang in user_dict.get("learning_languages", [])
    ]
    
    logger.info(f"✅ Login exitoso: {credentials.email}")
    
    return Token(
        access_token=access_token,
        refresh_token=refresh_token,    # ✅ AGREGAR ESTO
        token_type="bearer",
        expires_in=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        user_id=user_dict.get("_id") or user_dict.get("id"),
        email=credentials.email,
        learning_languages=learning_languages
    )

@router.get("/me", response_model=User)
async def get_current_user_info(current_user: User = Depends(get_current_user)):
    """
    👤 Obtiene información del usuario actual autenticado
    AHORA INCLUYE: learning_languages + native_language
    """
    logger.info(f"✅ Información del usuario solicitada: {current_user.email}")
    return current_user

@router.post("/verify-token")
async def verify_token_endpoint(credentials: HTTPAuthorizationCredentials = Depends(security)):
    """
    ✅ Verifica si un token es válido
    """
    token_data = verify_token(credentials.credentials)
    
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

# ===============================================================================
# ✅ NUEVO ENDPOINT: REFRESH TOKEN
# ===============================================================================

@router.post("/refresh", response_model=dict)
async def refresh_access_token(
    credentials: HTTPAuthorizationCredentials = Depends(security)
):
    """
    🔄 REFRESCA EL ACCESS TOKEN USANDO REFRESH TOKEN
    
    Cuando el access_token expire (15 minutos),
    usa el refresh_token (7 días) para obtener uno nuevo.
    
    PARÁMETROS:
    - Authorization header con refresh_token
    
    EJEMPLO:
    POST /api/v1/auth/refresh
    Headers: Authorization: Bearer {refresh_token}
    
    RESPUESTA:
    {
      "access_token": "eyJhbGc... (nuevo)",
      "token_type": "bearer",
      "expires_in": 900,
      "status": "success"
    }
    """
    
    try:
        logger.info(f"🔄 Intentando refrescar token...")
        
        # Verificar que es un refresh_token válido
        token_data = await verify_refresh_token(credentials.credentials)
        
        if not token_data:
            logger.warning("⚠️  Refresh token inválido o expirado")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or expired refresh token",
                headers={"WWW-Authenticate": "Bearer"},
            )
        
        email = token_data.get("email")
        
        # Crear nuevo access_token
        access_token_expires = timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
        access_token = create_access_token(
            data={"sub": email},
            expires_delta=access_token_expires
        )
        
        logger.info(f"✅ Token refrescado exitosamente para: {email}")
        
        return {
            "access_token": access_token,
            "token_type": "bearer",
            "expires_in": settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
            "status": "success"
        }
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error refrescando token: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error refreshing token"
        )