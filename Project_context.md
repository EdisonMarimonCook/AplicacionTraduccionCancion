# 🎵 MusicTransIAtor - Contexto del Proyecto

**Versión:** 4.5 (MVP Feature-Complete + Profile System)
**Estado:** 🎯 MVP COMPLETO - Sistema de Perfil Implementado
**Hito Reciente:** Perfil de usuario con avatar, contadores dinámicos y caché persistente.

---

## 📌 VISIÓN DEL PROYECTO
App Android para aprender idiomas con música mediante análisis semántico de letras en tiempo real.
**Arquitectura:** Cliente-Servidor (Android Nativo + Python FastAPI).

---

## 🛠️ ESTADO TÉCNICO ACTUAL (v4.5)

### 🐍 BACKEND (FastAPI + Mongo Atlas) - **COMPLETO ✅**

* **IA:**
    * **Motor:** `gemini-2.5-flash-Lite` (Estable, Free Tier 15 RPM).
    * **Lógica:** Recibe la letra completa desde el Frontend para evitar errores de "0 chars".
    * **Fallback:** Respuesta Mock si se agota la cuota (evita crashes).

* **Autenticación JWT + Email Verification + Auto-Login (v4.4):**
    * Access Token (15 min) + Refresh Token (7 días).
    * Refresh automático en `AuthInterceptor` del Android.
    * ✅ **Verificación por email:** PIN de 4 dígitos enviado al registrarse
    * ✅ **Recuperación de contraseña:** Sistema completo con códigos por email
    * ✅ **Email service:** SMTP via Gmail con templates HTML personalizados
    * ✅ **Auto-Login (NUEVO v4.4):** 
        * Al iniciar la app, valida token guardado con `/users/profile`
        * Si es válido → Acceso directo (sin pantalla de login)
        * Si es inválido → Limpia tokens y muestra login
        * Solo funciona si usuario marcó "Recordarme" en login anterior
    * ✅ Endpoints: `/auth/verify`, `/auth/forgot-password`, `/auth/reset-password`

* **Gestión de Usuarios:**
    * ✅ Endpoint `/users/profile` - Obtener y actualizar perfil con contadores dinámicos
    * ✅ Endpoint `/users/change-password` - Cambiar contraseña (con validación `confirm_password`)
    * ✅ Endpoint `/users/change-email` - Cambiar email (sin bug de índice Mock DB)
    * ✅ Endpoint `/users/upload-avatar` - Subida de avatar con limpieza automática de archivos antiguos
    * ✅ Función `username_exists()` con parámetro `exclude_email` para evitar falsos positivos
    * ✅ **Nivel de idioma:** Se obtiene desde `learning_languages` del usuario
    * ✅ **Contadores en perfil:** `words_count`, `reviews_count`, `streak` calculados dinámicamente
    * ✅ **Sistema de avatares:** Archivos guardados en `backend/static/avatars/`, servidos vía StaticFiles mount

* **Sistema de Racha Diaria (MEJORADO v4.4):**
    * ✅ Calcula días consecutivos de estudio automáticamente
    * ✅ Se actualiza al guardar palabras en el diccionario
    * ✅ **NUEVO:** Se actualiza también al revisar flashcards
    * ✅ Resetea si pasan más de 1 día sin actividad
    * ✅ Guarda récord personal (`longest_streak`)
    * **Campos nuevos en User:** `current_streak`, `last_activity_date`, `longest_streak`

* **Flashcards con SRS:**
    * ✅ Algoritmo **SuperMemo SM-2** (mismo de Anki)
    * ✅ Colección MongoDB: `flashcard_srs` (datos de repetición espaciada)
    * ✅ Endpoints:
        * `GET /flashcards/due` → Tarjetas pendientes de hoy
        * `POST /flashcards/review/{id}` → Registrar resultado (calidad 0-5)
        * `GET /flashcards/stats` → Estadísticas (total, dominadas, pendientes hoy)
    * Actualmente: Localhost (`127.0.0.1:8000` o `0.0.0.0:8000` para móvil físico).
    * Soporte para: Emulador (`10.0.2.2`), USB Tethering, WiFi Local, Reverse Tethering (ADB).
    * ✅ **Caché persistente:** Top Grammy guardado en `backend/top10_cache.json` entre sesiones del servidor
