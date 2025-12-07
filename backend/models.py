"""
ARCHIVO: models.py
PROPÓSITO: Definir estructura de datos (usuarios, palabras, etc.)
<<<<<<< HEAD
USADO POR: Todos los endpoints
=======
>>>>>>> feature/lyrics-translation
"""

from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, Field, EmailStr
<<<<<<< HEAD

# ================================================================
# LEARNING LANGUAGE (NUEVO)
# ================================================================

class LearningLanguage(BaseModel):
    """Schema para idioma que está aprendiendo"""
    language: str = Field(..., description="es, en, fr, de, it, pt, ja, ko, zh")
    level: str = Field(default="A1", description="A1, A2, B1, B2, C1, C2")
    started_at: datetime = Field(default_factory=datetime.now)
    last_tested: Optional[datetime] = None
=======
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
    
    # Esto ayuda a convertir automáticamente tipos si es necesario
    class Config:
        populate_by_name = True
>>>>>>> feature/lyrics-translation

# ================================================================
# USUARIO
# ================================================================

class UserBase(BaseModel):
<<<<<<< HEAD
    """Schema base de usuario (común a create/read)"""
=======
>>>>>>> feature/lyrics-translation
    email: EmailStr
    username: str = Field(..., min_length=3, max_length=50)
    full_name: Optional[str] = None

<<<<<<< HEAD
class UserCreate(UserBase):
    """Schema para crear usuario (incluye password)"""
    password: str = Field(..., min_length=8)
    learning_languages: List[LearningLanguage] = Field(
        default=[LearningLanguage(language="es", level="A1")],
        description="Idiomas que quiere aprender"
    )

class UserLogin(BaseModel):
    """Schema para login"""
=======
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
>>>>>>> feature/lyrics-translation
    email: EmailStr
    password: str

class User(UserBase):
<<<<<<< HEAD
    """Schema de usuario guardado en BD"""
=======
>>>>>>> feature/lyrics-translation
    id: str = Field(default_factory=lambda: str(datetime.now().timestamp()))
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
    is_active: bool = True
    
<<<<<<< HEAD
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
=======
    learning_languages: List[LearningLanguage] = Field(default=[])
    native_language: str = "es"
    
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
>>>>>>> feature/lyrics-translation
    email: Optional[str] = None
    exp: Optional[int] = None

# ================================================================
<<<<<<< HEAD
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
=======
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
    created_at: datetime = Field(default_factory=datetime.now)
    last_reviewed: Optional[datetime] = None
    times_reviewed: int = 0
    is_learned: bool = False

class Song(BaseModel):
>>>>>>> feature/lyrics-translation
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
<<<<<<< HEAD
    """Sesión de estudio con una canción"""
=======
>>>>>>> feature/lyrics-translation
    id: str = Field(default_factory=lambda: str(datetime.now().timestamp()))
    user_id: str
    song_id: str
    started_at: datetime = Field(default_factory=datetime.now)
    ended_at: Optional[datetime] = None
    words_learned: int = 0
    words_saved: int = 0
    score: Optional[int] = None