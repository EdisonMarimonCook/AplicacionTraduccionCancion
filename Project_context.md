# 🎵 MusicTransIAtor - Contexto del Proyecto

**Versión:** 4.1 (Stabilized MVP)
**Estado:** 🚀 LISTO PARA DEMO / GRABACIÓN
**Hito Reciente:** Solución de crisis de límites de IA y sincronización de Registro.

---

## 📌 VISIÓN DEL PROYECTO
App Android para aprender idiomas con música mediante análisis semántico de letras en tiempo real.
**Arquitectura:** Cliente-Servidor (Android Nativo + Python FastAPI).

---

## 🛠️ ESTADO TÉCNICO ACTUAL (v4.1)

### 🐍 BACKEND (FastAPI + Mongo Atlas)
* **IA:**
    * **Motor:** `gemini-2.5-flash-Lite` (Estable, Free Tier 15 RPM).
    * **Lógica:** Recibe la letra completa desde el Frontend para evitar errores de "0 chars".
* **Registro Multi-idioma:**
    * Modelo de datos adaptado para recibir listas (`learning_languages: [...]`) soportando expansión futura, aunque el MVP solo use uno.
    * Login devuelve estructura plana (`username` en raíz) para facilitar el parsing en Android.
* **Infraestructura:**
    * Actualmente: Localhost (`127.0.0.1:8000`).
    * Futuro inmediato: Despliegue en **Render.com**.

### 📱 FRONTEND (Android Kotlin)
* **Red:**
    * Configurado para `10.0.2.2` (Emulador) o IP Local (Móvil Físico).
    * `AuthInterceptor` gestiona refresco de tokens transparente.
* **Flujo de Aprendizaje (`SongLearningActivity`):**
    1.  Carga letra de Genius/Spotify (Endpoint `/lyrics`).
    2.  Muestra letra en negro (Feedback inmediato).
    3.  Envía letra a IA (`/ai/analyze`).
    4.  Pinta `ClickableSpans`: **Naranja** (Palabras), **Azul/Cyan** (Expresiones).
* **Diccionario:**
    * Adaptado a nuevo modelo `UserWord` (con campos `type`, `example`, `isRecommended`).

---

## 🐛 BUGS CONOCIDOS (Para arreglar post-Demo)
1.  **Contador de Progreso:** Al borrar una palabra del diccionario, el contador total del perfil no se decrementa.
2.  **Audio:** Limitado a 30s (Preview URL de iTunes/Spotify).

---

## 🛣️ HOJA DE RUTA (ROADMAP ACTUALIZADO)

### 🟢 FASE 1: PRESENTACIÓN PROTOTIPO (AHORA)
* **Objetivo:** Grabar vídeo promocional y defensa en clase.
* **Infraestructura:** PC del alumno haciendo de servidor (Ollama o Gemini 1.5).
* **IA:** Gemini 2.5 Flash-Lite (Google Cloud).

### 🟡 FASE 2: PRODUCTO FINAL (ENTREGA)
* **Deployment:**
    * Backend subido a **Render** (Gratis).
    * APK subida a **GitHub Releases** o web sencilla.
* **IA Robusta:** Migración a **Groq (Llama 3)** para velocidad extrema y cero costes, o **Ollama** si se exige ejecución local estricta.
* **Features:**
    * **Selección de Usuario:** Permitir seleccionar texto no resaltado para pedir traducción a la IA bajo demanda.
    * **Error Handling Gracioso:** "La IA se está enfriando 🧊" si se agota la cuota.
    * **Flashcards SRS:** Implementar el algoritmo de repaso.

### 🔴 FASE 3: FUTURO (V2.0)
* **Audio Completo:** Integración con fuentes alternativas (Youtube-dl/Grayjay logic).
* **Modo Karaoke:** Sincronización temporal sílaba a sílaba.

---

## 📋 REGLAS DE DESARROLLO (V4.1)
1.  **Frontend manda, Backend obedece:** El frontend envía la letra a la IA, no el backend por su cuenta (evita desincronización).
2.  **Modelos Pydantic:** `UserCreate` debe aceptar `learning_languages` como lista.
3.  **Parsers:** El Frontend espera JSON plano en Login (sin `user` anidado).
4.  **IA:** Usar siempre `gemini-1.5-flash` para desarrollo. No usar `2.0-experimental` por bloqueo de cuota `limit: 0`.

---