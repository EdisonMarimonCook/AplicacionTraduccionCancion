"""
ARCHIVO: models.py
PROPÓSITO: Definir estructura de datos (usuarios, palabras, etc.)
USADO POR: Todos los endpoints
"""

from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, Field, EmailStr
from enum import Enum

# ================================================================
# NIVELES DE IDIOMA
# ================================================================

class LanguageLevel(str, Enum):
    """Niveles según Marco Común Europeo"""
    A1 = "A1"
    A2 = "A2"
    B1 = "B1"
    B2 = "B2"
    C1 = "C1"
    C2 = "C2"

# ================================================================
# PREFERENCIAS DEL USUARIO
# ================================================================

class UserPreferences(BaseModel):
    """Qué idiomas estudia el usuario y su nivel"""
    preferred_languages: List[str] = Field(
        default=["en"],
        description="Idiomas que estudia (ej: ['es', 'en', 'jp'])"
    )
    levels: dict = Field(
        default={"en": LanguageLevel.A1},
        description="Nivel por idioma (ej: {'es': 'B1', 'en': 'A2'})"
    )

# ================================================================
# USUARIO
# ================================================================

class User(BaseModel):
    """
    Usuario de la aplicación
    """
    id: Optional[str] = Field(None, alias="_id")
    email: EmailStr  # Valida que sea email
    password_hash: str  # Contraseña encriptada (NO en texto plano)
    username: str
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
    preferences: UserPreferences = Field(default_factory=UserPreferences)
    is_active: bool = True
    songsCompleted: List[str] = Field(
        default=[],
        description="IDs de Spotify de canciones que completó"
    )
    
    class Config:
        populate_by_name = True

class UserPublicResponse(BaseModel):
    """
    Lo que se ENVÍA al cliente (sin password_hash) ← IMPORTANTE
    Esto es lo que ve Android
    """
    id: str
    email: str
    username: str
    level: str  # Nivel principal
    songsCompleted: List[str]
    preferences: UserPreferences

# ================================================================
# DICCIONARIO PERSONAL
# ================================================================

class DictionaryEntry(BaseModel):
    """
    Palabra que el usuario añadió a su diccionario personal
    """
    id: Optional[str] = Field(None, alias="_id")
    user_id: str
    word: str  # Ej: "blinding"
    translation: str  # Ej: "cegador"
    definition: str  # Ej: "que ciega o deslumbra"
    language: str  # Ej: "en"
    context: str  # Frase: "I'm blinded by the lights"
    source_song_id: str  # ID de Spotify
    source_song_name: str  # Nombre canción
    
    # Para spaced repetition (repaso espaciado)
    created_at: datetime = Field(default_factory=datetime.now)
    last_reviewed: Optional[datetime] = None
    review_count: int = 0  # Cuántas veces la revisó
    next_review_date: Optional[datetime] = None
    difficulty_rating: int = 0  # 1-5 (para ajustar frecuencia)
    
    class Config:
        populate_by_name = True

# ================================================================
# HISTORIAL DE CANCIONES ESTUDIADAS
# ================================================================

class SongSession(BaseModel):
    """
    Sesión donde el usuario estudió una canción
    """
    id: Optional[str] = Field(None, alias="_id")
    user_id: str
    song_id: str
    song_name: str
    artist: str
    language: str
    
    # Tiempo
    started_at: datetime = Field(default_factory=datetime.now)
    ended_at: Optional[datetime] = None
    duration_seconds: int = 0
    
    # Progreso
    fragments_studied: int = 0  # Fragmentos estudiados
    total_fragments: int = 0
    words_added: int = 0  # Palabras añadidas al diccionario
    
    class Config:
        populate_by_name = True

# ================================================================
# PROGRESO DEL USUARIO
# ================================================================

class LanguageProgress(BaseModel):
    """Estadísticas por idioma"""
    language: str
    songs_studied: int = 0
    words_added: int = 0
    words_mastered: int = 0  # Revisadas 5+ veces
    total_study_hours: float = 0.0
    current_level: LanguageLevel = LanguageLevel.A1

class UserProgress(BaseModel):
    """Progreso general del usuario"""
    id: Optional[str] = Field(None, alias="_id")
    user_id: str
    stats_by_language: dict = Field(default_factory=dict)
    updated_at: datetime = Field(default_factory=datetime.now)
    
    class Config:
        populate_by_name = True

# ================================================================
# AUTENTICACIÓN
# ================================================================

class TokenResponse(BaseModel):
    """Respuesta después de login"""
    access_token: str
    refresh_token: Optional[str] = None
    token_type: str = "bearer"
    expires_in: int

class TokenPayload(BaseModel):
    """Contenido del JWT"""
    sub: str  # user_id
    iat: datetime
    exp: datetime

# ================================================================
# REQUESTS (Entrada de datos)
# ================================================================

class UserRegisterRequest(BaseModel):
    """Datos para registrarse"""
    email: EmailStr
    username: str
    password: str
    preferred_languages: List[str] = ["en"]

class UserLoginRequest(BaseModel):
    """Datos para login"""
    email: EmailStr
    password: str

class AddToDictionaryRequest(BaseModel):
    """Datos para añadir palabra"""
    word: str
    translation: str
    definition: str
    language: str
    context: str
    source_song_id: str
    source_song_name: str