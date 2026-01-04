---

# 🎵 MusicTransIAtor - Contexto del Proyecto

**Versión:** 5.5 (Bottom Nav Completed & Multilenguaje Foundation)  
**Fecha de Actualización:** 3 de Enero de 2026  
**Estado:** 🌟 MVP COMPLETO + AUDIO LOCAL + BOTTOM NAVIGATION. Funcionalidad Core terminada (Auth, Anki, Backend, Audio, Navegación Principal). Foco Actual: Gestión Multilenguaje y Perfil Inteligente (Fase 2.5).

---

## 📂 Estructura del Proyecto

### Backend (Python - FastAPI)

```
backend/
├── main.py                    # Punto de entrada de la API
├── auth.py                    # Sistema de autenticación JWT
├── cache.py                   # Gestión de caché en memoria
├── config.py                  # Configuración y variables de entorno
├── crud.py                    # Operaciones CRUD genéricas
├── database.py                # Conexión y configuración de BD
├── database_mock.py           # Mock de datos para testing
├── database_mongo.py          # Implementación MongoDB
├── models.py                  # Modelos de datos Pydantic
├── requirements.txt           # Dependencias Python
├── routers/                   # Endpoints organizados por dominio
│   ├── __init__.py
│   ├── ai_analysis.py         # Análisis IA de letras (Gemini)
│   ├── audio.py               # Extracción y procesamiento de audio
│   ├── auth.py                # Endpoints de autenticación
│   ├── dictionary.py          # Gestión de diccionario personal
│   ├── flashcards.py          # Sistema SRS y flashcards
│   ├── lyrics.py              # Obtención de letras (LRCLib/Genius)
│   ├── progress.py            # Seguimiento de progreso y rachas
│   ├── schemas.py             # Schemas de validación
│   ├── songs.py               # Búsqueda y gestión de canciones
│   └── users.py               # Gestión de usuarios y perfiles
├── services/                  # Lógica de negocio externa
│   ├── cloudinary_service.py  # Upload de avatares a Cloudinary
│   ├── email_service.py       # Envío de correos (verificación)
│   ├── gemini_client.py       # Cliente API de Gemini AI
│   └── level_mapper.py        # Mapeo de niveles (CEFR, JLPT, HSK, TOPIK)
└── utils/                     # Utilidades auxiliares
    ├── __init__.py
    ├── audio_extractor.py     # Wrapper de yt-dlp para audio
    ├── genius_client.py       # Cliente API de Genius
    ├── lyrics_fragmenter.py   # Fragmentación de letras por palabras
    └── spotify.py             # Cliente API de Spotify
```

### Frontend (Android - Kotlin)

