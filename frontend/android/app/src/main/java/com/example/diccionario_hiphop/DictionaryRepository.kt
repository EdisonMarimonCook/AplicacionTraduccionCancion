package com.example.diccionario_hiphop

import android.content.Context
import retrofit2.Response

class DictionaryRepository(context: Context) {
    private val apiService = RetrofitService.getInstance(context)

    suspend fun getDictionary(type: String? = null): Response<List<UserWord>> {
        return apiService.getDictionary(type)
    }
}