package com.rudy.quizingo.ui.quiz.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rudy.quizingo.ui.theme.QuizingoTheme

/**
 * In-flow loading indicator for content that's already inside the app - a module's
 * questions, a module's saved results. The app's own branding (icon, name) is the
 * platform splash screen's job at cold start (see MainActivity.installSplashScreen);
 * screens the user reaches by tapping something just need a quiet spinner, not a
 * second splash moment.
 */
@Composable
fun LoadingSpinner(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1016)
@Composable
private fun LoadingSpinnerPreview() {
    QuizingoTheme {
        LoadingSpinner()
    }
}
