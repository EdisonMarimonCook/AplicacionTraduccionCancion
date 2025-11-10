# Music TransIAtor API

Aplicación FastAPI para aprender idiomas a través de canciones con IA.

---

## 🔧 Requisitos

- **Python 3.13+** (IMPORTANTE: No usar versiones anteriores)
- **pip** (gestor de paquetes)
- **Spotify Developer Account** (opcional, para usar datos reales)

---

## 📥 Instalación

### 1. Clonar el repositorio
```bash
git clone <URL-del-repositorio>
cd AplicacionTraduccionCancion/backend
```

### 2. Crear entorno virtual
```bash
# Windows
python -m venv venv
.\venv\Scripts\Activate.ps1

# macOS/Linux
python3 -m venv venv
source venv/bin/activate
```

### 3. Instalar dependencias
```bash
pip install --upgrade pip
pip install -r requirements.txt
```

⚠️ **NOTA**: En la primera instalación puede tardar 2-3 minutos descargando wheels precompilados.

### 4. Configurar variables de entorno (OPCIONAL)
```bash
# Copiar el archivo de ejemplo
cp .env.example .env

# Editar .env con tus credenciales
# SPOTIFY_CLIENT_ID=tu_id_aqui
# SPOTIFY_CLIENT_SECRET=tu_secret_aqui
# OPENAI_API_KEY=tu_clave_aqui
```

---

## 🚀 Ejecutar la aplicación

```bash
# Asegúrate de estar en el venv
.\venv\Scripts\Activate.ps1  # Windows
source venv/bin/activate     # macOS/Linux

# Inicia el servidor
uvicorn main:app --reload
```

La API estará disponible en: **http://127.0.0.1:8000**

---

## 🏥 Health Check

Verifica que la API está funcionando:

```bash
curl http://localhost:8000/health

# O en navegador:
# http://localhost:8000/health
```

**Respuesta esperada:**
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "database": "🔄 Mock DB",
  "cache_size": 7,
  "environment": "development"
}
```

---

## 📦 Stack Tecnológico

- **FastAPI 0.104.1** - Framework web async
- **Pydantic 2.12.4** - Validación de datos
- **Python 3.13** - Versión de Python
- **Uvicorn 0.24.0** - Servidor ASGI
- **PyMongo 4.6.0** - Cliente MongoDB
- **Spotipy 2.23.0** - API de Spotify
- **OpenAI 1.3.0** - API de IA
- **PyJWT** - Autenticación JWT

---

## 🔐 Credenciales Necesarias

### Spotify Developer (RECOMENDADO)
1. Ve a [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Crea una aplicación
3. Obtén: `SPOTIFY_CLIENT_ID` y `SPOTIFY_CLIENT_SECRET`
4. Añade a `.env`

### OpenAI (OPCIONAL)
1. Ve a [OpenAI API](https://platform.openai.com/api-keys)
2. Genera una API key
3. Añade a `.env`

⚠️ **NUNCA** subas el archivo `.env` con credenciales a Git. Está en `.gitignore`.

---

## 📊 Estructura de Carpetas

```
backend/
├── main.py              # Punto de entrada de la API
├── config.py            # Configuración centralizada
├── database.py          # Conexión a MongoDB/Mock
├── models.py            # Esquemas Pydantic
├── requirements.txt     # Dependencias Python
├── .env.example         # Variables de entorno ejemplo
├── .env                 # Variables de entorno (NO subir a Git)
├── .gitignore           # Archivos ignorados por Git
└── routers/             # Endpoints de la API
    ├── auth.py
    ├── songs.py
    ├── translations.py
    └── users.py
```

---

## 🐛 Solución de Problemas

### Error: `ModuleNotFoundError: No module named 'fastapi'`
```bash
# Verifica que estés en el venv
.\venv\Scripts\Activate.ps1

# Reinstala dependencias
pip install -r requirements.txt
```

### Error: `pydantic-core compilation failed`
```bash
# Asegúrate de tener Python 3.13+
python --version

# Si tienes versión anterior, actualiza Python
# https://www.python.org/downloads/
```

### La API no conecta a Spotify
```bash
# Verifica credenciales en .env
# SPOTIFY_CLIENT_ID y SPOTIFY_CLIENT_SECRET deben estar presentes

# Si no tienes, la API usa datos MOCK (simulados)
```

---

## 📚 Documentación de la API

Una vez el servidor está corriendo:

- **Swagger UI**: http://127.0.0.1:8000/docs
- **ReDoc**: http://127.0.0.1:8000/redoc

---

## 🤝 Contribuir

1. Crea una rama: `git checkout -b feature/nueva-funcion`
2. Haz cambios y commitea: `git commit -am 'Añade nueva función'`
3. Push: `git push origin feature/nueva-funcion`
4. Abre Pull Request

---

## 📝 Licencia

Este proyecto es parte de la asignatura PINF (UCA).

---

## ✅ Estado Actual

- ✅ Backend funcional con FastAPI
- ✅ Pydantic v2 compatible con Python 3.13
- ✅ Base de datos Mock lista
- ✅ Cache de Spotify poblado
- ⏳ Endpoints en desarrollo
- ⏳ Frontend en desarrollo