* **Infraestructura:**
    * Actualmente: Localhost (`127.0.0.1:8000` o `0.0.0.0:8000` para móvil físico).
    * Soporte para: Emulador (`10.0.2.2`), USB Tethering, WiFi Local, Reverse Tethering (ADB).
    * Futuro: Despliegue en **Render.com**.

---

### 📱 FRONTEND (Android Kotlin) - **FEATURE-COMPLETE ✅**

* **Red:**
    * Configurado para `10.0.2.2` (Emulador) o IP Local (Móvil Físico).
    * `AuthInterceptor` gestiona refresco de tokens transparente (401 → Refresh automático).

* **Autenticación Completa (v4.4):**
    * ✅ **MainActivity:** Auto-login inteligente que valida tokens al inicio
    * ✅ **VerifyAccountActivity:** Pantalla de verificación con PIN de 4 dígitos
    * ✅ **ForgotPasswordActivity:** Solicitar código de recuperación
    * ✅ **ResetPasswordActivity:** Cambiar contraseña con código
    * ✅ Auto-navegación tras registro exitoso

* **Sistema de Perfil Completo (v4.5 - NUEVO):**
    * ✅ **Avatar de usuario:**
        * Upload desde galería con recorte circular (uCrop)
        * Visualización con `ShapeableImageView` (recorte nativo circular)
        * Limpieza automática de avatares antiguos al subir uno nuevo
        * Carga sin caché con `Glide` (signature con timestamp)
        * Placeholder con ícono por defecto si no hay avatar
    * ✅ **Contadores dinámicos:**
        * Palabras aprendidas (`words_count`)
        * Racha actual (`current_streak`)
        * Flashcards pendientes (`reviews_count`)
        * Actualización automática al volver de Dictionary/Flashcards/SongLearning
    * ✅ **Recarga inteligente:**
        * `onResume()` recarga perfil SIEMPRE que se muestra la pantalla
        * Evita múltiples cargas simultáneas con flag `isLoadingProfile`
        * Propagación de `RESULT_OK` desde actividades hijas
    * ✅ **Tema sin ActionBar:** Configurado para evitar barra superior en modo oscuro/claro

* **UI/UX (v4.5):**
    * ✅ **Modo oscuro:** Implementado y testeado en casi todas las interfaces
    * ✅ **ProfileActivity:** Sin barra superior (theme NoActionBar)
    * ✅ **Menú flotante:** Iconos completamente clickables, layers correctos
    * ✅ **Caché persistente:** Top Grammy se mantiene entre sesiones de la app
    * ✅ Manejo de errores 403 (cuenta no verificada)
5 - COMPLETO):**
    * ✅ **Avatar circular funcional:**
        * Upload desde galería con recorte (uCrop)
        * ShapeableImageView con cornerSize 50%
        * Glide con skipMemoryCache y signature única
        * Eliminación automática de avatares antiguos en backend
    * ✅ **Contadores en tiempo real:**
        * Palabras: Cuenta entradas en `dictionary_entries`
        * Racha: Actualizada en backend al guardar palabras
        * Repasos: Flashcards con `next_review_date <= hoy`
        * Se recarga automáticamente al volver de cualquier actividad
    * ✅ Muestra racha actual (`current_streak`) y récord (`longest_streak`)
    * ✅ **Nivel real del usuario** obtenido desde `learning_languages`
    * ✅ **Recarga en onResume():** Se actualiza siempre que se muestra la pantalla
    * ✅ Integración completa con `/user/profile` endpoint
    * ✅ Tema NoActionBar (sin barra superior molesta)
* **Diccionario:**
    * Adaptado a nuevo modelo `UserWord` (con campos `type`, `example`, `isRecommended`).
    * Endpoints sincronizados: `/dictionary/add`, `/dictionary/list`, `/dictionary/delete/{id}`.

