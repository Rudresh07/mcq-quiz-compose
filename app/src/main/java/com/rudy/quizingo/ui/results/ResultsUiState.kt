package com.rudy.quizingo.ui.results

data class ResultsUiState(
    val isLoading: Boolean = true,
    val correctCount: Int = 0,
    val totalQuestions: Int = 0,
    val bestStreak: Int = 0,
    val skippedCount: Int = 0
)
