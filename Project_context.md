🎵 MusicTransIAtor - Contexto del Proyecto

Versión: 3.0 (Post-Refactorización Backend)

Rol: Arquitecto de Software / Lead Developer

Estado: MVP Avanzado (Backend 100% - Frontend en Integración)

Próxima Entrega: 16/12/2025 (MVP)

📌 VISIÓN DEL PROYECTO

App Android "F2P" (Free to Play) para aprender idiomas con música.

Filosofía: Máxima funcionalidad con coste cero (APIs públicas, IAs gratuitas).

Innovación: Análisis contextual de la letra (no diccionario estático) y estrategia de audio híbrida.

📅 CALENDARIO Y ALCANCE

FASE FECHA OBJETIVO PRINCIPAL MVP
16 Dic 2025
Flujo completo: Buscar → Escuchar (30s) → Analizar (IA) → Guardar.

FINAL Enero 2026Retención (Flashcards/SRS) + Experiencia Avanzada (Audio Completo).🎯 ESTADO TÉCNICO ACTUAL (v3.0)

Plaintext

BACKEND (FastAPI): 100% (MVP Ready) ✅

├─ 🧠 IA: Gemini 2.0 Flash integrado (Contextual + Multilingüe).

├─ 🎵 Audio: Sistema "iTunes Rescue" implementado (si Spotify falla, usa iTunes).

├─ 🔍 Búsqueda: Unificada (Spotify Metadata + Audio).

├─ 📄 Letras: Genius API + Fragmentación básica.

└─ 🗄️ DB: Mock Database (memoria) funcionando.



FRONTEND (Android): 60% (En integración) ⏳

├─ 🔌 API: Retrofit configurado para Backend v3.0.

├─ 📱 UI: Pantallas base listas (Home, Player, Dictionary).

├─ ⏳ Lógica: Falta conectar el botón "Analizar IA" con la respuesta nueva.

└─ ⏳ Audio: Falta probar el MediaPlayer con URLs de iTunes.

✅ LO QUE ENTRA EN EL MVP (16/12)

1. Funcionalidades Core

Auth: Login/Registro JWT.

Home: Listas curadas ("Grammy Nominees", "Global Hits") para demo segura.

Player:

Reproducción de preview (30s) vía iTunes.

Visualización de letra completa.

IA Tutor (Gemini):

El usuario pide analizar.

La IA detecta idioma y nivel.

Devuelve JSON con palabras, traducciones contextuales y "Recommended ⭐".

Diccionario: Guardar palabras recomendadas.

2. Restricciones Técnicas (MVP)

Base de Datos: Usar MockDatabase, hay que conectar con Mongo si es posible, si aún no se puede, pues usar mock

Audio: Conformarse con los 30s de iTunes. No implementar scraping complejo.

Sincronización: Scroll manual. No implementar .lrc automático.

🚀 LO QUE VA AL PRODUCTO FINAL (Enero)

1. Sistema de Aprendizaje (SRS)

Flashcards: Algoritmo Spaced Repetition (estilo Anki/SuperMemo).

Progreso: Gráficos de retención y palabras aprendidas.

2. Infraestructura Real

Persistencia: Migración a MongoDB Atlas.

Despliegue: Servidor nube (Render/Railway) + HTTPS.

3. I+D (Propuestas "Pirata")

Audio Completo ("Estrategia Grayjay"): Investigar NewPipeExtractor para obtener audio de YouTube en el cliente (Android) y saltar la restricción de 30s.

Modo Karaoke: Integrar API de LRCLIB para tiempos exactos.

4. Cambio de modelo a IA Opensource Gratis

Posteriormente del MVP, debatiremos si cambiamos la IA por otra con más usos gratuita open source para tratar de aumentar la vida util de la aplicación

📋 GUÍA PARA DESARROLLADORES (AI ASSISTANTS)

Si eres una IA ayudando con el código, sigue estas reglas estrictas:

Backend:

El archivo de IA es services/gemini_client.py. 

El modelo es gemini-2.0-flash.

El endpoint de análisis es POST /api/v1/ai/analyze.

Frontend:

Usa 10.0.2.2:8000 para localhost en el emulador.

Los modelos de datos deben tener recommended: Boolean y translation: String.

Prohibido en MVP:

No intentar descargar MP3s completos en el servidor.

No implementar websockets (modelos -live).

📝 ÚLTIMOS CAMBIOS APLICADOS

Refactor: Eliminado openai_client.py.

Feature: Añadida detección automática de idioma (langdetect).

Feature: Añadido fallback a iTunes en utils/spotify.py.

Fix: Unificados endpoints de búsqueda en routers/songs.py.

