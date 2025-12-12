"""
ADAPTADOR MONGODB REAL (Motor)
Implementa la misma interfaz que MockDatabase pero conecta con Atlas.
"""

import logging
from motor.motor_asyncio import AsyncIOMotorClient
from bson import ObjectId
from config import settings
from datetime import datetime

logger = logging.getLogger(__name__)

class MongoDatabase:
    def __init__(self):
        self.client = None
        self.db = None
        logger.info("⏳ Inicializando driver de MongoDB...")

    def connect(self):
        """Crea la conexión real"""
        try:
            self.client = AsyncIOMotorClient(settings.MONGODB_URL)
            self.db = self.client[settings.MONGODB_DB_NAME]
            logger.info(f"✅ Conectado a MongoDB Atlas: {settings.MONGODB_DB_NAME}")
        except Exception as e:
            logger.error(f"❌ Error fatal conectando a Mongo: {e}")
            raise e

    # ================================================================
    # HELPERS
    # ================================================================
    def _fix_id(self, doc):
        """Convierte _id (ObjectId) a id (str) para el frontend"""
        if doc and "_id" in doc:
            doc["id"] = str(doc["_id"])
            del doc["_id"]
        return doc

    # ================================================================
    # USUARIOS (Colección: users)
    # ================================================================
    
    async def create_user(self, user_data: dict) -> str:
        # Aseguramos que se guarde en 'users'
        result = await self.db["users"].insert_one(user_data)
        return str(result.inserted_id)

    async def get_user_by_email(self, email: str) -> dict:
        user = await self.db["users"].find_one({"email": email})
        return self._fix_id(user)

    async def user_exists(self, email: str) -> bool:
        count = await self.db["users"].count_documents({"email": email})
        return count > 0

    async def get_user_by_id(self, user_id: str) -> dict:
        try:
            oid = ObjectId(user_id)
            user = await self.db["users"].find_one({"_id": oid})
        except:
            # Fallback por si acaso
            user = await self.db["users"].find_one({"id": user_id})
            
        return self._fix_id(user)
    
    async def get_user_by_username(self, username: str) -> dict:
        """Obtener usuario por username (para validar duplicados)"""
        user = await self.db["users"].find_one({"username": username})
        return self._fix_id(user)
    
    async def get_user_by_username(self, username: str) -> dict:
        """Obtener usuario por username"""
        user = await self.db["users"].find_one({"username": username})
        return self._fix_id(user)
    
    async def update_user(self, email: str, update_data: dict) -> dict:
        await self.db["users"].update_one(
            {"email": email}, 
            {"$set": update_data}
        )
        return await self.get_user_by_email(email)
    
    async def update_user_email(self, current_email: str, new_email: str) -> bool:
        """
        Actualizar email del usuario.
        En MongoDB esto es simple: solo actualizar el campo.
        No hay problema de índice como en Mock DB.
        """
        try:
            result = await self.db["users"].update_one(
                {"email": current_email},
                {"$set": {"email": new_email}}
            )
            logger.info(f"✅ Mongo: Email actualizado - {current_email} → {new_email}")
            return result.modified_count > 0
        except Exception as e:
            logger.error(f"❌ Mongo: Error actualizando email - {str(e)}")
            return False
    
    async def update_user_email(self, current_email: str, new_email: str) -> bool:
        """
        Actualizar email del usuario.
        En MongoDB esto es simple: solo actualizar el campo.
        No hay problema de índice como en Mock DB.
        """
        try:
            result = await self.db["users"].update_one(
                {"email": current_email},
                {"$set": {"email": new_email}}
            )
            return result.modified_count > 0
        except Exception as e:
            logger.error(f"❌ Mongo: Error actualizando email - {str(e)}")
            return False

    async def update_user_activity(self, user_id: str) -> bool:
        """
        📅 Actualiza la fecha de actividad y calcula la racha
        """
        try:
            from bson.objectid import ObjectId
            
            # Buscar usuario
            user = await self.db["users"].find_one({"_id": ObjectId(user_id)})
            
            if not user:
                return False
            
            today = datetime.utcnow().date()
            last_activity = user.get("last_activity_date")
            current_streak = user.get("current_streak", 0)
            longest_streak = user.get("longest_streak", 0)
            
            # Primera vez
            if not last_activity:
                await self.db["users"].update_one(
                    {"_id": ObjectId(user_id)},
                    {"$set": {
                        "current_streak": 1,
                        "longest_streak": 1,
                        "last_activity_date": datetime.utcnow()
                    }}
                )
                return True
            
            # Calcular diferencia de días
            last_date = last_activity.date() if isinstance(last_activity, datetime) else last_activity
            days_diff = (today - last_date).days
            
            # Ya estudió hoy
            if days_diff == 0:
                return True
            
            # Estudió ayer → incrementar
            if days_diff == 1:
                current_streak += 1
            # Pasó más de 1 día → resetear
            else:
                current_streak = 1
            
            # Actualizar récord
            if current_streak > longest_streak:
                longest_streak = current_streak
            
            # Guardar en BD
            await self.db["users"].update_one(
                {"_id": ObjectId(user_id)},
                {"$set": {
                    "current_streak": current_streak,
                    "longest_streak": longest_streak,
                    "last_activity_date": datetime.utcnow()
                }}
            )
            
            logger.info(f"✅ MongoDB: Racha actualizada = {current_streak}")
            return True
            
        except Exception as e:
            logger.error(f"❌ Error actualizando actividad: {e}")
            return False

    # ================================================================
    # DICCIONARIO (Colección: dictionary_entries)
    # ================================================================

    async def create_dictionary_entry(self, entry_data: dict) -> str:
        if "id" in entry_data:
            del entry_data["id"] 
            
        # ⚠️ CORRECCIÓN: Usamos 'dictionary_entries' como en tu Atlas
        result = await self.db["dictionary_entries"].insert_one(entry_data)
        return str(result.inserted_id)

    async def get_user_dictionary(self, user_id: str, language: str = None) -> list:
        query = {"user_id": user_id}
        if language:
            query["language"] = language
            
        # ⚠️ CORRECCIÓN: Usamos 'dictionary_entries'
        cursor = self.db["dictionary_entries"].find(query)
        entries = await cursor.to_list(length=1000)
        return [self._fix_id(e) for e in entries]

    async def delete_dictionary_entry(self, entry_id: str) -> bool:
        try:
            # Intentar borrar por ObjectId
            result = await self.db["dictionary_entries"].delete_one({"_id": ObjectId(entry_id)})
            if result.deleted_count == 0:
                result = await self.db["dictionary_entries"].delete_one({"id": entry_id})
            return result.deleted_count > 0
        except:
            return False

    # ================================================================
    # FLASHCARDS SRS (Colección: flashcard_srs)
    # ================================================================
    
    async def create_flashcard_srs_data(self, srs_data: dict) -> str:
        """Crear datos SRS para una flashcard nueva"""
        if "id" in srs_data:
            del srs_data["id"]
        result = await self.db["flashcard_srs"].insert_one(srs_data)
        return str(result.inserted_id)
    
    async def get_flashcard_srs_data(self, word_id: str) -> dict:
        """Obtener datos SRS de una palabra"""
        srs = await self.db["flashcard_srs"].find_one({"word_id": word_id})
        return self._fix_id(srs)
    
    async def update_flashcard_srs_data(self, word_id: str, update_data: dict) -> bool:
        """Actualizar datos SRS tras una revisión"""
        try:
            result = await self.db["flashcard_srs"].update_one(
                {"word_id": word_id},
                {"$set": update_data}
            )
            return result.modified_count > 0
        except Exception as e:
            logger.error(f"❌ Error actualizando SRS: {e}")
            return False

# Instancia global
mongo_db = MongoDatabase()