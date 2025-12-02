"""
ARCHIVO: models.py
PROPÓSITO: Definir estructura de datos (usuarios, palabras, etc.)
USADO POR: Todos los endpoints
"""

from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, Field, EmailStr
from routers.schemas import LearningLanguage

# ================================================================
# USUARIO
# ================================================================

class UserBase(BaseModel):
    """Schema base de usuario (común a create/read)"""
    email: EmailStr
    username: str = Field(..., min_length=3, max_length=50)
    full_name: Optional[str] = None

class UserCreate(BaseModel):
    """Schema para crear usuario (incluye password)"""
    email: str
    username: str
    full_name: Optional[str] = None
    password: str
    native_language: str = "es"
    learning_languages: List[LearningLanguage] = Field(
        default=[LearningLanguage(language="es", level="A1")],
        description="Idiomas que quiere aprender"
    )

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
    
    learning_languages: List[LearningLanguage] = Field(
        default=[LearningLanguage(language="es", level="A1")]
    )
    native_language: str = "es"
    
    class Config:
        populate_by_name = True

class Token(BaseModel):
    """Schema de token JWT"""
    access_token: str
    refresh_token: Optional[str] = None
    token_type: str = "bearer"
    expires_in: int  # segundos
    user_id: Optional[str] = None
    email: Optional[str] = None
    learning_languages: Optional[List[LearningLanguage]] = None

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
    translation: Optional[str] = None
    
    # 🔥 CAMPOS NUEVOS V3.1
    type: str = "word"           # Para tus carpetas
    example: Optional[str] = None # Para flashcards
    is_recommended: bool = False  # Estrellita
    
    notes: Optional[str] = None
    song_id: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.now)
    last_reviewed: Optional[datetime] = None
    times_reviewed: int = 0
    is_learned: bool = False

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