"""
MÓDULO: Version Check
PROPÓSITO: Sistema de control de versiones y actualizaciones forzadas
USO: El frontend consulta /api/v1/version al iniciar para verificar si hay actualizaciones
"""

from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional
import logging

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1", tags=["Version"])

# ===============================================================================
# CONFIGURACIÓN DE VERSIONES (Actualizar manualmente cuando sea necesario)
# ===============================================================================

# Versión mínima REQUERIDA (si el usuario tiene menor, DEBE actualizar)
MIN_REQUIRED_VERSION = "1.0.0"

# Versión más reciente disponible
LATEST_VERSION = "1.0.0"

# URL de descarga (tu portal de descarga o GitHub Releases)
DOWNLOAD_URL = "https://webappmusictransiator.pages.dev/"

# Changelog resumido
CHANGELOG = "Primera versión estable del Producto"

# ===============================================================================
# SCHEMAS
# ===============================================================================

class VersionResponse(BaseModel):
    """Respuesta del endpoint de versión"""
    min_version: str  # Versión mínima requerida
    latest_version: str  # Versión más reciente
    force_update: bool  # True si la app del usuario es muy antigua
    download_url: str  # URL para descargar la nueva versión
    changelog: Optional[str] = None  # Notas de la versión

# ===============================================================================
# ENDPOINTS
# ===============================================================================

@router.get("/version", response_model=VersionResponse)
async def check_version(
    app_version: str = "0.0.0"  # Versión de la app que hace la request
):
    """
    🔍 Version Check - Verifica si la app está actualizada
    
    Lógica:
    - Si app_version < MIN_REQUIRED_VERSION → force_update = True (bloquear app)
    - Si app_version < LATEST_VERSION → force_update = False (sugerir actualización)
    - Si app_version >= LATEST_VERSION → Todo OK
    """
    try:
        # Comparar versiones (simplificado, asume formato X.Y.Z)
        app_parts = list(map(int, app_version.split(".")))
        min_parts = list(map(int, MIN_REQUIRED_VERSION.split(".")))
        
        # Determinar si es necesario forzar actualización
        force_update = False
        for app_part, min_part in zip(app_parts, min_parts):
            if app_part < min_part:
                force_update = True
                break
            elif app_part > min_part:
                break
        
        logger.info(f"📱 Version check: app={app_version}, min={MIN_REQUIRED_VERSION}, force_update={force_update}")
        
        return {
            "min_version": MIN_REQUIRED_VERSION,
            "latest_version": LATEST_VERSION,
            "force_update": force_update,
            "download_url": DOWNLOAD_URL,
            "changelog": CHANGELOG
        }
        
    except Exception as e:
        # Si hay error parseando versión, asumir que está OK (no bloquear app)
        logger.error(f"❌ Error en version check: {e}")
        return {
            "min_version": MIN_REQUIRED_VERSION,
            "latest_version": LATEST_VERSION,
            "force_update": False,
            "download_url": DOWNLOAD_URL,
            "changelog": CHANGELOG
        }
