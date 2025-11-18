def get_lyrics(song: str, level: str):
    letra_fake = """
    I see it, I see it, and now it's all within my reach
    Endless possibility
    I see it, I see it now; it's always been inside of me
    And now I feel so free, endless possibility
    """
    return {
        "song": song,
        "level": level,
        "lyrics": letra_fake.strip()
    }
