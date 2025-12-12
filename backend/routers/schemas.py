"""
ARCHIVO: routers/schemas.py
PROPÓSITO: Definir los esquemas Pydantic para Request/Response de la API
"""

from typing import List, Optional
from pydantic import BaseModel, Field, EmailStr
from datetime import datetime

# ===============================================================================
# 1️⃣ BASICS & AUTH TOKENS
# ===============================================================================

class LearningLanguage(BaseModel):
    """Idioma que el usuario está aprendiendo"""
    language: str
    level: str = "A1"
    started_at: Optional[datetime] = None
    last_tested: Optional[datetime] = None

class Token(BaseModel):
    access_token: str
    token_type: str
    refresh_token: str

class TokenData(BaseModel):
    username: Optional[str] = None

# ===============================================================================
# 2️⃣ USUARIOS - REQUESTS (Entrada de datos)
# ===============================================================================

class LoginRequest(BaseModel):
    email: str
    password: str

class RegisterRequest(BaseModel):
    """
    Datos para registrar usuario.
    Incluye idioma nativo y el idioma objetivo inicial.
    """
    email: EmailStr
    username: str
    full_name: Optional[str] = None
    password: str
    
    # 🌍 IDIOMA NATIVO (El tuyo)
    native_language: str = "es"
    
    # 🎯 OBJETIVO (Lo que quieres aprender) - ✅ CAMPOS CRÍTICOS PARA AUTH.PY
    target_language: str = "en" 
    target_level: str = "A1"

class RegisterResponse(BaseModel):
    id: str
    email: EmailStr
    username: str

class UserProfileUpdate(BaseModel):
    """Para actualizar datos básicos del perfil"""
    username: Optional[str] = None
    full_name: Optional[str] = None
    native_language: Optional[str] = None
    learning_languages: Optional[List[LearningLanguage]] = None  # 🔥 FIX: Permite actualizar idiomas

class PasswordChangeRequest(BaseModel):
    """Para cambiar contraseña"""
    current_password: str
    new_password: str
    confirm_password: str  # 🔥 FIX: Campo que faltaba

class EmailChangeRequest(BaseModel):
    """Para cambiar email"""
    new_email: EmailStr
    password: str

# ===============================================================================
# 3️⃣ USUARIOS - RESPONSES (Salida de datos)
# ===============================================================================

class UserProfileResponse(BaseModel):
    """
    Datos completos del usuario para el perfil.
    """
    id: str
    username: str
    email: EmailStr
    full_name: Optional[str] = None
    native_language: str
    learning_languages: List[LearningLanguage] = []
    created_at: datetime
    updated_at: Optional[datetime] = None  # 🔥 FIX: Agregado
    is_active: bool = True  # 🔥 FIX: Agregado

# ===============================================================================
# 4️⃣ DICCIONARIO (Base de Datos)
# ===============================================================================

class DictionaryEntryCreate(BaseModel):
    """
    Schema para crear entrada en el diccionario.
    Coincide con lo que envía el Frontend (AddWordRequest).
    """
    word: str = Field(..., min_length=1, max_length=100)
    translation: Optional[str] = None
    
    # 📂 CARPETAS: 'word' vs 'expression'
    type: str = Field(default="word", description="word, expression, idiom")
    
    # 🧠 CONTENIDO EXTRA
    notes: Optional[str] = Field(None, description="La explicación contextual de la IA")
    example: Optional[str] = Field(None, description="Frase de ejemplo para la Flashcard")
    
    # 🎵 METADATOS
    song_id: Optional[str] = None
    
    # ⭐ IMPORTANTE MVP
    is_recommended: bool = Field(default=False, description="Si la IA lo marcó con estrellita")

class DictionaryEntryResponse(DictionaryEntryCreate):
    """Lo que devolvemos al frontend (incluye ID y fechas)"""
    id: str
    user_id: str
    created_at: datetime
    last_reviewed: Optional[datetime] = None
    times_reviewed: int = 0
    is_learned: bool = False

# ===============================================================================
# 5️⃣ ANÁLISIS IA (Gemini)
# ===============================================================================

class AnalyzeLyricsRequest(BaseModel):
    """Request que envía el móvil para pedir análisis"""
    title: str
    artist: str
    lyrics: str
    user_level: str = "B1"

class WordHighlight(BaseModel):
    """Palabra individual detectada por Gemini"""
    word: str
    type: str         # 'noun', 'verb', 'adj' -> Frontend lo usa para iconos
    translation: str  # Traducción contextual
    explanation: str  # Explicación del porqué
    example: str      # 🔥 Frase de ejemplo para Flashcards
    difficulty: str   # A1-C2
    color: str        # orange, red, green
    recommended: bool = False  # ⭐ La estrellita

class ExpressionHighlight(BaseModel):
    """Expresión o Slang detectado por Gemini"""
    expression: str
    type: str = "expression" # Siempre 'expression' para tu carpeta
    translation: str
    explanation: str
    example: str      # 🔥
    difficulty: str
    color: str
    recommended: bool = False

class HighlightWordsResponse(BaseModel):
    """Respuesta final de la IA al móvil"""
    detected_language: str
    words: List[WordHighlight]
    expressions: List[ExpressionHighlight]
    suggestions: List[str]

# ===============================================================================
# 6️⃣ FLASHCARDS SRS (Sistema de Repetición Espaciada)
# ===============================================================================

class FlashcardReviewRequest(BaseModel):
    """
    Request para enviar resultado de revisión
    quality: 0-5 (SuperMemo SM-2)
      0: Blackout (No recordaba nada)
      1: Incorrect (Respuesta incorrecta)
      2: Incorrect but remembered (Incorrecto pero casi)
      3: Correct with difficulty (Correcto pero difícil)
      4: Correct (Correcto normal)
      5: Perfect (Fácil, perfecto)
    """
    quality: int = Field(..., ge=0, le=5, description="Calidad de recuerdo (0-5)")

class FlashcardData(BaseModel):
    """
    Datos SRS de una tarjeta (basado en SuperMemo SM-2)
    """
    id: str
    word_id: str  # Referencia al diccionario
    word: str
    translation: str
    example: str
    type: str
    
    # Datos SRS
    easiness_factor: float = 2.5  # Factor de facilidad (1.3-2.5+)
    interval: int = 0  # Días hasta próxima revisión
    repetitions: int = 0  # Número de repeticiones correctas consecutivas
    next_review_date: datetime  # Cuándo debe revisarse
    last_reviewed: Optional[datetime] = None
    
    # Estadísticas
    times_reviewed: int = 0
    times_correct: int = 0
    times_incorrect: int = 0

class FlashcardReviewResponse(BaseModel):
    """Respuesta tras revisar una tarjeta"""
    success: bool
    next_review_date: datetime
    interval_days: int
    message: str