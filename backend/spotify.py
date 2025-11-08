# ==============================================================================
# MÓDULO SPOTIFY - Integración con la API de Spotify
# ==============================================================================
# Este módulo se encarga de obtener canciones populares de Spotify según el idioma
# del usuario. Utiliza la API oficial de Spotify para buscar y recuperar tracks.
#
# Funcionalidades principales:
# - Autenticación con la API de Spotify
# - Búsqueda de canciones populares por idioma
# - Procesamiento y estructuración de datos
# - Fallback a canciones por defecto si hay errores
# ==============================================================================

import os  # Para acceder a variables de entorno
import base64  # Para codificar credenciales en autenticación
import requests  # Para hacer peticiones HTTP a la API de Spotify
from dotenv import load_dotenv  # Para cargar variables de .env
from typing import List, Dict  # Para definir tipos de datos
import logging  # Para registrar eventos y errores
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials

# Configuración de logging - registra eventos en la consola
logging.basicConfig(level=logging.INFO)

# Cargamos variables de entorno del archivo .env
load_dotenv()

# Credenciales de Spotify - obtenidas de las variables de entorno
# Estas deben estar configuradas en el archivo .env
CLIENT_ID = os.getenv("SPOTIFY_CLIENT_ID")
CLIENT_SECRET = os.getenv("SPOTIFY_CLIENT_SECRET")

# ==============================================================================
# CONFIGURACIÓN: Mapeo de idiomas a códigos de mercado de Spotify
# ==============================================================================
# Spotify requiere un código de mercado (market code) para filtrar resultados
# por región específica. Este diccionario mapea idiomas a sus códigos ISO 3166.
# Ejemplo: "es" (español) -> "ES" (España)
MARKET_MAP = {
    "en": "US",  # Inglés - Mercado USA
    "es": "ES",  # Español - Mercado España
    "fr": "FR",  # Francés - Mercado Francia
    "de": "DE",  # Alemán - Mercado Alemania
    "it": "IT",  # Italiano - Mercado Italia
    "pt": "PT",  # Portugués - Mercado Portugal
    "jp": "JP"   # Japonés - Mercado Japón
}

# ==============================================================================
# CONFIGURACIÓN: Consultas de búsqueda por idioma
# ==============================================================================
# Estas son las consultas de búsqueda que usamos para encontrar canciones populares
# Se intenta con cada una hasta encontrar resultados válidos.
# Si la primera falla, intenta con la siguiente (fallback mechanism)
SEARCH_QUERIES = {
    "en": ["top 50 usa", "trending now", "viral hits"],  # Búsquedas para inglés
    "es": ["éxitos españa", "canciones populares español", "top latino"],  # Búsquedas para español
    "fr": ["hits france", "chansons populaires", "top france"],  # Búsquedas para francés
    "de": ["deutsche hits", "top deutschland", "deutsche pop"],  # Búsquedas para alemán
    "it": ["canzoni italiane", "hit italia", "pop italiano"],  # Búsquedas para italiano
    "jp": ["アニメ オープニング", "日本のヒット曲", "トレンド", "バイラルヒット"]  # Búsquedas para japonés
}

# OBJETO que soluciona toda la autentificacion
sp = spotipy.Spotify(auth_manager=SpotifyClientCredentials(client_id=CLIENT_ID, client_secret=CLIENT_SECRET))

