"""
ARCHIVO: main.py
PROPÓSITO: Archivo principal de la aplicación FastAPI
CONTIENE: Configuración, eventos, endpoints básicos
"""

import logging
import asyncio
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

# ===============================================================================
# IMPORTS INTERNOS
# ===============================================================================
from config import settings
from database import connect_to_mongo, close_mongo_connection, is_using_mock_db
from spotify import get_top10_playlist, search_songs
from lyrics import get_lyrics
from cache import top10_cache

# ===============================================================================
# CONFIGURAR LOGGING
# ===============================================================================
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# ===============================================================================
# CREAR APLICACIÓN FASTAPI
# ===============================================================================
app = FastAPI(
    title="Music TransIAtor API",
    description="API para aprender idiomas a través de canciones con IA",
    version="1.0.0",
    docs_url=None,  # ← CAMBIAR A None
    redoc_url=None,  # ← CAMBIAR A None
    openapi_url=None  # ← CAMBIAR A None
)

# ===============================================================================
# CONFIGURAR CORS (Para que Frontend/Mobile puedan conectar)
# ===============================================================================
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000",      # Frontend React/Vue local
        "http://localhost:8080",      # Frontend alternativo
        "http://127.0.0.1:3000",      # Localhost local
        "http://127.0.0.1:8080",      # Localhost alternativo
        "*"                            # TEMPORAL: Permitir todos (cambiar en producción)
    ],
    allow_credentials=True,
    allow_methods=["*"],              # Permitir GET, POST, PUT, DELETE, etc.
    allow_headers=["*"],              # Permitir todos los headers
    max_age=600,                       # Cache CORS por 10 minutos
)

# ===============================================================================
# EVENTOS DE CICLO DE VIDA
# ===============================================================================

@app.on_event("startup")
async def startup_event():
    """
    Se ejecuta cuando INICIA la aplicación
    
    Qué hace:
    1. Conecta a MongoDB (o usa mock_db)
    2. Llena el cache de Top 10 canciones
    3. Inicia tarea de actualización automática
    """
    logger.info("=" * 80)
    logger.info("🚀 INICIANDO MUSIC TRANSIATOR API")
    logger.info("=" * 80)
    
    # ========== INFORMACIÓN DE ENTORNO ==========
    logger.info(f"📍 Entorno: {settings.ENVIRONMENT}")
    logger.info(f"🔧 Debug: {settings.DEBUG}")
    logger.info(f"🎵 Spotify Client ID: {settings.SPOTIFY_CLIENT_ID[:10]}..." if settings.SPOTIFY_CLIENT_ID else "❌ Spotify no configurado")
    
    # ========== CONECTAR A BASE DE DATOS ==========
    logger.info("📊 Conectando a base de datos...")
    await connect_to_mongo()
    
    # Mostrar advertencia si está en MOCK
    if is_using_mock_db():
        logger.warning("=" * 80)
        logger.warning("⚠️  USANDO BASE DE DATOS SIMULADA (MOCK)")
        logger.warning("⚠️  Los datos NO se guardarán después de reiniciar")
        logger.warning("=" * 80)
    else:
        logger.info("✅ Usando MongoDB real")
    
    # ========== LLENAR CACHE DE TOP 10 ==========
    logger.info("📊 Actualizando cache de Top 10 canciones...")
    SUPPORTED_LANGUAGES = ["en", "es", "fr", "de", "it", "pt", "jp"]
    
    def update_full_cache():
        """Actualiza el cache con Top 10 de cada idioma"""
        logger.info("🔄 Iniciando actualización del cache...")
        
        for lang in SUPPORTED_LANGUAGES:
            try:
                logger.info(f"  📥 Obteniendo Top 10 para: {lang}")
                top10_cache[lang] = get_top10_playlist(lang)
                logger.info(f"  ✅ Cache actualizado para: {lang} ({len(top10_cache[lang])} canciones)")
            except Exception as e:
                logger.error(f"  ❌ Error obteniendo Top 10 para {lang}: {e}")
                top10_cache[lang] = []
    
    # Ejecutar actualización inicial
    update_full_cache()
    logger.info(f"✅ Cache poblado: {len(top10_cache)} idiomas")
    
    # ========== ACTUALIZACIÓN AUTOMÁTICA CADA HORA ==========
    async def actualizar_top10_periodicamente():
        """Actualiza el cache cada hora en segundo plano"""
        while True:
            try:
                await asyncio.sleep(3600)  # Esperar 1 hora
                logger.info("⏰ Actualizando cache (actualización periódica)...")
                update_full_cache()
            except Exception as e:
                logger.error(f"❌ Error en actualización periódica: {e}")
    
    # Crear tarea asincrónica que corra en background
    asyncio.create_task(actualizar_top10_periodicamente())
    
    logger.info("=" * 80)
    logger.info("✅ APLICACIÓN INICIADA CORRECTAMENTE")
    logger.info("=" * 80)

