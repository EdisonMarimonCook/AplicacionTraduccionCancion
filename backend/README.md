# Music TransIAtor

## Configuración inicial

### 1. Clonar el repositorio
```bash
git clone <URL-del-repositorio>
cd AplicacionTraduccionCancion/backend
```

### 2. Crear entorno virtual
```bash
python -m venv venv
.\venv\Scripts\activate
```

### 3. Instalar dependencias
```bash
pip install -r requirements.txt
```

### 4. Configurar credenciales (IMPORTANTE)
```bash
# Copiar el archivo de ejemplo
cp .env.example .env

# Editar .env con tus credenciales personales
# Obtener credenciales en https://developer.spotify.com/dashboard
```

### 5. Ejecutar la aplicación
```bash
uvicorn main:app --reload
```

## Credenciales necesarias

- **SPOTIFY_CLIENT_ID**: Obtener en [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
- **SPOTIFY_CLIENT_SECRET**: Obtener en [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)

⚠️ **NUNCA** subas el archivo `.env` con credenciales reales a Git
