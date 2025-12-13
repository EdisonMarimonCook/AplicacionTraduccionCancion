# backend/routers/flashcards.py (NUEVO ARCHIVO)
"""
ROUTER: Flashcards con Repetición Espaciada (SRS)
ALGORITMO: SuperMemo SM-2
"""

import logging
from datetime import datetime, timedelta
from fastapi import APIRouter, Depends, HTTPException
from typing import List

from models import User
from routers.auth import get_current_user
from routers.schemas import FlashcardReviewRequest, FlashcardData, FlashcardReviewResponse
from database import db

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/flashcards", tags=["Flashcards"])

def calculate_next_review(quality: int, repetitions: int, easiness_factor: float, interval: int) -> tuple:
    """
    Algoritmo SuperMemo SM-2
    
    Args:
        quality: 0-5 (calidad del recuerdo)
        repetitions: Número de repeticiones correctas consecutivas
        easiness_factor: Factor de facilidad (EF)
        interval: Intervalo actual en días
    
    Returns:
        (new_repetitions, new_easiness_factor, new_interval)
    """
    # Si la respuesta fue mala (quality < 3), resetear
    if quality < 3:
        new_repetitions = 0
        new_interval = 1
        new_ef = easiness_factor
    else:
        # Calcular nuevo EF
        new_ef = easiness_factor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
        
        # EF mínimo es 1.3
        if new_ef < 1.3:
            new_ef = 1.3
        
        # Incrementar repeticiones
        new_repetitions = repetitions + 1
        
        # Calcular nuevo intervalo
        if new_repetitions == 1:
            new_interval = 1
        elif new_repetitions == 2:
            new_interval = 6
        else:
            new_interval = round(interval * new_ef)
    
    return new_repetitions, new_ef, new_interval

@router.get("/due", response_model=List[FlashcardData])
async def get_due_flashcards(current_user: User = Depends(get_current_user)):
    """
    📚 Obtiene las tarjetas que deben revisarse HOY
    """
    try:
        # Obtener palabras del diccionario que tengan ejemplo (son flashcards)
        all_words = await db.get_user_dictionary(current_user.id)
        
        # Filtrar las que tienen ejemplo
        flashcard_words = [w for w in all_words if w.get("example")]
        
        # Obtener datos SRS de cada una
        flashcards = []
        today = datetime.utcnow()
        
        for word in flashcard_words:
            word_id = word.get("id")
            
            # Obtener datos SRS (o crear si es nueva)
            srs_data = await db.get_flashcard_srs_data(word_id)
            
            if not srs_data:
                # Primera vez: crear datos SRS
                srs_data = {
                    "word_id": word_id,
                    "easiness_factor": 2.5,
                    "interval": 0,
                    "repetitions": 0,
                    "next_review_date": today,
                    "last_reviewed": None,
                    "times_reviewed": 0,
                    "times_correct": 0,
                    "times_incorrect": 0
                }
                await db.create_flashcard_srs_data(srs_data)
            
            # Verificar si toca revisar hoy
            next_review = srs_data.get("next_review_date", today)
            if isinstance(next_review, str):
                next_review = datetime.fromisoformat(next_review)
            
            if next_review.date() <= today.date():
                flashcards.append(FlashcardData(
                    id=srs_data.get("id", word_id),
                    word_id=word_id,
                    word=word.get("word"),
                    translation=word.get("translation"),
                    example=word.get("example"),
                    type=word.get("type", "word"),
                    easiness_factor=srs_data.get("easiness_factor", 2.5),
                    interval=srs_data.get("interval", 0),
                    repetitions=srs_data.get("repetitions", 0),
                    next_review_date=next_review,
                    last_reviewed=srs_data.get("last_reviewed"),
                    times_reviewed=srs_data.get("times_reviewed", 0),
                    times_correct=srs_data.get("times_correct", 0),
                    times_incorrect=srs_data.get("times_incorrect", 0)
                ))
        
        logger.info(f"📚 {len(flashcards)} tarjetas para revisar hoy")
        return flashcards
        
    except Exception as e:
        logger.error(f"❌ Error obteniendo flashcards: {e}")
        raise HTTPException(status_code=500, detail="Error loading flashcards")