```
frontend/android/app/src/main/
├── AndroidManifest.xml        # Configuración de la app y Activities
├── java/com/example/diccionario_hiphop/  # Código fuente Kotlin
│   ├── MainActivity.kt                    # Contenedor principal (BottomNav + ViewPager2)
│   ├── SplashActivity.kt                  # Pantalla de carga inicial
│   ├── LoginActivity.kt                   # Pantalla de login
│   ├── RegisterActivity.kt                # Pantalla de registro
│   ├── VerifyAccountActivity.kt           # Verificación de cuenta por email
│   ├── ForgotPasswordActivity.kt          # Recuperación de contraseña
│   ├── ResetPasswordActivity.kt           # Reseteo de contraseña
│   ├── EditProfileActivity.kt             # Edición de datos de perfil
│   ├── SettingsActivity.kt                # Configuración de la app
│   ├── SongLearningActivity.kt            # Pantalla de aprendizaje con letra
│   ├── DictionaryActivity.kt              # Vista de diccionario personal
│   ├── FlashcardsActivity.kt              # Sistema de flashcards con SRS
│   │
│   ├── HomeFragment.kt                    # Fragment de búsqueda (Descubrir)
│   ├── GrammysFragment.kt                 # Fragment de rankings/Top 50
│   ├── ProfileFragment.kt                 # Fragment de perfil de usuario
│   │
│   ├── ApiService.kt                      # Interfaz Retrofit
│   ├── RetrofitService.kt                 # Configuración de Retrofit
│   ├── AuthInterceptor.kt                 # Interceptor para JWT
│   ├── TokenManager.kt                    # Gestión de tokens (SharedPreferences)
│   ├── ConnectionTester.kt                # Verificación de conectividad
│   │
│   ├── SongAdapter.kt                     # Adapter para lista de canciones
│   ├── DictionaryAdapter.kt               # Adapter para diccionario
│   ├── SongRepository.kt                  # Repositorio de canciones
│   ├── DictionaryRepository.kt            # Repositorio de diccionario
│   ├── SongLearningViewModel.kt           # ViewModel para aprendizaje
│   │
│   ├── Song.kt                            # Modelo de canción
│   ├── HipHopTerm.kt                      # Modelo de término/palabra
│   ├── ApiModels.kt                       # Modelos de respuesta API
│   │
│   └── GrayjayAudioExtractor.kt           # Extractor de audio (yt-dlp wrapper)
│
└── res/                       # Recursos de la app
    ├── layout/                # Layouts XML
    │   ├── activity_main.xml              # BottomNav + ViewPager2
    │   ├── activity_splash.xml            # Splash screen
    │   ├── activity_login.xml             # Pantalla de login
    │   ├── activity_register.xml          # Pantalla de registro
    │   ├── activity_verify_account.xml    # Verificación
    │   ├── activity_forgot_password.xml   # Olvido de contraseña
    │   ├── activity_reset_password.xml    # Reset de contraseña
    │   ├── activity_edit_profile.xml      # Edición de perfil
    │   ├── activity_settings.xml          # Configuración
    │   ├── activity_song_learning.xml     # Aprendizaje con letra
    │   ├── activity_flashcards.xml        # Flashcards
    │   │
    │   ├── fragment_home.xml              # Búsqueda con SearchView
    │   ├── fragment_profile.xml           # Perfil con stats
    │   │
    │   ├── fragment_dictionary.xml        # Lista de palabras
    │   ├── item_song.xml                  # Item de canción
    │   ├── item_song_placeholder.xml      # Skeleton loader
    │   ├── item_word.xml                  # Item de palabra
    │   └── dialog_add_word.xml            # Dialog para añadir palabra
    │
    ├── drawable/              # Recursos gráficos y shapes
    │   ├── bg_gradient_main.xml           # Gradiente principal
    │   ├── bg_search_rounded.xml          # Fondo del SearchView
    │   ├── album_cover_background.xml     # Placeholder de imágenes
    │   ├── circle_background.xml          # Fondos circulares
    │   ├── login_background.xml           # Fondo de login
    │   ├── ic_*.xml                       # Iconos vectoriales
    │   └── logo_*.png                     # Logos de la app
    │
    ├── values/                # Valores (tema claro)
    │   ├── colors.xml         # Paleta de colores modo claro
    │   ├── strings.xml        # Textos de la app
    │   ├── themes.xml         # Tema principal DayNight
    │   └── styles.xml         # Estilos personalizados
    │
    ├── values-night/          # Valores (tema oscuro)
    │   └── colors.xml         # Paleta de colores modo oscuro
    │
    └── menu/
        └── bottom_nav_menu.xml            # Menú del BottomNav
```

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


### Onboarding y Gestión Multilenguaje (Actualizado - Fase 2.5)

**Sistema Multi-idioma desde el Registro:**

- **Registro (Onboarding Mejorado):**
  - El usuario proporciona: **Usuario, Email y Contraseña**.
  - **Selector Multi-idioma (NUEVO):** Durante el registro, el usuario selecciona uno o varios idiomas de interés mediante checkboxes:
    - **Idiomas disponibles:** Inglés, Español, Francés, Alemán, Italiano, Portugués (CEFR), Japonés (JLPT), Chino (HSK), Coreano (TOPIK).
  - **Nivel por Idioma:** Para cada idioma seleccionado, el usuario elige su nivel estimado mediante selectores adaptativos:
    - **CEFR (Europeos):** A1, A2, B1, B2, C1, C2
    - **JLPT (Japonés):** N5, N4, N3, N2, N1 (N5 = principiante, N1 = avanzado)
    - **HSK (Chino):** 1, 2, 3, 4, 5, 6 (1 = principiante, 6 = avanzado)
    - **TOPIK (Coreano):** 1, 2, 3, 4, 5, 6
  - **Idioma Principal (Primary Language):** El primer idioma seleccionado (o el marcado como favorito) se establece como `primary_language`, que aparece en el perfil (ej: "EN • C1").

