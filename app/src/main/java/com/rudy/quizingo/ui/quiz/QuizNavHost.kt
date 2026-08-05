package com.rudy.quizingo.ui.quiz

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rudy.quizingo.ui.quiz.components.ResultsScreen

private object QuizDestinations {
    const val QUIZ = "quiz"
    const val RESULTS = "results"
}

@Composable
fun QuizNavHost(
    viewModel: QuizViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = QuizDestinations.QUIZ,
        modifier = modifier
    ) {
        composable(QuizDestinations.QUIZ) {
            QuizScreen(
                viewModel = viewModel,
                onQuizFinished = {
                    // Pop QUIZ off the back stack: it's now in its finished state, so
                    // leaving it in place would make system back immediately re-fire
                    // onQuizFinished and bounce straight back to RESULTS.
                    navController.navigate(QuizDestinations.RESULTS) {
                        popUpTo(QuizDestinations.QUIZ) { inclusive = true }
                    }
                }
            )
        }
        composable(QuizDestinations.RESULTS) {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ResultsScreen(
                correctCount = state.correctCount,
                totalQuestions = state.totalQuestions,
                longestStreak = state.longestStreak,
                skippedCount = state.skippedCount,
                onRestart = {
                    viewModel.restart()
                    navController.navigate(QuizDestinations.QUIZ) {
                        popUpTo(QuizDestinations.RESULTS) { inclusive = true }
                    }
                }
            )
        }
    }
}
