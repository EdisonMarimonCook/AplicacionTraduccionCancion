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
import logging

app = FastAPI()

# Endpoint raíz para comprobar que el servidor funciona
@app.get("/")
def root():
    return {"message": "Servidor funcionando"}

# Endpoint para obtener letra y traducción de una canción
@app.get("/api/songs/{song}/lyrics")
def lyrics_endpoint(song: str, level: str = "default"):
    return get_lyrics(song, level)

# Endpoint para obtener top10 de canciones
@app.get("/api/songs/top10")
def top10_endpoint(language: str = "en"):
    """
    Devuelve el top10 de canciones por idioma.
    Usa el cache que se llena al iniciar la app y se actualiza cada hora.
    
    Args:
        language (str): Código del idioma ('en', 'es', 'fr', etc.)
        
    Returns:
        dict: Top 10 de canciones del idioma especificado
    """
    # Obtener canciones del cache, retornar lista vacía si no existen
    songs = top10_cache.get(language, [])
    
    if not songs:
        logging.warning(f"No hay canciones en cache para idioma: {language}")
    
    return {"language": language, "top10": songs}

# Endpoint para obtener top10 de canciones según el idioma
@app.get("/api/songs/top10/{language}")
def top10_by_language(language: str):
    """
    Devuelve el top10 de canciones según el idioma especificado.
    """
    songs = top10_cache.get(language)
    if not songs:
        # Opcional: devolver un error 404
        return {"error": "Language not supported or cache not ready"}, 404
    return {
        "language": language,
        "top10": songs
    }

# Evento al iniciar la app: llenamos cache y lanzamos actualización cada hora
@app.on_event("startup")
async def startup_event():
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

    def update_full_cache():
        logging.info("Actualizando todo el cache de Top 10...")
        for lang in SUPPORTED_LANGUAGES:
            top10_cache[lang] = get_top10_playlist(lang)

    # Llenamos el cache inicialmente
    update_full_cache()

    # Función que actualiza el cache cada hora en segundo plano
    async def actualizar_top10():
        while True:
            await asyncio.sleep(3600)
            update_full_cache()

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
# ===============================================================================

app.include_router(auth_router)
app.include_router(songs_router)
app.include_router(lyrics_router)

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

# ===============================================================================
# MANEJO DE ERRORES
# ===============================================================================

@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
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
