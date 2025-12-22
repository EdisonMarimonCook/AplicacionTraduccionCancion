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

## 🎯 PRODUCTO FINAL - TAREAS PENDIENTES

### 🚨 **PRIORIDAD CRÍTICA** (Bloquean lanzamiento)

#### 1. **Infraestructura de Producción**
- [ ] **Despliegue en Render.com:**
  - Conectar repo GitHub (rama `main`)
  - Configurar variables de entorno
  - Build/Start commands configurados
  - Health check endpoint `/health`
  
- [ ] **Migración a Cloudinary:**
  - Crear cuenta y obtener credenciales
  - Migrar `backend/static/avatars/` a Cloudinary
  - Actualizar `routers/users.py` para usar Cloudinary API
  - Eliminar dependencia de almacenamiento local

- [ ] **IP Automática en RetrofitService:**
  - Detección automática emulador vs móvil físico
  - Leer URL desde `BuildConfig` o `SharedPreferences`
  - Eliminar hardcoding de IP

#### 2. **Sistema de Audio (NewPipe Style)**
- [ ] **Investigar extracción de audio:**
  - YouTube Music scraping (NewPipeExtractor)
  - SoundCloud API como alternativa
  - Rotar User-Agents para evitar bloqueos
  
- [ ] **Integración con ExoPlayer:**
  - Player embebido en `SongLearningActivity`
  - Controles play/pause/seek
  - Sincronización con análisis de letras

- [ ] **Fallback a Spotify/iTunes:**
  - Si falla extracción, usar preview oficial (30s)
  - Mostrar mensaje al usuario sobre limitación

#### 3. **Portal de Descarga (Landing Page)**
- [ ] Página web simple con botón de descarga APK
- [ ] Sección de características principales
- [ ] Screenshots de la app
- [ ] Enlace a GitHub repo
- [ ] Hosting gratuito (GitHub Pages o Netlify)

---

### 🔥 **ALTA PRIORIDAD** (Mejoran UX significativamente)

#### 4. **Reestructuración de Navegación**
- [ ] **Bottom Navigation + ViewPager2:**
  - 🏠 Inicio: Canciones recomendadas (nivel + idioma usuario)
  - 🔥 Grammys 2025: Top 10 actual (evento especial)
  - 👤 Perfil: Pantalla actual con mejoras
  
- [ ] **Swipe horizontal entre tabs**
- [ ] **Buscador global** en tab Inicio

#### 5. **Diccionario Multinivel**
- [ ] **Agrupación por idioma:**
  ```
  📂 INGLÉS (EN - B1) - 247 palabras
     ├── 📝 Palabras (229)
     └── 💬 Expresiones (18)
  
  📂 FRANCÉS (FR - A2) - 12 palabras
     ├── 📝 Palabras (10)
     └── 💬 Expresiones (2)
  ```

- [ ] **Backend:** Endpoint `/dictionary/list?language=en&type=word`
- [ ] **Frontend:** `RecyclerView` con `ExpandableListAdapter`
- [ ] **Soporte CEFR para múltiples idiomas** (actualmente solo inglés)

#### 6. **Flashcards Mejoradas**
- [ ] **Rediseño de tarjetas:**
  - **Frente:** Solo palabra + ejemplo en idioma original
  - **Reverso:** Traducción + explicación detallada
  
- [ ] **Animación de volteo 3D:**
  - Rotación en eje Y con interpolación suave
  - Duración: 300ms
  
- [ ] **Audio TTS:**
  - Reproducción automática al mostrar palabra
  - Botón para repetir audio
  - Soporte para múltiples idiomas (Android TTS)

- [ ] **Fix: Tarjetas no aparecen tras añadir palabras:**
  - Crear flashcard automáticamente al guardar en diccionario
  - `next_review_date` = hoy (disponible inmediatamente)

#### 7. **Actualización en Tiempo Real de Contadores**
- [ ] **Perfil:**
  - Recargar datos en `onResume()`
  - Escuchar cambios desde diccionario/flashcards
  
- [ ] **Diccionario:**
  - Actualizar contador al añadir/eliminar palabra
  - Refresh pull-to-refresh

---

### 🟡 **MEDIA PRIORIDAD** (Pulido y optimización)

#### 8. **Caché Persistente Avanzado**
- [ ] **Top Grammy 2025:**
  - Guardar en Room DB (no solo SharedPreferences)
  - Expiración: 7 días o cuando haya nuevo nominado
  - Precarga en Splash Screen

- [ ] **Perfil de Usuario:**
  - Cachear datos localmente
  - Sincronizar en background cada 1 hora
  - Modo offline parcial

- [ ] **Canciones Recomendadas:**
  - Endpoint nuevo `/songs/recommended?language=en&level=B1`
  - Cachear 20 canciones por idioma/nivel
  - Algoritmo de recomendación basado en palabras guardadas

