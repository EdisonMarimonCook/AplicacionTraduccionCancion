---

# 🎵 MusicTransIAtor - Contexto del Proyecto

**Versión:** 5.3 (Audio Nativo Unleashed & Pedagogical Core)  
**Fecha de Actualización:** 31 de Diciembre de 2025  
**Estado:** 🌟 MVP COMPLETO + AUDIO LOCAL. Funcionalidad Core terminada (Auth, Anki, Backend, Audio). Foco Actual: Unificación de Navegación (BottomNav) y Pulido Visual.

---


## 📌 Estado Real del MVP

La aplicación es funcionalmente completa y ahora:

- **Modo Portrait Único:** Toda la app fuerza orientación vertical para simplificar la experiencia y evitar bugs de rotación.
- **Gestión de Usuarios:** Registro, Login, Cambio de Credenciales, Avatar (Cloudinary).
- **Aprendizaje:** Algoritmo de repetición espaciada (SRS - Anki), gestión de rachas, Flashcards automáticas.
- **Core Musical:** Buscador de canciones, Top Grammys, Bypass de letras (Cloudflare).
- **Audio Mejorado:**
   - Reproducción local sin límites usando un wrapper Android de yt-dlp (descarga y reproducción de audio real, no solo previews).
   - El sistema de previews de 30s sigue activo como fallback si falla el audio completo.
   - Carga paralela inteligente.
- **Render Uptime:** El backend Render está monitorizado y siempre activo gracias a UptimeRobot.
- **Flashcards Mejoradas:**
   - Swipe robusto y animado (mejor UX).
   - Barra de progreso visual añadida al sistema de flashcards.
- **UI/UX:**
   - Layouts adaptados a modo claro/oscuro usando colors.xml centralizado.
   - Botones y márgenes estandarizados.
   - Pantallas principales revisadas para accesibilidad y compatibilidad visual.
- **Cropper Integrado:**
   - El recorte de imagen de perfil usa CanHub Cropper con tema MaterialComponents para asegurar visibilidad de botones y coherencia visual.

## 📚 Notas Técnicas: Estrategia Pedagógica (Diccionario & Flashcards)


### Onboarding y Gestión Multilenguaje

Para minimizar la fricción y facilitar la experiencia de usuarios políglotas, se adopta la siguiente estrategia:

- **Registro (Onboarding):**
  - Solo se solicita usuario, email y contraseña. No se pregunta idioma ni nivel en el registro inicial para evitar abandono.

- **Configuración Inicial (First Run):**
  - El usuario selecciona manualmente los idiomas que quiere aprender.
  - Para cada idioma, elige su nivel mediante selectores adaptativos según el estándar correspondiente:
    - **Idiomas Europeos:** CEFR (A1 - C2)
    - **Japonés:** JLPT (N5 - N1)
    - **Chino:** HSK (1 - 6)
    - **Coreano:** TOPIK (1 - 6)
  - Cada selector irá acompañado de una breve descripción funcional de cada nivel (ej: "C2: Usuario experto", "N5: Principiante").
  - No se realiza test de nivel automático en el MVP.

- **Idioma Activo:**
  - Se define un idioma "principal" para la sesión, que determina qué canciones y flashcards se muestran en la Home.

- **Gestión de Idiomas en el Perfil:**
  - El usuario puede añadir nuevos idiomas y su nivel.
  - Puede cambiar el nivel de un idioma existente.
  - Puede eliminar idiomas de su lista (la racha específica de ese idioma se pierde, pero los contenidos -diccionario y flashcards- quedan ocultos y recuperables si vuelve a añadir el idioma).
  - Siempre se muestra la equivalencia de dificultad de los niveles (ej: "N1 es más difícil que N5", "C2 es más difícil que A1").

Esta arquitectura permite máxima flexibilidad y claridad para usuarios que estudian varios idiomas, y prepara el sistema para futuras ampliaciones de estándares y lógica de progresión.


### Arquitectura Técnica (Single Activity & Navegación)

- **Transición Multi-Activity → Single-Activity:**
   - En la Fase 2, la app pasará de una arquitectura basada en múltiples Activities a una arquitectura de **Single Activity** con **Fragmentos**.
   - Toda la navegación principal (Home, Explorar/Grammys, Perfil) se gestionará mediante un menú de **Bottom Navigation** y swipe lateral entre pantallas (estilo Duolingo).
   - Esto permite una experiencia más fluida, mejor manejo del back stack y facilita la integración de animaciones y transiciones modernas.