@app.on_event("shutdown")
async def shutdown_event():
    """
    Se ejecuta cuando TERMINA la aplicación
    
    Qué hace:
    1. Cierra conexión a MongoDB
    2. Limpia recursos
    """
    logger.info("=" * 80)
    logger.info("🛑 CERRANDO MUSIC TRANSIATOR API")
    logger.info("=" * 80)
    
    await close_mongo_connection()
    
    logger.info("✅ Aplicación cerrada correctamente")
    logger.info("=" * 80)

# ===============================================================================
# ENDPOINTS BÁSICOS
# ===============================================================================

@app.get("/", tags=["Health"])
def root():
    """
    Endpoint raíz - Verifica que la API está funcionando
    
    Returns:
        dict: Información básica de la API
    """
    db_status = "🗄️  MongoDB Real" if not is_using_mock_db() else "🔄 Mock DB (Simulada)"
    
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
    """
    Health check - Para monitoreo y load balancers
    
    Returns:
        dict: Estado de salud de la API
    """
    return {
        "status": "healthy",
        "version": "1.0.0",
        "database": "🗄️  MongoDB Real" if not is_using_mock_db() else "🔄 Mock DB",
        "cache_size": len(top10_cache),
        "cache_languages": list(top10_cache.keys()),
        "environment": settings.ENVIRONMENT
    }

@app.get("/api/v1/status", tags=["Health"])
def status_detailed():
    """
    Estado detallado de la API
    
    Returns:
        dict: Información completa del estado
    """
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
            "type": "MongoDB Real" if not is_using_mock_db() else "Mock DB",
            "name": settings.MONGODB_DB_NAME,
            "url": settings.MONGODB_URL if settings.ENVIRONMENT == "development" else "***"
        },
        "cache": {
            "languages": len(top10_cache),
            "total_songs": sum(len(songs) for songs in top10_cache.values())
        },
        "integrations": {
            "spotify": "✅ Configured" if settings.SPOTIFY_CLIENT_ID else "❌ Not configured",
            "openai": "✅ Configured" if settings.OPENAI_API_KEY else "❌ Not configured"
        }
    }

# ===============================================================================
# ENDPOINTS DE CANCIONES
# ===============================================================================

@app.get("/api/v1/songs/top10", tags=["Songs"])
def get_top10(language: str = "en"):
    """
    Obtiene las 10 canciones más populares de un idioma
    
    Args:
        language (str): Código de idioma (en, es, fr, de, it, pt, jp)
    
    Returns:
        dict: Lista de Top 10 canciones
        
    Examples:
        GET /api/v1/songs/top10?language=es
        GET /api/v1/songs/top10?language=en
    """
    language = language.lower()
    
    # Verificar que el idioma es soportado
    if language not in top10_cache:
        logger.warning(f"⚠️  Idioma no soportado: {language}")
        return JSONResponse(
            status_code=404,
            content={
                "error": f"Language '{language}' not supported",
                "supported_languages": list(top10_cache.keys())
            }
        )
    
    songs = top10_cache.get(language, [])
    
    logger.info(f"✅ Top 10 solicitado para: {language} ({len(songs)} canciones)")
    
    return {
        "language": language,
        "count": len(songs),
        "songs": songs
    }