#### 9. **Optimización de Tiempos de Carga**
- [ ] **Precarga en Splash Screen:**
  - Top Grammy + Perfil + Recomendadas en paralelo
  - Mínimo 2s de splash para mostrar logo
  
- [ ] **Backend:**
  - Indexar campos de MongoDB (`user_id`, `email`, `created_at`)
  - Caché de queries frecuentes (Redis en futuro)
  - Optimizar agregaciones de contadores

- [ ] **Frontend:**
  - Lazy loading de imágenes con Glide
  - Paginación en listas largas (diccionario, flashcards)

#### 10. **Pantallas de Carga Personalizadas**
- [ ] **LoadingDialog reutilizable:**
  - Animación Lottie de música/notas musicales
  - Mensajes motivacionales ("Analizando letra...", "Preparando tu aprendizaje...")
  
- [ ] **Usar en:**
  - `SongLearningActivity` (análisis IA)
  - `FlashcardsActivity` (carga de tarjetas)
  - Primera carga de Top Grammy

#### 11. **Animaciones y Personalidad**
- [ ] **Transiciones suaves:**
  - Fade in/out entre actividades
  - Slide up para diálogos
  
- [ ] **Micro-interacciones:**
  - Botones con efecto ripple
  - Iconos animados (ej: estrella al guardar palabra)
  - Confetti al completar racha de 7 días

- [ ] **Tema visual consistente:**
  - Paleta de colores inspirada en música
  - Iconos personalizados (notas, micrófonos, etc.)

#### 12. **Indicador Visual `isRecommended`**
- [ ] **Diccionario:**
  - Añadir estrella dorada junto a palabras recomendadas
  - Ordenar: Recomendadas primero
  
- [ ] **Flashcards:**
  - Badge "IA Recomienda" en esquina superior

---

### 🟢 **BAJA PRIORIDAD** (Nice to have)

#### 13. **Groq como Fallback de IA**
- [ ] Crear cliente para Groq (Llama 3.1)
- [ ] Lógica de fallback: Gemini → Groq → Mock
- [ ] Comparativa de calidad antes de decidir

#### 14. **Gamificación Local**
- [ ] **Logros offline:**
  - "Primera racha de 7 días"
  - "50 palabras guardadas"
  - "100 flashcards revisadas"
  
- [ ] **Estadísticas visuales:**
  - Gráfico de progreso semanal (MPAndroidChart)
  - Heatmap de días de estudio
  
- [ ] **Guardado en SharedPreferences** (sin backend adicional)

#### 15. **Configuración de Pantalla Horizontal**
- [ ] `android:configChanges="orientation|screenSize"` en Manifest
- [ ] Layouts alternativos `layout-land/` para pantallas críticas
- [ ] Testing en todas las actividades

---

## 🛠️ ESTADO TÉCNICO ACTUAL (v4.5 MVP)

### 🐍 **BACKEND (FastAPI + MongoDB Atlas) - COMPLETO ✅**

#### **IA y Análisis**
- **Motor:** `gemini-2.5-flash-lite` (15 RPM gratis)
- **Fallback:** Mock response si se agota cuota
- **Caché:** `top10_cache.json` persistente

#### **Autenticación JWT + Email**
- Access Token: 15 min
- Refresh Token: 7 días
- Endpoints: `/auth/register`, `/login`, `/verify`, `/forgot-password`, `/reset-password`, `/refresh`

#### **Gestión de Usuarios**
- `/users/profile` (GET/PUT)
- `/users/upload-avatar` (POST)
- `/users/change-password` (POST)
- `/users/change-email` (POST)
- Avatares en `static/avatars/` (migrar a Cloudinary)

#### **Sistema de Racha**
- Campos: `current_streak`, `longest_streak`, `last_activity_date`
- Actualización: Al guardar palabras o revisar flashcards
- Reset: Si pasan >24h sin actividad

#### **Flashcards SRS**
- Algoritmo: SuperMemo SM-2
- Endpoints: `/flashcards/pending`, `/review/{id}`, `/stats`

#### **Diccionario**
- Modelo: `word`, `translation`, `context`, `type`, `isRecommended`
- Endpoints: `/dictionary/add`, `/list`, `/delete/{id}`

#### **Letras y Canciones**
- `/lyrics/analyze` (Gemini)
- `/songs/top-grammy` (cached)
- `/songs/search`

---

### 📱 **FRONTEND (Android Kotlin) - FEATURE-COMPLETE ✅**

#### **Actividades Principales**
- `MainActivity`: Auto-login + Login
- `SongSelectionActivity`: Top Grammy + búsqueda
- `SongLearningActivity`: Análisis + diccionario
- `FlashcardsActivity`: Swipe + SRS
- `ProfileActivity`: Avatar + contadores
- `DictionaryActivity`: Lista de palabras

