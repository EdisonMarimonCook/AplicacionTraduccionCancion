"""
ARCHIVO: main.py
PROPÓSITO: Archivo principal de la aplicación FastAPI
CONTIENE: Configuración, eventos, endpoints básicos
"""

import logging
import asyncio
from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from fastapi.openapi.utils import get_openapi
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

from routers import auth_router, songs_router, lyrics_router, openai_router
from routers.users import router as users_router
from routers.dictionary import router as dictionary_router  # ← AGREGAR

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
    allow_origins=[  # En producción: ["http://frontend.com"]
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

    # ========== BASE DE DATOS ==========
    if USE_MOCK:
        logger.warning("=" * 80)
        logger.warning("⚠️  USANDO BASE DE DATOS SIMULADA (MOCK)")
        logger.warning("=" * 80)
    else:
        logger.info("✅ Usando MongoDB real")

    # ========== LLENAR CACHE DE TOP 10 ==========
    logger.info("📊 Actualizando cache de Top 10 canciones...")

    async def load_top10_cache():
        """
        📥 Carga el cache de Top 10 al iniciar la app

        ESTRATEGIA:
        1. Intentar cargar desde Spotify (dinámico)
        2. Si falla, usar datos hardcodeados (fallback)
        """
        logger.info("📥 Inicializando cache de Top 10...")

        try:
            from utils.spotify import search_on_spotify

            # Mapeo: idioma → query de búsqueda
            language_queries = {
                "en": "popular English songs trending",
                "es": "canciones populares español",
                "fr": "musique française populaire",
                "de": "deutsche Musik beliebt",
                "it": "musica italiana popolare",
                "pt": "música portuguesa popular",
                "jp": "日本語の人気曲"
            }

            for lang, query in language_queries.items():
                try:
                    logger.info(f"🔍 Buscando Top 10 para {lang}...")

                    # Buscar en Spotify
                    results = search_on_spotify(query, limit=10)

                    if results and len(results) > 0:
                        # Ordenar por popularidad (descendente)
                        enriched_sorted = sorted(
                            results,
                            key=lambda x: x.get("popularity", 0),
                            reverse=True
                        )

                        # Guardar en cache
                        top10_cache[lang] = enriched_sorted
                        logger.info(f"✅ Top 10 cargado para {lang}: {len(enriched_sorted)} canciones")

                    else:
                        logger.warning(f"⚠️  No se encontraron resultados para {lang}, usando fallback")
                        top10_cache[lang] = _get_fallback_top10(lang)

                except Exception as e:
                    logger.warning(f"⚠️  Error cargando {lang}, usando fallback: {str(e)}")
                    top10_cache[lang] = _get_fallback_top10(lang)

            logger.info("✅ Cache de Top 10 inicializado completamente")

        except Exception as e:
            logger.error(f"❌ Error crítico inicializando cache: {str(e)}")
            logger.info("🔄 Usando fallback para todos los idiomas...")

            # Fallback total
            for lang in ["en", "es", "fr", "de", "it", "pt", "jp"]:
                top10_cache[lang] = _get_fallback_top10(lang)

    # Ejecutar actualización inicial
    await load_top10_cache()

    # ========== ACTUALIZACIÓN AUTOMÁTICA CADA HORA ==========
    async def actualizar_top10_periodicamente():
        """Actualiza el cache cada hora en segundo plano"""
        while True:
            try:
                await asyncio.sleep(3600)  # Esperar 1 hora
                logger.info("⏰ Actualizando cache (actualización periódica)...")
                await load_top10_cache()
            except Exception as e:
                logger.error(f"❌ Error en actualización periódica: {e}")

    # Crear tarea asincrónica que corra en background
    asyncio.create_task(actualizar_top10_periodicamente())

    logger.info("=" * 80)
    logger.info("✅ APLICACIÓN INICIADA CORRECTAMENTE")
    logger.info("=" * 80)


def _get_fallback_top10(language: str):
    """
    📦 FALLBACK - Datos hardcodeados si Spotify falla

    Estos son datos reales de canciones populares
    Se usan si la búsqueda en Spotify no funciona
    """

    fallback_data = {
        "en": [
            {
                "id": "11dFghVXANMlKmJXsNCQvb",
                "name": "Blinding Lights",
                "artist": "The Weeknd",
                "preview_url": "https://p.scdn.co/mp3-preview/...",
                "image_url": "https://i.scdn.co/image/ab67616d0000b273...",
                "popularity": 96,
                "spotify_url": "https://open.spotify.com/track/11dFghVXANMlKmJXsNCQvb",
                "duration_ms": 200040,
                "has_preview": True,
                "language": "en"
            },
            {
                "id": "3n3Ppam7vgaVa1iaRUc9Lp",
                "name": "Shape of You",
                "artist": "Ed Sheeran",
                "preview_url": "https://p.scdn.co/mp3-preview/...",
                "image_url": "https://i.scdn.co/image/ab67616d0000b273...",
                "popularity": 95,
                "spotify_url": "https://open.spotify.com/track/3n3Ppam7vgaVa1iaRUc9Lp",
                "duration_ms": 236973,
                "has_preview": True,
                "language": "en"
            },
        ],
        "es": [
            {
                "id": "2takcwFFpFEt1K3w8LLKkR",
                "name": "Despacito",
                "artist": "Luis Fonsi",
                "preview_url": "https://p.scdn.co/mp3-preview/...",
                "image_url": "https://i.scdn.co/image/ab67616d0000b273...",
                "popularity": 93,
                "spotify_url": "https://open.spotify.com/track/2takcwFFpFEt1K3w8LLKkR",
                "duration_ms": 228973,
                "has_preview": True,
                "language": "es"
            },
        ],
        "fr": [],
        "de": [],
        "it": [],
        "pt": [],
        "jp": [],
    }

    result = fallback_data.get(language, [])

    if result:
        logger.info(f"📦 Usando fallback para {language}: {len(result)} canciones")
    else:
        logger.warning(f"⚠️  Fallback vacío para {language}")

    return result

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
app.include_router(openai_router)
app.include_router(users_router)
app.include_router(dictionary_router)  # ← AGREGAR

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
        "languages": supported
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
