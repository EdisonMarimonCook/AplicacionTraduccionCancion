"""
MÓDULO: Tareas de limpieza automática
PROPÓSITO: Eliminar cuentas no verificadas y datos obsoletos
"""
import logging
from datetime import datetime, timedelta
from database import db

logger = logging.getLogger(__name__)

async def cleanup_unverified_accounts():
    """
    🧹 Eliminar cuentas no verificadas después de 24 horas
    """
    try:
        # Calcular fecha límite (24 horas atrás)
        cutoff_time = datetime.now() - timedelta(hours=24)
        
        # Buscar cuentas no verificadas antiguas
        result = await db.db["users"].delete_many({
            "is_verified": False,
            "created_at": {"$lt": cutoff_time}
        })
        
        if result.deleted_count > 0:
            logger.info(f"🧹 Eliminadas {result.deleted_count} cuentas no verificadas antiguas")
        
        return result.deleted_count
        
    except Exception as e:
        logger.error(f"❌ Error en cleanup_unverified_accounts: {e}")
        return 0

async def cleanup_old_verification_codes():
    """
    🧹 Limpiar códigos de verificación expirados (mayores a 30 min)
    """
    try:
        # Calcular fecha límite (30 minutos atrás)
        cutoff_time = datetime.now() - timedelta(minutes=30)
        
        # Limpiar solo el código, no eliminar la cuenta
        result = await db.db["users"].update_many(
            {
                "is_verified": False,
                "created_at": {"$lt": cutoff_time},
                "verification_code": {"$exists": True}
            },
            {
                "$unset": {"verification_code": ""}
            }
        )
        
        if result.modified_count > 0:
            logger.info(f"🧹 Limpiados {result.modified_count} códigos de verificación expirados")
        
        return result.modified_count
        
    except Exception as e:
        logger.error(f"❌ Error en cleanup_old_verification_codes: {e}")
        return 0