* **Flashcards con Gestos Mejorados (v4.3):**
    * ✅ Sistema de **swipe tipo Tinder/Duolingo**
    * ✅ **Feedback visual en tie
- avatar_url (ruta relativa: /static/avatars/avatar_userid_timestamp.jpg)

### UserProfileResponse (Pydantic Schema)
- id, username, email, full_name, native_language
- avatar_url, learning_languages, created_at, is_active
- streak (int), reviews_count (int), words_count (int) ← Calculados dinámicamentempo real:**
        * Rotación de tarjeta mientras arrastras (±15°)
        * Overlay de color verde/rojo según dirección
        * Indicador de texto "FÁCIL ✓" / "DIFÍCIL ✗"
    * ✅ Deslizar **derecha (→)** → Fácil (verde) → Se verá en varios días
    * ✅ Deslizar **izquierda (←)** → Difícil (rojo) → Se verá mañana
    * ✅ Reseteo suave si no completas el swipe (>50px)
    * ✅ Indicador visual de tiempo: "La verás en 6 días"
    * ✅ Animación fluida de deslizamiento con rotación
    * ✅ Umbral reducido (50px) para mejor UX en emulador

* **Perfil de Usuario (v4.3):**
    * ✅ Muestra racha actual (`current_streak`) y récord (`longest_streak`)
    * ✅ Contador de palabras aprendidas
    * ✅ **Nivel real del usuario** obtenido desde `learning_languages`
    * ✅ **Flashcards pendientes** de hoy (actualización dinámica)
    * ✅ Botón de editar foto de perfil (preparado para galería)
    * ✅ Integración completa con `/user/progress`

* **Menú Flotante (ARREGLADO v4.3):**
    * ✅ **Iconos completamente clickables** (no solo textos)
    * ✅ Layers correctos: Dimmer (10dp) → Textos (15dp) → FABs (20dp)
    * ✅ Textos decorativos, FABs funcionales
    * ✅ Animaciones suaves de apertura/cierre

---

## 📊Avatar en producción:** Pendiente migración a Cloudinary/Render para deploy

### User (MongoDB)
- email, username, password_hash
- learning_languages: [{ language: "en", level: "B1" }]
- current_streak, longest_streak, last_activity_date
- is_verified, verification_code
100%) ✅

**Estado:** Todos los endpoints funcionales. Flashcards, Racha, Email Verification y Sistema de Perfil
- next_review_date, times_reviewed

## 🐛 BUGS CONOCIDOS (Para arreglar post-Demo)

1. **Audio:** Limitado a 30s (Preview URL de iTunes/Spotify).
2. **Modo oscuro:** Pendiente de testing en dispositivo físico.
3. **Rotación de pantalla:** Pendiente de validación (configChanges configurado).

---

## 🛣️ HOJA DE RUTA (ROADMAP ACTUALIZADO)

### 🟢 FASE 1: MVP (COMPLETO 98%) ✅
Grabación de vídeo demo

**Alcance Cumplido:**
1. ✅ Registro: Usuario elige "Aprender Inglés B1" con verificación por email.
2. ✅ Discovery: Ve lista "Top Grammy" cacheada o busca canciones.
3. ✅ Reproducción: Suena el audio (Spotify o iTunes).
4. ✅ Análisis: Gemini extrae vocabulario adaptado al nivel.
5. ✅ Diccionario: Guarda palabras/expresiones con contexto.
6. ✅ Flashcards: Sistema SRS con gestos intuitivos tipo Tinder.
7. ✅ Perfil: Avatar circular + racha diaria + contadores dinámicos.
8. ✅ Seguridad: Cambio de contraseña, cambio de email, auto-login".
2. ✅ Discovery: Ve lista "Top Grammy" o busca canciones.
3. ✅ Reproducción: Suena el audio (Spotify o iTunes).
4. ✅ Análisis: Gemini extrae vocabulario adaptado al nivel.
5. ✅ Diccionario: Guarda palabras/expresiones con contexto.
6. ✅ Flashcards: Sistema SRS con gestos intuitivos.
7. ✅ Perfil: Racha diaria + contador de palabras.

--**Avatar storage:** Migrar a Cloudinary (archivos estáticos persistentes).
- -

