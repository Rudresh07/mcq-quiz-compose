package com.rudy.quizingo.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rudy.quizingo.data.QuizRepository

class QuizViewModelFactory(
    private val repository: QuizRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(QuizViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return QuizViewModel(repository) as T
    }
}