- **Perfil Inteligente (Visualización Jerárquica):**
  - **Estado Normal:** Muestra solo el idioma principal y contadores globales:
    - Ejemplo: "EN • C1" | "120 Palabras" | "🔥 5 Racha" | "30 Repasos"
  - **Interacción:** Al pulsar sobre el texto del idioma ("EN • C1") o los contadores numéricos, se abre un **BottomSheet** con el desglose completo:
    - **Idiomas:** 🇬🇧 Inglés (C1) - 100 palabras | 🇫🇷 Francés (A2) - 20 palabras
    - **Total Palabras:** 120 (suma de todos los idiomas)
    - **Repasos:** Desglosados por idioma
  - **Sección "Modificar Datos":**
    - Desde el perfil, el usuario puede:
      - **Añadir nuevo idioma:** Selecciona idioma y nivel, se añade a su lista.
      - **Eliminar idioma:** Marca el idioma como `archived` o `hidden` (ver "Soft Delete" abajo).
      - **Cambiar nivel:** Actualiza el nivel de un idioma existente.
      - **Reordenar prioridad:** Cambiar cuál es el idioma principal.

- **Gestión de Idioma Activo:**
  - El usuario puede cambiar el idioma "activo" desde el perfil, lo que determina:
    - Qué idioma se muestra en la Home (canciones sugeridas filtradas por idioma).
    - Qué flashcards aparecen en la sesión de repaso.

- **Soft Delete (No romper nada):**
  - **Eliminación de Idioma:** Si el usuario borra un idioma (ej: Francés):
    - **NO SE BORRAN** las palabras ni las flashcards de la base de datos.
    - **ACCIÓN:** Se marca el idioma como `archived: true` o `hidden: true` en el perfil del usuario.
    - **EFECTO:**
      - Las palabras desaparecen del diccionario (no se muestran en la lista).
      - Las flashcards pausan su algoritmo SRS (se "congelan", `next_review_date` se ignora).
      - La racha específica de ese idioma se pierde.
    - **RECUPERACIÓN:** Si el usuario vuelve a añadir Francés en el futuro:
      - Se reactiva el idioma (`archived: false`).
      - Todas las palabras y flashcards reaparecen exactamente donde las dejó.
      - El progreso SRS se reanuda desde el último estado guardado.

Esta arquitectura permite máxima flexibilidad y claridad para usuarios que estudian varios idiomas, y prepara el sistema para futuras ampliaciones de estándares y lógica de progresión.


### Arquitectura Técnica (Navegación Híbrida - ACTUALIZADO)

- **Bottom Navigation + Fragments (Fase 2 - COMPLETADO):**
   - La app usa **MainActivity** como contenedor único con 3 fragmentos principales accesibles vía Bottom Navigation:
     - **GrammysFragment:** Rankings y Top 50 canciones.
     - **HomeFragment (Descubrir):** Búsqueda de canciones con sugerencias en tiempo real.
     - **ProfileFragment:** Perfil del usuario con estadísticas y acciones.
   - **Swipe lateral:** ViewPager2 permite deslizar entre los 3 fragmentos.
   - **Activities Secundarias:** Las pantallas de flujo menos frecuente (Login, Register, SongLearning, Dictionary, Flashcards, Settings, etc.) se mantienen como Activities separadas.
   - **NO es Full Single Activity:** La arquitectura es híbrida para optimizar la experiencia sin complicar innecesariamente el back stack.

### Librería de Crop (Actualización)

- Se ha sustituido la librería **uCrop** por **Android-Image-Cropper (CanHub)** para el recorte de imagen de perfil, asegurando compatibilidad visual y visibilidad de botones con tema MaterialComponents.

### Arquitectura Drill-down del Diccionario & Flashcards (Actualizado - Fase 2.5)

El sistema está diseñado para usuarios políglotas, priorizando la claridad mediante una navegación de lo general a lo particular:

- **Nivel 0 (Dashboard/Perfil):** Contador Global. Se muestra el total de palabras aprendidas en todos los idiomas (ej. "120 Palabras").
   - **Interacción:** Al pulsar este contador global, se despliega el desglose especializado en un **BottomSheet** (Nivel 1).
   
- **Nivel 1 (BottomSheet - Desglose por Idioma):** Visualización detallada en panel deslizante inferior:
   - **Idiomas con Banderas:** 🇬🇧 Inglés (C1) - 100 palabras | 🇫🇷 Francés (A2) - 20 palabras
   - **Estadísticas:** Total de palabras, repasos pendientes por idioma, racha actual.
   - **Acciones:** Botón para cambiar idioma activo, acceder a gestión de idiomas.
   
- **Nivel 2 (Diccionario con Filtros - NUEVO):** Al abrir el diccionario desde el perfil:
   - **Chips de Filtrado (Material Chips):** Píldoras en la parte superior para filtrar por idioma:
     - `[Todos]` `[🇬🇧 Inglés]` `[🇫🇷 Francés]`
   - **Lógica:** Al pulsar un chip, la lista se filtra dinámicamente sin recargar la Activity.
   - **Por defecto:** Se carga el idioma activo del usuario (`primary_language`).
   