### 🟡 FASE 2: PRODUCTO FINAL (Enero 2026)

**Deployment:**
- Backend subido a **Render** (Gratis).
- APK subida a **GitHub Releases** o web sencilla.

**IA Robusta:**
- Migración a **Groq (Llama 3)** para velocidad extrema y cero costes.
- O **Ollama** local si se exige ejecución estricta.

**Features Adicionales:**
- **Selección de Usuario:** Permitir seleccionar texto no resaltado para traducción bajo demanda.
- **Error Handling Gracioso:** "La IA se está enfriando, disculpe las molestias y vuelva luego 🧊" si se agota la cuota.
- **Optimización SRS:** Ajustes finos del algoritmo según feedback de usuarios.

---

### 🔴 FASE 3: FUTURO (V2.0)

* **Audio Completo:** Integración con fuentes alternativas (Youtube-dl/Grayjay logic).
* **Modo Karaoke:** Sincronización temporal sílaba a sílaba con `/lyrics/with-fragments`.
* **IA Open Source:** Migrar a Llama 3 / Mistral (vía Groq o Ollama).

---

## 📋 REGLAS DE DESARROLLO (V4.2)

1. **Frontend manda, Backend obedece:** El frontend envía la letra a la IA, no el backend por su cuenta (evita desincronización).
2. **Modelos Pydantic:** `UserCreate` debe aceptar `learning_languages` como lista.
3. **Parsers:** El Frontend espera JSON plano en Login (con `username` en raíz).
4. **IA:** Usar siempre `gemini-2.5-flash-lite` para desarrollo. NO usar `2.0-flash` (quota limit: 0).
5. **Flashcards:** Cualquier palabra con campo `example` es elegible para SRS automáticamente.
6. **Racha:** Se actualiza solo al guardar palabras, no al revisarlas (evita inflar artificialmente).

---

## 🤖 GUÍA PARA ASISTENTES IA

### Convenciones de Código
- **Backend:** snake_case (Python PEP8)
- **Frontend:** camelCase (Kotlin standard)
- **API:** snake_case en JSON (serialización con @SerializedName)

### Decisiones de Diseño Clave
1. **Frontend envía letra a IA:** Para evitar desincronización
2. **Refresh automático de tokens:** En Aut O revisar flashcards:** Ambas acciones cuentan
4. **Swipe threshold 50px:** Para mejor UX en emulador
5. **Avatar con ShapeableImageView:** Recorte circular nativo sin `.circleCrop()` de Glide
6. **Perfil onResume() siempre recarga:** Evita contadores desactualizados
7. **Caché persistente:** Top Grammy y perfil mantienen datos entre sesionesevisar flashcards
4. **Swipe threshold 50px:** Para mejor UX en emulador

### Testing
- **Emulador:** BASE_URL = "http://10.0.2.2:8000"
- **Móvil físico:** BASE_URL = "http://IP_LOCAL:8000" (mismo WiFi)
- **Backend:** `uvicorn main:app --host 0.0.0.0 --port 8000`

- **Avatar no se ve:** Verificar que ShapeableImageView tenga `shapeAppearanceOverlay="@style/CircleImageView"`
- **Contadores en 0:** Asegurar que `words_count` esté en `UserProfileResponse` schema
- **Perfil no se actualiza:** Verificar que `onResume()` llama a `loadUserProfile()`
### Debugging Común
- **401 Unauthorized:** Token expirado → AuthInterceptor debe renovar
- **403 Forbidden:** Cuenta no verificada → Revisar email
- **500 Internal:** Ver logs de uvicorn en terminal

---

## 🔧 CONFIGURACIÓN DE RED (Testing Móvil Físico)

### **Opción 1: Emulador Android Studio**
```kotlin
private const val BASE_URL = "http://10.0.2.2:8000/"
```users.py         # Profile, avatar upload, change password/email
│   ├── flashcards.py    # Sistema SRS con SM-2
│   ├── progress.py      # Racha y estadísticas
│   └── ...
├── static/
│   └── avatars/         # Avatar uploads (avatar_{user_id}_{timestamp}.jpg)
├── models.py            # User, UserWord
├── database.py          # Abstracción MongoDB/Mock
└── main.py

