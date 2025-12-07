"""
ROUTER: Autenticación JWT (Versión Multi-idioma)
"""
import logging
from datetime import timedelta, datetime
from fastapi import APIRouter, HTTPException, Depends, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials  
from email_validator import validate_email, EmailNotValidError

from config import settings
from models import UserCreate, UserLogin, User, Token, LearningLanguage
from auth import (
    hash_password, verify_password, create_access_token,
    create_refresh_token, verify_refresh_token, verify_token
)
from crud import create_user, get_user_by_email, user_exists

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/auth", tags=["Authentication"])
security = HTTPBearer()

# --- DEPENDENCY ---
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

# --- ENDPOINTS ---

@router.post("/register", response_model=dict)
async def register(user_data: UserCreate):
    """
    🔐 Registro soportando lista de idiomas
    """
    try:
        validate_email(user_data.email)
    except EmailNotValidError as e:
        raise HTTPException(status_code=400, detail=f"Invalid email: {str(e)}")
    
    if await user_exists(user_data.email):
        raise HTTPException(status_code=400, detail="Email already registered")
    
    # Validar que venga al menos un idioma
    if not user_data.learning_languages:
        raise HTTPException(status_code=400, detail="Must select at least one language")
    
    # Procesar la lista para la BD
    processed_languages = []
    for lang in user_data.learning_languages:
        processed_languages.append({
            "language": lang.language,
            "level": lang.level,
            # Si no viene fecha, usamos ahora.
            "started_at": datetime.now(), 
            "last_tested": None
        })
    
    user_dict = {
        "email": user_data.email,
        "username": user_data.username,
        "full_name": user_data.full_name,
        "password_hash": hash_password(user_data.password),
        "is_active": True,
        "native_language": user_data.native_language,
        "learning_languages": processed_languages, # ✅ Guardamos la lista
        "created_at": datetime.now()
    }
    
    await create_user(user_dict)
    logger.info(f"✅ Usuario registrado: {user_data.email}")
    
    return {
        "message": "User registered successfully",
        "email": user_data.email,
        "username": user_data.username,
        "learning_languages": processed_languages
    }

@router.post("/login", response_model=Token)
async def login(credentials: UserLogin):
    user_dict = await get_user_by_email(credentials.email)
    
    if not user_dict or not verify_password(credentials.password, user_dict.get("password_hash", "")):
        raise HTTPException(status_code=401, detail="Invalid email or password")
    
    access_token = create_access_token(data={"sub": user_dict["email"]}, expires_delta=timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES))
    refresh_token = create_refresh_token(data={"sub": user_dict["email"]}, expires_delta=timedelta(days=7))
    
    # Mapear respuesta
    langs = user_dict.get("learning_languages", [])
    mapped_langs = []
    for l in langs:
        start = l.get("started_at")
        # Asegurar formato string para JSON
        if isinstance(start, datetime): start = start.isoformat()
        
        mapped_langs.append(LearningLanguage(
            language=l.get("language"),
            level=l.get("level"),
            started_at=str(start) if start else None,
            last_tested=None
        ))

    return Token(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer",
        expires_in=settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        user_id=str(user_dict.get("_id") or user_dict.get("id")),
        email=credentials.email,
        username=user_dict.get("username"), # ✅ AÑADIR ESTO
        learning_languages=mapped_langs
    )

@router.post("/refresh", response_model=dict)
async def refresh_access_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token_data = verify_refresh_token(credentials.credentials)
    if not token_data: raise HTTPException(status_code=401, detail="Invalid refresh token")
    email = token_data.get("email") or token_data.get("sub")
    
    new_token = create_access_token(data={"sub": email}, expires_delta=timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES))
    
    return {"access_token": new_token, "token_type": "bearer", "expires_in": settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60, "status": "success"}

@router.get("/me", response_model=User)
async def get_current_user_info(current_user: User = Depends(get_current_user)):
    return current_user