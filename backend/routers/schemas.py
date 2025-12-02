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

class PasswordChangeRequest(BaseModel):
    """Para cambiar contraseña"""
    current_password: str
    new_password: str

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