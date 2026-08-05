package com.rudy.quizingo.ui.quiz

import com.rudy.quizingo.data.model.Question

/**
 * Single flat state holder for the whole quiz session. Deliberately not a
 * sealed hierarchy - this app has few enough states that plain nullable /
 * boolean fields read more easily than a Loading/Error/Success wrapper.
 */
data class QuizUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswered: Boolean = false,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val correctCount: Int = 0,
    val skippedCount: Int = 0,
    val celebrationMilestone: Int? = null,
    val isQuizFinished: Boolean = false
) {
    val currentQuestion: Question?
        get() = questions.getOrNull(currentQuestionIndex)

    val totalQuestions: Int
        get() = questions.size
}
