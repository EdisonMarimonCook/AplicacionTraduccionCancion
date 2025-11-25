"""
ARCHIVO: routers/dictionary.py
PROPÓSITO: Endpoints del diccionario personal
RUTAS: /dictionary/add, /dictionary/list, /dictionary/delete
USUARIO: Frontend/Mobile

"""

from typing import List, Optional, Dict, Any
import logging
from fastapi import APIRouter, Depends, HTTPException, Query, status
from uuid import uuid4
from datetime import datetime, timezone

from models import User  # Pydantic model del usuario
from routers.auth import get_current_user
from crud import (
    create_dictionary_entry,
    list_user_dictionary,  # ✅ AHORA EXISTE
    delete_dictionary_entry
)
from routers.schemas import DictItemCreate, DictItemOut
from services.level_mapper import get_level_system, validate_level_for_system

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/dictionary", tags=["Dictionary"])


# ---------- Helpers ----------
def _now_utc() -> datetime:
    return datetime.now(timezone.utc)


async def get_user_current_language(user_id: str) -> tuple:
    """
    Obtiene el idioma y sistema de niveles que está estudiando el usuario.
    Retorna: (language, level_system)
    """
    # TODO: Obtener del modelo User
    # Por ahora, default a inglés/CEFR
    from database_mock import MOCK_DATABASE
    
    user = next(
        (u for u in MOCK_DATABASE.get("users", []) if u.get("id") == user_id),
        None
    )
    
    if user and user.get("learning_languages"):
        # Obtener el primer idioma en aprendizaje
        first_lang = user["learning_languages"][0]
        return first_lang.get("language", "en"), first_lang.get("level_system", "CEFR")
    
    return "en", "CEFR"  # Default


def build_dictionary_entry_payload(user_id: str, item: dict) -> dict:
    """
    Convierte DictItemCreate a la forma que guarda la BD.
    Maneja multiidioma (CEFR, JLPT, HSK, TOPIK).
    """
    # Validar que el nivel existe en el sistema
    level_system = item.get("level_system", "CEFR")
    difficulty_level = item.get("difficulty_level", "A1")
    
    # Validar combinación nivel/sistema
    valid_system = get_level_system(item.get("source_lang", "en"))
    if not validate_level_for_system(difficulty_level, level_system):
        raise ValueError(f"Nivel {difficulty_level} no válido para sistema {level_system}")
    
    return {
        "id": str(uuid4()),
        "user_id": user_id,
        "word": item.get("word"),
        "language": item.get("source_lang", "en").lower(),
        "translation": item.get("translation"),
        "definition": item.get("notes"),
        "example": item.get("example"),
        "level_system": level_system,
        "difficulty_level": difficulty_level,
        "tags": item.get("tags", []),
        "type": item.get("type", "word"),
        "target_lang": item.get("target_lang", "es"),
        "is_hiphop_term": item.get("is_hiphop_term", False),
        "source_song_id": item.get("song_id"),
        "created_at": datetime.now(timezone.utc),
        "last_reviewed": None,
        "review_count": 0,
    }
    
    # Está hecho para nuestra BD fake, cuando tengamos la real habrá que adaptarlo


def _map_db_item_to_out(it: Dict[str, Any]) -> DictItemOut:
    """
    Mapear documento/registro de BD a DictItemOut.
    Maneja campos antiguos (source_lang, level) y nuevos (language, difficulty_level).
    """
    return DictItemOut(
        id=str(it.get("id") or it.get("_id") or ""),
        word=it.get("word") or "",
        translation=it.get("translation") or "",
        source_lang=(it.get("language") or it.get("source_lang") or "en"),
        target_lang=it.get("target_lang") or "es",
        type=it.get("type") or "word",
        level_system=it.get("level_system") or "CEFR",
        difficulty_level=it.get("difficulty_level") or it.get("level") or "A1",
        example=it.get("example"),
        notes=it.get("definition") or it.get("notes"),
        tags=it.get("tags", []),
        is_hiphop_term=it.get("is_hiphop_term", False),
        song_id=it.get("source_song_id") or it.get("song_id"),
        created_at=it.get("created_at") or datetime.now(timezone.utc),
        last_reviewed=it.get("last_reviewed"),
        is_learned=it.get("is_learned", False),
        times_reviewed=it.get("review_count", 0)
    )


# ---------- Endpoints ----------
@router.post("/add", response_model=DictItemOut, status_code=status.HTTP_201_CREATED)
async def add_dictionary_item(
    item: DictItemCreate,
    current_user: User = Depends(get_current_user)
):
    """
    Añade una entrada al diccionario personal del usuario.
    """
    try:
        payload = build_dictionary_entry_payload(current_user.id, item.model_dump())
        entry = await create_dictionary_entry(payload)  # se asume que crea y devuelve el documento creado
        # Si el CRUD no devuelve el documento, usamos payload como fallback
        doc = entry if isinstance(entry, dict) and entry else payload
        return _map_db_item_to_out(doc)
    except HTTPException:
        raise
    except Exception as e:
        logger.exception("Error creating dictionary entry")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Error creating entry")


@router.get("/list", response_model=List[DictItemOut])
async def list_dictionary_items(
    q: Optional[str] = Query(None, description="Filtro por palabra o tag"),
    language: Optional[str] = Query(None, description="Idioma: en, es, ja, etc"),
    level_system: Optional[str] = Query(None, description="CEFR, JLPT, HSK, TOPIK"),
    difficulty_level: Optional[str] = Query(None, description="Nivel específico: A1, B1, N3, etc"),
    is_hiphop_term: Optional[bool] = Query(None, description="Solo HipHop terms"),
    page: int = Query(1, ge=1),
    page_size: int = Query(50, ge=1, le=200),
    current_user: User = Depends(get_current_user)
):
    """
    Lista las entradas del diccionario del usuario. Soporta búsqueda simple y paginación.
    """
    try:
        items = await list_user_dictionary(
            current_user.id,
            q=q,
            language=language,
            level_system=level_system,
            difficulty_level=difficulty_level,
            is_hiphop_term=is_hiphop_term,
            page=page,
            page_size=page_size
        )
        return [_map_db_item_to_out(it) for it in (items or [])]
    except Exception:
        logger.exception("Error listing dictionary items")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Error listing entries")


@router.delete("/delete/{item_id}", response_model=dict)
async def delete_dictionary_item(
    item_id: str,
    current_user: User = Depends(get_current_user)
):
    """
    Borra una entrada del diccionario del usuario (comprueba ownership en CRUD).
    """
    try:
        deleted = await delete_dictionary_entry(current_user.id, item_id)
        if not deleted:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Entry not found")
        return {"status": "deleted", "id": item_id}
    except HTTPException:
        raise
    except Exception:
        logger.exception("Error deleting dictionary item")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Error deleting entry")