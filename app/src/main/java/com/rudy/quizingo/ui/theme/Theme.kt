package com.rudy.quizingo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val QuizDarkColorScheme = darkColorScheme(
    primary = QuizPrimary,
    onPrimary = QuizOnPrimary,
    primaryContainer = QuizPrimaryContainer,
    onPrimaryContainer = QuizOnPrimaryContainer,
    secondary = QuizSecondary,
    onSecondary = QuizOnSecondary,
    background = QuizBackground,
    onBackground = QuizOnBackground,
    surface = QuizSurface,
    onSurface = QuizOnBackground,
    surfaceVariant = QuizSurfaceVariant,
    onSurfaceVariant = QuizOnSurfaceVariant,
    outline = QuizOutline,
    error = QuizError,
    onError = QuizOnError
)

/**
 * Quizingo is a dark-only experience by design - the palette is tuned for it,
 * so we don't branch on system light/dark or apply Material You dynamic color.
 */
@Composable
fun QuizingoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QuizDarkColorScheme,
        typography = Typography,
        content = content
    )
}
