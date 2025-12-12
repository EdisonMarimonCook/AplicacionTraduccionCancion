"""
MÓDULO: Configuración
PROPÓSITO: Gestionar variables de entorno y configuraciones
"""

from pydantic_settings import BaseSettings
from typing import Optional

class Settings(BaseSettings):
    """Configuración de la aplicación basada en variables de entorno (.env)"""
    
    # ===== ENTORNO =====
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    
    # ===== BASE DE DATOS - MONGODB =====
    MONGODB_URL: str = "mongodb://localhost:27017"
    MONGODB_DB_NAME: str = "music_translator"
    USE_MOCK_DB: bool = True
    
    # ===== AUTENTICACIÓN =====
    SECRET_KEY: str = "your-secret-key-change-in-production"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    
    # ===== APIs EXTERNAS - SPOTIFY =====
    SPOTIFY_CLIENT_ID: str = ""
    SPOTIFY_CLIENT_SECRET: str = ""
    
    # ===== APIs EXTERNAS - GENIUS =====
    GENIUS_API_TOKEN: str = ""
    
    # ===== IA - GEMINI =====
    USE_GEMINI: bool = True
    GEMINI_API_KEY: str = ""
    
    # ===== IA - OLLAMA (Alternativa local) =====
    USE_OLLAMA: bool = False
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_MODEL: str = "llama2"
    
    # ===== EMAIL (Para recuperación de contraseña) =====
    MAIL_USERNAME: str = ""
    MAIL_PASSWORD: str = ""
    MAIL_FROM: str = ""
    MAIL_PORT: int = 587
    MAIL_SERVER: str = "smtp.gmail.com"
    
    # ===== URLs DE FRONTEND =====
    FRONTEND_URL: str = "http://localhost:3000"
    ANDROID_BASE_URL: str = "http://10.0.2.2:8000"
    
    # ===== LOGGING =====
    LOG_LEVEL: str = "INFO"
    
    # ===== CACHE =====
    CACHE_TTL_MINUTES: int = 120
    
    class Config:
        # Permite leer de archivo .env
        env_file = ".env"
        # ⚠️ IMPORTANTE: Permite campos extra del .env
        extra = "allow"  # ← ESTO ES LO IMPORTANTE

# Instanciar configuración
settings = Settings()