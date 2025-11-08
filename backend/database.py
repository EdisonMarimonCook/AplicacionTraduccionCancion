"""
Configuración de la base de datos MongoDB
Con fallback a BD simulada (mock) para desarrollo
"""
from pymongo import MongoClient
from pymongo.errors import ConnectionFailure
from config import settings
import logging
import asyncio

logger = logging.getLogger(__name__)

mongodb_client = None
database = None

# BD simulada (para desarrollo)
mock_db = {
    "users": [],
    "dictionary_entries": [],
    "song_sessions": [],
    "user_progress": []
}

async def connect_to_mongo():  # ← CAMBIAR A async
    """
    Conecta a MongoDB cuando inicia la app
    """
    global mongodb_client, database
    
    if settings.USE_MOCK_DB:
        logger.warning("⚠️  USANDO BASE DE DATOS SIMULADA (MOCK)")
        logger.warning("⚠️  Los datos NO se guardarán después de reiniciar")
        return
    
    try:
        # Correr en thread para no bloquear
        def _connect():
            global mongodb_client, database
            mongodb_url = settings.MONGODB_URL
            mongodb_client = MongoClient(mongodb_url, serverSelectionTimeoutMS=5000)
            mongodb_client.admin.command("ping")
            database = mongodb_client[settings.MONGODB_DB_NAME]
        
        await asyncio.to_thread(_connect)
        
        logger.info("✅ Conectado a MongoDB exitosamente")
        logger.info(f"📊 Base de datos: {settings.MONGODB_DB_NAME}")
        logger.info(f"🌐 Servidor: {settings.MONGODB_URL}")
        
    except ConnectionFailure as e:
        logger.error(f"❌ Error conectando a MongoDB: {e}")
        logger.warning("⚠️  Usando BASE DE DATOS SIMULADA (MOCK)")

async def close_mongo_connection():  # ← CAMBIAR A async
    """Cierra conexión a MongoDB"""
    global mongodb_client
    if mongodb_client:
        mongodb_client.close()
        logger.info("🛑 Desconectado de MongoDB")
    else:
        logger.info("🛑 BD Simulada cerrada")

def get_database():
    """Retorna BD real o mock"""
    if settings.USE_MOCK_DB or database is None:
        return mock_db
    return database

def is_using_mock_db():
    """Indica si se usa BD simulada"""
    return settings.USE_MOCK_DB or database is None