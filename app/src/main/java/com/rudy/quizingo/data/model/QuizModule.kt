package com.rudy.quizingo.data.model

import com.google.gson.annotations.SerializedName

data class QuizModule(
    val id: String,
    val title: String,
    val description: String,
    @SerializedName("questions_url") val questionsUrl: String
)
