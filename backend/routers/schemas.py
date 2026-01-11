"""
ARCHIVO: routers/schemas.py
PROPÓSITO: Definir los esquemas Pydantic para Request/Response de la API
"""

from typing import List, Optional
from pydantic import BaseModel, Field, EmailStr
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field

# ===============================================================================
# 1️⃣ BASICS & AUTH TOKENS
# ===============================================================================

class LearningLanguage(BaseModel):
    """Idioma que el usuario está aprendiendo"""
    language: str
    level: str = "A1"
    started_at: Optional[datetime] = None
    last_tested: Optional[datetime] = None
    
    # ✨ FASE 2.5: Campos de progreso y gestión
    daily_goal: int = 10
    reviews_pending: int = 0
    is_active: bool = True
    words_learned: int = 0

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
    avatar_url: Optional[str] = None
    learning_languages: List[LearningLanguage] = []
    created_at: datetime
    is_active: bool = True
    onboarding_completed: bool = False  # ✅ Para tutorial por cuenta
    
    # ✨ FASE 2.5: Gamificación y multi-idioma
    primary_language: str = "en"
    total_xp: int = 0
    current_streak: int = 0
    longest_streak: int = 0
    burnout_limit: int = 50
    
    # Estadísticas globales (compatibilidad)
    reviews_count: int = 0
    words_count: int = 0

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
    
    # 🌍 IDIOMA (OBLIGATORIO para multi-idioma)
    language: str = Field(..., min_length=2, max_length=10, description="Código del idioma (en, es, fr, etc.)")
    
    # 📂 CARPETAS: 'word' vs 'expression'
    type: str = Field(default="word", description="word, expression, idiom")
    
    # 🧠 CONTENIDO EXTRA
    notes: Optional[str] = Field(None, description="La explicación contextual de la IA")
    example: Optional[str] = Field(None, description="Frase de ejemplo para la Flashcard")
    
    # 🎵 METADATOS
    song_id: Optional[str] = None
    
    # 🔊 AUDIO (NUEVO)
    song_youtube_url: Optional[str] = Field(None, description="URL de YouTube si viene de una canción")
    timestamp_start: Optional[float] = Field(None, description="Segundo exacto donde empieza la palabra")
    timestamp_end: Optional[float] = Field(None, description="Segundo exacto donde termina la palabra")
    
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
    
    # � AUDIO (heredado de DictionaryEntryCreate, pero lo exponemos explícitamente)
    song_youtube_url: Optional[str] = None
    timestamp_start: Optional[float] = None
    timestamp_end: Optional[float] = None
    
    # �🔥 FASE 4: Anti-burnout
    warning: Optional[str] = None  # Advertencia si superó daily_goal

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
    already_saved: bool = False  # ✅ Indica si ya está en el diccionario del usuario

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
    already_saved: bool = False  # ✅ Indica si ya está en el diccionario del usuario

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
    explanation: Optional[str] = None  # 🔥 Explicación de la IA (mapeada desde 'notes')
    type: str
    language: str  # Código del idioma (ja, ko, zh, es, etc.)
    
    # 🔊 AUDIO (NUEVO)
    song_youtube_url: Optional[str] = None  # URL de YouTube si viene de una canción
    timestamp_start: Optional[float] = None  # Segundo exacto donde empieza la palabra
    timestamp_end: Optional[float] = None  # Segundo exacto donde termina la palabra
    
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

class AvatarUpdateResponse(BaseModel):
    message: str
    url: str

# ===============================================================================
# 7️⃣ AUDIO & LYRICS (Modificado)
# ===============================================================================

class AudioStreamResponse(BaseModel):
    """Respuesta del endpoint directo de audio"""
    title: str
    stream_url: str 
    duration: Optional[int] = None
    thumbnail: Optional[str] = None
    source: str = "YouTube"

class LyricsResponse(BaseModel):
    """
    ✅ RESPUESTA COMPLETA PARA LA PANTALLA DE APRENDIZAJE
    Incluye letras, metadatos y AMBOS audios (Preview y Full).
    """
    title: str
    artist: str
    lyrics: str
    image_url: Optional[str] = None
    genius_url: Optional[str] = None
    
    # 🎵 AUDIOS
    preview_url: Optional[str] = None      # iTunes (30s, carga instantánea)
    full_audio_url: Optional[str] = None   # Cobalt (Canción completa)
    
    # 🔍 VALIDACIÓN DE PRECISIÓN (NUEVO)
    confidence: Optional[str] = "medium"   # high/medium/low - Confianza en el match
    validation_warnings: Optional[list] = None  # Advertencias si hay discrepancias
    
    # 🔊 SYNCED LYRICS (para extraer fragmentos de audio)
    synced_lyrics: Optional[str] = None  # Formato LRC con timestamps

# ===============================================================================
# 8️⃣ LANGUAGE MANAGEMENT (BLOQUE 2)
# ===============================================================================

class DeleteLanguageResponse(BaseModel):
    """Respuesta al eliminar un idioma de aprendizaje"""
    message: str
    deleted_words: int
    deleted_flashcards: int
    remaining_languages: int

class GrammyCheckResponse(BaseModel):
    """Respuesta de verificación Grammy/Top Hit"""
    is_grammy: bool
    badge: Optional[str] = None