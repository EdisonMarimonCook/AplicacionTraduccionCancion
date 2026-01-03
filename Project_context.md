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

### 🔥 FASE 2.5: GESTIÓN MULTILENGUAJE Y PERFIL INTELIGENTE (EN CURSO)

**Fecha de Inicio:** 3 de Enero de 2026  
**Objetivo:** Permitir a los usuarios estudiar múltiples idiomas simultáneamente con gestión inteligente desde el registro hasta el perfil.

#### A. Registro y Onboarding Mejorado ("El Primer Contacto")

**🎯 Objetivo:** Capturar los idiomas de interés y niveles del usuario desde el primer momento.

- **Selector Multi-idioma en Registro:**
  - ❌ **Pendiente:** Añadir checkboxes en la pantalla de registro para seleccionar idiomas de interés:
    - **Idiomas disponibles:** Inglés, Español, Francés, Alemán, Italiano, Portugués (CEFR).
    - **Futuros:** Japonés (JLPT), Chino (HSK), Coreano (TOPIK).
  - ❌ **Pendiente:** Para cada idioma seleccionado, añadir selector de nivel:
    - **CEFR (Europeos):** Spinner con opciones A1, A2, B1, B2, C1, C2 + descripción breve.
    - **JLPT (Japonés):** Spinner con N5, N4, N3, N2, N1 (N5 = principiante, N1 = avanzado).
    - **HSK (Chino):** Spinner con 1, 2, 3, 4, 5, 6.
    - **TOPIK (Coreano):** Spinner con 1, 2, 3, 4, 5, 6.
  - ❌ **Pendiente:** Implementar lógica para marcar el primer idioma (o favorito) como `primary_language`.

- **Backend:**
  - ❌ **Pendiente:** Modificar endpoint `POST /auth/register` para aceptar array de idiomas:
    ```json
    {
      "username": "Hchurches04",
      "email": "user@example.com",
      "password": "password123",
      "languages": [
        {"code": "en", "level": "C1", "is_primary": true},
        {"code": "fr", "level": "A2", "is_primary": false}
      ]
    }
    ```
  - ❌ **Pendiente:** Actualizar modelo de usuario en `models.py` para incluir campo `languages` (array con idiomas y niveles).

#### B. Perfil Inteligente & UX de Detalles

**🎯 Objetivo:** Mostrar información jerarquizada y permitir gestión fácil de idiomas.

- **Visualización Jerárquica (BottomSheet):**
  - ❌ **Pendiente:** Implementar **BottomSheetDialogFragment** que se abre al pulsar:
    - El texto del idioma principal ("EN • C1").
    - Los contadores numéricos (Palabras, Racha, Repasos).
  - ❌ **Pendiente:** Diseño del BottomSheet:
    - **Título:** "Tus Idiomas" o "Desglose de Vocabulario".
    - **Contenido:** RecyclerView con lista de idiomas:
      - Ejemplo: 🇬🇧 **Inglés (C1)** - 100 palabras - 20 repasos pendientes
      - Ejemplo: 🇫🇷 **Francés (A2)** - 20 palabras - 5 repasos pendientes
    - **Total Global:** Suma de palabras y repasos de todos los idiomas.
    - **Acción:** Botón "Cambiar idioma activo" para alternar el `primary_language`.

- **Sección "Modificar Datos" (ProfileFragment):**
  - ❌ **Pendiente:** Al pulsar "⚙ MODIFICAR DATOS", abrir nueva pantalla o BottomSheet con opciones:
    - **Añadir nuevo idioma:** Selecciona idioma (Spinner) + nivel (Spinner) → Se añade a la lista.
    - **Eliminar idioma:** Lista de idiomas actuales con botón de eliminar (marca como `archived`).
    - **Cambiar nivel:** Para cada idioma, permitir actualizar el nivel.
    - **Reordenar prioridad:** Cambiar cuál es el idioma principal (drag & drop o botón "Marcar como principal").

- **Backend:**
  - ❌ **Pendiente:** Endpoint `GET /users/profile/languages` para obtener lista de idiomas del usuario con estadísticas.
  - ❌ **Pendiente:** Endpoint `POST /users/profile/languages/add` para añadir nuevo idioma.
  - ❌ **Pendiente:** Endpoint `PUT /users/profile/languages/{lang_code}` para actualizar nivel o marcar como principal.
  - ❌ **Pendiente:** Endpoint `DELETE /users/profile/languages/{lang_code}` para archivar idioma (soft delete).

#### C. Gestión de Datos y "Soft Delete" (No romper nada)

**🎯 Objetivo:** Permitir eliminar idiomas sin perder datos, con posibilidad de recuperación.

- **Soft Delete de Idiomas:**
  - ❌ **Pendiente:** Al eliminar un idioma (ej: Francés):
    - **NO BORRAR** las palabras ni flashcards de la base de datos.
    - **ACCIÓN:** Marcar el idioma como `archived: true` en el perfil del usuario.
    - **EFECTO:**
      - Las palabras desaparecen del diccionario (filtradas en el frontend).
      - Las flashcards pausan su algoritmo SRS (`next_review_date` se ignora).
      - La racha específica de ese idioma se resetea a 0.
    - **RECUPERACIÓN:** Si el usuario vuelve a añadir Francés:
      - Se reactiva (`archived: false`).
      - Todas las palabras y flashcards reaparecen.
      - El progreso SRS se reanuda desde el último estado.

