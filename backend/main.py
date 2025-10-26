from fastapi import FastAPI
import asyncio
from spotify import get_top10_playlist
from lyrics import get_lyrics
from cache import top10_cache

app = FastAPI()

@app.get("/")
def root():
    return {"message": "Servidor funcionando"}

@app.get("/lyrics")
def lyrics_endpoint(song: str, level: str):
    return get_lyrics(song, level)

@app.get("/top10")
def top10_endpoint():
    return {"top10": top10_cache}

@app.on_event("startup")
async def startup_event():
    async def actualizar_top10():
        while True:
            top10_cache.clear()
            top10_cache.extend(get_top10_playlist())
            await asyncio.sleep(3600)  # actualiza cada hora
    asyncio.create_task(actualizar_top10())
