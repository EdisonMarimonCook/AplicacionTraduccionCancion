"""
DATABASE - Conexión a MongoDB o MOCK si no está disponible
"""

import logging
from config import settings

logger = logging.getLogger(__name__)

"""
DATABASE - Gestor de Conexión (Mock vs Real)
"""

import logging
from config import settings

logger = logging.getLogger(__name__)

# Variable global que exportaremos
db = None

# ✅ LOGICA DE SELECCIÓN
USE_MOCK = settings.USE_MOCK_DB

if USE_MOCK:
    logger.warning("⚠️  MODO: MOCK DATABASE (Datos en memoria)")
    from database_mock import mock_db
    db = mock_db
else:
    logger.info("🌍 MODO: MONGODB ATLAS (Base de datos real)")
    from database_mongo import mongo_db
    
    # Conectamos explícitamente
    try:
        mongo_db.connect()
        db = mongo_db
    except Exception as e:
        logger.critical(f"❌ FALLO CONEXIÓN MONGO: {e}")
        # Fallback de emergencia al Mock si Mongo falla
        logger.warning("⚠️  Activando Mock DB de emergencia...")
        from database_mock import mock_db
        db = mock_db

# Exportar
__all__ = ['db', 'USE_MOCK']