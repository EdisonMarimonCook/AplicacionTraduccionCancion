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
    list_user_dictionary,
    delete_dictionary_entry
)
from schemas import DictItemCreate, DictItemOut

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/dictionary", tags=["Dictionary"])


# ---------- Helpers ----------
def _now_utc() -> datetime:
    return datetime.now(timezone.utc)


def build_dictionary_entry_payload(user_id: str, item: DictItemCreate) -> Dict[str, Any]:
    """
    Convierte DictItemCreate a la forma que guarda la BD.
    Normaliza nombres: source_lang -> language, level -> difficulty_level, notes -> definition.
    """
    return {
        "id": str(uuid4()),
        "user_id": user_id,
        "word": item.get("word"),
        "language": (item.get("source_lang") or item.get("language") or "en").lower(),
        "translation": item.get("translation"),
        "definition": item.get("notes"),
        "example": item.get("example"),
        "difficulty_level": (item.get("level") or "A1").upper(),
        "tags": item.get("tags", []),
        "type": item.get("type", "word"),
        "target_lang": item.get("target_lang", "es"),
        "created_at": _now_utc(),
        "last_reviewed": None,
        "review_count": 0,
    }
    
    # Está hecho para nuestra BD fake, cuando tengamos la real habrá que adaptarlo


def _map_db_item_to_out(it: Dict[str, Any]) -> DictItemOut:
    """
    Mapear documento/registro de BD a DictItemOut.
    Acepta tanto claves 'language' como 'source_lang' y 'difficulty_level' o 'level'.
    """
    return DictItemOut(
        id=str(it.get("id") or it.get("_id") or ""),
        word=it.get("word") or "",
        translation=it.get("translation") or "",
        source_lang=(it.get("language") or it.get("source_lang") or "en"),
        target_lang=it.get("target_lang") or "es",
        type=it.get("type") or "word",
        level=it.get("difficulty_level") or it.get("level") or "A1",
        example=it.get("example"),
        notes=it.get("definition") or it.get("notes"),
        tags=it.get("tags", []),
        created_at=it.get("created_at") if isinstance(it.get("created_at"), datetime) else _now_utc()
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
    level: Optional[str] = Query(None, description="Filtrar por nivel (A1, B1, ...)"),
    page: int = Query(1, ge=1),
    page_size: int = Query(50, ge=1, le=200),
    current_user: User = Depends(get_current_user)
):
    """
    Lista las entradas del diccionario del usuario. Soporta búsqueda simple y paginación.
    """
    try:
        items = await list_user_dictionary(current_user.id, q=q, level=level, page=page, page_size=page_size)
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