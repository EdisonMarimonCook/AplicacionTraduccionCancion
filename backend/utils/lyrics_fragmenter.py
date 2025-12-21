"""
MÓDULO: Fragmentador de Letras
PROPÓSITO: Dividir letras en fragmentos lógicos (versos, coros, etc)
UTILIDAD: Base para análisis IA, display y sincronización de audio
"""

import logging
from typing import List, Dict
import re

logger = logging.getLogger(__name__)

# ===============================================================================
# CONSTANTES
# ===============================================================================

VERSE_MARKERS = ["[Verse", "[V", "Verse"]
CHORUS_MARKERS = ["[Chorus", "[Chorus]", "[Ch", "Chorus"]
BRIDGE_MARKERS = ["[Bridge", "[B", "Bridge"]
PRE_CHORUS_MARKERS = ["[Pre-Chorus", "[Pre", "Pre-Chorus"]
OUTRO_MARKERS = ["[Outro", "[O", "Outro"]
INTRO_MARKERS = ["[Intro", "[I", "Intro"]

# ===============================================================================
# FRAGMENTADOR PRINCIPAL
# ===============================================================================

def fragment_lyrics(
    lyrics: str,
    max_lines_per_fragment: int = 4,
    min_lines_per_fragment: int = 2
) -> List[Dict]:
    """
    Divide la letra en fragmentos lógicos.
    
    PARÁMETROS:
    - lyrics: Letra completa (string)
    - max_lines_per_fragment: Máximo de líneas por fragmento (default: 4)
    - min_lines_per_fragment: Mínimo de líneas por fragmento (default: 2)
    
    RETORNA:
    [
        {
            "id": "frag_0",
            "type": "verse",  # verse, chorus, bridge, pre-chorus, intro, outro
            "text": "I can't sleep until I feel your touch...",
            "lines": ["I can't sleep until", "I feel your touch"],
            "line_count": 2,
            "start_time": 0,
            "end_time": 5,
            "duration": 5
        },
        ...
    ]
    
    LÓGICA:
    1. Detecta secciones (verso, coro, etc)
    2. Divide en fragmentos de X líneas
    3. Estima tiempos de audio (aprox 2.5 seg por línea)
    4. Retorna lista de fragmentos
    """
    
    if not lyrics or not isinstance(lyrics, str):
        logger.warning("⚠️  Letra vacía o inválida")
        return []
    
    logger.info(f"📝 Fragmentando letra ({len(lyrics)} caracteres)")
    
    # ===== PASO 1: Limpiar y parsear líneas =====
    lines = [line.strip() for line in lyrics.split("\n") if line.strip()]
    
    if not lines:
        logger.warning("⚠️  No hay líneas en la letra")
        return []
    
    logger.info(f"📊 Total de líneas: {len(lines)}")
    
    # ===== PASO 2: Detectar secciones =====
    sections = _detect_sections(lines)
    logger.info(f"🏷️  Secciones detectadas: {len(sections)}")
    
    # ===== PASO 3: Crear fragmentos =====
    fragments = _create_fragments(
        lines,
        sections,
        max_lines_per_fragment,
        min_lines_per_fragment
    )
    
    logger.info(f"✅ Fragmentos creados: {len(fragments)}")
    
    return fragments


# ===============================================================================
# DETECTAR SECCIONES (VERSO, CORO, BRIDGE, ETC)
# ===============================================================================

def _detect_sections(lines: List[str]) -> List[Dict]:
    """
    Detecta secciones de la letra (verso, coro, etc).
    
    RETORNA:
    [
        {"line_index": 0, "type": "intro", "name": "[Intro]"},
        {"line_index": 5, "type": "verse", "name": "[Verse 1]"},
        {"line_index": 10, "type": "chorus", "name": "[Chorus]"},
        ...
    ]
    """
    
    sections = []
    
    for idx, line in enumerate(lines):
        section_type = _identify_section_type(line)
        
        if section_type:
            sections.append({
                "line_index": idx,
                "type": section_type,
                "name": line
            })
    
    # Si no hay secciones detectadas, asumimos que todo es verso
    if not sections:
        logger.warning("⚠️  No se detectaron secciones, asumiendo todo es verso")
        sections.append({
            "line_index": 0,
            "type": "verse",
            "name": "[Verse]"
        })
    
    return sections


