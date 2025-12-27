---

# 🎵 MusicTransIAtor - Contexto del Proyecto

**Versión:** 5.2 (Audio Nativo Unleashed & Pedagogical Core)  
**Fecha de Actualización:** 27 de Diciembre de 2025  
**Estado:** 🌟 MVP COMPLETO + AUDIO LOCAL. Funcionalidad Core terminada (Auth, Anki, Backend, Audio). Foco Actual: Unificación de Navegación (BottomNav) y Pulido Visual.

---

## 📌 Estado Real del MVP

La aplicación es funcionalmente completa. Ya incluye:

- **Gestión de Usuarios:** Registro, Login, Cambio de Credenciales, Avatar (Cloudinary).
- **Aprendizaje:** Algoritmo de repetición espaciada (SRS - Anki), gestión de rachas, Flashcards automáticas.
- **Core Musical:** Buscador de canciones, Top Grammys, Bypass de letras (Cloudflare).
- **Audio (NUEVO):** Reproducción local sin límites (yt-dlp/Grayjay) + Carga paralela inteligente.

## 📚 Notas Técnicas: Estrategia Pedagógica (Diccionario & Flashcards)

### 1. Arquitectura del Diccionario (Multilenguaje)
El sistema soporta polyglots (usuarios aprendiendo varios idiomas). La estructura de datos y UI se divide en 3 niveles:

- **Nivel 1 (Idioma):** Filtrado visual inicial (ej. Pestañas/Banderas: 🇬🇧 EN, 🇫🇷 FR, 🇯🇵 JP).
- **Nivel 2 (Tipología):** Separación lógica entre:
   - Palabras: Vocabulario general.
   - Expresiones: Idioms, Phrasal Verbs, Slang (Detectados por la IA).
- **Nivel 3 (Items):** Listado optimizado para visualización rápida y acceso al repaso.

### 2. Motor SRS (Flashcards 2.0) con "Burnout Protection"
Implementación del algoritmo de Repetición Espaciada con límites para evitar la saturación del usuario:

- **Daily Caps (Límites Diarios):** Configuración para evitar colas de repaso infinitas.
   - Nuevas tarjetas: Máx. sugerido 10-20/día.
   - Repasos: Máx. sugerido 50/día.
   - Cola: El excedente se reprograma automáticamente para el día siguiente.
- **Dual Audio System:** Cada tarjeta reproduce:
   - TTS: Pronunciación estándar.
   - Clip Original: Fragmento de la canción (Contexto real).

### 3. Estandarización de Niveles (Proficiency Mapping)
En lugar de tests invasivos, se usa Autoevaluación Guiada mapeando estándares internacionales a nuestra dificultad interna (1-6):

- **Idiomas Europeos:** Estándar CEFR (A1, A2, B1, B2, C1, C2).
- **Japonés:** Estándar JLPT (N5 a N1).
- **Chino:** Estándar HSK.

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

- **Bottom Navigation (PRIORIDAD):**
   - Dejar de usar Activities sueltas y botones dispersos.
   - Implementar menú estándar: Inicio | Explorar/Grammys | Perfil.
- **Auditoría Visual:**
   - Revisar márgenes (paddings) que tocan los bordes.
   - Estandarizar estilos de botones.
- **Gestión de Datos UI:**
   - Visualizar las listas de diccionarios/flashcards (ya existentes en BD) dentro de la nueva estructura de navegación (Fragmentos).

### 🟡 FASE 3: MANTENIMIENTO Y OPTIMIZACIÓN

- **Configuración Retrofit:** Automatizar el switch DEBUG/RELEASE para la URL de Render.
- **Wake-Up Screen:** Optimizar la carga inicial de datos (Top Grammys, Perfil) al abrir la app.
- **Modo Offline:** Persistencia local de datos críticos (Room) para cuando falle la red.

### 🔵 FASE 4: EXTRAS (OPCIONALES / "SI DA TIEMPO")

- **Modo Karaoke Oculto:** Easter Egg para tener reproducción de música como reproductor simplemente sin analisis, aprovechando el sincronizado de audio con letre de la libreria de usada para obtención de letras.
- **Resiliencia IA:** Integración de Groq/Llama 3 como fallback si Gemini falla.
- **Gamificación Avanzada:** Logros visuales, gráficas de progreso y medallas.

---

## 📝 Notas para el Commit / Equipo

- **Aviso Importante:** Se ha modificado la configuración de red (RetrofitClient o NetworkModule) para apuntar forzosamente a Render. Esto es para que todos puedan probar el audio y la IA sin configurar el entorno local.
- **Cambio Crítico:** Se ha añadido lógica Python local (yt-dlp) en el cliente Android. La primera carga de una canción puede tardar unos segundos extra por la inicialización del entorno (necesario avisar a testers).