#### **Características**
- Modo oscuro completo
- Swipe gestures en flashcards
- Avatar circular con uCrop
- Autenticación completa (registro, login, verificación, recuperación)
- FloatingActionButton para navegación

#### **Configuración de Red**
- Emulador: `10.0.2.2:8000`
- Móvil físico: `192.168.1.141:8000` (hardcodeado - **PENDIENTE: Auto-detección**)
- AuthInterceptor: Refresh automático de tokens
- Timeouts: 30s connect/read

---

## 📊 ESTRUCTURA DE DATOS (Sin cambios)

### **User (MongoDB)**
```python
{
    "_id": ObjectId,
    "email": str,
    "username": str,
    "password_hash": str,
    "full_name": str,
    "native_language": str,
    "learning_languages": [
        {
            "language": str,  # "en", "fr", "de", etc.
            "level": str,     # "A1", "A2", "B1", "B2", "C1", "C2"
            "started_at": datetime
        }
    ],
    "avatar_url": str,
    "current_streak": int,
    "longest_streak": int,
    "last_activity_date": datetime,
    "is_verified": bool,
    "verification_code": str,
    "reset_code": str,
    "reset_code_expires": datetime,
    "is_active": bool,
    "created_at": datetime,
    "updated_at": datetime
}
```

### **DictionaryEntry (MongoDB)**
```python
{
    "_id": ObjectId,
    "user_id": ObjectId,
    "word": str,
    "translation": str,
    "context": str,
    "example": str,
    "type": str,  # "word" o "expression"
    "language": str,  # "en", "fr", etc. (NUEVO)
    "isRecommended": bool,
    "created_at": datetime
}
```

### **FlashcardSRS (MongoDB)**
```python
{
    "_id": ObjectId,
    "user_id": ObjectId,
    "word_id": ObjectId,
    "easiness_factor": float,
    "interval": int,
    "repetitions": int,
    "next_review_date": date,
    "last_reviewed": datetime,
    "created_at": datetime
}
```

---

## 🚀 ROADMAP ACTUALIZADO

### 🟢 **FASE 1: MVP (COMPLETO 100%) ✅**
1. ✅ Registro con verificación por email
2. ✅ Login + auto-login + "Recordarme"
3. ✅ Top Grammy + búsqueda
4. ✅ Análisis de letras con IA
5. ✅ Diccionario personalizado
6. ✅ Flashcards SRS
7. ✅ Sistema de racha
8. ✅ Perfil con avatar
9. ✅ Cambio de contraseña/email
10. ✅ Recuperación de contraseña
11. ✅ Modo oscuro
12. ✅ Swipe gestures
13. ✅ Vídeo demo grabado

---

### 🔥 **FASE 2: PRODUCTO FINAL (Dic 2024 - Ene 2025)**

**Infraestructura (CRÍTICO):**
- [ ] Render.com deployment
- [ ] Cloudinary migration
- [ ] IP auto-detection
- [ ] Portal de descarga (landing page)

**UX Core (ALTO):**
- [ ] Bottom Navigation + Swipe
- [ ] Diccionario multinivel (idiomas → tipo)
- [ ] Flashcards mejoradas (audio TTS + animación 3D)
- [ ] Fix: Contadores en tiempo real
- [ ] Sistema de audio (NewPipe style)

**Optimización (MEDIO):**
- [ ] Caché persistente (Room DB)
- [ ] Pantallas de carga personalizadas
- [ ] Optimización de tiempos iniciales
- [ ] Animaciones y personalidad

**Opcionales (BAJO):**
- [ ] Groq como fallback IA
- [ ] Gamificación local
- [ ] Soporte pantalla horizontal

---

### 🟡 **FASE 3: POST-LANZAMIENTO (Futuro)**

**Funcionalidades:**
- [ ] Modo offline completo
- [ ] Exportar a Anki
- [ ] Notificaciones push (Firebase)
- [ ] Estadísticas avanzadas
- [ ] Logros y badges

**Escalabilidad:**
- [ ] Google Play Store (requiere $25)
- [ ] Monetización opcional (Freemium $4.99/mes)
- [ ] Leaderboards (Firebase Realtime DB)

---

## 🔐 VARIABLES DE ENTORNO

