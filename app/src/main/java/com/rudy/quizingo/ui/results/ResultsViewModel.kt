package com.rudy.quizingo.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudy.quizingo.data.ModuleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Reads a module's persisted progress and renders it - used both right after a
 *  quiz finishes and when "Review" is tapped on an already-finished module, so
 *  there's exactly one Results implementation instead of a separate read-only one. */
class ResultsViewModel(
    private val moduleId: String,
    private val moduleRepository: ModuleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val progress = moduleRepository.getProgress(moduleId)
            _uiState.value = ResultsUiState(
                isLoading = false,
                correctCount = progress?.correctCount ?: 0,
                totalQuestions = progress?.totalQuestions ?: 0,
                bestStreak = progress?.bestStreak ?: 0,
                skippedCount = progress?.skippedCount ?: 0
            )
        }
    }

    /** Clears this module's persisted progress, then invokes [onCleared] so the
     *  caller can navigate into a fresh quiz attempt. */
    fun restart(onCleared: () -> Unit) {
        viewModelScope.launch {
            moduleRepository.clearProgress(moduleId)
            onCleared()
        }
    }
}
