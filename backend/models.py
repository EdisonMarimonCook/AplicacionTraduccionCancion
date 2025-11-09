"""
ARCHIVO: models.py
PROPÓSITO: Definir estructura de datos (usuarios, palabras, etc.)
USADO POR: Todos los endpoints
"""

from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, Field, EmailStr

# ================================================================
# USUARIO
# ================================================================

class UserBase(BaseModel):
    """Schema base de usuario (común a create/read)"""
    email: EmailStr
    username: str = Field(..., min_length=3, max_length=50)
    full_name: Optional[str] = None

class UserCreate(UserBase):
    """Schema para crear usuario (incluye password)"""
    password: str = Field(..., min_length=8)

class UserLogin(BaseModel):
    """Schema para login"""
    email: EmailStr
    password: str

class User(UserBase):
    """Schema de usuario guardado en BD"""
    id: str = Field(default_factory=lambda: str(datetime.now().timestamp()))
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
    is_active: bool = True
    language_level: str = "A1"  # A1, A2, B1, B2, C1, C2
    native_language: str = "en"
    learning_language: str = "es"
    
    class Config:
        populate_by_name = True

class Token(BaseModel):
    """Schema de token JWT"""
    access_token: str
    refresh_token: Optional[str] = None
    token_type: str = "bearer"
    expires_in: int  # segundos

class TokenData(BaseModel):
    """Data dentro del JWT"""
    email: Optional[str] = None
    exp: Optional[int] = None

# ================================================================
# DICCIONARIO PERSONAL
# ================================================================

class DictionaryEntry(BaseModel):
    """Entrada en diccionario personal del usuario"""
    id: str = Field(default_factory=lambda: str(datetime.now().timestamp()))
    user_id: str
    word: str
    language: str
    translation: Optional[str] = None
    definition: Optional[str] = None
    example: Optional[str] = None
    difficulty_level: str = "A1"
    created_at: datetime = Field(default_factory=datetime.now)
    last_reviewed: Optional[datetime] = None
    review_count: int = 0

# ================================================================
# CANCIÓN
# ================================================================

class Song(BaseModel):
    """Información de canción"""
    id: str
    title: str
    artist: str
    language: str
    spotify_id: Optional[str] = None
    preview_url: Optional[str] = None
    image_url: Optional[str] = None
    album: Optional[str] = None
    duration_ms: Optional[int] = None

class SongSession(BaseModel):
    """Sesión de estudio con una canción"""
    id: str = Field(default_factory=lambda: str(datetime.now().timestamp()))
    user_id: str
    song_id: str
    started_at: datetime = Field(default_factory=datetime.now)
    ended_at: Optional[datetime] = None
    words_learned: int = 0
    words_saved: int = 0
    score: Optional[int] = None