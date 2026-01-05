"""
ARCHIVO: main.py
PROPÓSITO: Archivo principal de la aplicación FastAPI
CONTIENE: Configuración, eventos, endpoints básicos y Registro de Routers
"""

import logging
import asyncio
from fastapi import FastAPI, HTTPException, Response
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware  # 🔥 COMPRESIÓN

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
from routers import (
    auth_router,
    songs_router,
    lyrics_router,
    ai_router,
    users_router,
    dictionary_router,
    progress_router,
    flashcards_router,
    audio_router  # 👈 Importamos el OBJETO router, no el archivo
)

# 2. Routers Fase 2.5
from routers.languages import router as languages_router
from routers.version import router as version_router  # 🔥 Version Check

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
# COMPRESIÓN GZIP (🔥 OPTIMIZACIÓN - Reduce payload ~60-70%)
# ===============================================================================
app.add_middleware(
    GZipMiddleware,
    minimum_size=1000  # Comprimir respuestas >1KB (letras, listas de canciones)
)
logger.info("✅ Gzip compression habilitada (minimum_size=1KB)")

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
            # 🔥 OPTIMIZACIÓN: Crear índices para queries rápidas
            await create_indexes()
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
    
    # 4. 🧹 Iniciar tarea de limpieza de cuentas no verificadas
    asyncio.create_task(periodic_cleanup())

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

async def periodic_cleanup():
    """
    🧹 Tarea periódica de limpieza (cada hora)
    - Elimina cuentas no verificadas > 24h
    - Limpia códigos de verificación expirados > 30min
    """
    from tasks.cleanup import cleanup_unverified_accounts, cleanup_old_verification_codes
    
    while True:
        try:
            # Esperar 1 hora entre limpiezas
            await asyncio.sleep(3600)  # 3600 segundos = 1 hora
            
            logger.info("🧹 Ejecutando limpieza automática...")
            
            # Limpiar cuentas no verificadas
            deleted_accounts = await cleanup_unverified_accounts()
            
            # Limpiar códigos expirados
            deleted_codes = await cleanup_old_verification_codes()
            
            logger.info(f"✅ Limpieza completada: {deleted_accounts} cuentas, {deleted_codes} códigos")
            
        except Exception as e:
            logger.error(f"❌ Error en limpieza periódica: {e}")
            await asyncio.sleep(3600)  # Esperar 1 hora antes de reintentar

async def create_indexes():
    """
    🔥 OPTIMIZACIÓN: Crear índices en MongoDB para queries rápidas
    Reduce tiempo de queries de ~2s a <200ms
    """
    try:
        logger.info("📊 Creando índices en MongoDB...")
        
        # Índice en diccionario (búsqueda por user_id + language + type)
        await db.db["dictionary"].create_index([("user_id", 1), ("language", 1), ("type", 1)])
        logger.info("✅ Índice creado: dictionary(user_id, language, type)")
        
        # Índice en flashcards (búsqueda por user_id + language + next_review)
        await db.db["flashcard_srs"].create_index([("user_id", 1), ("language", 1), ("next_review", 1)])
        logger.info("✅ Índice creado: flashcard_srs(user_id, language, next_review)")
        
        # Índice en usuarios (búsqueda por email y username únicos)
        await db.db["users"].create_index([("email", 1)], unique=True)
        await db.db["users"].create_index([("username", 1)], unique=True)
        logger.info("✅ Índices únicos creados: users(email), users(username)")
        
        # 🆕 Índice en caché de letras (búsqueda por spotify_id)
        # 1. Eliminar índice antiguo de song_id si existe
        try:
            await db.db["lyrics_cache"].drop_index("song_id_1")
            logger.info("🗑️ Índice antiguo 'song_id_1' eliminado")
        except:
            pass  # No existe, continuar
        
        # 2. Crear índice único en spotify_id
        await db.db["lyrics_cache"].create_index([("spotify_id", 1)], unique=True)
        await db.db["lyrics_cache"].create_index([("expires_at", 1)], expireAfterSeconds=0)  # TTL index
        logger.info("✅ Índices creados: lyrics_cache(spotify_id), TTL(expires_at)")
        
        logger.info("🎉 Todos los índices creados exitosamente")
        
    except Exception as e:
        logger.error(f"❌ Error creando índices: {e}")
        # No crashear el servidor si fallan los índices
        pass

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
app.include_router(audio_router)
app.include_router(languages_router)  # ✨ Fase 2.5
app.include_router(version_router)  # 🔥 Version Check (Fase 3)
# MANEJO DE ERRORES
# ===============================================================================

@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return JSONResponse(status_code=exc.status_code, content={"error": exc.detail})

@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    logger.error(f"❌ General Exception: {str(exc)}")
    return JSONResponse(status_code=500, content={"error": "Internal server error"})

@app.head("/health")
def health_check_head():
    return Response(status_code=200)

@app.head("/api/v1/health")
def health_check_v1_head():
    return Response(status_code=200)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)