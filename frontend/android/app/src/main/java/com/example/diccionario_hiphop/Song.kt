package com.example.diccionario_hiphop

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val spotifyRank: Int,
    val lyrics: String,
    val terms: List<HipHopTerm>,
    val albumCover: String
)