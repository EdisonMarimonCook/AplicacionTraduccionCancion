# 🎵 MusicTransIAtor - Contexto del Proyecto

**Versión:** 4.3 (MVP Feature-Complete + Email Verification)
**Estado:** 🎯 MVP COMPLETO - Listo para Testing en Dispositivo Físico
**Hito Reciente:** Email verification, password recovery, UI improvements (menú flotante clickable, swipe gestures mejorados).

---

## 📌 VISIÓN DEL PROYECTO
App Android para aprender idiomas con música mediante análisis semántico de letras en tiempo real.
**Arquitectura:** Cliente-Servidor (Android Nativo + Python FastAPI).

---

## 🛠️ ESTADO TÉCNICO ACTUAL (v4.3)

### 🐍 BACKEND (FastAPI + Mongo Atlas) - **COMPLETO ✅**

* **IA:**
    * **Motor:** `gemini-2.5-flash-Lite` (Estable, Free Tier 15 RPM).
    * **Lógica:** Recibe la letra completa desde el Frontend para evitar errores de "0 chars".
    * **Fallback:** Respuesta Mock si se agota la cuota (evita crashes).

* **Autenticación JWT + Email Verification (NUEVO v4.3):**
    * Access Token (15 min) + Refresh Token (7 días).
    * Refresh automático en `AuthInterceptor` del Android.
    * ✅ **Verificación por email:** PIN de 4 dígitos enviado al registrarse
    * ✅ **Recuperación de contraseña:** Sistema completo con códigos por email
    * ✅ **Email service:** SMTP via Gmail con templates HTML personalizados
    * ✅ Endpoints: `/auth/verify`, `/auth/forgot-password`, `/auth/reset-password`

* **Gestión de Usuarios:**
    * ✅ Endpoint `/users/profile` - Obtener y actualizar perfil
    * ✅ Endpoint `/users/change-password` - Cambiar contraseña (con validación `confirm_password`)
    * ✅ Endpoint `/users/change-email` - Cambiar email (sin bug de índice Mock DB)
    * ✅ Función `username_exists()` con parámetro `exclude_email` para evitar falsos positivos
    * ✅ **Nivel de idioma:** Se obtiene desde `learning_languages` del usuario

* **Sistema de Racha Diaria:**
    * ✅ Calcula días consecutivos de estudio automáticamente
    * ✅ Se actualiza al guardar palabras en el diccionario
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
    * ✅ Cálculo automático de próxima revisión basado en dificultad

* **Infraestructura:**
    * Actualmente: Localhost (`127.0.0.1:8000` o `0.0.0.0:8000` para móvil físico).
    * Soporte para: Emulador (`10.0.2.2`), USB Tethering, WiFi Local, Reverse Tethering (ADB).
    * Futuro: Despliegue en **Render.com**.

---

### 📱 FRONTEND (Android Kotlin) - **FEATURE-COMPLETE ✅**

* **Red:**
    * Configurado para `10.0.2.2` (Emulador) o IP Local (Móvil Físico).
    * `AuthInterceptor` gestiona refresco de tokens transparente (401 → Refresh automático).

* **Autenticación Completa (NUEVO v4.3):**
    * ✅ **VerifyAccountActivity:** Pantalla de verificación con PIN de 4 dígitos
    * ✅ **ForgotPasswordActivity:** Solicitar código de recuperación
    * ✅ **ResetPasswordActivity:** Cambiar contraseña con código
    * ✅ Auto-navegación tras registro exitoso
    * ✅ Manejo de errores 403 (cuenta no verificada)

* **Flujo de Aprendizaje (`SongLearningActivity`):**
    1. Carga letra de Genius/Spotify (Endpoint `/lyrics`).
    2. Muestra letra en negro (Feedback inmediato).
    3. Envía letra a IA (`/ai/analyze`).
    4. Pinta `ClickableSpans`: **Naranja** (Palabras), **Azul/Cyan** (Expresiones).

* **Diccionario:**
    * Adaptado a nuevo modelo `UserWord` (con campos `type`, `example`, `isRecommended`).
    * Endpoints sincronizados: `/dictionary/add`, `/dictionary/list`, `/dictionary/delete/{id}`.

