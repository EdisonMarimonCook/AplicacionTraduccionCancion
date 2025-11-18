"""
DATABASE - Conexión a MongoDB o MOCK si no está disponible
"""

import logging
from config import settings

logger = logging.getLogger(__name__)

# ================================================================
# DETECTAR SI USAR MOCK O REAL
# ================================================================

USE_MOCK = getattr(settings, 'USE_MOCK_DB', True)

if USE_MOCK:
    logger.warning("⚠️  USANDO MOCK DATABASE (datos en memoria)")
    from database_mock import mock_db as db
else:
    logger.info("✅ Conectando a MongoDB real...")
    # Aquí va la conexión real a MongoDB cuando esté lista
    # from motor.motor_asyncio import AsyncClient
    # ...
    pass

# ================================================================
# EXPORTS
# ================================================================

__all__ = ['db']