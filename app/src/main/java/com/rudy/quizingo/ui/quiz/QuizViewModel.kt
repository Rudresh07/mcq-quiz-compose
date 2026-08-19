package com.rudy.quizingo.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudy.quizingo.data.ModuleRepository
import com.rudy.quizingo.data.model.ModuleStatus
import com.rudy.quizingo.data.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuizViewModel(
    private val moduleId: String,
    private val moduleRepository: ModuleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadQuestions()
    }

    /** Cancels any in-flight load first - otherwise a slow first request finishing after
     *  a Retry-triggered second one could overwrite the newer result with a stale one. */
    fun loadQuestions() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            moduleRepository.getQuestions(moduleId)
                .onSuccess { questions ->
                    val progress = moduleRepository.getProgress(moduleId)
                    _uiState.update {
                        if (progress != null && progress.status == ModuleStatus.PAUSED) {
                            it.copy(
                                isLoading = false,
                                questions = questions,
                                currentQuestionIndex = progress.currentQuestionIndex,
                                streak = progress.currentStreak,
                                longestStreak = progress.bestStreak,
                                correctCount = progress.correctCount,
                                skippedCount = progress.skippedCount
                            )
                        } else {
                            it.copy(isLoading = false, questions = questions)
                        }
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, error = throwable.toUserMessage())
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
        // Persist the answer's effect on score/streak right away - advanceToNext() only runs
        // after a multi-second reveal delay, and a process death in that window would
        // otherwise lose this answer's contribution to the score.
        persistProgress(nextQuestionIndex = state.currentQuestionIndex + 1)
    }

    /** Skip is neutral: no reveal, no streak change, advances right away. */
    fun skip() {
        _uiState.update { it.copy(skippedCount = it.skippedCount + 1) }
        advanceToNext()
    }

    /** Advances to the next question (or finishes the run), then persists the new
     *  position as PAUSED - or FINISHED once there's nothing left to advance to. */
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
        persistProgress()
    }

    /** [nextQuestionIndex] defaults to the (already up to date) current index for the normal
     *  advance/skip/finish path; [selectAnswer] passes the not-yet-applied next index instead,
     *  since at that point the UI is still showing the just-answered question mid-reveal. */
    private fun persistProgress(nextQuestionIndex: Int = _uiState.value.currentQuestionIndex) {
        val state = _uiState.value
        val isFinished = state.isQuizFinished || nextQuestionIndex >= state.totalQuestions
        val status = if (isFinished) ModuleStatus.FINISHED else ModuleStatus.PAUSED
        viewModelScope.launch {
            moduleRepository.saveProgress(
                moduleId = moduleId,
                status = status,
                correctCount = state.correctCount,
                totalQuestions = state.totalQuestions,
                bestStreak = state.longestStreak,
                skippedCount = state.skippedCount,
                currentQuestionIndex = nextQuestionIndex,
                currentStreak = state.streak
            )
        }
    }

    companion object {
        private val MILESTONES = setOf(3, 5, 10)
    }
}