* **Flashcards con Gestos Mejorados (v4.3):**
    * ✅ Sistema de **swipe tipo Tinder/Duolingo**
    * ✅ **Feedback visual en tiempo real:**
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

## 📊 MODELOS PRINCIPALES

### User (MongoDB)
- email, username, password_hash
- learning_languages: [{ language: "en", level: "B1" }]
- current_streak, longest_streak, last_activity_date
- is_verified, verification_code

### FlashcardSRS
- word_id, easiness_factor, interval, repetitions
- next_review_date, times_reviewed

## 🐛 BUGS CONOCIDOS (Para arreglar post-Demo)

1. **Audio:** Limitado a 30s (Preview URL de iTunes/Spotify).
2. **Modo oscuro:** Pendiente de testing en dispositivo físico.
3. **Rotación de pantalla:** Pendiente de validación (configChanges configurado).

---

## 🛣️ HOJA DE RUTA (ROADMAP ACTUALIZADO)

### 🟢 FASE 1: MVP (COMPLETO 98%) ✅

**Estado:** Todos los endpoints funcionales. Flashcards, Racha, y Email Verification implementados.

**Pendiente para Demo:**
- [ ] Testing completo del flujo Usuario → Diccionario → Flashcards
- [ ] Validar racha con múltiples días de prueba
- [ ] Probar conexión móvil físico vs emulador
- [ ] Grabación de vídeo demo

**Alcance Cumplido:**
1. ✅ Registro: Usuario elige "Aprender Inglés B1".
2. ✅ Discovery: Ve lista "Top Grammy" o busca canciones.
3. ✅ Reproducción: Suena el audio (Spotify o iTunes).
4. ✅ Análisis: Gemini extrae vocabulario adaptado al nivel.
5. ✅ Diccionario: Guarda palabras/expresiones con contexto.
6. ✅ Flashcards: Sistema SRS con gestos intuitivos.
7. ✅ Perfil: Racha diaria + contador de palabras.

---

### 🟡 FASE 2: PRODUCTO FINAL (Enero 2026)

**Deployment:**
- Backend subido a **Render** (Gratis).
- APK subida a **GitHub Releases** o web sencilla.

**IA Robusta:**
- Migración a **Groq (Llama 3)** para velocidad extrema y cero costes.
- O **Ollama** local si se exige ejecución estricta.

**Features Adicionales:**
- **Selección de Usuario:** Permitir seleccionar texto no resaltado para traducción bajo demanda.
- **Error Handling Gracioso:** "La IA se está enfriando 🧊" si se agota la cuota.
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
2. **Refresh automático de tokens:** En AuthInterceptor, no manual
3. **Racha se actualiza al guardar palabras:** No al revisar flashcards
4. **Swipe threshold 50px:** Para mejor UX en emulador

### Testing
- **Emulador:** BASE_URL = "http://10.0.2.2:8000"
- **Móvil físico:** BASE_URL = "http://IP_LOCAL:8000" (mismo WiFi)
- **Backend:** `uvicorn main:app --host 0.0.0.0 --port 8000`

### Debugging Común
- **401 Unauthorized:** Token expirado → AuthInterceptor debe renovar
- **403 Forbidden:** Cuenta no verificada → Revisar email
- **500 Internal:** Ver logs de uvicorn en terminal

---

## 🔧 CONFIGURACIÓN DE RED (Testing Móvil Físico)

### **Opción 1: Emulador Android Studio**
```kotlin
private const val BASE_URL = "http://10.0.2.2:8000/"
```

## 📂 ESTRUCTURA DEL PROYECTO

backend/
├── routers/
│   ├── auth.py          # Login, register, verify email
│   ├── flashcards.py    # Sistema SRS con SM-2
│   ├── progress.py      # Racha y estadísticas
│   └── ...
├── models.py            # User, UserWord
├── database.py          # Abstracción MongoDB/Mock
└── main.py

frontend/android/app/src/main/java/com/example/diccionario_hiphop/
├── MainActivity.kt      # Login screen
├── FlashcardsActivity.kt # Swipe gestures
├── ApiService.kt        # Retrofit endpoints
└── ApiModels.kt         # Data classes

## 🔌 EJEMPLOS DE API

POST /api/v1/auth/login
Request: { "email": "...", "password": "..." }
Response: { "access_token": "...", "username": "..." }

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