frontend/android/app/src/main/java/com/example/diccionario_hiphop/
├── MainActivity.kt          # Login screen + Auto-login
├── ProfileActivity.kt       # Avatar, contadores, onResume() reload
├── FlashcardsActivity.kt    # Swipe gestures
├── SongLearningActivity.kt  # Añadir palabras con RESULT_OK
├── ApiService.kt            # Retrofit endpoints
└── ApiModels.kt             # Abstracción MongoDB/Mock
└── main.py

GET /api/v1/users/profile
Headers: { "Authorization": "Bearer <token>" }
Response: { "id": "...", "username": "...", "words_count": 3, "reviews_count": 1, "streak": 5, ... }

POST /api/v1/users/upload-avatar
Headers: { "Authorization": "Bearer <token>" }
Body: multipart/form-data { "file": <image> }
Response: { "url": "/static/avatars/avatar_xxx_xxx.jpg", "message": "..." }

frontend/android/app/src/main/java/com/example/diccionario_hiphop/
├── MainActivity.kt      # Login screen
├── FlashcardsActivity.kt # Swipe gestures
├── ApiService.kt        # Retrofit endpoints
└── ApiModels.kt         # Data classes

## 🔌 EJEMPLOS DE API

POST /api/v1/auth/login
Request: { "email": "...", "password": "..." }
Response: { "access_token": "...", "username": "..." }
🆕 NOVEDADES v4.5 - Sistema de Perfil Completo

### **Perfil de Usuario Funcional al 100%**

**Avatar de Usuario:**
- ✅ Upload desde galería con recorte circular (librería uCrop)
- ✅ Visualización con `ShapeableImageView` (Material Design)
- ✅ Recorte circular nativo con `cornerSize="50%"` en themes.xml
- ✅ Backend elimina automáticamente avatar antiguo al subir uno nuevo
- ✅ Archivos guardados en `backend/static/avatars/` con nombres únicos
- ✅ Glide carga sin caché con `signature(ObjectKey(timestamp))`
- ✅ Placeholder con ícono si no hay avatar

**Contadores Dinámicos:**
- ✅ **Palabras aprendidas:** Cuenta entradas en `dictionary_entries` collection
- ✅ **Racha actual:** Actualizada en backend al guardar palabras o revisar flashcards
- ✅ **Flashcards pendientes:** Cuenta flashcards con `next_review_date <= hoy`
- ✅ Schema `UserProfileResponse` incluye: `words_count`, `reviews_count`, `streak`
- ✅ Actualización automática al volver de Dictionary/Flashcards/SongLearning

**Recarga Inteligente:**
- ✅ `onResume()` recarga perfil SIEMPRE que se muestra la pantalla
- ✅ Flag `isLoadingProfile` evita múltiples llamadas simultáneas
- ✅ `SongLearningActivity` devuelve `RESULT_OK` cuando añade palabras
- ✅ Propagación de resultados con `startActivityForResult` en cadena

**UI/UX Mejoradas:**
- ✅ Tema `NoActionBar` elimina barra superior molesta
- ✅ Modo oscuro testeado y funcional
- ✅ Logs detallados para debugging de avatar (AVATAR_DEBUG)
- ✅ Caché persistente de Top Grammy entre sesiones

**Archivos Modificados:**

**Backend:**
1. `routers/users.py`:
   - `get_user_profile()`: Calcula `words_count`, `reviews_count`, `streak` dinámicamente
   - `upload_avatar()`: Guarda archivo, elimina antiguo, actualiza BD
   - Logs detallados de debugging

2. `routers/schemas.py`:
   - `UserProfileResponse`: Añadido `words_count: int = 0`

3. `main.py`:
   - StaticFiles mount para `/static` con ruta absoluta
   - Creación automática de carpeta `avatars/`

**Frontend:**
1. `ProfileActivity.kt`:
   - `onResume()`: Recarga perfil siempre
   - Avatar con `ShapeableImageView`
   - Logs AVATAR_DEBUG para debugging
   - Listener de Glide para detectar errores de carga

