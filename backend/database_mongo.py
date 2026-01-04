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
            user = await self.db["users"].find_one({"id": user_id})
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
        """Actualizar email del usuario."""
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
        📅 Actualiza la fecha de actividad y calcula la racha (CORREGIDO)
        """
        try:
            # Buscar usuario
            user = await self.db["users"].find_one({"_id": ObjectId(user_id)})
            
            if not user:
                return False
            
            today = datetime.utcnow().date()
            last_activity = user.get("last_activity_date")
            current_streak = user.get("current_streak", 0)
            longest_streak = user.get("longest_streak", 0)
            
            # --- Lógica de Racha Corregida ---

            # Caso 1: Primera vez
            if not last_activity:
                new_streak = 1
            else:
                # Asegurar que last_activity sea datetime
                if isinstance(last_activity, str):
                    try:
                        last_activity = datetime.fromisoformat(last_activity.replace('Z', '+00:00'))
                    except ValueError:
                        last_activity = datetime.utcnow() # Fallback

                last_date = last_activity.date()
                days_diff = (today - last_date).days

                if days_diff == 0:
                    # Mismo día: Mantener racha, pero actualizaremos la HORA
                    new_streak = current_streak
                elif days_diff == 1:
                    # Día consecutivo: Aumentar racha
                    new_streak = current_streak + 1
                else:
                    # Se rompió la racha: Reiniciar a 1 (porque hoy ha estudiado)
                    new_streak = 1
            
            # Actualizar récord si aplica
            if new_streak > longest_streak:
                longest_streak = new_streak
            
            # Guardar en BD (Siempre actualizamos last_activity_date para tener la hora exacta)
            await self.db["users"].update_one(
                {"_id": ObjectId(user_id)},
                {"$set": {
                    "current_streak": new_streak,
                    "longest_streak": longest_streak,
                    "last_activity_date": datetime.utcnow()
                }}
            )
            
            if new_streak != current_streak:
                logger.info(f"🔥 Racha actualizada para {user_id}: {current_streak} -> {new_streak}")
            
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
            
        result = await self.db["dictionary_entries"].insert_one(entry_data)
        return str(result.inserted_id)

    async def get_user_dictionary(self, user_id: str, language: str = None) -> list:
        query = {"user_id": user_id}
        if language:
            query["language"] = language
            
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
        if "id" in srs_data:
            del srs_data["id"]
        result = await self.db["flashcard_srs"].insert_one(srs_data)
        return str(result.inserted_id)
    
    async def get_flashcard_srs_data(self, word_id: str) -> dict:
        srs = await self.db["flashcard_srs"].find_one({"word_id": word_id})
        return self._fix_id(srs)
    
    async def update_flashcard_srs_data(self, word_id: str, update_data: dict) -> bool:
        try:
            result = await self.db["flashcard_srs"].update_one(
                {"word_id": word_id},
                {"$set": update_data}
            )
            return result.modified_count > 0
        except Exception as e:
            logger.error(f"❌ Error actualizando SRS: {e}")
            return False

    # ================================================================
    # 🔥 NUEVAS FUNCIONES PARA CONTADORES (Añadir al final de la clase)
    # ================================================================

    async def count_user_dictionary_items(self, user_id: str) -> int:
        """Cuenta cuántas palabras tiene el usuario en total"""
        try:
            count = await self.db["dictionary_entries"].count_documents({"user_id": user_id})
            return count
        except Exception as e:
            logger.error(f"Error contando diccionario: {e}")
            return 0

    async def count_user_flashcards_reviews(
        self, 
        user_id: str, 
        active_languages_only: list = None
    ) -> int:
        """
        Cuenta flashcards pendientes de repaso (next_review_date <= HOY)
        Opcionalmente filtra solo idiomas activos.
        """
        try:
            from datetime import datetime, timezone
            
            # 1. Obtener IDs de palabras del usuario que tienen ejemplo
            query = {"user_id": user_id, "example": {"$exists": True, "$ne": ""}}
            
            # 🔥 Filtrar por idiomas activos si se especifica
            if active_languages_only is not None:
                query["language"] = {"$in": active_languages_only}
            
            user_words = await self.db["dictionary_entries"].find(
                query,
                {"_id": 1}
            ).to_list(None)
            
            if not user_words:
                logger.info(f"📊 Usuario {user_id}: 0 palabras con ejemplo")
                return 0
            
            word_ids = [str(w["_id"]) for w in user_words]
            logger.info(f"📊 Usuario {user_id}: {len(word_ids)} palabras con ejemplo")
            
            # 2. Contar flashcards con next_review_date <= ahora
            now = datetime.now(timezone.utc)
            count = await self.db["flashcard_srs"].count_documents({
                "word_id": {"$in": word_ids},
                "next_review_date": {"$lte": now}
            })
            
            logger.info(f"📊 Usuario {user_id}: {count} flashcards pendientes de repaso")
            return count
            
        except Exception as e:
            logger.error(f"❌ Error contando flashcards: {e}", exc_info=True)
            return 0

    async def update_user_streak(self, user_id: str) -> int:
        """
        Refuerza y actualiza la racha del usuario (solo incrementa UNA VEZ al día, UTC estricto).
        - Siempre usa UTC para fechas y horas.
        - Evita doble incremento diario.
        - Actualiza récord de racha si corresponde.
        - Devuelve siempre el valor actualizado de la racha.
        - Logs claros para debug y doble llamada.
        """
        from datetime import datetime, timezone, timedelta
        try:
            user = await self.db["users"].find_one({"_id": ObjectId(user_id)})
            if not user:
                logger.warning(f"[Racha] Usuario no encontrado: {user_id}")
                return 0

            now = datetime.now(timezone.utc)
            today_utc = now.replace(hour=0, minute=0, second=0, microsecond=0, tzinfo=timezone.utc)
            last_activity = user.get("last_activity_date")
            current_streak = user.get("current_streak", 0)
            longest_streak = user.get("longest_streak", 0)

            # Si no hay actividad previa, iniciar racha
            if not last_activity:
                await self.db["users"].update_one(
                    {"_id": ObjectId(user_id)},
                    {"$set": {
                        "current_streak": 1,
                        "longest_streak": max(1, longest_streak),
                        "last_activity_date": now
                    }}
                )
                logger.info(f"🔥 [Racha] Iniciada para usuario {user_id}: 1 día (UTC)")
                return 1

            # Convertir last_activity a datetime UTC si es necesario
            if isinstance(last_activity, str):
                try:
                    last_activity = datetime.fromisoformat(last_activity.replace('Z', '+00:00'))
                except Exception:
                    logger.warning(f"[Racha] last_activity malformateada para usuario {user_id}, usando now")
                    last_activity = now
            if last_activity.tzinfo is None:
                last_activity = last_activity.replace(tzinfo=timezone.utc)

            last_activity_day = last_activity.replace(hour=0, minute=0, second=0, microsecond=0, tzinfo=timezone.utc)

            # YA HIZO ACTIVIDAD HOY → No incrementar
            if last_activity_day == today_utc:
                logger.info(f"🔥 [Racha] Usuario {user_id}: Ya hizo actividad hoy. Racha={current_streak}")
                return current_streak

            # AYER hizo actividad → Incrementar racha
            yesterday_utc = today_utc - timedelta(days=1)
            if last_activity_day == yesterday_utc:
                new_streak = current_streak + 1
                new_longest = max(new_streak, longest_streak)
                await self.db["users"].update_one(
                    {"_id": ObjectId(user_id)},
                    {"$set": {
                        "current_streak": new_streak,
                        "longest_streak": new_longest,
                        "last_activity_date": now
                    }}
                )
                logger.info(f"🔥 [Racha] Incrementada para usuario {user_id}: {new_streak} días (UTC)")
                return new_streak

            # Más de 1 día sin actividad → Reiniciar racha (pero actualizar récord si corresponde)
            new_longest = max(1, longest_streak)
            await self.db["users"].update_one(
                {"_id": ObjectId(user_id)},
                {"$set": {
                    "current_streak": 1,
                    "longest_streak": new_longest,
                    "last_activity_date": now
                }}
            )
            logger.info(f"🔥 [Racha] Reiniciada para usuario {user_id}: 1 día (UTC)")
            return 1

        except Exception as e:
            logger.error(f"❌ [Racha] Error actualizando racha: {e}", exc_info=True)
            return 0

    async def update_user_avatar(self, user_id: str, avatar_url: str) -> bool:
        """
        Actualiza la URL del avatar del usuario en MongoDB.
        
        Args:
            user_id: ID del usuario
            avatar_url: Nueva URL del avatar en Cloudinary
        
        Returns:
            bool: True si se actualizó correctamente
        """
        try:
            result = await self.db["users"].update_one(
                {"_id": ObjectId(user_id)},
                {"$set": {"avatar_url": avatar_url}}
            )
            
            if result.modified_count > 0:
                logger.info(f"✅ Avatar actualizado en BD para usuario {user_id}")
                return True
            else:
                logger.warning(f"⚠️ No se modificó el avatar (puede que ya fuera el mismo)")
                return False
                
        except Exception as e:
            logger.error(f"❌ Error actualizando avatar en BD: {e}")
            raise

# Instancia global
mongo_db = MongoDatabase()