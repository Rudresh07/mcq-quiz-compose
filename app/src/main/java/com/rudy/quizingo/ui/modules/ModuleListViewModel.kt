package com.rudy.quizingo.ui.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudy.quizingo.data.ModuleRepository
import com.rudy.quizingo.data.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ModuleListViewModel(
    private val moduleRepository: ModuleRepository
) : ViewModel() {

    private val isLoading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ModuleListUiState> = combine(
        moduleRepository.modulesWithProgress,
        isLoading,
        error
    ) { items, loading, err ->
        ModuleListUiState(isLoading = loading, error = err, items = items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = ModuleListUiState()
    )

    private var loadJob: Job? = null

    init {
        loadModules()
    }

    /** Cancels any in-flight load first - otherwise a slow first request finishing after
     *  a Retry-triggered second one could overwrite the newer result with a stale one. */
    fun loadModules() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading.value = true
            error.value = null
            moduleRepository.refreshModules()
                .onFailure { error.value = it.toUserMessage() }
            isLoading.value = false
        }
    }

    /** Clears [moduleId]'s persisted progress, then invokes [onCleared] so the
     *  caller can navigate into a fresh quiz attempt. */
    fun restartModule(moduleId: String, onCleared: () -> Unit) {
        viewModelScope.launch {
            moduleRepository.clearProgress(moduleId)
            onCleared()
        }
    }
}
