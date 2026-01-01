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
    MONGODB_URL: str = "mongodb://localhost:27017"  # Para desarrollo local
    MONGO_URI: Optional[str] = None  # Para MongoDB Atlas (producción) - OPCIONAL
    MONGODB_DB_NAME: str = "music_translator"
    USE_MOCK_DB: bool = False
    
    # ===== AUTENTICACIÓN JWT =====
    SECRET_KEY: str = "your-secret-key-change-in-production"  # Deprecado
    JWT_SECRET_KEY: Optional[str] = None  # Usar este - OPCIONAL (fallback a SECRET_KEY)
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 1440  # 24 horas
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30  # 30 días
    # ===== APIs EXTERNAS - SPOTIFY =====
    SPOTIFY_CLIENT_ID: Optional[str] = None
    SPOTIFY_CLIENT_SECRET: Optional[str] = None
    
    # ===== APIs EXTERNAS - GENIUS =====
    GENIUS_API_TOKEN: Optional[str] = None
    
    # ===== IA - GEMINI =====
    USE_GEMINI: bool = True
    GEMINI_API_KEY: Optional[str] = None
    
    # ===== IA - OLLAMA (Alternativa local) =====
    USE_OLLAMA: bool = False
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_MODEL: str = "llama2"
    
    # ===== EMAIL (Para recuperación de contraseña) =====
    MAIL_USERNAME: str = ""
    MAIL_PASSWORD: str = ""  # Deprecado
    EMAIL_PASSWORD: Optional[str] = None  # Usar este - OPCIONAL
    MAIL_FROM: str = ""
    MAIL_PORT: int = 587
    MAIL_SERVER: str = "smtp.gmail.com"
    
    # ===== CLOUDINARY (Subida de avatares) =====
    CLOUDINARY_URL: Optional[str] = None
    
    # ===== URLs DE FRONTEND =====
    FRONTEND_URL: str = "http://localhost:3000"
    ANDROID_BASE_URL: str = "http://10.0.2.2:8000"
    
    # ===== LOGGING =====
    LOG_LEVEL: str = "INFO"
    
    # ===== CACHE =====
    CACHE_TTL_MINUTES: int = 120

    class Config:
        env_file = ".env"
        extra = "allow"  # Permite campos extra del .env

# Instanciar configuración
settings = Settings()

def get_settings():
    return settings

# Helper para obtener JWT_SECRET_KEY con fallback
def get_jwt_secret() -> str:
    """Devuelve JWT_SECRET_KEY si existe, sino SECRET_KEY"""
    return settings.JWT_SECRET_KEY or settings.SECRET_KEY

# Helper para obtener EMAIL_PASSWORD con fallback
def get_email_password() -> str:
    """Devuelve EMAIL_PASSWORD si existe, sino MAIL_PASSWORD"""
    return settings.EMAIL_PASSWORD or settings.MAIL_PASSWORD