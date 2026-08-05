package com.rudy.quizingo.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudy.quizingo.data.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuizViewModel(
    private val repository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        loadQuestions()
    }

    fun loadQuestions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getQuestions()
                .onSuccess { questions ->
                    _uiState.update { it.copy(isLoading = false, questions = questions) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, error = throwable.message ?: "Couldn't load questions")
                    }
                }
        }
    }

    /** Records the tapped option, reveals correct/incorrect, and updates streak/score. */
    fun selectAnswer(optionIndex: Int) {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (state.isAnswered) return

        val isCorrect = optionIndex == question.correctOptionIndex
        val newStreak = if (isCorrect) state.streak + 1 else 0
        val hitMilestone = isCorrect && newStreak in MILESTONES

        _uiState.update {
            it.copy(
                selectedOptionIndex = optionIndex,
                isAnswered = true,
                streak = newStreak,
                longestStreak = maxOf(it.longestStreak, newStreak),
                correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount,
                celebrationMilestone = if (hitMilestone) newStreak else null
            )
        }
    }

    /** Skip is neutral: no reveal, no streak change, advances right away. */
    fun skip() {
        _uiState.update { it.copy(skippedCount = it.skippedCount + 1) }
        advanceToNext()
    }

    fun advanceToNext() {
        _uiState.update { state ->
            val nextIndex = state.currentQuestionIndex + 1
            if (nextIndex >= state.questions.size) {
                state.copy(isQuizFinished = true)
            } else {
                state.copy(
                    currentQuestionIndex = nextIndex,
                    selectedOptionIndex = null,
                    isAnswered = false,
                    celebrationMilestone = null
                )
            }
        }
    }

    /** Restarts the run using the already-fetched questions - no re-fetch. */
    fun restart() {
        _uiState.update { QuizUiState(isLoading = false, questions = it.questions) }
    }

    companion object {
        private val MILESTONES = setOf(3, 5, 10)
    }
}
