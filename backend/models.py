"""
ARCHIVO: models.py
PROPÓSITO: Definir estructura de datos (usuarios, palabras, etc.)
USADO POR: Todos los endpoints
"""

from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, Field, EmailStr

# ================================================================
# LEARNING LANGUAGE (NUEVO)
# ================================================================

class LearningLanguage(BaseModel):
    """Schema para idioma que está aprendiendo"""
    language: str = Field(..., description="es, en, fr, de, it, pt, ja, ko, zh")
    level: str = Field(default="A1", description="A1, A2, B1, B2, C1, C2")
    started_at: datetime = Field(default_factory=datetime.now)
    last_tested: Optional[datetime] = None

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
    
    # 🆕 NUEVOS CAMPOS
    learning_languages: List[LearningLanguage] = Field(
        default=[LearningLanguage(language="es", level="A1")]
    )
    native_language: str = "en"
    
    # ❌ ELIMINADOS (ya no necesarios)
    # language_level: str = "A1"
    # learning_language: str = "es"
    
    class Config:
        populate_by_name = True

class Token(BaseModel):
    """Schema de token JWT"""
    access_token: str
    refresh_token: Optional[str] = None
    token_type: str = "bearer"
    expires_in: int  # segundos
    user_id: Optional[str] = None  # 🆕
    email: Optional[str] = None  # 🆕
    learning_languages: Optional[List[LearningLanguage]] = None  # 🆕

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
    category: str = Field(default="palabra", description="palabra o expresión")  # 🆕
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