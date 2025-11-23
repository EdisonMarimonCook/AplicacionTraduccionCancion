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
from fastapi.openapi.utils import get_openapi

# ===============================================================================
# IMPORTS INTERNOS
# ===============================================================================
from config import settings
from database import connect_to_mongo, close_mongo_connection, is_using_mock_db
from cache import top10_cache
from utils.genius_client import is_genius_configured

# ===============================================================================
# IMPORTS DE ROUTERS
# ===============================================================================

from routers import auth_router, songs_router, lyrics_router, openai_router

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
    docs_url="/docs",           
    redoc_url="/redoc",         
    openapi_url="/openapi.json" 
)

# ===============================================================================
# ✅ CONFIGURAR SEGURIDAD PARA SWAGGER
# ===============================================================================

def custom_openapi():
    """
    Configura el esquema OpenAPI para que Swagger entienda Bearer tokens
    """
    if app.openapi_schema:
        return app.openapi_schema
    
    openapi_schema = get_openapi(
        title="Music TransIAtor API",
        version="1.0.0",
        description="API para aprender idiomas a través de canciones con IA",
        routes=app.routes,
    )
    
    # ✅ Agregar SecurityScheme para Bearer Token
    openapi_schema["components"]["securitySchemes"] = {
        "HTTPBearer": {
            "type": "http",
            "scheme": "bearer",
            "bearerFormat": "JWT",
            "description": "Introduce tu JWT token"
        }
    }
    
    # ✅ Aplicar seguridad a todos los endpoints
    for path in openapi_schema["paths"].values():
        for operation in path.values():
            if isinstance(operation, dict) and "responses" in operation:
                operation["security"] = [{"HTTPBearer": []}]
    
    app.openapi_schema = openapi_schema
    return app.openapi_schema

app.openapi = custom_openapi

# ===============================================================================
# CONFIGURAR CORS
# ===============================================================================
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000",
        "http://localhost:8080",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:8080",
        "*"
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    max_age=600,
)

# ===============================================================================
# EVENTOS DE CICLO DE VIDA
# ===============================================================================

@app.on_event("startup")
async def startup_event():
    """Se ejecuta cuando INICIA la aplicación"""
    
    logger.info("=" * 80)
    logger.info("🚀 INICIANDO MUSIC TRANSIATOR API")
    logger.info("=" * 80)
    
    # ========== INFORMACIÓN DE ENTORNO ==========
    logger.info(f"📍 Entorno: {settings.ENVIRONMENT}")
    logger.info(f"🔧 Debug: {settings.DEBUG}")
    logger.info(f"🎵 Genius API: {'✅ Configurado' if is_genius_configured() else '❌ No configurado'}")
    
    # ========== CONECTAR A BASE DE DATOS ==========
    logger.info("📊 Conectando a base de datos...")
    await connect_to_mongo()
    
    if is_using_mock_db():
        logger.warning("=" * 80)
        logger.warning("⚠️  USANDO BASE DE DATOS SIMULADA (MOCK)")
        logger.warning("=" * 80)
    else:
        logger.info("✅ Usando MongoDB real")
    
    # ========== LLENAR CACHE DE TOP 10 ==========
    logger.info("📊 Actualizando cache de Top 10 canciones...")
    SUPPORTED_LANGUAGES = ["en", "es", "fr", "de", "it", "pt", "jp"]
    
    def update_full_cache():
        """Actualiza el cache con Top 10 de cada idioma"""
        logger.info("🔄 Iniciando actualización del cache...")
        
        try:
            from spotify import get_top10_playlist
            
            for lang in SUPPORTED_LANGUAGES:
                try:
                    logger.info(f"  📥 Obteniendo Top 10 para: {lang}")
                    top10_cache[lang] = get_top10_playlist(lang)
                    logger.info(f"  ✅ Cache actualizado para: {lang} ({len(top10_cache[lang])} canciones)")
                except Exception as e:
                    logger.error(f"  ❌ Error obteniendo Top 10 para {lang}: {e}")
                    top10_cache[lang] = []
        except ImportError:
            logger.warning("⚠️  Spotify module no disponible, usando cache vacío")
            for lang in SUPPORTED_LANGUAGES:
                top10_cache[lang] = []
    
    # Ejecutar actualización inicial
    update_full_cache()
    logger.info(f"✅ Cache poblado: {len(top10_cache)} idiomas, {sum(len(s) for s in top10_cache.values())} canciones")
    
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
    """Se ejecuta cuando TERMINA la aplicación"""
    logger.info("=" * 80)
    logger.info("🛑 CERRANDO MUSIC TRANSIATOR API")
    logger.info("=" * 80)
    
    await close_mongo_connection()
    
    logger.info("✅ Aplicación cerrada correctamente")
    logger.info("=" * 80)

# ===============================================================================
# INCLUIR ROUTERS
# ===============================================================================

app.include_router(auth_router)
app.include_router(songs_router)
app.include_router(lyrics_router)
app.include_router(openai_router)  # <-- añadir para exponer /api/v1/openai

# ===============================================================================
# ENDPOINTS BÁSICOS
# ===============================================================================

@app.get("/", tags=["Health"])
def root():
    """Endpoint raíz - Verifica que la API está funcionando"""
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
    """Health check - Para monitoreo y load balancers"""
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
            "type": "MongoDB Real" if not is_using_mock_db() else "Mock DB",
            "name": settings.MONGODB_DB_NAME,
            "url": settings.MONGODB_URL if settings.ENVIRONMENT == "development" else "***"
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
