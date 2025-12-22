"""
SERVICIO: Cloudinary
PROPÓSITO: Gestión de subida de imágenes de avatares a la nube
"""

import cloudinary
import cloudinary.uploader
from config import settings
import logging
from urllib.parse import urlparse

logger = logging.getLogger(__name__)

# Configurar Cloudinary de forma explícita
def configure_cloudinary():
    """Configura Cloudinary parseando CLOUDINARY_URL del .env"""
    try:
        if not settings.CLOUDINARY_URL:
            logger.error("❌ CLOUDINARY_URL no encontrado en .env")
            raise Exception("CLOUDINARY_URL no configurado")
        
        # Parsear la URL: cloudinary://api_key:api_secret@cloud_name
        parsed = urlparse(settings.CLOUDINARY_URL)
        
        cloud_name = parsed.hostname
        api_key = parsed.username
        api_secret = parsed.password
        
        # Verificar que todos los valores existan
        if not all([cloud_name, api_key, api_secret]):
            raise Exception("CLOUDINARY_URL mal formado. Formato: cloudinary://api_key:api_secret@cloud_name")
        
        # Configurar Cloudinary con los valores parseados
        cloudinary.config(
            cloud_name=cloud_name,
            api_key=api_key,
            api_secret=api_secret,
            secure=True
        )
        
        logger.info(f"✅ Cloudinary configurado correctamente (cloud: {cloud_name})")
        
    except Exception as e:
        logger.error(f"❌ Error configurando Cloudinary: {e}")
        raise

# Configurar al importar el módulo
configure_cloudinary()

def upload_avatar(file_bytes: bytes, user_id: str) -> str:
    """
    Sube un avatar a Cloudinary y devuelve la URL pública.
    AUTOMÁTICAMENTE BORRA EL AVATAR ANTERIOR (overwrite=True).
    
    Args:
        file_bytes: Bytes del archivo de imagen
        user_id: ID del usuario (para nombrar el archivo)
    
    Returns:
        str: URL segura (HTTPS) de la imagen en Cloudinary
    
    Raises:
        Exception: Si hay un error al subir la imagen
    """
    try:
        logger.info(f"📤 Subiendo avatar para usuario {user_id}")
        
        # 🔥 overwrite=True borra automáticamente la imagen anterior con el mismo public_id
        # Esto ahorra espacio en Cloudinary sin necesidad de borrado manual
        result = cloudinary.uploader.upload(
            file_bytes,
            folder="avatars",
            public_id=f"user_{user_id}",
            overwrite=True,  # 🔥 Reemplaza la imagen anterior automáticamente
            invalidate=True,  # 🔥 Invalida cache del CDN para mostrar la nueva imagen
            resource_type="image",
            transformation=[
                {'width': 400, 'height': 400, 'crop': 'fill', 'gravity': 'face'},
                {'quality': 'auto:good'},
                {'fetch_format': 'auto'}
            ]
        )
        
        avatar_url = result.get("secure_url")
        logger.info(f"✅ Avatar subido exitosamente (anterior reemplazado): {avatar_url}")
        
        return avatar_url
        
    except Exception as e:
        logger.error(f"❌ Error subiendo avatar a Cloudinary: {e}")
        raise Exception(f"Error al subir imagen: {str(e)}")

def delete_avatar(user_id: str) -> bool:
    """
    Elimina un avatar de Cloudinary.
    
    Args:
        user_id: ID del usuario
    
    Returns:
        bool: True si se eliminó correctamente, False en caso contrario
    """
    try:
        public_id = f"avatars/user_{user_id}"
        result = cloudinary.uploader.destroy(public_id)
        
        if result.get("result") == "ok":
            logger.info(f"🗑️ Avatar eliminado: {public_id}")
            return True
        else:
            logger.warning(f"⚠️ No se pudo eliminar avatar: {result}")
            return False
            
    except Exception as e:
        logger.error(f"❌ Error eliminando avatar: {e}")
        return False