2. `activity_profile.xml`:
   - `ShapeableImageView` reemplaza `ImageView`
   - Referencia a `@style/CircleImageView`

3. `themes.xml`:
   - `NoActionBar` theme para ProfileActivity
   - `CircleImageView` style con `cornerSize="50%"`

4. `AndroidManifest.xml`:
   - ProfileActivity usa theme NoActionBar

5. `SongLearningActivity.kt`:
   - Flag `wordWasAdded` para marcar cuando se añade una palabra
   - `onDestroy()` devuelve `RESULT_OK` si `wordWasAdded == true`

6. `SongSelectionActivity.kt`:
   - `onActivityResult()` para códigos 200 (SongLearning) y 300 (Profile)

**Flujo Técnico de Actualización:**
```
SongLearningActivity: Añade palabra → wordWasAdded = true → onDestroy() → setResult(RESULT_OK)
     ↓
SongSelectionActivity: onActivityResult(200) → setResult(RESULT_OK)
     ↓
Usuario abre ProfileActivity → onResume() → loadUserProfile() → Contadores actualizados
```

**Testing Realizado:**
- ✅ Upload de avatar desde galería
- ✅ Recorte circular con uCrop
- ✅ Visualización correcta en modo claro y oscuro
- ✅ Eliminación de avatar antiguo al subir uno nuevo
- ✅ Contadores se actualizan al añadir palabras
- ✅ Contadores se actualizan al revisar flashcards
- ✅ Perfil se recarga automáticamente al volver de otras pantallas

---

## 
## 🔐 VARIABLES DE ENTORNO (.env)

MONGO_URI=mongodb+srv://...
GEMINI_API_KEY=...
MAIL_USERNAME=... (Gmail)
MAIL_PASSWORD=... (App Password de Google)
SECRET_KEY=...

## ⚡ QUICK START

Backend:
cd backend
pip install -r requirements.txt
uvicorn main:app --reload --host 0.0.0.0 --port 8000

Frontend:
Cambiar BASE_URL en RetrofitService.kt
Android Studio → Run
---

##  NOVEDADES v4.4 - Auto-Login Implementado

### **Funcionalidad "Remember Me" Completa**

**Comportamiento:**
-  Usuario marca "Recordarme"  Tokens se guardan persistentemente
-  Al abrir la app  Valida tokens autom�ticamente con el backend
-  Si tokens v�lidos  Acceso directo sin login
-  Si tokens inv�lidos  Limpia autom�ticamente y muestra login

**Archivos Modificados:**

1. **MainActivity.kt** (Login Screen):
   - `onCreate()`: Detecta tokens guardados y ejecuta `tryAutoLogin()`
   - `tryAutoLogin()`: Valida token con `/users/profile` endpoint
   - `performLogin()`: Guarda tokens cuando "Remember Me" est� activo
   - Limpia tokens inv�lidos autom�ticamente con `clearTokens()`

2. **TokenManager.kt** (Gesti�n de Tokens):
   - Nuevo m�todo: `clearTokens()` (alias de `clearSession()`)
   - Mantiene separaci�n entre limpiar tokens y logout completo
   - `forceLogout()`: Limpia tokens + datos de usuario + redirige a login

**Flujo T�cnico:**
```
App Start  TokenManager.getToken() != null?
     S�  tryAutoLogin()
              apiService.getProfile() v�lido?
                    S�  navigateToSongSelection()
                    NO  clearTokens() + showLoginScreen()
     NO  showLoginScreen()
```

**UX Mejorada:**
- Usuario con sesi�n activa NO ve pantalla de login (experiencia fluida)
- Tokens expirados/inv�lidos se limpian transparentemente
- Logout manual disponible en ProfileActivity
- Seguridad: Validaci�n backend obligatoria antes de acceso

**Testing:**
1. Login con "Recordarme" activado
2. Cerrar app completamente
3. Reabrir app  Debe entrar directamente (sin login)
4. En ProfileActivity  Cerrar sesi�n
5. Reabrir app  Debe mostrar login (tokens limpiados)