@router.post("/review/{word_id}", response_model=FlashcardReviewResponse)
async def review_flashcard(
    word_id: str,
    review: FlashcardReviewRequest,
    current_user: User = Depends(get_current_user)
):
    """
    ✅ Registra el resultado de una revisión y calcula próxima fecha
    """
    try:
        # Obtener datos SRS actuales
        srs_data = await db.get_flashcard_srs_data(word_id)
        
        if not srs_data:
            raise HTTPException(status_code=404, detail="Flashcard not found")
        
        # Datos actuales
        current_ef = srs_data.get("easiness_factor", 2.5)
        current_reps = srs_data.get("repetitions", 0)
        current_interval = srs_data.get("interval", 0)
        
        # Calcular nuevos valores con SM-2
        new_reps, new_ef, new_interval = calculate_next_review(
            review.quality,
            current_reps,
            current_ef,
            current_interval
        )
        
        # Calcular próxima fecha
        next_review = datetime.utcnow() + timedelta(days=new_interval)
        
        # Actualizar estadísticas
        times_reviewed = srs_data.get("times_reviewed", 0) + 1
        times_correct = srs_data.get("times_correct", 0)
        times_incorrect = srs_data.get("times_incorrect", 0)
        
        if review.quality >= 3:
            times_correct += 1
        else:
            times_incorrect += 1
        
        # Actualizar en BD
        updated_data = {
            "easiness_factor": new_ef,
            "interval": new_interval,
            "repetitions": new_reps,
            "next_review_date": next_review,
            "last_reviewed": datetime.utcnow(),
            "times_reviewed": times_reviewed,
            "times_correct": times_correct,
            "times_incorrect": times_incorrect
        }
        
        await db.update_flashcard_srs_data(word_id, updated_data)
        
        # 🆕 ACTUALIZAR RACHA DEL USUARIO (mismo comportamiento que guardar palabra)
        await db.update_user_activity(current_user.id)
        
        # Mensaje de feedback
        if review.quality >= 4:
            message = f"¡Perfecto! La volverás a ver en {new_interval} días."
        elif review.quality == 3:
            message = f"Bien. Próxima revisión en {new_interval} días."
        else:
            message = f"Necesitas repasarla. La verás mañana."
        
        logger.info(f"✅ Flashcard revisada: {word_id} | Quality: {review.quality} | Next: {new_interval}d")
        
        return FlashcardReviewResponse(
            success=True,
            next_review_date=next_review,
            interval_days=new_interval,
            message=message
        )
        
    except Exception as e:
        logger.error(f"❌ Error revisando flashcard: {e}")
        raise HTTPException(status_code=500, detail="Error reviewing flashcard")

@router.get("/stats")
async def get_flashcard_stats(current_user: User = Depends(get_current_user)):
    """
    📊 Estadísticas de las flashcards del usuario
    """
    try:
        all_words = await db.get_user_dictionary(current_user.id)
        flashcard_words = [w for w in all_words if w.get("example")]
        
        total = len(flashcard_words)
        due_today = 0
        mastered = 0
        learning = 0
        
        today = datetime.utcnow().date()
        
        for word in flashcard_words:
            srs_data = await db.get_flashcard_srs_data(word.get("id"))
            
            if not srs_data:
                due_today += 1
                continue
            
            next_review = srs_data.get("next_review_date")
            if isinstance(next_review, str):
                next_review = datetime.fromisoformat(next_review).date()
            elif isinstance(next_review, datetime):
                next_review = next_review.date()
            
            if next_review <= today:
                due_today += 1
            
            reps = srs_data.get("repetitions", 0)
            if reps >= 5:
                mastered += 1
            else:
                learning += 1
        
        return {
            "total_flashcards": total,
            "due_today": due_today,
            "mastered": mastered,
            "learning": learning
        }
        
    except Exception as e:
        logger.error(f"❌ Error obteniendo stats: {e}")
        return {
            "total_flashcards": 0,
            "due_today": 0,
            "mastered": 0,
            "learning": 0
        }