def get_access_token() -> str:
    """
    Obtiene un token de acceso de Spotify usando Client ID y Secret.
    
    Proceso:
    1. Verifica que tenemos las credenciales en las variables de entorno
    2. Codifica las credenciales en base64 para la autenticación
    3. Realiza una petición POST a Spotify para obtener el token
    4. Retorna el token válido por 1 hora
    
    Returns:
        str: Token de acceso para usar en peticiones a la API
        
    Raises:
        Exception: Si faltan credenciales o si la autenticación falla
    """
    # Validar que tenemos las credenciales necesarias
    if not CLIENT_ID or not CLIENT_SECRET:
        raise Exception("Faltan credenciales de Spotify en .env")

    # Endpoint de autenticación de Spotify
    url = "https://accounts.spotify.com/api/token"
    
    # Codificar credenciales en base64 (requerido por Spotify)
    # Formato: base64(CLIENT_ID:CLIENT_SECRET)
    auth_header = base64.b64encode(f"{CLIENT_ID}:{CLIENT_SECRET}".encode()).decode()
    
    # Headers para la petición de autenticación
    headers = {"Authorization": f"Basic {auth_header}"}
    
    # Datos para obtener token (grant_type: client_credentials)
    data = {"grant_type": "client_credentials"}

    # Realizar petición POST a Spotify
    resp = requests.post(url, headers=headers, data=data)
    
    # Validar que la autenticación fue exitosa
    if resp.status_code != 200:
        raise Exception(f"Error obteniendo token: {resp.text}")

    # Extraer y retornar el token del JSON de respuesta
    return resp.json()["access_token"]

def get_top10_playlist(language: str = "en") -> List[Dict[str, str]]:
    """
    Obtiene las 10 canciones más populares de un idioma específico.
    
    Proceso:
    1. Obtiene las queries de búsqueda del idioma solicitado
    2. Obtiene el token de autenticación de Spotify
    3. Intenta buscar con cada query hasta encontrar resultados
    4. Si alguna falla, pasa a la siguiente (estrategia de fallback)
    5. Procesa los resultados y retorna el top 10
    6. Si todo falla, retorna canciones por defecto
    
    Args:
        language (str): Código del idioma ('en', 'es', 'fr', etc.)
        
    Returns:
        List[Dict[str, str]]: Lista de canciones con rank, nombre, artista, etc.
        
    Ejemplo:
        >>> canciones = get_top10_playlist("es")
        >>> print(canciones[0]["name"])
        "Canción Popular"
    """
    # Obtener queries de búsqueda para el idioma (o usar inglés por defecto)
    search_queries = SEARCH_QUERIES.get(language, SEARCH_QUERIES["en"])
    
    # Obtener código de mercado para el idioma (o usar US por defecto)
    market = MARKET_MAP.get(language, "US")

    logging.info(f"Buscando canciones populares para idioma: {language}")

    # Intentar obtener el token de acceso
    try:
        token = get_access_token()
    except Exception as e:
        logging.error(f"No se pudo obtener token: {e}")
        # Si falla la autenticación, retornar canciones por defecto
        return get_default_songs()

    # Intentar con cada query de búsqueda
    for q in search_queries:
        try:
            logging.info(f"Intentando buscar: {q}")
            
            # Endpoint de búsqueda de Spotify
            url = "https://api.spotify.com/v1/search"
            
            # Headers con el token de autenticación
            headers = {"Authorization": f"Bearer {token}"}
            
            # Parámetros de la búsqueda
            params = {
                "q": q,  # Término de búsqueda
                "type": "track",  # Buscar solo canciones (tracks)
                "limit": 10,  # Obtener máximo 10 resultados
                "market": market  # Filtrar por mercado/región específica
            }

            # Realizar la petición GET con timeout de 5 segundos
            resp = requests.get(url, headers=headers, params=params, timeout=5)
            
            # Lanzar excepción si hay error HTTP (4xx, 5xx)
            resp.raise_for_status()
            
            # Parsear JSON y extraer canciones
            data = resp.json()
            tracks = data.get("tracks", {}).get("items", [])

            # Validar que obtuvimos resultados
            if tracks and len(tracks) > 0:
                logging.info(f"Éxito obteniendo {len(tracks)} canciones para '{q}'")
                # Procesar y retornar los tracks
                return process_tracks(tracks, from_search=True)
            else:
                # Si no hay resultados, intentar con siguiente query
                logging.warning(f"No se encontraron canciones para '{q}'")
                continue

        # Manejar timeout (la conexión tardó demasiado)
        except requests.exceptions.Timeout:
            logging.error(f"Timeout buscando '{q}': La conexión tardó demasiado")
            continue
            
        # Manejar error de conexión
        except requests.exceptions.ConnectionError:
            logging.error(f"Error de conexión buscando '{q}'")
            continue
            
        # Manejar errores HTTP específicos
        except requests.HTTPError as e:
            # Token caducado (error 401)
            if e.response.status_code == 401:
                logging.warning("Token caducado, intentando refrescar...")
                try:
                    token = get_access_token()  # Obtener nuevo token
                except Exception as refresh_error:
                    logging.error(f"No se pudo refrescar token: {refresh_error}")
                    return get_default_songs()
                continue
            logging.error(f"HTTPError {e.response.status_code} buscando '{q}': {e}")
            
        # Manejar cualquier otro error inesperado
        except Exception as e:
            logging.error(f"Error inesperado buscando '{q}': {str(e)}")

    # Si todas las queries fallan, retornar canciones por defecto
    logging.warning("No se pudieron obtener canciones, usando lista por defecto")
    return get_default_songs()

