package com.rudy.quizingo.data.remote

import com.rudy.quizingo.data.model.Question
import com.rudy.quizingo.data.model.QuizModule
import retrofit2.http.GET
import retrofit2.http.Url

interface QuizApiService {

    @GET("dr-samrat/ee986f16da9d8303c1acfd364ece22c5/raw")
    suspend fun getModules(): List<QuizModule>

    @GET
    suspend fun getQuestions(@Url questionsUrl: String): List<Question>

    companion object {
        const val BASE_URL = "https://gist.githubusercontent.com/"
    }
}
