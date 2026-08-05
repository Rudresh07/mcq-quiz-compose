package com.rudy.quizingo.ui.quiz.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rudy.quizingo.ui.theme.AnswerCorrect
import com.rudy.quizingo.ui.theme.AnswerIncorrect
import com.rudy.quizingo.ui.theme.QuizingoTheme

enum class AnswerOptionVisualState { NEUTRAL, CORRECT, INCORRECT_SELECTED, DIMMED }

@Composable
fun AnswerOption(
    label: Char,
    text: String,
    state: AnswerOptionVisualState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            AnswerOptionVisualState.NEUTRAL -> MaterialTheme.colorScheme.outline
            AnswerOptionVisualState.CORRECT -> AnswerCorrect
            AnswerOptionVisualState.INCORRECT_SELECTED -> AnswerIncorrect
            AnswerOptionVisualState.DIMMED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        label = "answerBorderColor"
    )
    val containerColor = when (state) {
        AnswerOptionVisualState.CORRECT -> AnswerCorrect.copy(alpha = 0.12f)
        AnswerOptionVisualState.INCORRECT_SELECTED -> AnswerIncorrect.copy(alpha = 0.12f)
        AnswerOptionVisualState.DIMMED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        AnswerOptionVisualState.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (state) {
        AnswerOptionVisualState.CORRECT -> AnswerCorrect
        AnswerOptionVisualState.INCORRECT_SELECTED -> AnswerIncorrect
        AnswerOptionVisualState.DIMMED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        AnswerOptionVisualState.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val description = when (state) {
        AnswerOptionVisualState.CORRECT -> "Option $label: $text, correct answer"
        AnswerOptionVisualState.INCORRECT_SELECTED -> "Option $label: $text, your answer, incorrect"
        else -> "Option $label: $text"
    }
    val borderWidth = if (state == AnswerOptionVisualState.NEUTRAL || state == AnswerOptionVisualState.DIMMED) 1.dp else 2.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color = contentColor.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp)
            )
            if (state == AnswerOptionVisualState.CORRECT || state == AnswerOptionVisualState.INCORRECT_SELECTED) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(color = borderColor, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state == AnswerOptionVisualState.CORRECT) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1016)
@Composable
private fun AnswerOptionPreview() {
    QuizingoTheme {
        Column {
            AnswerOption('A', "Flappy Bird–style game", AnswerOptionVisualState.NEUTRAL, true, {})
            Spacer(Modifier.height(12.dp))
            AnswerOption('B', "Virtual pet", AnswerOptionVisualState.CORRECT, false, {})
            Spacer(Modifier.height(12.dp))
            AnswerOption('C', "Hidden performance menu", AnswerOptionVisualState.INCORRECT_SELECTED, false, {})
            Spacer(Modifier.height(12.dp))
            AnswerOption('D', "System UI tuner", AnswerOptionVisualState.DIMMED, false, {})
        }
    }
}
