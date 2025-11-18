"""
MOCK DATABASE - Simula MongoDB en memoria
Para desarrollo sin BD real conectada
"""

import logging
from datetime import datetime
from typing import Optional, Dict, List

logger = logging.getLogger(__name__)

# ================================================================
# ALMACENAMIENTO EN MEMORIA
# ================================================================

class MockDatabase:
    """Simula MongoDB mientras se configura la BD real"""
    
    def __init__(self):
        # Tablas simuladas
        self.users: Dict[str, dict] = {}
        self.dictionary_entries: Dict[str, dict] = {}
        self.sessions: Dict[str, dict] = {}
        self.progress: Dict[str, dict] = {}
        
        # Contadores para IDs
        self.user_counter = 0
        self.dict_counter = 0
        self.session_counter = 0
        
        logger.info("✅ MOCK DATABASE INICIADA (datos en memoria)")
    
    # ================================================================
    # USUARIOS
    # ================================================================
    
    async def create_user(self, user_data: dict) -> str:
        """Crear usuario"""
        try:
            user_id = f"mock_user_{self.user_counter}"
            self.user_counter += 1
            
            user = {
                "_id": user_id,
                **user_data,
                "created_at": datetime.now().isoformat(),
                "updated_at": datetime.now().isoformat()
            }
            
            self.users[user_data["email"]] = user
            
            logger.info(f"✅ MOCK: Usuario creado - {user_data['email']}")
            
            # Log idiomas de forma segura
            langs = user.get('learning_languages', [])
            if langs:
                lang_str = ", ".join([f"{l.get('language')}({l.get('level')})" for l in langs])
                logger.info(f"📚 MOCK: Idiomas - {lang_str}")
            
            return user_id
        
        except Exception as e:
            logger.error(f"❌ MOCK: Error creando usuario - {str(e)}")
            raise
    
    async def get_user_by_email(self, email: str) -> Optional[dict]:
        """Obtener usuario por email"""
        try:
            user = self.users.get(email)
            
            if user:
                logger.info(f"✅ MOCK: Usuario encontrado - {email}")
                langs = user.get('learning_languages', [])
                if langs:
                    lang_str = ", ".join([f"{l.get('language')}({l.get('level')})" for l in langs])
                    logger.info(f"📚 MOCK: Idiomas - {lang_str}")
            else:
                logger.info(f"⚠️  MOCK: Usuario no encontrado - {email}")
            
            return user
        
        except Exception as e:
            logger.error(f"❌ MOCK: Error obteniendo usuario - {str(e)}")
            return None
    
    async def user_exists(self, email: str) -> bool:
        """Verificar si usuario existe"""
        return email in self.users
    
    async def get_user_by_id(self, user_id: str) -> Optional[dict]:
        """Obtener usuario por ID"""
        for user in self.users.values():
            if user.get("_id") == user_id:
                return user
        return None
    
    # ================================================================
    # DICCIONARIO
    # ================================================================
    
    async def create_dictionary_entry(self, entry_data: dict) -> str:
        """Crear entrada en diccionario"""
        try:
            entry_id = f"mock_dict_{self.dict_counter}"
            self.dict_counter += 1
            
            entry = {
                "_id": entry_id,
                **entry_data,
                "created_at": datetime.now().isoformat()
            }
            
            self.dictionary_entries[entry_id] = entry
            
            word = entry_data.get('word', 'unknown')
            language = entry_data.get('language', 'unknown')
            logger.info(f"✅ MOCK: Palabra guardada - {word} ({language})")
            
            return entry_id
        
        except Exception as e:
            logger.error(f"❌ MOCK: Error creando entrada - {str(e)}")
            raise
    
    async def get_user_dictionary(self, user_id: str, language: Optional[str] = None) -> List[dict]:
        """Obtener diccionario del usuario"""
        try:
            entries = [
                e for e in self.dictionary_entries.values()
                if e.get("user_id") == user_id
            ]
            
            if language:
                entries = [e for e in entries if e.get("language") == language]
                logger.info(f"✅ MOCK: {len(entries)} palabras en {language}")
            else:
                logger.info(f"✅ MOCK: {len(entries)} palabras totales")
            
            return entries
        
        except Exception as e:
            logger.error(f"❌ MOCK: Error obteniendo diccionario - {str(e)}")
            return []
    
    async def delete_dictionary_entry(self, entry_id: str) -> bool:
        """Eliminar entrada del diccionario"""
        try:
            if entry_id in self.dictionary_entries:
                del self.dictionary_entries[entry_id]
                logger.info(f"✅ MOCK: Palabra eliminada - {entry_id}")
                return True
            
            logger.warning(f"⚠️  MOCK: Palabra no encontrada - {entry_id}")
            return False
        
        except Exception as e:
            logger.error(f"❌ MOCK: Error eliminando palabra - {str(e)}")
            return False
    
    # ================================================================
    # SESIONES
    # ================================================================
    
    async def create_session(self, session_data: dict) -> str:
        """Crear sesión de estudio"""
        try:
            session_id = f"mock_session_{self.session_counter}"
            self.session_counter += 1
            
            session = {
                "_id": session_id,
                **session_data,
                "created_at": datetime.now().isoformat()
            }
            
            self.sessions[session_id] = session
            
            song_name = session_data.get('song_name', 'unknown')
            logger.info(f"✅ MOCK: Sesión creada - {song_name}")
            
            return session_id
        
        except Exception as e:
            logger.error(f"❌ MOCK: Error creando sesión - {str(e)}")
            raise
    
    async def get_user_sessions(self, user_id: str) -> List[dict]:
        """Obtener sesiones del usuario"""
        try:
            sessions = [
                s for s in self.sessions.values()
                if s.get("user_id") == user_id
            ]
            
            logger.info(f"✅ MOCK: {len(sessions)} sesiones")
            return sessions
        
        except Exception as e:
            logger.error(f"❌ MOCK: Error obteniendo sesiones - {str(e)}")
            return []
    
    # ================================================================
    # PROGRESO
    # ================================================================
    
    async def get_user_progress(self, user_id: str) -> dict:
        """Obtener progreso del usuario"""
        try:
            progress = self.progress.get(user_id, {
                "user_id": user_id,
                "total_words_learned": 0,
                "total_songs_completed": 0,
                "total_study_hours": 0,
                "current_streak": 0,
                "longest_streak": 0,
                "stats_by_language": {},
                "created_at": datetime.now().isoformat()
            })
            
            logger.info("✅ MOCK: Progreso obtenido")
            return progress
        
        except Exception as e:
            logger.error(f"❌ MOCK: Error obteniendo progreso - {str(e)}")
            return {}
    
    async def update_user_progress(self, user_id: str, progress_data: dict) -> bool:
        """Actualizar progreso del usuario"""
        try:
            progress = await self.get_user_progress(user_id)
            progress.update(progress_data)
            progress["updated_at"] = datetime.now().isoformat()
            
            self.progress[user_id] = progress
            
            logger.info("✅ MOCK: Progreso actualizado")
            return True
        
        except Exception as e:
            logger.error(f"❌ MOCK: Error actualizando progreso - {str(e)}")
            return False
    
    # ================================================================
    # UTILIDADES
    # ================================================================
    
    def clear_all(self):
        """Limpiar todas las tablas (útil para testing)"""
        self.users.clear()
        self.dictionary_entries.clear()
        self.sessions.clear()
        self.progress.clear()
        logger.info("🗑️  MOCK: Base de datos limpiada")
    
    def get_stats(self) -> dict:
        """Obtener estadísticas de la MOCK DB"""
        return {
            "usuarios": len(self.users),
            "palabras": len(self.dictionary_entries),
            "sesiones": len(self.sessions),
            "progreso_registros": len(self.progress)
        }

# ================================================================
# INSTANCIA GLOBAL
# ================================================================

mock_db = MockDatabase()