package com.rudy.quizingo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.rudy.quizingo.data.QuizRepository
import com.rudy.quizingo.data.remote.NetworkModule
import com.rudy.quizingo.ui.quiz.QuizNavHost
import com.rudy.quizingo.ui.quiz.QuizViewModel
import com.rudy.quizingo.ui.quiz.QuizViewModelFactory
import com.rudy.quizingo.ui.theme.QuizingoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: QuizViewModel by viewModels {
        QuizViewModelFactory(QuizRepository(NetworkModule.quizApiService))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuizingoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    QuizNavHost(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}