"""
ADAPTADOR MONGODB REAL (Motor)
Implementa la misma interfaz que MockDatabase pero conecta con Atlas.
"""

import logging
from motor.motor_asyncio import AsyncIOMotorClient
from bson import ObjectId
from config import settings

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
    
    async def update_user(self, email: str, update_data: dict) -> dict:
        await self.db["users"].update_one(
            {"email": email}, 
            {"$set": update_data}
        )
        return await self.get_user_by_email(email)

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

# Instancia global
mongo_db = MongoDatabase()