def _identify_section_type(line: str) -> str:
    """
    Identifica el tipo de sección basado en el texto de la línea.
    
    RETORNA: "verse", "chorus", "bridge", "pre-chorus", "intro", "outro" o None
    """
    
    line_lower = line.lower()
    
    # Buscar patrones
    if any(marker.lower() in line_lower for marker in INTRO_MARKERS):
        return "intro"
    elif any(marker.lower() in line_lower for marker in VERSE_MARKERS):
        return "verse"
    elif any(marker.lower() in line_lower for marker in PRE_CHORUS_MARKERS):
        return "pre-chorus"
    elif any(marker.lower() in line_lower for marker in CHORUS_MARKERS):
        return "chorus"
    elif any(marker.lower() in line_lower for marker in BRIDGE_MARKERS):
        return "bridge"
    elif any(marker.lower() in line_lower for marker in OUTRO_MARKERS):
        return "outro"
    
    return None


# ===============================================================================
# CREAR FRAGMENTOS
# ===============================================================================

def _create_fragments(
    lines: List[str],
    sections: List[Dict],
    max_lines: int,
    min_lines: int
) -> List[Dict]:
    """
    Crea fragmentos basados en líneas y secciones.
    
    LÓGICA:
    1. Agrupa líneas por sección
    2. Divide cada sección en fragmentos de X líneas
    3. Calcula tiempos aproximados
    4. Asigna IDs únicos
    """
    
    fragments = []
    current_time = 0  # Segundos
    time_per_line = 2.5  # Aproximadamente 2.5 segundos por línea
    fragment_id = 0
    
    # Procesar cada sección
    for section_idx, section in enumerate(sections):
        section_start_line = section["line_index"]
        
        # Determinar dónde termina esta sección
        if section_idx < len(sections) - 1:
            section_end_line = sections[section_idx + 1]["line_index"]
        else:
            section_end_line = len(lines)
        
        # Obtener líneas de esta sección (excluyendo el marcador)
        section_lines = lines[section_start_line + 1 : section_end_line]
        
        # Filtrar líneas vacías
        section_lines = [line for line in section_lines if line.strip()]
        
        if not section_lines:
            continue
        
        # ===== Dividir sección en fragmentos =====
        for i in range(0, len(section_lines), max_lines):
            fragment_lines = section_lines[i : i + max_lines]
            
            # Respetar mínimo de líneas
            if len(fragment_lines) < min_lines and i + max_lines < len(section_lines):
                continue
            
            # Calcular tiempos
            start_time = int(current_time)
            duration = len(fragment_lines) * time_per_line
            end_time = int(current_time + duration)
            
            # Crear fragmento
            fragment = {
                "id": f"frag_{fragment_id}",
                "type": section["type"],
                "section_name": section["name"],
                "text": "\n".join(fragment_lines),
                "lines": fragment_lines,
                "line_count": len(fragment_lines),
                "start_time": start_time,
                "end_time": end_time,
                "duration": duration
            }
            
            fragments.append(fragment)
            current_time += duration
            fragment_id += 1
    
    return fragments


# ===============================================================================
# FUNCIONES AUXILIARES
# ===============================================================================

def get_fragment_by_id(fragments: List[Dict], fragment_id: str) -> Dict:
    """
    Obtiene un fragmento específico por su ID.
    
    PARÁMETROS:
    - fragments: Lista de fragmentos
    - fragment_id: ID del fragmento (ej: "frag_0")
    
    RETORNA: Fragmento o None
    """
    for fragment in fragments:
        if fragment["id"] == fragment_id:
            return fragment
    return None