def process_tracks(tracks: List[Dict], from_search: bool = False) -> List[Dict[str, str]]:
    """
    Procesa la lista de tracks devueltos por Spotify y estructura los datos.
    
    Convierte los datos brutos de Spotify en un formato limpio y ordenado.
    Valida que cada track tenga los campos necesarios antes de incluirlo.
    
    Args:
        tracks (List[Dict]): Lista de tracks de Spotify (del endpoint de búsqueda o playlist)
        from_search (bool): Indica si los tracks vienen de una búsqueda (True) o playlist (False)
        
    Returns:
        List[Dict[str, str]]: Lista procesada con datos estructurados
        
    Estructura de retorno:
        {
            "rank": int,  # Posición en el ranking (1-10)
            "name": str,  # Nombre de la canción
            "artist": str,  # Nombre del artista principal
            "preview_url": str,  # URL para escuchar preview (puede ser None)
            "spotify_url": str  # URL para abrir en Spotify
        }
    """
    top10 = []  # Lista que almacenará los tracks procesados
    
    # Iterar sobre cada track con su índice
    for idx, item in enumerate(tracks):
        try:
            # Extraer el track según su origen
            # Si viene de búsqueda, item es el track directamente
            # Si viene de playlist, item es un wrapper con campo "track"
            track = item if from_search else item.get("track")
            
            # Validar que track no es None o inválido
            if not track:
                logging.warning(f"Track {idx} es inválido o None")
                continue

            # Validar y extraer la lista de artistas
            artists = track.get("artists", [])
            # Obtener nombre del primer artista (principal) o "Unknown" si no hay
            artist_name = artists[0]["name"] if artists and len(artists) > 0 else "Unknown"

            # Crear diccionario con la información de la canción
            top10.append({
                "rank": idx + 1,  # Ranking (empezando en 1, no en 0)
                "name": track.get("name", "Unknown"),  # Nombre de la canción
                "artist": artist_name,  # Artista principal
                "preview_url": track.get("preview_url"),  # URL de preview (puede ser None)
                "spotify_url": track.get("external_urls", {}).get("spotify", "")  # URL de Spotify
            })
            
        except Exception as e:
            # Registrar error pero continuar con siguiente track
            logging.error(f"Error procesando track {idx}: {str(e)}")
            continue
    
    return top10

def get_default_songs() -> List[Dict[str, str]]:
    """
    Retorna una lista de canciones por defecto cuando falla la API de Spotify.
    
    Esta función actúa como fallback (respaldo) si:
    - No se pueden obtener credenciales válidas
    - La API de Spotify está caída
    - Hay problemas de conexión
    - Ninguna de las búsquedas retorna resultados
    
    Garantiza que la aplicación siempre tiene algo que mostrar al usuario,
    incluso si Spotify no está disponible.
    
    Returns:
        List[Dict[str, str]]: Lista de canciones populares hardcodeadas
    """
    return [
        {
            "rank": 1,
            "name": "Blinding Lights",
            "artist": "The Weeknd",
            "preview_url": "",
            "spotify_url": ""
        },
        {
            "rank": 2,
            "name": "As It Was",
            "artist": "Harry Styles",
            "preview_url": "",
            "spotify_url": ""
        },
        {
            "rank": 3,
            "name": "Heat Waves",
            "artist": "Glass Animals",
            "preview_url": "",
            "spotify_url": ""
        }
    ]
