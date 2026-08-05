package com.rudy.quizingo.ui.quiz.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rudy.quizingo.data.model.Question
import com.rudy.quizingo.ui.theme.AnswerCorrect
import com.rudy.quizingo.ui.theme.AnswerIncorrect
import com.rudy.quizingo.ui.theme.QuizSecondary
import com.rudy.quizingo.ui.theme.QuizingoTheme

@Composable
fun QuestionContent(
    question: Question,
    questionNumber: Int,
    totalQuestions: Int,
    streak: Int,
    selectedOptionIndex: Int?,
    isAnswered: Boolean,
    onOptionSelected: (Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUESTION $questionNumber OF $totalQuestions",
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (streak > 0) {
                    StreakBadge(streak = streak)
                }
            }
            LinearProgressIndicator(
                progress = { questionNumber / totalQuestions.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Text(
            text = question.question,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            question.options.forEachIndexed { index, optionText ->
                val state = when {
                    !isAnswered -> AnswerOptionVisualState.NEUTRAL
                    index == question.correctOptionIndex -> AnswerOptionVisualState.CORRECT
                    index == selectedOptionIndex -> AnswerOptionVisualState.INCORRECT_SELECTED
                    else -> AnswerOptionVisualState.DIMMED
                }
                AnswerOption(
                    label = 'A' + index,
                    text = optionText,
                    state = state,
                    enabled = !isAnswered,
                    onClick = { onOptionSelected(index) }
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (isAnswered) {
                val isCorrect = selectedOptionIndex == question.correctOptionIndex
                AnswerResultChip(isCorrect = isCorrect)
            } else {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip Question", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun StreakBadge(
    streak: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = QuizSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = streak.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnswerResultChip(
    isCorrect: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isCorrect) AnswerCorrect else AnswerIncorrect
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = if (isCorrect) "Correct" else "Incorrect",
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1016)
@Composable
private fun QuestionContentUnansweredPreview() {
    QuizingoTheme {
        QuestionContent(
            question = previewQuestion,
            questionNumber = 3,
            totalQuestions = 10,
            streak = 2,
            selectedOptionIndex = null,
            isAnswered = false,
            onOptionSelected = {},
            onSkip = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1016)
@Composable
private fun QuestionContentAnsweredWrongPreview() {
    QuizingoTheme {
        QuestionContent(
            question = previewQuestion,
            questionNumber = 3,
            totalQuestions = 10,
            streak = 0,
            selectedOptionIndex = 1,
            isAnswered = true,
            onOptionSelected = {},
            onSkip = {}
        )
    }
}

private val previewQuestion = Question(
    id = 1,
    question = "What hidden feature do recent Android versions reveal when you tap the version number multiple times in Settings?",
    options = listOf(
        "Flappy Bird–style game",
        "Virtual pet",
        "Hidden performance menu",
        "System UI tuner"
    ),
    correctOptionIndex = 0
)