- **Nivel 3 (Tipo - Separación Conceptual):** Dentro del idioma seleccionado:
   - **Palabras:** Vocabulario suelto (sustantivos, verbos, adjetivos).
   - **Expresiones (Idioms):** Frases hechas o slang ("Break a leg", "Hustle", "On fleek").
   
- **Nivel 4 (Lista Final):** RecyclerView con las entradas, permitiendo:
   - **Búsqueda:** SearchView para encontrar palabras específicas.
   - **Ordenación:** Por fecha de adición, alfabético, frecuencia de repaso.

### Flashcards 2.0 (Experiencia de Repaso y Flow - Actualizado)

El repaso no es solo ver una tarjeta, es una experiencia audiovisual diseñada para la retención musical y la fluidez cognitiva.

- **Dual Audio System:**
   - **TTS (Text-to-Speech):** Al mostrar la tarjeta, suena la pronunciación limpia de la palabra aislada. Se reproduce mientras se busca el audio de la canción (usando timestamps de LRCLib).
   - **Clip de Contexto:** Botón "🎵 Oír en canción" reproduce el fragmento de 5-10 segundos exactos donde aparece la palabra en la canción original (usando los timestamps guardados y yt-dlp para extraer el audio).
   - **Fallback:** Si no se encuentra el audio de la canción, se mantiene solo el TTS.

- **Estrategia de Repaso (General vs Específico):**
   - **Estadísticas Interactivas:** El contador de "Repasos Pendientes" en el perfil es la suma global. Al pulsarlo, se abre el BottomSheet con el desglose por idioma.
   - **Separación de Contexto (Language Flow):** Las sesiones de flashcards **nunca mezclan idiomas**. Se repasa un idioma por sesión para mantener el "flow" mental del usuario.
   - **Selector de Modo:**
      - **Repaso General:** El algoritmo SRS selecciona las palabras más urgentes del idioma activo.
      - **Repaso Específico:** El usuario filtra por categoría (ej. "Solo verbos", "Canciones de Eminem") para sesiones enfocadas.
   - **Tipos de Repaso (NUEVO):**
      - **Normal:** Palabra → Definición (ej: "Hustle" → "Trabajar duro, esforzarse").
      - **Inverso:** Definición → Palabra (ej: "Trabajar duro" → "Hustle").
      - **Listening:** Audio de la palabra (TTS) → Escribir la palabra.

- **Gestión de Racha (Global Streak):**
   - La racha es **independiente del idioma**. Completar una sesión de repaso en Inglés o en Francés cuenta igualmente para mantener el fuego encendido 🔥.
   - Esto evita castigar al usuario por diversificar su aprendizaje.

- **Burnout Protection (Salud Mental - NUEVO):**
   - **Visualización clara:** "15 Cartas para hoy" vs "45 Total acumulado".
   - **Límite de Nuevas Palabras:** Si `repasos_pendientes > X` (ej: 50), el sistema bloquea el aprendizaje de palabras nuevas hasta limpiar la cola.
   - **Mensajes motivacionales:** "¡Casi terminas! Solo 5 más para hoy 💪" o "Tómate un descanso, vuelve mañana 😊".
   - **Control de Ritmo:** El usuario puede ajustar cuántas palabras nuevas quiere ver por día (5, 10, 15, 20).

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

---

## 📋 Roadmap Actualizado (v5.5)

### ✅ FASE 1: LOGROS CONSOLIDADOS (COMPLETADA)

- **Infraestructura:** Backend Render + Mongo Atlas + Retrofit (con force-render para testing).
- **Audio Engine:** ✅ HITO: Audio local completo (4+ min) sustituyendo a las previews de 30s (se usan como fallback).
- **UX de Carga:** ✅ Gestión de espera con mensajes rotativos ("Afinando...", "Calentando voz...") y carga paralela (Letra + Audio + IA).
- **Interfaz Reproductor:** ✅ SeekBar funcional, Padding de letras corregido, Spinner reubicado.
- **Lógica de Estudio:** Sistema de repaso, guardado de palabras y resaltado IA.

### ✅ FASE 2: NAVEGACIÓN Y PULIDO VISUAL (COMPLETADA)

**Fecha de Finalización:** 1 de Enero de 2026

El backend y la lógica funcionan. Todas las pantallas principales están organizadas y pulidas.

