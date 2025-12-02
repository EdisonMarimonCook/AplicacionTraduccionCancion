# 🎵 MusicTransIAtor - Contexto del Proyecto

**Versión:** 4.0 (Integración Backend-Frontend Estable)  
**Rol:** Arquitecto de Software / Lead Developer  
**Estado:** 🔥 FASE FINAL MVP (Backend Cerrado - Frontend Integrando)  
**Próxima Entrega:** 16/12/2025 (MVP Funcional)

---

## 📌 VISIÓN DEL PROYECTO

App Android "F2P" (Free to Play) para aprender idiomas con música.  
**Filosofía:** Máxima funcionalidad con coste cero (APIs públicas, IAs gratuitas, Hosting Free Tier).  
**Innovación:** Análisis contextual de la letra (no diccionario estático) y estrategia de audio híbrida.

---

## 🎯 ESTADO TÉCNICO ACTUAL (v4.0)

### 🐍 BACKEND (FastAPI + Mongo Atlas) - **ESTADO: TERMINADO ✅**
El servidor es la fuente de verdad. No usamos Mocks para la demo final.

* **IA (Gemini 2.0 Flash):**
    * Prompt V3: Devuelve JSON estructurado separando `words` (Vocabulario) de `expressions` (Idioms).
    * Contexto: Explica el significado basándose en el nivel real del usuario (`target_level`).
* **🎵 Audio & Música:**
    * **Estrategia "iTunes Rescue":** Si Spotify no da `preview_url` (común ahora), el backend busca automáticamente en iTunes API para rescatar el MP3 de 30s.
    * **Búsqueda Unificada:** `/api/v1/songs/search` devuelve canciones reales.
* **🛡️ Seguridad:**
    * JWT con **Refresh Token** (Access 15min / Refresh 7 días).
    * Registro con selección de `target_language` y `target_level`.
* **💾 Base de Datos:**
    * **MongoDB Atlas** (Producción).
    * Fallback automático a `MockDatabase` (Memoria) si falla la conexión.

### 📱 FRONTEND (Android Kotlin) - **ESTADO: EN INTEGRACIÓN 🔄**
Adaptado para consumir el Backend Real v4.0.

* **Modelos:** Sincronizados con los Schemas de Pydantic (`UserWord` con `type`, `example`, `isRecommended`).
* **Red:** `AuthInterceptor` maneja automáticamente el error 401 refrescando el token.
* **UI:** `SongLearningActivity` renderiza palabras (Naranja) y expresiones (Azul) interactivas.

---

## 🛣️ HOJA DE RUTA (ROADMAP)

### 🟢 FASE 1: EL MVP (Objetivo: 16 Diciembre)
**Alcance:** Flujo "Happy Path" completo y REAL.
1.  **Registro:** Usuario elige "Aprender Inglés B1".
2.  **Discovery:** Ve lista "Top Grammy" o busca "Estopa".
3.  **Reproducción:** Suena el audio (Spotify o iTunes).
4.  **Análisis:** Gemini extrae vocabulario adaptado al B1.
5.  **Diccionario:** Guarda palabras ("Word") o frases ("Expression") con su contexto.
6.  **Perfil:** Ve su progreso básico (nº palabras guardadas).

### 🟡 FASE 2: PRODUCTO FINAL (Enero 2026)
**Alcance:** Retención y Calidad de Vida.
1.  **Flashcards SRS:** Algoritmo de Repaso Espaciado (SuperMemo/Anki) usando el campo `example` guardado.
2.  **Infraestructura:** Despliegue en Render/Railway (salir de localhost).
3.  **Seguridad:** Encriptación de secretos real (no hardcoded strings).

### 🔴 FASE 3: I+D (Futuro / "Zona Pirata")
1.  **Audio Completo ("Estrategia Grayjay"):** Investigar `NewPipeExtractor` o `yt-dlp` en servidor intermedio para saltar la restricción de 30s.
2.  **Modo Karaoke:** Integrar API de `LRCLIB` para tiempos exactos sílaba a sílaba.
3.  **IA Open Source:** Migrar de Gemini a Llama 3 / Mistral (vía Groq o Ollama local) para independencia total.

---

## 📋 GUÍA PARA DESARROLLADORES (AI ASSISTANTS)

Si vas a generar código, respeta estas reglas **SAGRADAS** de la v4.0:

### 1. Contrato de API (Endpoints Críticos)
* **Login:** `POST /api/v1/auth/login` -> Devuelve `{ access_token, refresh_token, user }`
* **Registro:** `POST /api/v1/auth/register` -> Envía `{ native_language, target_language, target_level }`
* **Buscar:** `GET /api/v1/songs/search?query=X` -> Devuelve lista directa `[...]` (no `{results: [...]}`)
* **Analizar:** `POST /api/v1/ai/analyze` -> Devuelve `{ words: [], expressions: [] }`
* **Diccionario:** `POST /api/v1/dictionary/add` -> Envía `{ type: "word"|"expression", example: "..." }`

### 2. Estructura de Datos
* **Fechas:** Siempre usar `isoformat()` en el Backend. El Frontend parsea Strings.
* **Niveles:** Usar escala CEFR (`A1`, `A2`, `B1`, `B2`, `C1`, `C2`).
* **Carpetas:** El campo `type` en el diccionario define si es palabra suelta o expresión.

### 3. Prohibiciones
* ❌ **NO** usar Mocks en los Routers (`songs.py`, `lyrics.py`). Usar la lógica real.
* ❌ **NO** crear endpoints `/flashcards` en el Backend todavía (usamos el Diccionario filtrado).
* ❌ **NO** cambiar la IP `10.0.2.2` en Android (es el localhost del emulador).

---
*Última actualización: 02/12/2025 - Hito: Backend v4.0 Completo*