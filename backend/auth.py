"""
MÓDULO: Utilidades de Autenticación
Contiene funciones reutilizables para JWT y contraseñas
"""

import logging
from datetime import datetime, timedelta, timezone
from typing import Optional, Dict
import bcrypt
from jose import JWTError, jwt
from config import settings

logger = logging.getLogger(__name__)

# ===============================================================================
# FUNCIONES DE CONTRASEÑA
# ===============================================================================

def hash_password(password: str) -> str:
    """
    🔐 HASHEA UNA CONTRASEÑA CON BCRYPT
    IMPORTANTE: Truncamos a 72 bytes para evitar errores de Bcrypt.
    """
    password_bytes = password.encode('utf-8')[:72]
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(password_bytes, salt)
    return hashed.decode('utf-8')

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """
    ✅ VERIFICA UNA CONTRASEÑA CONTRA SU HASH
    """
    plain_bytes = plain_password.encode('utf-8')[:72]
    return bcrypt.checkpw(plain_bytes, hashed_password.encode('utf-8'))

# ===============================================================================
# FUNCIONES JWT
# ===============================================================================

def create_access_token(data: Dict, expires_delta: Optional[timedelta] = None) -> str:
    """
    🎫 CREA UN ACCESS TOKEN (Para peticiones cortas)
    """
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(minutes=15)
    
    to_encode.update({"exp": expire})
    
    encoded_jwt = jwt.encode(
        to_encode,
        settings.SECRET_KEY,
        algorithm=settings.ALGORITHM
    )
    return encoded_jwt

def create_refresh_token(data: Dict, expires_delta: Optional[timedelta] = None) -> str:
    """
    🔄 CREA UN REFRESH TOKEN (Para mantener sesión, válido 7 días)
    """
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(days=7)
    
    to_encode.update({"exp": expire, "type": "refresh"})
    
    encoded_jwt = jwt.encode(
        to_encode,
        settings.SECRET_KEY,
        algorithm=settings.ALGORITHM
    )
    logger.info(f"✅ Refresh token creado para: {data.get('sub')}")
    return encoded_jwt

def verify_refresh_token(token: str) -> Optional[Dict]:
    """
    ✅ VERIFICA UN REFRESH TOKEN
    """
    try:
        payload = jwt.decode(
            token,
            settings.SECRET_KEY,
            algorithms=[settings.ALGORITHM]
        )
        
        if payload.get("type") != "refresh":
            logger.warning("⚠️  Token no es de tipo refresh")
            return None
        
        email: str = payload.get("sub")
        if email is None:
            return None
            
        logger.info(f"✅ Refresh token verificado para: {email}")
        return {"email": email} # El router de refresh busca "email", así que este está bien
    
    except JWTError:
        logger.warning("⚠️  Refresh token inválido o expirado")
        return None

def verify_token(token: str) -> Optional[Dict]:
    """
    ✅ VERIFICA UN ACCESS TOKEN
    """
    try:
        payload = jwt.decode(
            token,
            settings.SECRET_KEY,
            algorithms=[settings.ALGORITHM]
        )
        
        email: str = payload.get("sub")
        
        if email is None:
            return None
        
        # ⚠️ CORRECCIÓN CLAVE AQUÍ:
        # Devolvemos 'sub' porque routers/auth.py busca token_data.get("sub")
        return {"sub": email} 
    
    except JWTError:
        return None