**✅ Lo que está terminado:**

- **Arquitectura de Navegación:**
   - ✅ Implementada **MainActivity** como contenedor único (Single Activity híbrida).
   - ✅ **BottomNavigationView** operativa con 3 secciones accesibles vía swipe (ViewPager2):
     - **GrammysFragment:** Top canciones y rankings (Explorar).
     - **HomeFragment:** Búsqueda de canciones con sugerencias en tiempo real (Descubrir).
     - **ProfileFragment:** Perfil de usuario con estadísticas, avatar y configuración.
   - ✅ Fragmentos **HomeFragment** y **GrammysFragment** creados y enlazados.
   - ✅ Arquitectura híbrida: Bottom Nav con 3 Fragments principales, Activities secundarias para flujos específicos (Login, Register, SongLearning, Flashcards, Dictionary, EditProfile, Settings).

- **Interfaz de Usuario (UI/UX):**
   - ✅ **Perfil:** Diseño final implementado con:
     - Avatar circular (CircleImageView) con carga desde Cloudinary.
     - Nombre de usuario (`tvUsername`).
     - Info de idioma principal (ej: "EN • C1").
     - **Stats Card:** Contadores de Palabras, Racha 🔥, Repasos.
     - **Botonera de acciones:** Modificar Datos, Mi Diccionario, Practicar Flashcards, Cerrar Sesión.
   - ✅ **Feedback Visual:** Shimmer skeletons (9 placeholders) para evitar pantallas en blanco durante cargas.
   - ✅ **Imágenes optimizadas:** Integración de Glide con:
     - CrossFade transitions para transiciones suaves.
     - RoundedCorners (16dp) automáticas en portadas de canciones.
     - Placeholder y error handling con `album_cover_background`.

- **Búsqueda en Tiempo Real (HomeFragment):**
   - ✅ Sistema de autosugerencias mientras el usuario escribe (debounce de 300ms).
   - ✅ Shimmer visible inmediatamente al empezar a escribir.
   - ✅ SearchView configurado sin popup de sugerencias del teclado (`inputType="textNoSuggestions"`).
   - ✅ `windowSoftInputMode="adjustPan"` para evitar compresión de pantalla con teclado.

- **Theme Switching con State Preservation:**
   - ✅ Sistema de colores centralizado (colors.xml / colors-night.xml).
   - ✅ Atributos adaptativos (`?attr/colorOnSurface`, `?attr/colorSurface`, `@color/card_background`).
   - ✅ MainActivity guarda y restaura el estado (página del ViewPager) al cambiar tema.
   - ✅ Ripple effect eliminado del Bottom Navigation (`itemRippleColor="@android:color/transparent"`).

- **Funcionalidad Base:**
   - ✅ Simulación de búsqueda "Viral 50 Global" en Home al iniciar.
   - ✅ Acceso a **DictionaryActivity** y **FlashcardsActivity** mantenido desde el Perfil (como herramientas externas).
   - ✅ Gestión de sesión con TokenManager y logout funcional.

### ✅ FASE 2.5: GESTIÓN MULTILENGUAJE Y DESCUBRIMIENTO INTELIGENTE (COMPLETADA)

**Fecha de Inicio:** 3 de Enero de 2026  
**Fecha de Finalización:** 4 de Enero de 2026  
**Objetivo:** Personalizar la experiencia de aprendizaje según los idiomas y niveles del usuario, con recomendaciones adaptativas y control de burnout.

#### ✅ E. Recomendaciones Inteligentes y Grammys

**🎯 Objetivo:** Personalizar las recomendaciones según el idioma activo del usuario y su nivel de competencia.

- **HomeFragment (Descubrir) - Recomendaciones Adaptadas al Nivel:**
  - ✅ **ChipGroup dinámico:** Material Chips horizontales con todos los idiomas activos del usuario.
  - ✅ **Formato de chips:** Emoji + Nombre + Nivel (ej: 🇪🇸 Español (B2), 🇫🇷 Francés (A1)).
  - ✅ **Búsquedas adaptadas al nivel:**
    - **A1/A2 (Principiante):** Queries simplificadas como "canciones fáciles español", "chansons simples français".
    - **B1/B2 (Intermedio):** Tops populares como "Top 50 Spain", "Top France".
    - **C1/C2 (Avanzado):** Contenido avanzado con marcador "advanced".
  - ✅ **Idiomas soportados:** 🇬🇧 Inglés, 🇪🇸 Español, 🇫🇷 Francés, 🇩🇪 Alemán, 🇵🇹 Portugués, 🇮🇹 Italiano, 🇯🇵 Japonés, 🇰🇷 Coreano, 🇨🇳 Chino.
  - ✅ **Primary language por defecto:** El chip del idioma principal se selecciona automáticamente al cargar.
  - ✅ **Cambio dinámico:** Al hacer clic en un chip, recarga recomendaciones adaptadas al nuevo idioma y nivel.
  - ✅ **UX inteligente:**
    - **Chips visibles:** Durante recomendaciones automáticas (modo "Descubrir").
    - **Chips ocultos:** Durante búsqueda manual del usuario (se muestran de nuevo al vaciar SearchView o cerrar búsqueda).
  - ✅ **Header contextual:**
    - "🎧 Descubrir" para recomendaciones automáticas.
    - "Resultados para 'query'" para búsquedas manuales.

