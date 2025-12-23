# 🎵 MusicTransIAtor - Contexto del Proyecto

**Versión:** 5.0 (Producto Final en Desarrollo)  
**Estado:** 🎯 MVP COMPLETO (v4.5) → Transición a Producto Final  
**Hito Reciente:** MVP entregado con vídeo demo. Preparando infraestructura de producción.

---

## 📌 VISIÓN DEL PROYECTO

App Android **100% gratuita** para aprender idiomas con música mediante análisis semántico de letras en tiempo real.  
**Arquitectura:** Cliente-Servidor (Android Nativo + Python FastAPI).  
**Público objetivo:** Estudiantes universitarios (proyecto académico con potencial comercial).

---

## 🏗️ ARQUITECTURA DEL SISTEMA

```
┌─────────────────┐      HTTP/JSON       ┌──────────────────┐
│   Android App   │ ◄─────────────────► │   FastAPI        │
│   (Kotlin)      │   JWT Auth (15min)   │   (Python 3.11)  │
│   + ExoPlayer   │                      │   + Render.com   │
└─────────────────┘                      └──────────────────┘
       │                                          │
       │ Glide (Imágenes)                        │
       │ uCrop (Avatar)                          ▼
       │ TTS (Audio)                      ┌──────────────────┐
       │                                   │  MongoDB Atlas   │
       │                                   │  (Cloud DB)      │
       │                                   └──────────────────┘
       ▼                                          │
┌─────────────────┐                              ▼
│  Local Cache    │                      ┌──────────────────┐
│  (SharedPrefs)  │                      │   Gemini AI      │
│  + Room DB      │                      │  2.5 Flash Lite  │
└─────────────────┘                      │  (o Groq)        │
       │                                  └──────────────────┘
       └─ Tokens JWT                              │
       └─ Top Grammy                              └─ Análisis semántico
       └─ Perfil Usuario
       └─ Canciones Recomendadas

┌─────────────────┐
│   Cloudinary    │ ◄── Avatares persistentes
└─────────────────┘

┌─────────────────┐
│  NewPipe/Grayjay│ ◄── Audio streaming (YouTube/SoundCloud)
└─────────────────┘
```

---

## 📋 Roadmap y Tareas

### 🚨 ORDEN DE TAREAS (Roadmap)

#### 🟢 FASE 1: INFRAESTRUCTURA (CRÍTICO)
- **Render.com:**
  - ✅ Configurado Web Service en Render (repo GitHub, rama feature/lyrics-translation)
  - 🔄 Genius lyrics bloqueado por Cloudflare (solo funciona en local)
  - ✅ Health check, variables de entorno (MONGO_URI, GEMINI_API_KEY, etc.)
- **Cloudinary:**
  - ✅ Integrado en backend (avatars persistentes)
  - ✅ users.py actualizado, credenciales en .env
- **IP Automática en RetrofitService:**
  - ✅ Detección automática de entorno (emulador, móvil, producción)
  - ✅ APK universal, sin configuración manual de IP

#### 🔥 FASE 2: UX CORE (ALTO IMPACTO)
- **Bottom Navigation + ViewPager2:**
  - 🔄 3 Tabs (🏠 Inicio | 🔥 Grammys | 👤 Perfil)
  - 🔄 Swipe horizontal, buscador global, modo oscuro/horizontal
- **Diccionario Multinivel:**
  - 📚 **Nueva Estructura:**
    - **Nivel 1 (Idiomas Activos):** Carpetas por idioma (solo los que estudia el usuario)
    - **Nivel 2 (Tipo de Contenido):** Subcategorías: 📝 Palabras | 💬 Expresiones
    - **Nivel 3 (Contenido):** Lista de tarjetas finales
  - 🔄 Frontend: ExpandableListAdapter con banderas
  - 🔄 Backend: Endpoint con filtros por idioma y tipo
- **Flashcards Mejoradas:**
  - 🔄 Animación de volteo 3D (300ms)
  - 🔄 Audio TTS automático al mostrar palabra
  - 🔄 Fix: Las tarjetas se crean con next_review_date = hoy
- **Fix Contadores en Tiempo Real:**
  - 🔄 Implementar onResume y Pull-to-refresh para rachas y contadores
- **Audio con NewPipe (Fase 2):**
  - 🔄 Integrar librería NewPipeExtractor para reproducir previews/audio de YouTube
  - 🔄 Alternativas: ExoPlayer + NewPipeExtractor, fallback a iTunes/Spotify preview

