package com.rudy.quizingo.ui.modules.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rudy.quizingo.data.ModuleWithProgress
import com.rudy.quizingo.data.model.ModuleStatus
import com.rudy.quizingo.data.model.QuizModule
import com.rudy.quizingo.ui.theme.AnswerCorrect
import com.rudy.quizingo.ui.theme.QuizSecondary
import com.rudy.quizingo.ui.theme.QuizingoTheme
import java.util.concurrent.TimeUnit

@Composable
fun ModuleCard(
    item: ModuleWithProgress,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onReview: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(item.module.id) { mutableStateOf(false) }
    val statusColor = statusColor(item.status)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = if (item.status == ModuleStatus.NOT_STARTED) 0.3f else 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // Only the details disclosure (stats below) is gated by expand/collapse -
                    // the action buttons are always visible so Start/Resume/Review is never
                    // more than one tap away.
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(color = statusColor)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.module.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = statusLabel(item.status),
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    summaryLine(item)?.let { summary ->
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Hide details" else "Show details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded && item.status != ModuleStatus.NOT_STARTED) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    DetailRow(label = "Best Streak", value = item.bestStreak.toString())
                    DetailRow(label = "Skipped", value = item.skippedCount.toString())
                    item.lastAttemptTimestamp?.let {
                        DetailRow(label = "Last Attempt", value = formatRelativeTime(it))
                    }
                }
            }

            ActionRow(
                status = item.status,
                onStart = onStart,
                onResume = onResume,
                onReview = onReview,
                onRestart = onRestart,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(14.dp)
            .background(color = color, shape = CircleShape)
    )
}

@Composable
private fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ActionRow(
    status: ModuleStatus,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onReview: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (status) {
            ModuleStatus.NOT_STARTED -> PrimaryButton(text = "Start", onClick = onStart, modifier = Modifier.weight(1f))
            ModuleStatus.PAUSED -> {
                PrimaryButton(text = "Resume", onClick = onResume, modifier = Modifier.weight(1f))
                SecondaryButton(text = "Restart", onClick = onRestart, modifier = Modifier.weight(1f))
            }
            ModuleStatus.FINISHED -> {
                PrimaryButton(text = "Review", onClick = onReview, modifier = Modifier.weight(1f))
                SecondaryButton(text = "Restart", onClick = onRestart, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

private fun statusColor(status: ModuleStatus) = when (status) {
    ModuleStatus.NOT_STARTED -> QuizSecondary.copy(alpha = 0.5f)
    ModuleStatus.PAUSED -> QuizSecondary
    ModuleStatus.FINISHED -> AnswerCorrect
}

private fun statusLabel(status: ModuleStatus) = when (status) {
    ModuleStatus.NOT_STARTED -> "START"
    ModuleStatus.PAUSED -> "PAUSED"
    ModuleStatus.FINISHED -> "FINISHED"
}

/** Null for a never-attempted module: [ModuleWithProgress.totalQuestions] is only a
 *  placeholder default until the module's real question list has been fetched (which
 *  only happens once the quiz is actually started), so there's nothing accurate to show. */
private fun summaryLine(item: ModuleWithProgress): String? = when (item.status) {
    ModuleStatus.NOT_STARTED -> null
    else -> "${item.totalQuestions} Questions | Score: ${item.correctCount}/${item.totalQuestions}"
}

private fun formatRelativeTime(epochMillis: Long): String {
    val diffMs = System.currentTimeMillis() - epochMillis
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
        days < 7 -> "$days day${if (days == 1L) "" else "s"} ago"
        else -> "${days / 7} week${if (days / 7 == 1L) "" else "s"} ago"
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1016)
@Composable
private fun ModuleCardNotStartedPreview() {
    QuizingoTheme {
        ModuleCard(
            item = ModuleWithProgress(
                module = QuizModule(
                    id = "photosynthesis",
                    title = "Photosynthesis",
                    description = "Plant biology basics",
                    questionsUrl = ""
                ),
                status = ModuleStatus.NOT_STARTED,
                correctCount = 0,
                totalQuestions = 10,
                bestStreak = 0,
                skippedCount = 0,
                lastAttemptTimestamp = null
            ),
            onStart = {},
            onResume = {},
            onReview = {},
            onRestart = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1016)
@Composable
private fun ModuleCardPausedPreview() {
    QuizingoTheme {
        ModuleCard(
            item = ModuleWithProgress(
                module = QuizModule(
                    id = "cellular_respiration",
                    title = "Cellular Respiration",
                    description = "",
                    questionsUrl = ""
                ),
                status = ModuleStatus.PAUSED,
                correctCount = 4,
                totalQuestions = 10,
                bestStreak = 3,
                skippedCount = 1,
                lastAttemptTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2)
            ),
            onStart = {},
            onResume = {},
            onReview = {},
            onRestart = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1016)
@Composable
private fun ModuleCardFinishedPreview() {
    QuizingoTheme {
        ModuleCard(
            item = ModuleWithProgress(
                module = QuizModule(
                    id = "cell_membrane",
                    title = "Cell Membrane",
                    description = "",
                    questionsUrl = ""
                ),
                status = ModuleStatus.FINISHED,
                correctCount = 8,
                totalQuestions = 10,
                bestStreak = 5,
                skippedCount = 2,
                lastAttemptTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2)
            ),
            onStart = {},
            onResume = {},
            onReview = {},
            onRestart = {}
        )
    }
}
