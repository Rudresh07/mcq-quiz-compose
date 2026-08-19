package com.rudy.quizingo.ui.quiz.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.rudy.quizingo.R
import com.rudy.quizingo.ui.theme.QuizSecondary
import com.rudy.quizingo.ui.theme.QuizingoTheme

private const val SUCCESS_THRESHOLD = 7

@Composable
fun ResultsScreen(
    correctCount: Int,
    totalQuestions: Int,
    longestStreak: Int,
    skippedCount: Int,
    onRestart: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Restart discards this attempt's saved score/streak - confirm before it runs
    // instead of firing straight off the button tap.
    var showRestartConfirm by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLandscape) {
            ResultsContentLandscape(
                correctCount = correctCount,
                totalQuestions = totalQuestions,
                longestStreak = longestStreak,
                skippedCount = skippedCount,
                onRestart = { showRestartConfirm = true },
                onFinish = onFinish
            )
        } else {
            ResultsContentPortrait(
                correctCount = correctCount,
                totalQuestions = totalQuestions,
                longestStreak = longestStreak,
                skippedCount = skippedCount,
                onRestart = { showRestartConfirm = true },
                onFinish = onFinish
            )
        }

        if (correctCount >= SUCCESS_THRESHOLD) {
            SuccessCelebrationLottie(modifier = Modifier.fillMaxSize())
        }
    }

    if (showRestartConfirm) {
        AlertDialog(
            onDismissRequest = { showRestartConfirm = false },
            title = { Text("Restart this quiz?") },
            text = { Text("This clears your saved score and streak and starts a fresh attempt. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showRestartConfirm = false
                    onRestart()
                }) { Text("Restart") }
            },
            dismissButton = {
                TextButton(onClick = { showRestartConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ResultsContentPortrait(
    correctCount: Int,
    totalQuestions: Int,
    longestStreak: Int,
    skippedCount: Int,
    onRestart: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlowingTrophyBadge()

            Text(
                text = "Quiz Complete!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                text = "Great job finishing the session.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Filled.CheckCircle,
                    label = "Score",
                    value = "$correctCount/$totalQuestions",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = "Best streak",
                    value = longestStreak.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            SkippedRow(skippedCount = skippedCount, modifier = Modifier.padding(top = 12.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RestartButton(onClick = onRestart, modifier = Modifier.fillMaxWidth())
            FinishButton(onClick = onFinish, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ResultsContentLandscape(
    correctCount: Int,
    totalQuestions: Int,
    longestStreak: Int,
    skippedCount: Int,
    onRestart: () -> Unit,
    onFinish: () -> Unit
) {
    // Landscape phones don't have the vertical room for the portrait stack
    // (badge + headline + stats + skipped + two full-width buttons), so split
    // into a summary pane and an actions pane side by side instead.
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Column(
            modifier = Modifier.weight(0.42f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            GlowingTrophyBadge(size = 96.dp)
            Text(
                text = "Quiz Complete!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = "Great job finishing the session.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    icon = Icons.Filled.CheckCircle,
                    label = "Score",
                    value = "$correctCount/$totalQuestions",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = "Best streak",
                    value = longestStreak.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            SkippedRow(skippedCount = skippedCount, modifier = Modifier.padding(top = 12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RestartButton(onClick = onRestart, modifier = Modifier.weight(1f))
                FinishButton(onClick = onFinish, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RestartButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "Restart Quiz",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun FinishButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "Finish",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SkippedRow(skippedCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.SkipNext,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "Skipped questions",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = skippedCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuccessCelebrationLottie(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.success_celebration)
    )
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = modifier
    )
}

@Composable
private fun GlowingTrophyBadge(size: Dp = 140.dp) {
    val glowTransition = rememberInfiniteTransition(label = "badgeGlow")
    val glowIntensity by glowTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowIntensity"
    )
    val innerSize = size * (88f / 140f)
    val iconSize = size * (40f / 140f)

    Box(
        modifier = Modifier
            .size(size)
            .scale(0.85f + glowIntensity * 0.15f)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f * glowIntensity),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .border(
                    BorderStroke(
                        1.dp + 2.dp * glowIntensity,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f + 0.4f * glowIntensity)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f + 0.4f * glowIntensity),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = QuizSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1016)
@Composable
private fun ResultsScreenPreview() {
    QuizingoTheme {
        ResultsScreen(
            correctCount = 7,
            totalQuestions = 10,
            longestStreak = 5,
            skippedCount = 1,
            onRestart = {},
            onFinish = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1016, widthDp = 780, heightDp = 360)
@Composable
private fun ResultsScreenLandscapePreview() {
    QuizingoTheme {
        ResultsScreen(
            correctCount = 7,
            totalQuestions = 10,
            longestStreak = 5,
            skippedCount = 1,
            onRestart = {},
            onFinish = {}
        )
    }
}
