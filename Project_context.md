🎵 MusicTransIAtor - Contexto del Proyecto

Versión: 5.0 (Producto Final en Desarrollo) Fecha de Actualización: 23 de Diciembre de 2025 Estado: 🎯 MVP COMPLETO (v4.5) → Transición a Producto Final Distribución: 🏴‍☠️ Portal Propio (APK) (No Play Store - Sideloading)

📌 VISIÓN DEL PROYECTO

App Android 100% gratuita para aprender idiomas con música mediante análisis semántico de letras en tiempo real. Diferencial: Audio "Unleashed" (estilo Grayjay), Lyrics sincronizadas (LRCLIB) y Análisis IA gramatical. Público objetivo: Estudiantes universitarios (proyecto académico con potencial comercial).

🏗️ ARQUITECTURA DEL SISTEMA (ACTUALIZADA)

Fragmento de código

graph TD

User[Usuario Android] <--> App

subgraph "Cliente (Android Nativo - XML)"

App[App Kotlin]

Exo[ExoPlayer + Audio Híbrido]

Views[XML Layouts + Glide + uCrop]

Cache[Room DB + SharedPrefs]

TTS[Text-To-Speech]

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

📋 Roadmap y Tareas

🚨 ORDEN DE TAREAS (Roadmap v5.0)

🟢 FASE 1: INFRAESTRUCTURA (95% COMPLETADO)

Render.com (Backend):

✅ Configurado Web Service en Render (repo GitHub, rama feature/lyrics-translation).

✅ SOLUCIONADO: Sistema de Lyrics Híbrido implementado (Prioridad LRCLIB + Fallback a Genius con curl-cffi para saltar Cloudflare).

✅ Health check, variables de entorno (MONGO\_URI, GEMINI\_API\_KEY).

🔄 Pendiente (UX): Pantalla "Wake Up" (Splash Screen) para despertar al servidor gratuito de Render (Ping inicial).

Cloudinary:

✅ Integrado en backend (avatars persistentes).

✅ users.py actualizado, credenciales en .env.

IP Automática en RetrofitService:

✅ Detección automática de entorno (emulador, móvil, producción).

✅ APK universal, sin configuración manual de IP.

🔥 FASE 2: UX CORE & AUDIO (PRIORIDAD ACTUAL)

Sistema de Audio "Pirata" (Estrategia Híbrida):

🔄 Backend (Prototipo): Integración de yt-dlp en Python para validar lógica rápidamente.

⏳ Frontend (Producción): Integrar librería nativa (tipo NewPipeExtractor) en la App para evitar bloqueos de IP de Render.

⏳ OTA Updater: Sistema de auto-actualización de APK propio.

Interfaz y Navegación (Legacy XML):

🔄 Bottom Navigation: Implementar menú inferior (Inicio | Grammys | Perfil) reemplazando el actual.

✅ Fix Visual Lyrics: Añadido paddingBottom="150dp" en tvLyrics (XML) para evitar texto cortado.

⏳ Auditoría Visual: Revisar márgenes (layout\_margin) en buscadores y tarjetas.

Diccionario Multinivel:

🔄 Estructura Jerárquica: Nivel 1 (Idioma) → Nivel 2 (Tipo: Palabra/Expresión) → Nivel 3 (Lista).

🔄 Frontend: ExpandableListAdapter con banderas.

Flashcards Mejoradas:

🔄 Animación de volteo 3D.

🔄 Audio Dual: TTS (pronunciación) + Contexto Real (Audio de canción).

🔄 Fix: next\_review\_date = hoy.

🟡 FASE 3: OPTIMIZACIÓN (MEDIO PLAZO)

Persistencia (Room DB):

🔄 Guardar Top Grammy y Perfil en local (Offline First).

Optimización de Tiempos de Carga:

🔄 Precarga paralela en Splash Screen.

Feedback Visual:

🔄 LoadingDialog con Lottie, Ripple effects, Confetti en logros.

Indicador Visual isRecommended:

🔄 Estrella dorada/Badge para palabras recomendadas por IA.

🔵 FASE 4: EXTRAS (OPCIONALES)

Modo Karaoke Oculto (Easter Egg):

⏳ Reproductor puro con .lrc sincronizado y servicio en segundo plano.

Resiliencia IA:

🔄 Groq (Llama 3) como Fallback si Gemini cae.

Gamificación Local:

🔄 Logros y gráficas de progreso.

Soporte Pantalla Horizontal:

🔄 Layouts land para tablets.

📚 Diccionario Multinivel (Detalle Tarea 5)

Nivel 1 (Idiomas Activos):

Carpetas visuales: 🇬🇧 Inglés, 🇫🇷 Francés, 🇩🇪 Alemán.

Filtrado dinámico según lo que estudia el usuario.

Nivel 2 (Tipo de Contenido):

Sub-categorías claras dentro del idioma:

📝 Palabras (Vocabulario suelto).

💬 Expresiones (Idioms, frases hechas).

Nivel 3 (El Contenido):

Lista final de tarjetas flashcards.

🛠️ Estado Técnico Actual (Stack Tecnológico)

Backend: Python FastAPI + MongoDB Atlas + Gemini 2.0 Flash + Cloudinary.

Librerías clave: yt-dlp (Audio), curl-cffi (Genius Bypass).

Frontend: Android Nativo (Kotlin) + XML Layouts (Legacy).

Librerías clave: Retrofit (Red), Room (DB Local), ExoPlayer (Media), uCrop (Edición img), Glide (Carga img), Android TTS.

Infraestructura: Render.com (Web Service Gratuito).

📞 Contacto y Equipo

Edison Marimon Cook (@EdisonMarimonCook) - Propietario del repositorio.

Hugo - Desarrollador Backend/Arquitectura (Lyrics, Render, Audio).

Frontend Dev - UI/UX, XML Layouts, Navegación.

[Otros miembros] - Landing page y tareas adicionales.

Repositorio: GitHub - AplicacionTraduccionCancion

Ramas:

main - Versión estable (MVP 4.5).

feature/lyrics-translation - Desarrollo activo (Backend Render).

release/5.0 - Producto final (próximamente).

Última actualización: 23 de Diciembre de 2025
