"""
Configuración centralizada de la aplicación
Lee variables del archivo .env
"""
from pydantic_settings import BaseSettings
from pydantic import Field
import logging
from pathlib import Path

logger = logging.getLogger(__name__)

class Settings(BaseSettings):
    """Configuración de la aplicación"""
    
    # MongoDB
    MONGODB_URL: str = Field(default="mongodb://localhost:27017")
    MONGODB_DB_NAME: str = Field(default="music_translator")
    MONGODB_USER: str = Field(default="")
    MONGODB_PASSWORD: str = Field(default="")
    USE_MOCK_DB: bool = Field(default=True)
    
    # Spotify
    SPOTIFY_CLIENT_ID: str = Field(default="")
    SPOTIFY_CLIENT_SECRET: str = Field(default="")
    
    # JWT
    SECRET_KEY: str = Field(default="your-super-secret-key-change-in-production")
    ALGORITHM: str = Field(default="HS256")
    ACCESS_TOKEN_EXPIRE_MINUTES: int = Field(default=30)
    REFRESH_TOKEN_EXPIRE_DAYS: int = Field(default=7)
    
    # OpenAI
    OPENAI_API_KEY: str = Field(default="")
    
    # GENIUS API
    GENIUS_API_TOKEN: str = Field(
        default="your-genius-api-token",
        description="Token de Genius API para extraer letras"
    )
    
    # Entorno
    ENVIRONMENT: str = Field(default="development")
    DEBUG: bool = Field(default=True)
    
    class Config:
        env_file = ".env"
        case_sensitive = True

settings = Settings()

# Log configuración
logger.info(f"⚙️  Entorno: {settings.ENVIRONMENT}")
logger.info(f"🐛 Debug: {settings.DEBUG}")
logger.info(f"💾 Mock BD: {settings.USE_MOCK_DB}")