@app.get("/api/v1/songs/top10/{language}", tags=["Songs"])
def get_top10_by_language(language: str):
    """
    Obtiene las 10 canciones más populares de un idioma específico
    
    Args:
        language (str): Código de idioma en la ruta
    
    Returns:
        dict: Lista de Top 10 canciones
        
    Examples:
        GET /api/v1/songs/top10/es
        GET /api/v1/songs/top10/en
    """
    language = language.lower()
    
    if language not in top10_cache:
        logger.warning(f"⚠️  Idioma no soportado: {language}")
        return JSONResponse(
            status_code=404,
            content={
                "error": f"Language '{language}' not supported",
                "supported_languages": list(top10_cache.keys())
            }
        )
    
    songs = top10_cache.get(language, [])
    
    logger.info(f"✅ Top 10 solicitado para: {language}")
    
    return {
        "language": language,
        "count": len(songs),
        "songs": songs
    }

@app.get("/api/v1/songs/search", tags=["Songs"])
def search_songs_endpoint(q: str, language: str = "en", limit: int = 10):
    """
    Busca canciones por nombre o artista
    
    Args:
        q (str): Término de búsqueda (nombre canción o artista)
        language (str): Idioma para filtrar (default: en)
        limit (int): Máximo de resultados (default: 10, max: 50)
    
    Returns:
        dict: Resultados de la búsqueda
        
    Examples:
        GET /api/v1/songs/search?q=imagine&language=en
        GET /api/v1/songs/search?q=bohemian&language=en&limit=20
    """
    # Validar que el query no esté vacío
    if not q or len(q.strip()) < 2:
        logger.warning(f"⚠️  Query inválido: '{q}'")
        return JSONResponse(
            status_code=400,
            content={"error": "Query must be at least 2 characters", "results": []}
        )
    
    # Validar límite
    limit = min(max(limit, 1), 50)  # Entre 1 y 50
    language = language.lower()
    
    try:
        logger.info(f"🔍 Buscando: '{q}' en idioma: {language} (limit: {limit})")
        results = search_songs(q, language, limit)
        
        logger.info(f"✅ Búsqueda completada: {len(results)} resultados")
        
        return {
            "query": q,
            "language": language,
            "limit": limit,
            "count": len(results),
            "results": results
        }
    except Exception as e:
        logger.error(f"❌ Error en búsqueda: {str(e)}")
        return JSONResponse(
            status_code=500,
            content={"error": "Error searching songs", "details": str(e)}
        )

# ===============================================================================
# ENDPOINTS DE LETRAS
# ===============================================================================

@app.get("/api/v1/songs/{song_id}/lyrics", tags=["Lyrics"])
def get_song_lyrics(song_id: str, level: str = "default"):
    """
    Obtiene las letras de una canción con traducción y análisis
    
    Args:
        song_id (str): ID de Spotify de la canción
        level (str): Nivel de dificultad (default, easy, medium, hard)
    
    Returns:
        dict: Letras con traducciones y análisis
        
    Examples:
        GET /api/v1/songs/4cOdkLwLK6i33zt0B3MEMBX/lyrics
        GET /api/v1/songs/4cOdkLwLK6i33zt0B3MEMBX/lyrics?level=easy
    """
    try:
        logger.info(f"📝 Obteniendo letras para canción: {song_id} (nivel: {level})")
        lyrics_data = get_lyrics(song_id, level)
        
        logger.info(f"✅ Letras obtenidas para: {song_id}")
        
        return lyrics_data
    
    except Exception as e:
        logger.error(f"❌ Error obteniendo letras: {str(e)}")
        return JSONResponse(
            status_code=500,
            content={"error": "Error retrieving lyrics", "details": str(e)}
        )

# ===============================================================================
# ENDPOINTS DE INFORMACIÓN
# ===============================================================================

@app.get("/api/v1/languages", tags=["Info"])
def get_supported_languages():
    """
    Obtiene la lista de idiomas soportados
    
    Returns:
        dict: Lista de idiomas disponibles
    """
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
        content={"error": "Internal server error", "details": str(exc)}
    )

# ===============================================================================
# SI SE EJECUTA DIRECTAMENTE
# ===============================================================================

if __name__ == "__main__":
    import uvicorn
    
    # Ejecutar servidor
    uvicorn.run(
        "main:app",
        host="0.0.0.0",  # Escucha en todas las interfaces
        port=8000,
        reload=settings.DEBUG  # Reload automático en desarrollo
    )