- **Backend:**
  - ❌ **Pendiente:** Modificar lógica de endpoints de diccionario y flashcards para filtrar solo idiomas activos (`archived: false`).
  - ❌ **Pendiente:** Añadir campo `archived` al modelo de idiomas en el usuario.

#### D. Diccionario y Flashcards 2.0

**🎯 Objetivo:** Filtrado por idioma y prevención de burnout en el aprendizaje.

- **Diccionario con Chips de Filtrado:**
  - ❌ **Pendiente:** Añadir **Material Chips** (píldoras) en la parte superior de DictionaryActivity:
    - Chips: `[Todos]` `[🇬🇧 Inglés]` `[🇫🇷 Francés]` (dinámicos según idiomas del usuario).
  - ❌ **Pendiente:** Al pulsar un chip, filtrar la lista sin recargar la Activity:
    - Lógica: `adapter.filter(selectedLanguage)`.
  - ❌ **Pendiente:** Por defecto, mostrar el idioma activo del usuario (`primary_language`).

- **Flashcards con Tipos de Repaso:**
  - ❌ **Pendiente:** Implementar selector de modo de repaso en FlashcardsActivity:
    - **Normal:** Palabra → Definición.
    - **Inverso:** Definición → Palabra.
    - **Listening:** Audio (TTS) → Escribir la palabra.
  - ❌ **Pendiente:** Integrar TTS (Text-to-Speech) para reproducir la palabra al mostrar la tarjeta.
  - ❌ **Pendiente:** Botón "🎵 Oír en canción" que reproduce el fragmento de 5-10s usando timestamps + yt-dlp.

- **Control de Burnout (Anti-Agobie):**
  - ❌ **Pendiente:** Mostrar claramente "15 Cartas para hoy" vs "45 Total acumulado".
  - ❌ **Pendiente:** Si `repasos_pendientes > 50`, bloquear aprendizaje de nuevas palabras:
    - Mensaje: "¡Tienes muchas palabras pendientes! Termina tus repasos antes de añadir más 😊".
  - ❌ **Pendiente:** Permitir al usuario ajustar cuántas palabras nuevas quiere ver por día (5, 10, 15, 20) desde Settings.

- **Backend:**
  - ❌ **Pendiente:** Endpoint `GET /flashcards/pending?language={code}` para obtener repasos pendientes por idioma.
  - ❌ **Pendiente:** Endpoint `POST /flashcards/session` con parámetros: `language`, `mode` (normal/inverso/listening), `max_new_cards`.

#### E. Recomendaciones Inteligentes y Grammys (Ajuste de Lógica)

**🎯 Objetivo:** Personalizar las recomendaciones según el idioma activo del usuario y verificar la precisión de los nominados.

- **HomeFragment (Descubrir) - Recomendaciones Personalizadas:**
  - ❌ **Pendiente:** Revisar la lógica de búsqueda "Viral 50 Global" para que se adapte al idioma activo del usuario:
    - En lugar de buscar "Viral 50 Global" genérico, buscar según el idioma principal:
      - Inglés (EN): "Viral 50 Global" o "Top Hits USA"
      - Español (ES): "Viral 50 Spain" o "Top Latin"
      - Francés (FR): "Viral 50 France"
      - Alemán (DE): "Viral 50 Germany"
      - Italiano (IT): "Viral 50 Italy"
      - Portugués (PT): "Viral 50 Brazil"
  - ❌ **Pendiente:** Implementar filtro por idioma en la búsqueda inicial de HomeFragment:
    - Al cargar el fragmento, obtener el `primary_language` del usuario desde el perfil.
    - Buscar canciones en ese idioma usando el query adaptado.
  - ❌ **Pendiente:** Mostrar en el header el idioma activo: "🎧 Descubrir (Inglés)" o "🎧 Descubrir (Español)".

- **GrammysFragment - Verificación de Nominados:**
  - ❌ **Pendiente:** Revisar la lógica actual de búsqueda de nominados a los Grammys para asegurar precisión:
    - Verificar que la query de búsqueda retorna las canciones correctamente nominadas.
    - Comprobar si es necesario ajustar filtros (año, categoría, región).
    - Considerar usar una lista hardcodeada de nominados si la API no es precisa.
  - ❌ **Pendiente:** Implementar fallback: Si la búsqueda de Grammys falla o retorna resultados vacíos, mostrar lista curada manualmente.
  - ❌ **Pendiente:** Añadir filtro opcional por idioma en Grammys (ej: "Ver solo nominados en Inglés/Español").

- **Backend:**
  - ❌ **Pendiente:** Endpoint `GET /songs/recommendations?language={code}` que retorna canciones populares filtradas por idioma.
  - ❌ **Pendiente:** Verificar/ajustar endpoint de Grammys para asegurar que los resultados son precisos.
  - ❌ **Pendiente:** Considerar caché de nominados de Grammys para evitar búsquedas repetitivas (actualizar anualmente).

#### 💡 Sugerencia Técnica: BottomSheet vs Pop-up

**Recomendación:** Usar **BottomSheetDialogFragment** en lugar de Dialog tradicional.

- **Ventajas:**
  - Más moderno y alineado con Material Design 3.
  - Sale desde abajo, se puede cerrar deslizando hacia abajo.
  - Cómodo de usar con una mano (mejor UX en móviles).
  - Permite scroll si el contenido es largo.

- **Implementación:**
  ```kotlin
  class LanguageStatsBottomSheet : BottomSheetDialogFragment() {
      // RecyclerView con lista de idiomas + estadísticas
      // Botones de acción: "Cambiar idioma activo", "Gestionar idiomas"
  }
  ```

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
