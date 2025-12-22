"""
ARCHIVO: main.py
PROPÓSITO: Archivo principal de la aplicación FastAPI
CONTIENE: Configuración, eventos, endpoints básicos y Registro de Routers
"""

import logging
import asyncio
from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware

# ===============================================================================
# INICIALIZAR APP
# ===============================================================================
app = FastAPI(
    title="MusicTransIAtor API",
    description="Backend con Gemini 2.0 Flash + Spotify + Genius + MongoDB Atlas + Cloudinary",
    version="5.0.0"
)

# ===============================================================================
# IMPORTS INTERNOS
# ===============================================================================
from config import settings
from database import db, USE_MOCK
from cache import top10_cache
from utils.genius_client import is_genius_configured

# ===============================================================================
# IMPORTS DE ROUTERS
# ===============================================================================

# 1. Routers básicos
from routers import auth_router, songs_router, lyrics_router

# 2. Router de IA (Gemini V3)
from routers.ai_analysis import router as ai_router 

# 3. Routers de usuario y datos
from routers.users import router as users_router
from routers.dictionary import router as dictionary_router

# 🔥 IMPORTANTE: El router de progreso (Para ProfileActivity)
from routers.progress import router as progress_router

# 4. Router de Flashcards
from routers.flashcards import router as flashcards_router

# ===============================================================================
# CONFIGURAR LOGGING
# ===============================================================================
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# ===============================================================================
# CORS (Permitir conexiones desde el móvil/emulador)
# ===============================================================================
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ===============================================================================
# EVENTOS LIFECYCLE (Arrancar/Parar)
# ===============================================================================

@app.on_event("startup")
async def startup_event():
    logger.info("🚀 INICIANDO MUSIC TRANSIATOR API (v4.0)")
    
    # 1. Conectar BD (si no es Mock)
    if not USE_MOCK and hasattr(db, "connect"):
        try:
            db.connect()
        except Exception as e:
            logger.error(f"❌ Error conectando a Mongo en startup: {e}")

    # 2. Verificar APIs externas
    if is_genius_configured():
        logger.info("✅ Genius API configurada")
    else:
        logger.warning("⚠️ Genius API Token no encontrado (Lyrics limitadas)")
        
    if settings.GEMINI_API_KEY:
        logger.info("✅ Gemini IA configurada")
    else:
        logger.warning("⚠️ Gemini API Key no encontrada (IA desactivada)")

    # 3. Precargar Cache (Top 10) en segundo plano
    asyncio.create_task(update_top10_cache())

async def update_top10_cache():
    """Tarea en segundo plano para actualizar cache de canciones"""
    # Importación local para evitar ciclos si utils usa main (raro, pero preventivo)
    from utils.spotify import get_top_tracks_by_language
    logger.info("🎵 Actualizando cache de Top 10 canciones...")
    try:
        # Precargamos inglés y español
        top10_cache["en"] = get_top_tracks_by_language("en")
        top10_cache["es"] = get_top_tracks_by_language("es")
        logger.info("✅ Cache actualizada")
    except Exception as e:
        logger.error(f"❌ Error actualizando cache: {e}")

@app.on_event("shutdown")
async def shutdown_event():
    logger.info("🛑 CERRANDO MUSIC TRANSIATOR API")
    if hasattr(db, "client") and db.client:
        db.client.close()
        logger.info("✅ Conexión Mongo cerrada")

# ===============================================================================
# ENDPOINTS GLOBALES
# ===============================================================================

@app.get("/")
def read_root():
    return {
        "app": "MusicTransIAtor API", 
        "version": "5.0", 
        "status": "online",
        "database": "MongoDB Atlas" if not USE_MOCK else "Mock DB (Memory)"
    }

@app.get("/health")
def health_check():
    return {"status": "ok", "db_connected": True} # Simplificado

# 🏥 Health Check para Render.com (con prefijo API v1)
@app.get("/api/v1/health")
def health_check_v1():
    return {
        "status": "healthy",
        "service": "MusicTransIAtor API",
        "version": "5.0",
        "database": "MongoDB Atlas" if not USE_MOCK else "Mock DB",
        "genius_configured": is_genius_configured(),
        "gemini_configured": bool(settings.GEMINI_API_KEY)
    }

@app.get("/api/v1/languages", tags=["Info"])
def get_supported_languages():
    return {
        "total": 7,
        "languages": {
            "en": "English", "es": "Español", "fr": "Français",
            "de": "Deutsch", "it": "Italiano", "pt": "Português", "jp": "日本語"
        }
    }

# ===============================================================================
# REGISTRO DE ROUTERS
# ===============================================================================

app.include_router(auth_router)
app.include_router(songs_router)
app.include_router(lyrics_router)
app.include_router(ai_router)
app.include_router(dictionary_router)
app.include_router(users_router)
app.include_router(progress_router) 
app.include_router(flashcards_router) 

# ===============================================================================
# MANEJO DE ERRORES
# ===============================================================================

@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return JSONResponse(status_code=exc.status_code, content={"error": exc.detail})

@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    logger.error(f"❌ General Exception: {str(exc)}")
    return JSONResponse(status_code=500, content={"error": "Internal server error"})

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)