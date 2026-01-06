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
        self.flashcards: Dict[str, dict] = {}
        self.songs: Dict[str, dict] = {}
        self.song_analyses: Dict[str, dict] = {}
        self.flashcard_srs = {}  # word_id -> SRS data
        
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
    
    async def get_user_by_username(self, username: str) -> Optional[dict]:
        """Obtener usuario por username (para validar duplicados)"""
        for user in self.users.values():
            if user.get("username") == username:
                logger.info(f"✅ MOCK: Usuario encontrado por username - {username}")
                return user
        logger.info(f"⚠️  MOCK: Usuario no encontrado por username - {username}")
        return None
    
    async def get_user_by_username(self, username: str) -> Optional[dict]:
        """Obtener usuario por username"""
        for user in self.users.values():
            if user.get("username") == username:
                logger.info(f"✅ MOCK: Usuario encontrado por username - {username}")
                return user
        logger.info(f"⚠️  MOCK: Usuario no encontrado por username - {username}")
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
        """Eliminar entrada del diccionario y su flashcard SRS asociada"""
        try:
            if entry_id in self.dictionary_entries:
                # 1. Borrar entrada del diccionario
                del self.dictionary_entries[entry_id]
                
                # 2. Borrar flashcard SRS asociada si existe
                for srs_id, srs_data in list(self.flashcard_srs.items()):
                    if srs_data.get("word_id") == entry_id:
                        del self.flashcard_srs[srs_id]
                        logger.info(f"🗑️ MOCK: Flashcard SRS eliminada para word_id={entry_id}")
                        break
                
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
        """Obtener progreso del usuario - MEJORADO"""
        progress = self.progress.get(user_id, {
            "user_id": user_id,
            "total_words_learned": 0,
            "total_songs_completed": 0,
            "total_study_hours": 0,
            "current_streak": 0,
            "longest_streak": 0,
            "last_study_date": None,
            "words_learned_today": 0,
            "words_learned_this_week": 0,
            "words_learned_this_month": 0,
            "songs_this_week": 0,
            "daily_goals": {
                "target_words": 10,
                "target_minutes": 30,
                "completed_today": False
            },
            "stats_by_language": {
                "en": {
                    "words_learned": 0,
                    "songs_completed": 0,
                    "estimated_level": "A1",
                    "time_studied": 0,
                    "last_activity_date": None
                }
            },
            "created_at": datetime.now().isoformat(),
            "updated_at": datetime.now().isoformat()
        })
        return progress
    
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
    # USUARIOS - VALIDACIONES
    # ================================================================
    
    async def username_exists(self, username: str, exclude_email: Optional[str] = None) -> bool:
        """Verificar si username ya está cogido"""
        for user in self.users.values():
            if user.get("username") == username:
                # Si estamos editando usuario, permitir su propio username
                if exclude_email and user.get("email") == exclude_email:
                    continue
                return True
        return False
    
    async def email_exists(self, email: str) -> bool:
        """Verificar si email ya está cogido"""
        return email in self.users
    
    # ================================================================
    # USUARIOS - ACTUALIZAR
    # ================================================================
    
    async def update_user(self, email: str, update_data: dict) -> Optional[dict]:
        """
        Actualizar datos del usuario
        
        Campos permitidos:
        - username
        - full_name
        - native_language
        - learning_languages
        - password_hash (internamente)
        """
        try:
            user = self.users.get(email)
            
            if not user:
                logger.warning(f"⚠️  MOCK: Usuario no encontrado - {email}")
                return None
            
            # Actualizar campos
            if "username" in update_data and update_data["username"]:
                user["username"] = update_data["username"]
                logger.info(f"✅ MOCK: Username actualizado - {update_data['username']}")
            
            if "full_name" in update_data:
                user["full_name"] = update_data["full_name"]
                logger.info(f"✅ MOCK: Nombre actualizado")
            
            if "native_language" in update_data:
                user["native_language"] = update_data["native_language"]
                logger.info(f"✅ MOCK: Idioma nativo actualizado")
            
            if "learning_languages" in update_data and update_data["learning_languages"]:
                user["learning_languages"] = update_data["learning_languages"]
                langs = ", ".join([f"{l.get('language')}({l.get('level')})" for l in update_data["learning_languages"]])
                logger.info(f"✅ MOCK: Idiomas actualizados - {langs}")
            
            if "password_hash" in update_data:
                user["password_hash"] = update_data["password_hash"]
                logger.info(f"✅ MOCK: Contraseña actualizada")
            
            user["updated_at"] = datetime.now().isoformat()
            
            return user
        
        except Exception as e:
            logger.error(f"❌ MOCK: Error actualizando usuario - {str(e)}")
            return None
    
    async def update_user_email(self, old_email: str, new_email: str) -> bool:
        """
        🔥 SOLUCIÓN AL PROBLEMA DEL ÍNDICE:
        Cuando cambias el email, hay que MOVER el usuario en el diccionario.
        
        Pasos:
        1. Verificar que nuevo email no exista
        2. Sacar usuario del diccionario con clave vieja
        3. Actualizar su campo email
        4. Guardar con nueva clave (new_email)
        """
        try:
            if new_email in self.users:
                logger.warning(f"⚠️  MOCK: Email ya existe - {new_email}")
                return False
            
            # MOVER usuario (pop elimina la clave vieja)
            user = self.users.pop(old_email)
            user["email"] = new_email
            user["updated_at"] = datetime.now().isoformat()
            
            # Guardar con nueva clave
            self.users[new_email] = user
            
            logger.info(f"✅ MOCK: Email cambiado de {old_email} a {new_email}")
            logger.info(f"🗑️ MOCK: Clave vieja borrada del diccionario (ARREGLADO)")
            return True
        
        except Exception as e:
            logger.error(f"❌ MOCK: Error cambiando email - {str(e)}")
            return False
    
    async def update_user_activity(self, user_id: str) -> bool:
        """
        📅 Actualiza la fecha de actividad y calcula la racha
        """
        try:
            # Buscar usuario por ID
            user = None
            for email, u in self.users.items():
                if u.get("_id") == user_id or u.get("id") == user_id:
                    user = u
                    break
            
            if not user:
                return False
            
            today = datetime.utcnow().date()
            last_activity = user.get("last_activity_date")
            current_streak = user.get("current_streak", 0)
            longest_streak = user.get("longest_streak", 0)
            
            # Primera vez que estudia
            if not last_activity:
                user["current_streak"] = 1
                user["longest_streak"] = 1
                user["last_activity_date"] = datetime.utcnow()
                return True
            
            # Convertir a date si es datetime
            if isinstance(last_activity, datetime):
                last_date = last_activity.date()
            elif isinstance(last_activity, str):
                last_date = datetime.fromisoformat(last_activity).date()
            else:
                last_date = last_activity
            
            days_diff = (today - last_date).days
            
            # Si ya estudió hoy, no cambiar nada
            if days_diff == 0:
                return True
            
            # Si estudió ayer, incrementar racha
            if days_diff == 1:
                current_streak += 1
            # Si pasó más de 1 día, resetear racha
            else:
                current_streak = 1
            
            # Actualizar longest streak si superó el récord
            if current_streak > longest_streak:
                longest_streak = current_streak
            
            user["current_streak"] = current_streak
            user["longest_streak"] = longest_streak
            user["last_activity_date"] = datetime.utcnow()
            
            logger.info(f"✅ Actividad actualizada para {user.get('email')}: Racha = {current_streak}")
            return True
            
        except Exception as e:
            logger.error(f"❌ Error actualizando actividad: {e}")
            return False
    
    # ================================================================
    # FLASHCARDS
    # ================================================================
    
    async def create_flashcard(self, flashcard_data: dict) -> str:
        """Crear flashcard"""
        flashcard_id = f"mock_flashcard_{len(self.flashcards)}"
        flashcard = {
            "_id": flashcard_id,
            **flashcard_data,
            "created_at": datetime.now().isoformat()
        }
        self.flashcards[flashcard_id] = flashcard
        return flashcard_id

    async def get_user_flashcards(self, user_id: str) -> list:
        """Obtener flashcards del usuario"""
        return [f for f in self.flashcards.values() if f.get("user_id") == user_id]

    async def update_flashcard_review(self, user_id: str, flashcard_id: str, correct: bool) -> dict:
        """Actualizar resultado review"""
        flashcard = self.flashcards.get(flashcard_id)
        if not flashcard or flashcard.get("user_id") != user_id:
            return None
        
        if correct:
            flashcard["correct_count"] = flashcard.get("correct_count", 0) + 1
        else:
            flashcard["incorrect_count"] = flashcard.get("incorrect_count", 0) + 1
        
        flashcard["last_reviewed"] = datetime.now().isoformat()
        return flashcard
    
    # ================================================================
    # CANCIONES
    # ================================================================
    
    async def create_song(self, song_data: dict) -> str:
        """Guardar canción"""
        song_id = f"mock_song_{len(self.songs)}"
        song = {
            "_id": song_id,
            **song_data,
            "cached_at": datetime.now().isoformat()
        }
        self.songs[song_id] = song
        return song_id

    async def get_song_by_genius_id(self, genius_id: str) -> dict:
        """Obtener canción por Genius ID (para evitar duplicados)"""
        for song in self.songs.values():
            if song.get("genius_id") == genius_id:
                return song
        return None

    async def get_song(self, song_id: str) -> dict:
        """Obtener canción por ID"""
        return self.songs.get(song_id)

    async def get_songs_by_language(self, language: str) -> list:
        """Obtener canciones por idioma"""
        return [s for s in self.songs.values() if s.get("language") == language]
    
    # ================================================================
    # ANALISIS DE CANCIONES
    # ================================================================
    
    async def create_song_analysis(self, analysis_data: dict) -> str:
        """Guardar análisis de canción"""
        analysis_id = f"mock_analysis_{len(self.song_analyses)}"
        analysis = {
            "_id": analysis_id,
            **analysis_data,
            "created_at": datetime.now().isoformat()
        }
        self.song_analyses[analysis_id] = analysis
        return analysis_id

    async def get_song_analysis(self, song_id: str) -> dict:
        """Obtener análisis de una canción"""
        for analysis in self.song_analyses.values():
            if analysis.get("song_id") == song_id:
                return analysis
        return None

    # ================================================================
    # UTILIDADES
    # ================================================================
    
    def clear_all(self):
        """Limpiar todas las tablas (útil para testing)"""
        self.users.clear()
        self.dictionary_entries.clear()
        self.sessions.clear()
        self.progress.clear()
        self.flashcards.clear()
        self.songs.clear()
        self.song_analyses.clear()
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