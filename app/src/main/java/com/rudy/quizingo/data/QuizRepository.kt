package com.rudy.quizingo.data

import com.rudy.quizingo.data.model.Question
import com.rudy.quizingo.data.remote.QuizApiService

class QuizRepository(
    private val apiService: QuizApiService
) {
    suspend fun getQuestions(): Result<List<Question>> = runCatching {
        apiService.getQuestions()
    }
}
