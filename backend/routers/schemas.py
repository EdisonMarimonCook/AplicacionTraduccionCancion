from typing import List, Optional
from pydantic import BaseModel, Field
from datetime import datetime

# ---------- Dictionary item schemas ----------

class DictItemCreate(BaseModel):
    word: str
    translation: Optional[str] = None
    source_lang: str = "en"
    target_lang: str = "es"
    type: str = "word"
    level: Optional[str] = None
    example: Optional[str] = None
    notes: Optional[str] = None
    tags: List[str] = []

class DictItemOut(BaseModel):
    id: str
    word: str
    translation: Optional[str] = None
    source_lang: str = "en"
    target_lang: str = "es"
    type: str = "word"
    level: str = "A1"
    example: Optional[str] = None
    notes: Optional[str] = None
    tags: List[str] = []
    created_at: datetime
    
# ---------- OpenAI request/response schemas ----------
    
class AnalyzeRequest(BaseModel):
    lyrics: str
    level: str
    
class TranslateWordRequest(BaseModel):
    word: str
    
    """
    De momento he definido los idiomas por defecto aquí para el prototipo
    si Rioja quiere más idiomas modifico esto
    
    """
    source_lang: str = "en"
    target_lang: str = "es"