- **GrammysFragment - Selector de Idioma:**
  - ✅ **ChipGroup dinámico:** Chips con todos los idiomas activos del usuario.
  - ✅ **Formato de chips:** Emoji + Nombre (ej: 🇪🇸 Español, 🇬🇧 Inglés).
  - ✅ **Listas curadas por idioma:** Backend con `GRAMMY_SEARCHES` en `spotify.py`:
    - 🇬🇧 Inglés: Billie Eilish, Kendrick Lamar, Chappell Roan, Sabrina Carpenter, Taylor Swift, Beyoncé.
    - 🇪🇸 Español: Latin Grammy 2024, Bad Bunny, Karol G, Rosalía.
    - 🇫🇷 Francés: Stromae, Indila, Aya Nakamura.
    - 🇩🇪 Alemán: Apache 207, Top Germany.
    - 🇵🇹 Portugués: Anitta, Top Brasil.
    - 🇮🇹 Italiano: Måneskin, Top Italy.
    - 🇯🇵 Japonés: J-Pop Hits.
  - ✅ **Primary language seleccionado:** El idioma principal del usuario se marca por defecto.
  - ✅ **Cambio dinámico:** Al hacer clic en un chip, recarga los Grammys/Hits del nuevo idioma.

- **Backend:**
  - ✅ **Endpoint existente:** `GET /api/v1/songs/top-grammy?lang={code}` acepta parámetro de idioma.
  - ✅ **Lógica de búsqueda:** `get_grammy_songs(lang)` en `utils/spotify.py` retorna mezcla de 3 canciones por artista del idioma seleccionado.
  - ✅ **Eliminación de duplicados:** Filtra canciones repetidas por ID de Spotify.

#### ✅ D. Flashcards Multi-idioma con Anti-Burnout

**🎯 Objetivo:** Filtrado por idioma/tipo y prevención de agotamiento en el aprendizaje.

- **Selector de Idioma en ProfileFragment:**
  - ✅ **BottomSheet al pulsar "Practicar Flashcards":** Muestra lista de idiomas activos con estadísticas de repasos pendientes.
  - ✅ **Formato:** Emoji + Nombre + Contador (ej: 🇪🇸 Español - 15 repasos).
  - ✅ **Profile reload:** Antes de mostrar el BottomSheet, recarga el perfil del usuario para obtener contadores actualizados.
  - ✅ **Intent con parámetros:** Al seleccionar idioma, abre FlashcardsActivity pasando el código de idioma.

- **FlashcardsActivity - Filtrado por Tipo:**
  - ✅ **ChipGroup con 3 opciones:** Global (todas), Palabras (word), Expresiones (expression).
  - ✅ **Filtrado dinámico:** Al cambiar de chip, recarga flashcards usando `getDueFlashcards(language, type)`.
  - ✅ **Backend:** Endpoint `GET /api/v1/flashcards/due?language={lang}&type={type}`.
  - ✅ **Colores y diseño:** Usa `chip_background_selector` y `chip_text_selector` para estados checked/unchecked.

- **Control de Burnout (Anti-Agobio):**
  - ✅ **Límite de burnout:** Si el usuario tiene >50 repasos pendientes, el backend retorna HTTP 400.
  - ✅ **Dialog bloqueante:** SongLearningActivity muestra AlertDialog que impide añadir más palabras:
    - Título: "🧠 Demasiados repasos pendientes"
    - Mensaje: "Tienes [X] flashcards esperando. Completa algunos repasos antes de añadir más palabras."
    - Acción: Cierra la actividad y devuelve RESULT_CANCELED.
  - ✅ **Daily goal warning:** Si se alcanza la meta diaria (ej: 10 palabras), muestra AlertDialog informativo (no bloquea):
    - Título: "🎯 Meta diaria alcanzada"
    - Mensaje: "Has alcanzado tu meta de 10 palabras hoy. ¿Seguro que quieres continuar?"
    - Opciones: "Continuar" o "Volver".
  - ✅ **Backend response field:** `warning` en `DictionaryEntryResponse` para comunicar advertencias de daily_goal.

