package com.rudy.quizingo.data.remote

import com.rudy.quizingo.data.model.Question
import retrofit2.http.GET

interface QuizApiService {

    @GET("dr-samrat/53846277a8fcb034e482906ccc0d12b2/raw")
    suspend fun getQuestions(): List<Question>

    companion object {
        const val BASE_URL = "https://gist.githubusercontent.com/"
    }
}
