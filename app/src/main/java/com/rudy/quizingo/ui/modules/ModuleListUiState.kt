package com.rudy.quizingo.ui.modules

import com.rudy.quizingo.data.ModuleWithProgress

data class ModuleListUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val items: List<ModuleWithProgress> = emptyList()
)
