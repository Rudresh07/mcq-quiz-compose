package com.rudy.quizingo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rudy.quizingo.ui.modules.ModuleListScreen
import com.rudy.quizingo.ui.modules.ModuleListViewModel
import com.rudy.quizingo.ui.quiz.QuizScreen
import com.rudy.quizingo.ui.quiz.QuizViewModel
import com.rudy.quizingo.ui.quiz.components.LoadingSpinner
import com.rudy.quizingo.ui.quiz.components.ResultsScreen
import com.rudy.quizingo.ui.results.ResultsViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private object Destinations {
    const val MODULE_LIST = "moduleList"
    const val MODULE_ID_ARG = "moduleId"
    const val QUIZ = "quiz/{$MODULE_ID_ARG}"
    const val RESULTS = "results/{$MODULE_ID_ARG}"

    fun quiz(moduleId: String) = "quiz/$moduleId"
    fun results(moduleId: String) = "results/$moduleId"
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.MODULE_LIST,
        modifier = modifier
    ) {
        composable(Destinations.MODULE_LIST) {
            val viewModel = koinViewModel<ModuleListViewModel>()
            ModuleListScreen(
                viewModel = viewModel,
                // launchSingleTop guards against a double-tap on these buttons pushing
                // duplicate back-stack entries.
                onStart = { moduleId -> navController.navigate(Destinations.quiz(moduleId)) { launchSingleTop = true } },
                onResume = { moduleId -> navController.navigate(Destinations.quiz(moduleId)) { launchSingleTop = true } },
                onReview = { moduleId -> navController.navigate(Destinations.results(moduleId)) { launchSingleTop = true } },
                onRestart = { moduleId -> navController.navigate(Destinations.quiz(moduleId)) { launchSingleTop = true } }
            )
        }

        composable(
            route = Destinations.QUIZ,
            arguments = listOf(navArgument(Destinations.MODULE_ID_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString(Destinations.MODULE_ID_ARG).orEmpty()
            val viewModel = koinViewModel<QuizViewModel>(parameters = { parametersOf(moduleId) })
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // Quiz and Results are separate routes/ViewModels (Results reads persisted
            // progress rather than sharing the live QuizViewModel), so once the quiz
            // finishes we hand off to the results route and drop the quiz entry -
            // system back from Results then lands on Module List, not back into a
            // finished quiz.
            LaunchedEffect(state.isQuizFinished) {
                if (state.isQuizFinished) {
                    navController.navigate(Destinations.results(moduleId)) {
                        popUpTo(Destinations.QUIZ) { inclusive = true }
                    }
                }
            }

            if (!state.isQuizFinished) {
                QuizScreen(viewModel = viewModel)
            }
        }

        composable(
            route = Destinations.RESULTS,
            arguments = listOf(navArgument(Destinations.MODULE_ID_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val moduleId = backStackEntry.arguments?.getString(Destinations.MODULE_ID_ARG).orEmpty()
            val viewModel = koinViewModel<ResultsViewModel>(parameters = { parametersOf(moduleId) })
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            if (state.isLoading) {
                LoadingSpinner()
            } else {
                ResultsScreen(
                    correctCount = state.correctCount,
                    totalQuestions = state.totalQuestions,
                    longestStreak = state.bestStreak,
                    skippedCount = state.skippedCount,
                    onRestart = {
                        viewModel.restart {
                            navController.navigate(Destinations.quiz(moduleId)) {
                                popUpTo(Destinations.MODULE_LIST) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    },
                    onFinish = {
                        navController.popBackStack(Destinations.MODULE_LIST, inclusive = false)
                    }
                )
            }
        }
    }
}
