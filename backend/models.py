"""
ARCHIVO: models.py
PROPÓSITO: Definir estructura de datos (usuarios, palabras, etc.)
"""

from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, Field, EmailStr
from typing import Union

# ================================================================
# MODELOS AUXILIARES
# ================================================================

class LearningLanguage(BaseModel):
    """Modelo interno para la lista de idiomas en BD"""
    language: str
    level: str
    
    # 🔥 SOLUCIÓN: Aceptamos str (del JSON) o datetime (de MongoDB)
    started_at: Optional[Union[str, datetime]] = None 
    
    last_tested: Optional[datetime] = None
    
    # ✨ FASE 2.5: Campos con defaults para compatibilidad retroactiva
    daily_goal: int = Field(default=10)
    reviews_pending: int = Field(default=0)
    is_active: bool = Field(default=True)
    words_learned: int = Field(default=0)
    
    # Esto ayuda a convertir automáticamente tipos si es necesario
    class Config:
        populate_by_name = True

# ================================================================
# USUARIO
# ================================================================

class UserBase(BaseModel):
    email: EmailStr
    username: str = Field(..., min_length=3, max_length=50)
    full_name: Optional[str] = None

class UserCreate(BaseModel):
    """
    Schema para crear usuario (Versión Multi-idioma)
    Recibe una LISTA de idiomas para soportar la visión completa.
    """
    email: EmailStr
    username: str
    full_name: Optional[str] = None
    password: str
    native_language: str = "es"
    
    # ✅ AHORA SÍ: Aceptamos la lista (Coincide con tu RegisterActivity)
    learning_languages: List[LearningLanguage] = Field(default=[])

class UserLogin(BaseModel):
    email: EmailStr
    password: str

class User(UserBase):
    id: str = Field(default_factory=lambda: str(datetime.now().timestamp()))
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
    is_active: bool = True
    
    learning_languages: List[LearningLanguage] = Field(default=[])
    native_language: str = "es"
    
    # ✨ FASE 2.5: Idioma principal y gamificación
    primary_language: str = Field(default="en")
    total_xp: int = Field(default=0)
    burnout_limit: int = Field(default=50)
    
    # ✅ CAMPOS PARA RACHA
    current_streak: int = 0
    last_activity_date: Optional[datetime] = None
    longest_streak: int = 0
    avatar_url: Optional[str] = None
    
    class Config:
        populate_by_name = True
# En backend/models.py

class Token(BaseModel):
    access_token: str
    refresh_token: Optional[str] = None
    token_type: str = "bearer"
    expires_in: int
    user_id: Optional[str] = None
    email: Optional[str] = None
    username: Optional[str] = None  # ✅ AÑADIR ESTO
    learning_languages: Optional[List[LearningLanguage]] = None

class TokenData(BaseModel):
    email: Optional[str] = None
    exp: Optional[int] = None

# ================================================================
# DICCIONARIO Y CANCIONES
# ================================================================

class DictionaryEntry(BaseModel):
    id: str = Field(default_factory=lambda: str(datetime.now().timestamp()))
    user_id: str
    word: str
    translation: Optional[str] = None
    type: str = "word"
    example: Optional[str] = None
    is_recommended: bool = False
    notes: Optional[str] = None
    song_id: Optional[str] = None
    language: str = "en"  # Código del idioma
    
    # 🔊 AUDIO (NUEVO)
    song_youtube_url: Optional[str] = None  # URL de YouTube si viene de una canción
    timestamp_start: Optional[float] = None  # Segundo exacto donde empieza la palabra
    timestamp_end: Optional[float] = None  # Segundo exacto donde termina la palabra
    
    created_at: datetime = Field(default_factory=datetime.now)
    last_reviewed: Optional[datetime] = None
    times_reviewed: int = 0
    is_learned: bool = False

class Song(BaseModel):
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
    id: str = Field(default_factory=lambda: str(datetime.now().timestamp()))
    user_id: str
    song_id: str
    started_at: datetime = Field(default_factory=datetime.now)
    ended_at: Optional[datetime] = None
    words_learned: int = 0
    words_saved: int = 0
    score: Optional[int] = None