- **Fixes Críticos Implementados:**
  - ✅ **Collection name:** Corregido `flashcards_srs` → `flashcard_srs` (singular) en `users.py`.
  - ✅ **Timezone-aware comparison:** Uso de `datetime.now(timezone.utc)` para comparar `next_review_date`.
  - ✅ **Chip text visibility:** Cambiado `text_secondary` → `text_primary` en `chip_text_selector.xml` para mejor visibilidad.
  - ✅ **Real-time counters:** Profile reload antes de mostrar BottomSheet garantiza contadores frescos.

#### ✅ Mejoras en Búsqueda de Letras

**🎯 Objetivo:** Mejorar robustez y tasa de éxito en la búsqueda de letras de canciones.

- **LRCLib Optimizations:**
  - ✅ **Timeout aumentado:** 5s → 10s para conexiones lentas.
  - ✅ **Retry aumentado:** 2 → 3 intentos con exponential backoff (max 3s).
  - ✅ **3 variaciones de búsqueda:**
    1. Original: título y artista sin modificar.
    2. Normalizada: `normalize_search_query()` elimina caracteres especiales.
    3. Primer artista: `artist.split(',')[0].split('&')[0]` para manejar colaboraciones (ej: "Bowling For Soup & Punk Rock Factory" → "Bowling For Soup").
  - ✅ **Logs mejorados:** Contador de variaciones `[LRCLIB 1/3]`, `[LRCLIB 2/3]`, `[LRCLIB 3/3]`.

- **Genius API Optimizations:**
  - ✅ **Timeout aumentado:** 5s → 10s en ambas funciones (`get_genius_metadata`, `get_lyrics_genius_advanced`).
  - ✅ **Scraping con curl_cffi:** Impersonación de Chrome 120 para bypassear Cloudflare.
  - ✅ **Timeout de scraping:** 10s para descarga HTML.

- **Estrategia de Fallback:**
  1. **LRCLib** (prioridad alta): API pública sin autenticación.
  2. **Genius API Metadata** (fallback): Obtiene título/artista oficiales.
  3. **Genius Scraping** (último recurso): Extrae HTML con curl_cffi si API no tiene letras.

---

#### ✅ A. Registro Multi-idioma con Verificación Email (COMPLETADO)

**🎯 Objetivo:** Capturar los idiomas de interés y niveles del usuario desde el primer momento, con verificación de email mediante código PIN.

- **Selector Multi-idioma en Registro:**
  - ✅ **RegisterActivity completo:** Interfaz con checkboxes para seleccionar múltiples idiomas de aprendizaje.
  - ✅ **Idiomas soportados:** 
    - **CEFR (Europeos):** 🇬🇧 Inglés, 🇪🇸 Español, 🇫🇷 Francés, 🇩🇪 Alemán, 🇮🇹 Italiano, 🇵🇹 Portugués (niveles A1-C2).
    - **JLPT (Japonés):** 🇯🇵 (niveles N5-N1, N5 = principiante).
    - **HSK (Chino):** 🇨🇳 (niveles 1-6).
    - **TOPIK (Coreano):** 🇰🇷 (niveles 1-6).
  - ✅ **Spinners de nivel dinámicos:** Para cada idioma seleccionado, aparece un Spinner con el sistema de niveles correspondiente (CEFR/JLPT/HSK/TOPIK).
  - ✅ **Idioma nativo:** Spinner separado para seleccionar idioma nativo (pre-seleccionado: Español).
  - ✅ **Validación inteligente:** No permite seleccionar el idioma nativo como idioma de aprendizaje (la lista de checkboxes se actualiza dinámicamente).
  - ✅ **Primary language:** El primer idioma de la lista se marca automáticamente como `primary_language` en el backend.
  - ✅ **Niveles por defecto:** A1 (CEFR), N5 (JLPT), 1 (HSK/TOPIK) al marcar un checkbox.