#### 🟡 FASE 3: OPTIMIZACIÓN (MEDIO)
- **Room DB:**
  - 🔄 Guardar Top Grammy, Perfil y Recomendaciones en base local (offline/instantáneo)
- **Optimización de Tiempos de Carga:**
  - 🔄 Precarga paralela en Splash Screen, índices en MongoDB
- **Pantallas de Carga + Animaciones:**
  - 🔄 LoadingDialog con animación Lottie, mensajes motivacionales
  - 🔄 Ripple effects y confetti en logros/rachas
- **Indicador Visual isRecommended:**
  - 🔄 Estrella dorada o badge "IA Recomienda" en palabras importantes

#### 🔵 FASE 4: OPCIONALES (SI HAY TIEMPO)
- **Groq como Fallback:**
  - 🔄 Si Gemini falla, usar Groq (Llama 3) antes de dar error
- **Gamificación Local:**
  - 🔄 Logros guardados en local ("Primera racha", "50 palabras"), gráficos de progreso
- **Soporte Pantalla Horizontal:**
  - 🔄 Layouts land para tablet/proyector, activar rotación en Manifest

---

### ✅ Tareas Completadas
- Cloudinary avatars persistentes
- RetrofitService con IP automática universal
- Health check y variables en Render
- APK universal (emulador, móvil, producción)
- Diccionario multinivel: estructura y diseño definidos

### 🔄 Tareas Pendientes/Criticas
- Render: Genius lyrics bloqueado por Cloudflare (solo funciona en local)
- Bottom Navigation + ViewPager2
- Diccionario multinivel: implementación backend/frontend
- Flashcards mejoradas (animación, audio TTS)
- Audio con NewPipeExtractor (Fase 2)
- Room DB cache, optimización de carga, animaciones UI

---

## 📚 Diccionario Multinivel (Tarea 5)

**Nivel 1 (Idiomas Activos):**
- El usuario ve primero sus "carpetas" de idiomas: 🇬🇧 Inglés, 🇫🇷 Francés, 🇩🇪 Alemán.
- Solo aparecen los idiomas que el usuario esté estudiando o tenga guardados.

**Nivel 2 (Tipo de Contenido):**
- Al entrar (o desplegar) un idioma (ej. Inglés), ve dos sub-categorías claras:
  - 📝 Palabras (Vocabulario suelto)
  - 💬 Expresiones (Idioms, frases hechas)

**Nivel 3 (El Contenido):**
- Dentro de cada sub-categoría está la lista de tarjetas final.

---

## 🚀 Roadmap Resumido

- **FASE 1: MVP (COMPLETO 100%) ✅**
  - Registro con verificación por email
  - Login + auto-login + "Recordarme"
  - Top Grammy + búsqueda
  - Análisis de letras con IA
  - Diccionario personalizado
  - Flashcards SRS
  - Sistema de racha
  - Perfil con avatar
  - Cambio de contraseña/email
  - Recuperación de contraseña
  - Modo oscuro
  - Swipe gestures
  - Vídeo demo grabado

- **FASE 2: Producto Final (Dic 2024 - Ene 2025)**
  - Infraestructura: Render.com, Cloudinary, IP auto-detection, landing page
  - UX Core: Bottom Navigation, Diccionario multinivel, Flashcards mejoradas, contadores en tiempo real, audio NewPipe
  - Optimización: Room DB, pantallas de carga, animaciones
  - Opcionales: Groq fallback, gamificación local, soporte horizontal

---

## 🛠️ Estado Técnico Actual

- **Backend:** FastAPI + MongoDB Atlas + Gemini + Cloudinary (completo, salvo Genius en Render)
- **Frontend:** Android Kotlin, Retrofit, Room DB, ExoPlayer, uCrop, Glide, TTS
- **Infraestructura:** Render.com, Cloudinary, MongoDB Atlas, Gemini, Groq

---

## 📞 Contacto y Contribución

- Edison Marimon Cook (@EdisonMarimonCook) - Propietario del repositorio
- Hugo - Desarrollador principal
- [Otros miembros] - Landing page y tareas adicionales

**Repositorio:** [GitHub - AplicacionTraduccionCancion](https://github.com/EdisonMarimonCook/AplicacionTraduccionCancion)

**Ramas:**
- `main` - Versión estable (MVP 4.5)
- `feature/lyrics-translation` - Desarrollo activo
- `release/5.0` - Producto final (próximamente)

**Estado del Proyecto:**
- ✅ MVP completado y demostrado
- 🚧 Transición a producto final
- 🎯 Objetivo: Lanzamiento público Enero 2026

---

**Última actualización:** 23 de Diciembre de 2025
