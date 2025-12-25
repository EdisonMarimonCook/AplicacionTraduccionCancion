# 🎵 MusicTransIAtor - Contexto del Proyecto

**Versión:** 5.1 (Estabilización UI & Carga Paralela)  
**Fecha de Actualización:** 25 de Diciembre de 2025  
**Estado:** 🚀 Optimización SongLearning Completada → Próximo: UI Global & Diccionarios  
**Distribución:** 🏴‍☠️ Portal Propio (APK) *(No Play Store - Sideloading)*

---

## 📌 Visión del Proyecto

App Android **100% gratuita** para aprender idiomas con música mediante análisis semántico de letras en tiempo real.  
**Diferencial:** Audio "Unleashed" (estilo Grayjay), lyrics sincronizadas (LRCLIB) y análisis IA gramatical.  
**Público objetivo:** Estudiantes universitarios (proyecto académico con potencial comercial).

---

## 🏗️ Arquitectura del Sistema

```mermaid
graph TD
    User[Usuario Android] <--> App
    subgraph "Cliente (Android Nativo - XML)"
        App[App Kotlin]
        Exo[ExoPlayer + Audio Híbrido]
        Views[XML Layouts + Glide + uCrop]
        Cache[Room DB + SharedPrefs]
        TTS[Text-To-Speech]
        Async[Coroutines + Parallel Loading]
    end
    subgraph "Backend (Render.com + Python FastAPI)"
        API[FastAPI Service]
        Lyrics[Lyrics Engine Híbrido]
        AudioService[Audio Logic (yt-dlp)]
    end
    subgraph "Nube & Datos"
        Mongo[(MongoDB Atlas)]
        Cloudinary[Gestión de Imágenes]
        Gemini[IA: Gemini 2.0 Flash]
    end
    subgraph "Fuentes Externas"
        LRC[LRCLIB API (Lyrics + Tiempos)]
        Genius[Genius (Scraping Fallback)]
        YT[YouTube/Spotify (Audio Source)]
    end
    App <--> API
    API <--> Mongo
    API <--> Cloudinary
    API <--> Gemini
    API <--> Lyrics
    Lyrics <--> LRC
    Lyrics <--> Genius
    Exo <--> YT
```

---

## 📋 Roadmap y Tareas

### 🚨 Orden de Tareas (Roadmap v5.1)

#### 🟢 Fase 1: Infraestructura (Estado: Estable)
- **Render.com (Backend):** ✅ Configurado y operativo (Rama `feature/lyrics-translation`).
- **Lyrics Engine:** ✅ Híbrido (LRCLIB + Fallback Genius con curl-cffi).
- **Cloudinary:** ✅ Integrado para avatares persistentes.
- **Networking:** ✅ Detección IP automática (Retrofit) para APK universal.

#### 🔥 Fase 2: UX Core & Reproductor (Prioridad Inmediata)
1. **Optimización SongLearning (✅ COMPLETADO HOY):**
   - ✅ Carga Paralela: Letra + Análisis IA + Audio cargan simultáneamente (Coroutines async).
   - ✅ UI Limpia: Spinner de IA reubicado (encima de la letra), eliminación de bugs visuales (Toast fantasma negro).
   - ✅ Fix Lyrics: Padding inferior añadido para evitar corte de texto.
2. **Audio & Control (🚧 EN PROCESO):**
   - ⏳ SeekBar: Implementar barra de progreso arrastrable en SongLearningActivity.
   - ❓ Estabilidad Audio: Pendiente testear caducidad de enlaces de YouTube tras pausas largas/bloqueo de pantalla.
   - 🔄 Sistema "Pirata": Mantener integración yt-dlp en backend por ahora.
3. **Interfaz y Navegación (PRÓXIMO PASO):**
   - 🔄 Bottom Navigation: Implementar menú inferior estándar (Inicio | Grammys | Perfil).
   - ⏳ Auditoría Visual: Revisar márgenes y consistencia en todas las pantallas.
4. **Diccionario Multinivel & Flashcards:**
   - 🔄 Estructura Jerárquica: Idioma (Banderas) → Tipo (Palabra/Expresión) → Lista.
   - 🔄 Flashcards 2.0: Animación 3D, Audio Dual (TTS + Clip Canción).

#### 🟡 Fase 3: Optimización & Wake-Up (Medio Plazo)
*(A realizar tras completar la UI Global y Diccionarios)*
1. **Super Wake-Up Screen (Carga Paralela Masiva):**
   - Implementar WakeUpActivity que sirva de Splash Screen inteligente.
   - Objetivo: Paralelizar la carga de:
     - Ping a Render (Despertar servidor)
     - Perfil de Usuario completo
     - Top Grammys (Cachear datos)
     - Canciones Random: Precargar sugerencias basadas en el nivel e idioma del usuario
2. **Persistencia (Offline First):**
   - 🔄 Guardar Top Grammy y Perfil en Room DB para acceso sin conexión.
3. **Feedback Visual:**
   - 🔄 LoadingDialog con Lottie y efectos de confeti en logros.
   - 🔄 Indicador visual (estrella/badge) para palabras recomendadas por IA en la letra.

#### 🔵 Fase 4: Extras (Opcionales)
- Modo Karaoke Oculto (Easter Egg)
- Resiliencia IA (Groq/Llama 3 como fallback)
- Gamificación (Logros y gráficas)

---

## 📚 Diccionario Multinivel (Detalle Tarea)
- **Nivel 1 (Idiomas Activos):**
  - Carpetas visuales: 🇬🇧 Inglés, 🇫🇷 Francés, 🇩🇪 Alemán
  - Filtrado dinámico según lo que estudia el usuario
- **Nivel 2 (Tipo de Contenido):**
  - 📝 Palabras: Vocabulario suelto
  - 💬 Expresiones: Idioms, frases hechas (Phrasal verbs)
- **Nivel 3 (El Contenido):**
  - Lista final de tarjetas para repaso (Flashcards)

---

## 🛠️ Estado Técnico Actual (Stack)
- **Backend:** Python FastAPI + MongoDB Atlas + Gemini 2.0 Flash + Cloudinary
- **Tools:** yt-dlp, curl-cffi
- **Frontend:** Android Nativo (Kotlin) + XML Layouts
- **Libs:** Retrofit, Room, ExoPlayer, Coil (Imágenes), Coroutines
- **Infraestructura:** Render.com (Web Service Gratuito)

---

## 📞 Contacto y Equipo
- Edison Marimon Cook (@EdisonMarimonCook) - Owner
- Hugo - Backend/Arquitectura/AI
- Frontend Dev - UI/UX, XML, Navegación
- Repositorio: GitHub - AplicacionTraduccionCancion
- Rama actual: `feature/lyrics-translation` (Mergeada funcionalidades SongLearning)
- Próxima Rama: `feature/ui-navigation-update`
- Última actualización: 25 de Diciembre de 2025