- **Backend:**
  - ✅ **Endpoint `POST /api/v1/auth/register`:** Acepta `UserCreate` con array de `learning_languages`.
  - ✅ **Modelo `UserCreate`:** Campo `learning_languages: List[LearningLanguage]` con validación.
  - ✅ **Modelo `LearningLanguage`:** Campos `language`, `level`, `started_at`, `daily_goal`, `reviews_pending`, `is_active`, `words_learned`.
  - ✅ **Procesamiento de idiomas:** El backend procesa la lista y añade campos FASE 2.5 (daily_goal=10, is_active=True, etc.).
  - ✅ **Primary language detection:** El primer idioma de la lista se asigna a `user.primary_language`.
  - ✅ **Generación de PIN:** Código de 4 dígitos aleatorio para verificación email.
  - ✅ **Integración Brevo:** Envío de email de verificación mediante `send_verification_code()` (HTTP API de Brevo).
  - ✅ **Razón del cambio a Brevo:** El envío SMTP directo se bloqueaba en Render (port 587 blocked), se migró a Brevo HTTP API.

- **Verificación de Email:**
  - ✅ **VerifyAccountActivity:** Pantalla para introducir el código PIN de 4 dígitos.
  - ✅ **Endpoint `POST /api/v1/auth/verify`:** Valida el código y marca `is_verified=True`.
  - ✅ **Login bloqueado:** Si el usuario no está verificado, el login retorna HTTP 403 con mensaje "Email no verificado".
  - ✅ **Logs detallados:** Backend registra envío de emails y verificaciones exitosas.

---

#### ❌ B. Perfil Inteligente & Gestión de Idiomas (PENDIENTE PARA FASE 3)

---

### 🟠 FASE 3: MANTENIMIENTO Y OPTIMIZACIÓN (PENDIENTE)
   - SearchView configurado sin popup de sugerencias del teclado

- **Theme Switching con State Preservation:** ✅ COMPLETADO
   - Sistema de colores centralizado (colors.xml / colors-night.xml)
   - Atributos adaptativos (?attr/colorOnSurface, ?attr/colorSurface, @color/card_background)
   - MainActivity guarda y restaura el estado (página del ViewPager) al cambiar tema
   - `windowSoftInputMode="adjustPan"` para evitar compresión de pantalla con teclado

- **UI/UX Polish:** ✅ COMPLETADO
   - Colores adaptativos en ProfileFragment para visibilidad en modo claro y oscuro
   - Ripple effect eliminado del Bottom Navigation (`itemRippleColor="@android:color/transparent"`)
   - Padding y márgenes ajustados para mejor uso del espacio (fitsSystemWindows, paddingTop 24dp)
   - Contadores de estadísticas con colores temáticos (Palabras: adaptativo, Racha: naranja, Repasos: teal)

- **Gestión de Datos UI:**
   - Visualizar las listas de diccionarios/flashcards (ya existentes en BD) dentro de la nueva estructura de navegación (Fragmentos).

### � FASE 3: MANTENIMIENTO Y OPTIMIZACIÓN (PENDIENTE)

- **Configuración Retrofit:** Automatizar el switch DEBUG/RELEASE para la URL de Render.
- **Wake-Up Screen:** Optimizar la carga inicial de datos (Top Grammys, Perfil) al abrir la app.
- **Modo Offline:** Persistencia local de datos críticos (Room) para cuando falle la red.
- **Health Check:** Validación rápida de conexión al iniciar (Splash Screen) para despertar a Render.

### 🔵 FASE 4: EXTRAS (OPCIONALES / "SI DA TIEMPO")

- **Modo Karaoke Oculto:** Easter Egg para tener reproducción de música como reproductor simplemente sin análisis, aprovechando el sincronizado de audio con letra de la librería usada para obtención de letras (LRCLib).
- **Resiliencia IA:** Integración de Groq/Llama 3 como fallback si Gemini falla.
- **Gamificación Avanzada:** Logros visuales, gráficas de progreso y medallas.
- **Estadísticas de Uso:** Dashboard con gráficos de tiempo de estudio, palabras aprendidas por semana, etc.

---

## 📝 Notas para el Commit / Equipo

- **Aviso Importante:** Se ha modificado la configuración de red (RetrofitClient o NetworkModule) para apuntar forzosamente a Render. Esto es para que todos puedan probar el audio y la IA sin configurar el entorno local.
- **Cambio Crítico:** Se ha añadido lógica Python local (yt-dlp) en el cliente Android. La primera carga de una canción puede tardar unos segundos extra por la inicialización del entorno (necesario avisar a testers).
- **Fase 2.5 Activa:** A partir del 3 de Enero de 2026, el foco está en implementar gestión multilenguaje. Revisar este documento antes de cualquier cambio en autenticación o perfil de usuario.