### Librería de Crop (Actualización)

- Se ha sustituido la librería **uCrop** por **Android-Image-Cropper (CanHub)** para el recorte de imagen de perfil, asegurando compatibilidad visual y visibilidad de botones con tema MaterialComponents.

### Arquitectura Drill-down del Diccionario & Flashcards

El sistema está diseñado para usuarios políglotas, priorizando la claridad mediante una navegación de lo general a lo particular:

- **Nivel 0 (Dashboard/Perfil):** Contador Global. Se muestra el total de palabras aprendidas en todos los idiomas (ej. "500 Palabras").
   - Interacción: Al pulsar este contador global, se despliega el desglose especializado (Nivel 1).
- **Nivel 1 (Selector de Idioma):** Filtrado visual. (ej. Pestañas o Banderas: 🇬🇧 EN (300) | 🇫🇷 FR (200)).
   - Lógica: Al entrar desde la navegación principal, se carga por defecto el idioma que el usuario está "estudiando activamente" (campo current_learning_language en BD).
- **Nivel 2 (Tipo):** Separación conceptual dentro del idioma seleccionado.
   - Palabras: Vocabulario suelto.
   - Expresiones (Idioms): Frases hechas o slang ("Break a leg", "Hustle").
- **Nivel 3 (Lista Final):** El RecyclerView con los elementos, permitiendo búsqueda y ordenación.

### Flashcards 2.0 (Experiencia de Repaso y Flow)

El repaso no es solo ver una tarjeta, es una experiencia audiovisual diseñada para la retención musical y la fluidez cognitiva.

- **Dual Audio System:**
   - **TTS (Text-to-Speech):** Al mostrar la tarjeta, suena la pronunciación limpia de la palabra aislada y esto ocurrirá mientras se busca el audio haciendo uso de los timestamps proporcionados por los datos guardados de las flashcards, pues hay que utilizar la versión de LRClib que te proporciona los timestamps para facilitar a yt-dlp (el de android) la tarea de encontrar el audio real de la canción. El TTS sonará hasta que se encuentre el audio de la canción, si no se encuentra se mantiene el tts.
   - **Clip de Contexto:** Un botón "Oír en canción" reproduce el fragmento de 5-10 segundos exactos donde aparece la palabra en la canción original (usando los timestamps guardados).

- **Estrategia de Repaso (General vs Específico):**
   - **Estadísticas Interactivas:** El contador de "Repasos Pendientes" en el perfil es la suma global. Al pulsarlo, el usuario ve el desglose y decide qué idioma atacar.
   - **Separación de Contexto (Language Flow):** Las sesiones de flashcards nunca mezclan idiomas. Se repasa un idioma por sesión para mantener el "flow" mental del usuario.
   - **Selector de Modo:**
      - Repaso General: El algoritmo SRS selecciona las palabras más urgentes del idioma activo.
      - Repaso Específico: El usuario filtra por categoría (ej. "Solo verbos", "Canciones de Eminem") para sesiones enfocadas.

- **Gestión de Racha (Global Streak):**
   - La racha es independiente del idioma. Completar una sesión de repaso en Inglés o en Francés cuenta igualmente para mantener el fuego encendido 🔥. Esto evita castigar al usuario por diversificar su aprendizaje.

- **Burnout Protection (Salud Mental):**
   - Visualización clara de "Cartas para hoy" vs "Total acumulado".
   - El sistema limita artificialmente los repasos nuevos si hay muchos pendientes, para evitar que el usuario se agobie y abandone.

### Tabla de Idiomas Soportados y Estándares

| Idioma      | Código | Sistema de Niveles | Estado Actual |
|-------------|--------|--------------------|--------------|
| Inglés      | en     | CEFR (A1-C2)       | ✅ Activo     |
| Español     | es     | CEFR (A1-C2)       | ✅ Activo     |
| Francés     | fr     | CEFR (A1-C2)       | ✅ Activo     |
| Alemán      | de     | CEFR (A1-C2)       | ✅ Activo     |
| Italiano    | it     | CEFR (A1-C2)       | ✅ Activo     |
| Portugués   | pt     | CEFR (A1-C2)       | ✅ Activo     |
| Japonés     | ja     | JLPT (N5-N1)       | 🔜 Planificado|
| Chino       | zh     | HSK (1-6)          | 🔜 Planificado|
| Coreano     | ko     | TOPIK (1-6)        | 🔜 Planificado|

