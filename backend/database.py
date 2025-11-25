"""
DATABASE - Conexión a MongoDB o MOCK si no está disponible
"""

import logging
from config import settings

logger = logging.getLogger(__name__)

# ================================================================
# DETECTAR SI USAR MOCK O REAL
# ================================================================

# ✅ AHORA LEE DEL .env
USE_MOCK = getattr(settings, 'USE_MOCK_DB', True)

if USE_MOCK:
    logger.warning("⚠️  USANDO MOCK DATABASE (datos en memoria)")
    from database_mock import mock_db as db
else:
    logger.info("✅ Conectando a MongoDB real...")
    # TODO: Implementar conexión real con motor
    # from motor.motor_asyncio import AsyncClient
    # client = AsyncClient(settings.MONGODB_URL)
    # db = client[settings.MONGODB_DB_NAME]
    pass

# ================================================================
# EXPORTS
# ================================================================

__all__ = ['db', 'USE_MOCK']