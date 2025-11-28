"""
ARCHIVO: routers/schemas.py
PROPÓSITO: Definir los esquemas Pydantic para Request/Response de la API
"""

from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field, EmailStr
from datetime import datetime

# ===============================================================================
# SCHEMAS DE DICCIONARIO
# ===============================================================================

class DictItemCreate(BaseModel):
    """Schema para crear entrada diccionario"""
    word: str = Field(..., min_length=1, max_length=100)
    translation: Optional[str] = None
    source_lang: str = Field(default="en", description="en, es, fr, ja, zh, etc")
    target_lang: str = Field(default="es", description="Idioma nativo para traducción")
    type: str = Field(default="word", description="word, phrase, idiom")
    level_system: str = Field(default="CEFR", description="CEFR, JLPT, HSK, TOPIK")
    difficulty_level: str = Field(default="A1", description="A1, B1, N3, 3, etc")
    example: Optional[str] = Field(None, max_length=300)
    notes: Optional[str] = Field(None, max_length=500)
    tags: List[str] = Field(default=[])
    is_hiphop_term: bool = Field(default=False)
    song_id: Optional[str] = None
    # ✅ NUEVO: Soporte para recomendación
    is_recommended: bool = Field(default=False)


class DictItemOut(BaseModel):
    """Schema para respuesta diccionario"""
    id: str
    word: str
    translation: Optional[str] = None
    source_lang: str
    target_lang: str
    type: str
    level_system: str = Field(description="CEFR, JLPT, HSK, TOPIK")
    difficulty_level: str = Field(description="Nivel en ese sistema")
    example: Optional[str] = None
    notes: Optional[str] = None
    tags: List[str] = []
    is_hiphop_term: bool = False
    song_id: Optional[str] = None
    is_recommended: bool = False  # ✅ NUEVO
    created_at: datetime
    last_reviewed: Optional[datetime] = None
    is_learned: bool = False
    times_reviewed: int = 0

    class Config:
        from_attributes = True

# ===============================================================================
# SCHEMAS DE USUARIO Y PERFIL
# ===============================================================================

class LearningLanguage(BaseModel):
    language: str = Field(..., description="es, en, fr, de, it, pt, ja, ko, zh")
    level: str = Field(default="A1", description="A1, A2, B1, B2, C1, C2")
    started_at: datetime = Field(default_factory=datetime.now)
    last_tested: Optional[datetime] = None

class UserProfileUpdate(BaseModel):
    """Request para actualizar perfil del usuario"""
    username: Optional[str] = Field(None, min_length=3, max_length=50)
    full_name: Optional[str] = None
    email: Optional[str] = None  # Email nuevo (requiere verificación)
    native_language: Optional[str] = None
    learning_languages: Optional[List[LearningLanguage]] = None

class PasswordChangeRequest(BaseModel):
    """Request para cambiar contraseña"""
    current_password: str = Field(..., description="Contraseña actual para verificación")
    new_password: str = Field(..., min_length=8, description="Nueva contraseña")
    confirm_password: str = Field(..., description="Confirmar nueva contraseña")

class EmailChangeRequest(BaseModel):
    """Request para cambiar email"""
    new_email: str = Field(..., description="Nuevo email")
    password: str = Field(..., description="Contraseña para confirmar")

class UserProfileResponse(BaseModel):
    """Response con perfil actualizado"""
    id: str
    email: str
    username: str
    full_name: Optional[str]
    native_language: str
    learning_languages: List[LearningLanguage]
    created_at: datetime
    updated_at: datetime
    is_active: bool

class UserResponse(BaseModel):
    """Respuesta del usuario autenticado"""
    id: str
    email: str
    username: str
    native_language: str = "es"
    learning_languages: list
    created_at: datetime
    updated_at: datetime
    is_active: bool

# ===============================================================================
# SCHEMAS DE IA (GEMINI V3) - ✅ LO QUE FALTABA
# ===============================================================================

class AnalyzeLyricsRequest(BaseModel):
    """Request para analizar letras"""
    title: str
    artist: str
    lyrics: str
    user_level: str = "B1"
    
    class Config:
        json_schema_extra = {
            "example": {
                "title": "Knight of the Wind",
                "artist": "Crush 40",
                "lyrics": "Woah-oh! Woah-oh-oh!...",
                "user_level": "B1"
            }
        }

class WordHighlight(BaseModel):
    word: str
    type: str  # noun, verb, adj
    translation: str  # Traducción contextual
    explanation: str  # Explicación contextual
    difficulty: str   # A1-C2
    color: str        # orange, red, green
    recommended: bool = False  # ⭐ La estrellita

class ExpressionHighlight(BaseModel):
    expression: str
    type: str
    translation: str
    explanation: str
    difficulty: str
    color: str
    recommended: bool = False

class HighlightWordsResponse(BaseModel):
    detected_language: str
    words: List[WordHighlight]
    expressions: List[ExpressionHighlight]
    suggestions: List[str]

# Schemas legacy (por si acaso se usan en algún lado, no molestan)
class HipHopTerm(BaseModel):
    term: str
    meaning: str
    context: Optional[str] = None
    cultural_reference: Optional[str] = None

class HipHopTermResponse(BaseModel):
    terms: List[HipHopTerm]
    total_terms: int