**Nota:** Actualmente solo los idiomas europeos (CEFR) están activos. La arquitectura permite añadir JLPT, HSK y TOPIK fácilmente descomentando en `level_mapper.py` y ajustando los endpoints de registro/perfil.

### Fase 4: Health Check

- **Health Check:** Validación rápida de conexión al iniciar (Splash Screen) para despertar a Render.

---

## 📋 Roadmap Actualizado (v5.2)

### ✅ FASE 1: LOGROS CONSOLIDADOS (YA HECHO)

- **Infraestructura:** Backend Render + Mongo Atlas + Retrofit (con force-render para testing).
- **Audio Engine:** ✅ HITO: Audio local completo (4+ min) sustituyendo a las previews de 30s (se usan como fallback).
- **UX de Carga:** ✅ Gestión de espera con mensajes rotativos ("Afinando...", "Calentando voz...") y carga paralela (Letra + Audio + IA).
- **Interfaz Reproductor:** ✅ SeekBar funcional, Padding de letras corregido, Spinner reubicado.
- **Lógica de Estudio:** Sistema de repaso, guardado de palabras y resaltado IA.

### 🔥 FASE 2: NAVEGACIÓN Y PULIDO VISUAL (EN CURSO)


El backend y la lógica funcionan. Toca organizar las pantallas.

- **Arquitectura Single Activity:**
   - Migrar de Multi-Activity a Single-Activity con Fragmentos.
   - Implementar menú estándar: Bottom Navigation + swipe lateral entre pantallas (Home (empezamos aquí y estará en medio de las otras 2), Explorar/Grammys, Perfil).
   - El objetivo es una navegación fluida y moderna, similar a apps como Duolingo.

- **Gestión de Datos UI:**
   - Visualizar las listas de diccionarios/flashcards (ya existentes en BD) dentro de la nueva estructura de navegación (Fragmentos).

### 🟠 FASE 2.5: CAMBIOS DE REGISTRO Y GESTIÓN MULTILENGUAJE (PRIORIDAD)

- **Nuevo flujo de registro:** Registro minimalista (usuario, email, contraseña). La selección de idiomas y niveles se traslada a la primera configuración tras el login.
- **Gestión de idiomas en perfil:** El usuario puede añadir, eliminar o cambiar el nivel de los idiomas que estudia, con selectores adaptados a cada estándar (CEFR, JLPT, HSK, TOPIK) y descripciones claras de dificultad.
- **Ocultado/recuperación de contenidos:** Al eliminar un idioma, la racha específica se pierde pero los contenidos quedan ocultos y recuperables si el usuario lo vuelve a añadir.
- **Idioma activo:** El usuario puede definir el idioma principal de la sesión, que filtra la Home y el repaso.

> Estos cambios deben implementarse antes de abordar la refactorización de diccionario y flashcards, que se encuentran en las notas técnicas y que se realizarán al final de la fase 2.5 o al principio de la fase 3.

### 🟡 FASE 3: MANTENIMIENTO Y OPTIMIZACIÓN

- **Configuración Retrofit:** Automatizar el switch DEBUG/RELEASE para la URL de Render.
- **Wake-Up Screen:** Optimizar la carga inicial de datos (Top Grammys, Perfil) al abrir la app.
- **Modo Offline:** Persistencia local de datos críticos (Room) para cuando falle la red.

### 🔵 FASE 4: EXTRAS (OPCIONALES / "SI DA TIEMPO")

- **Modo Karaoke Oculto:** Easter Egg para tener reproducción de música como reproductor simplemente sin analisis, aprovechando el sincronizado de audio con letra de la libreria de usada para obtención de letras (LRCLIB).
- **Resiliencia IA:** Integración de Groq/Llama 3 como fallback si Gemini falla.
- **Gamificación Avanzada:** Logros visuales, gráficas de progreso y medallas.
- **Health Check:** Validación rápida de conexión al iniciar (Splash Screen) para despertar a Render.
---

## 📝 Notas para el Commit / Equipo

- **Aviso Importante:** Se ha modificado la configuración de red (RetrofitClient o NetworkModule) para apuntar forzosamente a Render. Esto es para que todos puedan probar el audio y la IA sin configurar el entorno local.
- **Cambio Crítico:** Se ha añadido lógica Python local (yt-dlp) en el cliente Android. La primera carga de una canción puede tardar unos segundos extra por la inicialización del entorno (necesario avisar a testers).