```bash
# MongoDB Atlas
MONGO_URI=mongodb+srv://usuario:password@cluster.mongodb.net/musicTransIAtor?retryWrites=true&w=majority

# Gemini AI
GEMINI_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXX

# Cloudinary (NUEVO)
CLOUDINARY_CLOUD_NAME=tu_cloud_name
CLOUDINARY_API_KEY=123456789012345
CLOUDINARY_API_SECRET=abcdefghijklmnopqrstuvwx

# Email (Gmail SMTP)
MAIL_USERNAME=tu_email@gmail.com
MAIL_PASSWORD=xxxx xxxx xxxx xxxx
MAIL_FROM=tu_email@gmail.com
MAIL_PORT=587
MAIL_SERVER=smtp.gmail.com

# JWT
SECRET_KEY=tu_clave_secreta_super_segura_de_al_menos_32_caracteres
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=15
REFRESH_TOKEN_EXPIRE_DAYS=7

# APIs Externas
GENIUS_API_KEY=tu_genius_api_key
SPOTIFY_CLIENT_ID=tu_spotify_client_id
SPOTIFY_CLIENT_SECRET=tu_spotify_client_secret

# Configuración
ENVIRONMENT=development  # development | production
```

---

## 📋 RESUMEN DE DECISIONES TÉCNICAS

### **Herramientas Gratuitas (Budget: $0)**
| Servicio | Plan | Límites | Uso |
|----------|------|---------|-----|
| **Render.com** | Free Tier | 750h/mes, sleep tras 15min inactivo | Backend API |
| **Cloudinary** | Free Tier | 25 GB storage, 25k transforms/mes | Avatares |
| **MongoDB Atlas** | Free Tier | 512 MB storage | Base de datos |
| **Gemini AI** | Free Tier | 15 RPM | Análisis de letras |
| **Groq** | Free Tier | 30 RPM | Fallback IA |
| **GitHub Pages** | Gratis | Sin límites | Landing page |

### **Decisiones Pendientes**
- [ ] **Audio:** NewPipe (YouTube) vs SoundCloud vs Spotify Preview
- [ ] **IA:** Gemini solo vs Gemini + Groq fallback
- [ ] **Navegación:** Bottom Nav definitivo vs FAB actual
- [ ] **Deployment:** Render vs Railway vs Fly.io

---

## 🧪 TESTING CHECKLIST (Actualizado)

### **Infraestructura**
- [ ] Backend funciona en Render (URL pública accesible)
- [ ] Avatares se suben correctamente a Cloudinary
- [ ] IP se detecta automáticamente (emulador vs móvil)
- [ ] APK funciona en dispositivo físico sin hardcoding

### **Audio**
- [ ] Reproducción de audio desde YouTube/SoundCloud
- [ ] Fallback a Spotify Preview si falla extracción
- [ ] Controles play/pause funcionan
- [ ] Sincronización con análisis de letras

### **Navegación**
- [ ] Swipe entre tabs funciona suavemente
- [ ] Tab "Grammys 2025" muestra Top 10 actual
- [ ] Tab "Inicio" muestra canciones recomendadas según perfil
- [ ] Buscador global funciona en tab Inicio

### **Diccionario**
- [ ] Palabras se agrupan por idioma
- [ ] Subsecciones "Palabras" y "Expresiones" funcionan
- [ ] Estrella dorada aparece en palabras recomendadas
- [ ] Soporte CEFR para inglés, francés, alemán, etc.

### **Flashcards**
- [ ] Audio TTS se reproduce al mostrar palabra
- [ ] Animación de volteo 3D es suave
- [ ] Tarjetas nuevas aparecen inmediatamente tras guardar palabra
- [ ] Contador en perfil se actualiza tras revisar

### **Rendimiento**
- [ ] Splash screen carga Top Grammy + Perfil en <3s
- [ ] Imágenes cargan con lazy loading
- [ ] No hay lag al hacer scroll en listas largas
- [ ] Modo oscuro funciona en todas las pantallas

---

## 📞 CONTACTO Y CONTRIBUCIÓN

**Equipo de Desarrollo:**
- Edison Marimon Cook (@EdisonMarimonCook) - Propietario del repositorio
- Hugo - Desarrollador principal
- [Otros miembros] - Landing page y tareas adicionales

**Repositorio:** [GitHub - AplicacionTraduccionCancion](https://github.com/EdisonMarimonCook/AplicacionTraduccionCancion)

**Ramas:**
- `main` - Versión estable (MVP 4.5)
- `feature/lyrics-translation` - Desarrollo activo (Hugo trabajando aquí)
- `release/5.0` - Producto final (próximamente)

**Estado del Proyecto:**
- ✅ MVP completado y demostrado
- 🚧 Transición a producto final
- 🎯 Objetivo: Lanzamiento público Enero 2025

**Responsabilidades actuales:**
- **Hugo:** Infraestructura (Render + Cloudinary) + UX Core + Optimización
- **Otro miembro:** Landing page (GitHub Pages)
- **Edison:** Propietario del repo y coordinación general

---

**Última actualización:** 21 de Diciembre de 2024  
**Versión del documento:** 5.0.1  
**Próxima revisión:** Tras configurar Render + Cloudinary  
**Próxima tarea (Hugo):** Despliegue en Render.com