def get_fragments_by_type(fragments: List[Dict], section_type: str) -> List[Dict]:
    """
    Obtiene todos los fragmentos de un tipo específico.
    
    PARÁMETROS:
    - fragments: Lista de fragmentos
    - section_type: Tipo de sección ("verse", "chorus", etc)
    
    RETORNA: Lista de fragmentos del tipo especificado
    """
    return [f for f in fragments if f["type"] == section_type]


def get_fragment_by_time(fragments: List[Dict], time_seconds: float) -> Dict:
    """
    Obtiene el fragmento que corresponde a un tiempo específico.
    
    PARÁMETROS:
    - fragments: Lista de fragmentos
    - time_seconds: Tiempo en segundos (ej: 5.5)
    
    RETORNA: Fragmento o None
    """
    for fragment in fragments:
        if fragment["start_time"] <= time_seconds < fragment["end_time"]:
            return fragment
    return None


def merge_fragments(fragments: List[Dict], merge_count: int = 2) -> List[Dict]:
    """
    Fusiona fragmentos consecutivos (útil si quedan muy pequeños).
    
    PARÁMETROS:
    - fragments: Lista de fragmentos
    - merge_count: Cuántos fragmentos fusionar
    
    RETORNA: Lista de fragmentos fusionados
    """
    if not fragments or merge_count < 1:
        return fragments
    
    merged = []
    current_batch = []
    
    for fragment in fragments:
        current_batch.append(fragment)
        
        if len(current_batch) == merge_count:
            # Fusionar fragmentos
            merged_fragment = {
                "id": f"frag_{len(merged)}",
                "type": current_batch[0]["type"],
                "section_name": current_batch[0]["section_name"],
                "text": "\n".join([f["text"] for f in current_batch]),
                "lines": [line for f in current_batch for line in f["lines"]],
                "line_count": sum(f["line_count"] for f in current_batch),
                "start_time": current_batch[0]["start_time"],
                "end_time": current_batch[-1]["end_time"],
                "duration": current_batch[-1]["end_time"] - current_batch[0]["start_time"]
            }
            merged.append(merged_fragment)
            current_batch = []
    
    # Agregar fragmentos restantes
    if current_batch:
        merged_fragment = {
            "id": f"frag_{len(merged)}",
            "type": current_batch[0]["type"],
            "section_name": current_batch[0]["section_name"],
            "text": "\n".join([f["text"] for f in current_batch]),
            "lines": [line for f in current_batch for line in f["lines"]],
            "line_count": sum(f["line_count"] for f in current_batch),
            "start_time": current_batch[0]["start_time"],
            "end_time": current_batch[-1]["end_time"],
            "duration": current_batch[-1]["end_time"] - current_batch[0]["start_time"]
        }
        merged.append(merged_fragment)
    
    return merged


# ===============================================================================
# EJEMPLO DE USO
# ===============================================================================

if __name__ == "__main__":
    # Test
    ejemplo_letra = """
[Intro]
(Mmm, yeah)

[Verse 1]
I can't sleep until I feel your touch
And how long will this take?
Can you hurry up and finish?
So we can go back to the top of the morning with you?

[Pre-Chorus]
Said I'll be waiting for ya
Baby, how you doin'?

[Chorus]
I said, ooh, I'm blinded by the lights
No, I can't sleep until I feel your touch
And every night this dream's the same
Every night they scream my name

[Verse 2]
Out my window, good God, I'm scared
Now I'm so numb, I can't feel my face
Is there somebody calling me?

[Bridge]
I'm afraid of the light
But I'm more afraid of the dark

[Outro]
(Yeah, yeah, yeah)
"""
    
    fragments = fragment_lyrics(ejemplo_letra)
    
    print("\n" + "="*80)
    print(f"TOTAL FRAGMENTOS: {len(fragments)}")
    print("="*80 + "\n")
    
    for frag in fragments:
        print(f"🏷️  {frag['id']} - {frag['type'].upper()}")
        print(f"   Líneas: {frag['line_count']} | Tiempo: {frag['start_time']}s - {frag['end_time']}s")
        print(f"   Texto: {frag['text'][:50]}...")
        print()