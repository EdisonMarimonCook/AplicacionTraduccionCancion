<<<<<<< HEAD
from fastapi import FastAPI
import asyncio
<<<<<<< HEAD
from spotify import get_top10_playlist
from lyrics import get_lyrics
=======
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.openapi.utils import get_openapi

# ===============================================================================
# IMPORTS INTERNOS
# ===============================================================================
from config import settings
from database import db, USE_MOCK
>>>>>>> b2e0717 (git commit -m "feat: Auth con learning_languages + MOCK Database)
from cache import top10_cache
=======
"""
ARCHIVO: main.py
PROPÓSITO: Archivo principal de la aplicación FastAPI
CONTIENE: Configuración, eventos, endpoints básicos y Registro de Routers
"""

>>>>>>> feature/lyrics-translation
import logging
import asyncio
from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware

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

# ===============================================================================
# CONFIGURAR LOGGING
# ===============================================================================
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# ===============================================================================
# INICIALIZAR APP
# ===============================================================================

app = FastAPI(
    title="MusicTransIAtor API",
    description="Backend con Gemini 2.0 Flash + Spotify + Genius + MongoDB Atlas",
    version="4.0.0"
)

# CORS (Permitir conexiones desde el móvil/emulador)
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
<<<<<<< HEAD
<<<<<<< HEAD
    # Lista de idiomas que soportas
=======
    """Se ejecuta cuando INICIA la aplicación"""
    
    logger.info("=" * 80)
    logger.info("🚀 INICIANDO MUSIC TRANSIATOR API")
    logger.info("=" * 80)
    
    # ========== INFORMACIÓN DE ENTORNO ==========
    logger.info(f"📍 Entorno: {settings.ENVIRONMENT}")
    logger.info(f"🔧 Debug: {settings.DEBUG}")
    logger.info(f"🎵 Genius API: {'✅ Configurado' if is_genius_configured() else '❌ No configurado'}")
    
    # ========== BASE DE DATOS ==========
    if USE_MOCK:
        logger.warning("=" * 80)
        logger.warning("⚠️  USANDO BASE DE DATOS SIMULADA (MOCK)")
        logger.warning("=" * 80)
    else:
        logger.info("✅ Usando MongoDB real")
    
    # ========== LLENAR CACHE DE TOP 10 ==========
    logger.info("📊 Actualizando cache de Top 10 canciones...")
>>>>>>> b2e0717 (git commit -m "feat: Auth con learning_languages + MOCK Database)
    SUPPORTED_LANGUAGES = ["en", "es", "fr", "de", "it", "pt", "jp"]
=======
    logger.info("🚀 INICIANDO MUSIC TRANSIATOR API (v4.0)")
    
    # 1. Conectar BD (si no es Mock)
    if not USE_MOCK and hasattr(db, "connect"):
        try:
            db.connect()
        except Exception as e:
            logger.error(f"❌ Error conectando a Mongo en startup: {e}")
>>>>>>> feature/lyrics-translation

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
    from utils.spotify import get_top_tracks_by_language
    logger.info("🎵 Actualizando cache de Top 10 canciones...")
    try:
        # Precargamos inglés y español
        top10_cache["en"] = get_top_tracks_by_language("en")
        top10_cache["es"] = get_top_tracks_by_language("es")
        logger.info("✅ Cache actualizada")
    except Exception as e:
        logger.error(f"❌ Error actualizando cache: {e}")

<<<<<<< HEAD
<<<<<<< HEAD
    asyncio.create_task(actualizar_top10())
=======
@app.on_event("shutdown")
async def shutdown_event():
    """Se ejecuta cuando TERMINA la aplicación"""
    logger.info("=" * 80)
    logger.info("🛑 CERRANDO MUSIC TRANSIATOR API")
    logger.info("=" * 80)
    
    logger.info("✅ Aplicación cerrada correctamente")
    logger.info("=" * 80)

# ===============================================================================
# INCLUIR ROUTERS
=======
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
        "version": "4.0", 
        "status": "online",
        "database": "MongoDB Atlas" if not USE_MOCK else "Mock DB (Memory)"
    }

@app.get("/health")
def health_check():
    return {"status": "ok", "db_connected": True} # Simplificado

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
>>>>>>> feature/lyrics-translation
# ===============================================================================

app.include_router(auth_router)
app.include_router(songs_router)
app.include_router(lyrics_router)
<<<<<<< HEAD

# ===============================================================================
# ENDPOINTS BÁSICOS
# ===============================================================================

@app.get("/", tags=["Health"])
def root():
    """Endpoint raíz - Verifica que la API está funcionando"""
    db_status = "🗄️  MongoDB Real" if not USE_MOCK else "🔄 Mock DB (Simulada)"
    
    return {
        "message": "🎵 Servidor Music TransIAtor funcionando correctamente",
        "version": "1.0.0",
        "status": "online",
        "database": db_status,
        "environment": settings.ENVIRONMENT,
        "debug": settings.DEBUG
    }

@app.get("/health", tags=["Health"])
def health_check():
    """Health check - Para monitoreo y load balancers"""
    return {
        "status": "healthy",
        "version": "1.0.0",
        "database": "🗄️  MongoDB Real" if not USE_MOCK else "🔄 Mock DB",
        "cache_size": len(top10_cache),
        "cache_languages": list(top10_cache.keys()),
        "environment": settings.ENVIRONMENT
    }

@app.get("/api/v1/status", tags=["Health"])
def status_detailed():
    """Estado detallado de la API"""
    return {
        "api": {
            "name": "Music TransIAtor API",
            "version": "1.0.0",
            "status": "running"
        },
        "environment": {
            "environment": settings.ENVIRONMENT,
            "debug": settings.DEBUG
        },
        "database": {
            "type": "MongoDB Real" if not USE_MOCK else "Mock DB",
            "name": settings.MONGODB_DB_NAME if hasattr(settings, 'MONGODB_DB_NAME') else "transiaditor",
            "url": settings.MONGODB_URL if (settings.ENVIRONMENT == "development" and hasattr(settings, 'MONGODB_URL')) else "***"
        },
        "cache": {
            "languages": len(top10_cache),
            "total_songs": sum(len(songs) for songs in top10_cache.values())
        },
        "integrations": {
            "genius": "✅ Configured" if is_genius_configured() else "❌ Not configured"
        }
    }

@app.get("/api/v1/languages", tags=["Info"])
def get_supported_languages():
    """Obtiene la lista de idiomas soportados"""
    supported = {
        "en": "English",
        "es": "Español",
        "fr": "Français",
        "de": "Deutsch",
        "it": "Italiano",
        "pt": "Português",
        "jp": "日本語"
    }
    
    return {
        "total": len(supported),
        "languages": supported,
        "cached": list(top10_cache.keys())
    }
=======
app.include_router(ai_router)
app.include_router(dictionary_router)
app.include_router(users_router)
app.include_router(progress_router) # ✅ AHORA SÍ: El perfil funcionará
>>>>>>> feature/lyrics-translation

# ===============================================================================
# MANEJO DE ERRORES
# ===============================================================================

@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
<<<<<<< HEAD
    """Manejo personalizado de excepciones HTTP"""
    logger.error(f"❌ HTTP Exception: {exc.detail}")
    return JSONResponse(
        status_code=exc.status_code,
        content={"error": exc.detail}
    )

@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    """Manejo personalizado de excepciones generales"""
    logger.error(f"❌ General Exception: {str(exc)}")
    return JSONResponse(
        status_code=500,
        content={"error": "Internal server error"}
    )

# ===============================================================================
# SI SE EJECUTA DIRECTAMENTE
# ===============================================================================

if __name__ == "__main__":
    import uvicorn
    
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=settings.DEBUG
    )
>>>>>>> b2e0717 (git commit -m "feat: Auth con learning_languages + MOCK Database)
=======
    return JSONResponse(status_code=exc.status_code, content={"error": exc.detail})

@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    logger.error(f"❌ General Exception: {str(exc)}")
    return JSONResponse(status_code=500, content={"error": "Internal server error"})

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
>>>>>>> feature/lyrics-translation
