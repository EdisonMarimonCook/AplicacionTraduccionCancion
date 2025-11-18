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
    
    IMPORTANTE: Bcrypt solo acepta máximo 72 bytes
    Si la contraseña es más larga, la truncamos ANTES de hashear
    """
    
    # ✅ TRUNCAR A 72 BYTES ANTES DE HASHEAR
    password_bytes = password.encode('utf-8')[:72]
    
    # Hashear
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(password_bytes, salt)
    
    return hashed.decode('utf-8')

# ===============================================================================

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """
    ✅ VERIFICA UNA CONTRASEÑA CONTRA SU HASH
    
    IMPORTANTE: También truncar aquí para ser consistente
    """
    
    # ✅ TRUNCAR A 72 BYTES ANTES DE VERIFICAR (mismo que en hash)
    plain_bytes = plain_password.encode('utf-8')[:72]
    
    # Verificar
    return bcrypt.checkpw(plain_bytes, hashed_password.encode('utf-8'))

# ===============================================================================
# FUNCIONES JWT
# ===============================================================================

def create_access_token(data: Dict, expires_delta: Optional[timedelta] = None) -> str:
    """
    🎫 CREA UN TOKEN JWT
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

# ===============================================================================

def verify_token(token: str) -> Optional[Dict]:
    """
    ✅ VERIFICA UN TOKEN JWT
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
        
        return {"email": email}
    
    except JWTError:
        return None