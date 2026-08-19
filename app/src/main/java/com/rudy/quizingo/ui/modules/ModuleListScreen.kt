package com.rudy.quizingo.ui.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudy.quizingo.data.ModuleWithProgress
import com.rudy.quizingo.data.model.ModuleStatus
import com.rudy.quizingo.data.model.QuizModule
import com.rudy.quizingo.ui.modules.components.ModuleCard
import com.rudy.quizingo.ui.quiz.components.ErrorContent
import com.rudy.quizingo.ui.quiz.components.LoadingSpinner
import com.rudy.quizingo.ui.theme.QuizingoTheme

@Composable
fun ModuleListScreen(
    viewModel: ModuleListViewModel,
    onStart: (String) -> Unit,
    onResume: (String) -> Unit,
    onReview: (String) -> Unit,
    onRestart: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Restart discards a module's saved score/streak - route every tap through a
    // confirmation instead of calling straight into viewModel.restartModule().
    var pendingRestartModuleId by remember { mutableStateOf<String?>(null) }

    ModuleListScreenContent(
        state = state,
        onStart = onStart,
        onResume = onResume,
        onReview = onReview,
        onRestart = { moduleId -> pendingRestartModuleId = moduleId },
        onRetry = viewModel::loadModules,
        modifier = modifier
    )

    val moduleId = pendingRestartModuleId
    if (moduleId != null) {
        RestartConfirmationDialog(
            onConfirm = {
                pendingRestartModuleId = null
                viewModel.restartModule(moduleId) { onRestart(moduleId) }
            },
            onDismiss = { pendingRestartModuleId = null }
        )
    }
}

@Composable
private fun RestartConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restart this module?") },
        text = { Text("This clears your saved score and streak for this module and starts a fresh attempt. This can't be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Restart") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ModuleListScreenContent(
    state: ModuleListUiState,
    onStart: (String) -> Unit,
    onResume: (String) -> Unit,
    onReview: (String) -> Unit,
    onRestart: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Modules",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
        )

        when {
            state.isLoading -> LoadingSpinner(modifier = Modifier.fillMaxSize())
            state.error != null -> ErrorContent(
                message = state.error,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize()
            )
            state.items.isEmpty() -> EmptyModulesContent(
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize()
            )
            // Adaptive instead of an explicit portrait/landscape switch: a phone in portrait
            // only fits one 340dp column, landscape and tablets naturally get two or more,
            // so the list uses the extra width instead of stretching cards edge to edge.
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.items, key = { it.module.id }) { item ->
                    ModuleCard(
                        item = item,
                        onStart = { onStart(item.module.id) },
                        onResume = { onResume(item.module.id) },
                        onReview = { onReview(item.module.id) },
                        onRestart = { onRestart(item.module.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyModulesContent(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "No modules yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "There's nothing to quiz on right now. Check back soon.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text("Refresh")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1016)
@Composable
private fun ModuleListScreenPreview() {
    QuizingoTheme {
        ModuleListScreenContent(
            state = ModuleListUiState(
                isLoading = false,
                items = listOf(
                    ModuleWithProgress(
                        module = QuizModule(
                            id = "android_basics",
                            title = "Android Basics",
                            description = "Fundamentals of Android development",
                            questionsUrl = ""
                        ),
                        status = ModuleStatus.FINISHED,
                        correctCount = 8,
                        totalQuestions = 10,
                        bestStreak = 5,
                        skippedCount = 1,
                        lastAttemptTimestamp = System.currentTimeMillis()
                    ),
                    ModuleWithProgress(
                        module = QuizModule(
                            id = "jetpack_compose",
                            title = "Jetpack Compose",
                            description = "Modern UI toolkit for Android",
                            questionsUrl = ""
                        ),
                        status = ModuleStatus.NOT_STARTED,
                        correctCount = 0,
                        totalQuestions = 10,
                        bestStreak = 0,
                        skippedCount = 0,
                        lastAttemptTimestamp = null
                    )
                )
            ),
            onStart = {},
            onResume = {},
            onReview = {},
            onRestart = {},
            onRetry = {}
        )
    }
}
