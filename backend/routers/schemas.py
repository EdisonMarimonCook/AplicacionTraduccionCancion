from typing import List, Optional
from pydantic import BaseModel, Field
from datetime import datetime

# ---------- Dictionary item schemas ----------

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
    created_at: datetime
    last_reviewed: Optional[datetime] = None
    is_learned: bool = False
    times_reviewed: int = 0

    class Config:
        from_attributes = True

# ---------- OpenAI request/response schemas ----------
    
class AnalyzeRequest(BaseModel):
    lyrics: str
    level: str
    
class TranslateWordRequest(BaseModel):
    word: str
    source_lang: str = "en"
    target_lang: str = "es"

class WordHighlight(BaseModel):
    """Palabra resaltada con su información"""
    word: str
    level: str = Field(..., description="A1, A2, B1, B2, C1, C2")
    color: str = Field(..., description="hexColor - #FF0000, #FFA500, #008000")
    translation: Optional[str] = None
    explanation: Optional[str] = None
    example_in_context: Optional[str] = None

class HighlightWordsRequest(BaseModel):
    """Request para resaltar palabras en letra"""
    lyrics: str = Field(..., description="Letra completa de la canción")
    user_level: str = Field(..., description="Nivel del usuario: A1, A2, B1, B2, C1, C2")
    language: str = Field(default="en", description="Idioma de la letra")

class HighlightWordsResponse(BaseModel):
    """Response con palabras resaltadas"""
    highlighted_words: List[WordHighlight]
    total_words: int
    words_by_level: dict = Field(default_factory=dict, description="Conteo por nivel")
    suggestions: List[str] = Field(default_factory=list, description="Sugerencias de estudio")

class HipHopTermRequest(BaseModel):
    """Request para identificar términos de HipHop"""
    lyrics: str
    language: str = "en"

class HipHopTerm(BaseModel):
    """Término de HipHop identificado"""
    term: str
    meaning: str
    context: Optional[str] = None
    cultural_reference: Optional[str] = None

class HipHopTermResponse(BaseModel):
    """Response con términos de HipHop"""
    terms: List[HipHopTerm]
    total_terms: int

# ✅ YA ESTÁ AQUÍ
class LearningLanguage(BaseModel):
    language: str = Field(..., description="es, en, fr, de, it, pt, ja, ko, zh")
    level: str = Field(default="A1", description="A1, A2, B1, B2, C1, C2")
    started_at: datetime = Field(default_factory=datetime.now)
    last_tested: Optional[datetime] = None

# ✅ AGREGAR ESTO AL FINAL

class SaveHighlightedWordsRequest(BaseModel):
    """Guardar palabras resaltadas en diccionario"""
    song_id: str
    highlighted_words: List[WordHighlight]
    language: str

class ProgressSummary(BaseModel):
    """Resumen de progreso"""
    total_words_learned: int
    words_by_level: dict
    songs_completed: int
    current_streak: int

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