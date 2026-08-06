package com.rudy.quizingo.ui.quiz.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.rudy.quizingo.R
import com.rudy.quizingo.ui.theme.QuizingoTheme

/** How many short haptic pulses a milestone celebration fires - escalates with tier
 *  so a 10-streak reads as more emphatic than a 3-streak without needing raw
 *  vibration-amplitude control. Shared with [QuizScreen] so the pulse count used
 *  for haptics always matches what the badge visually represents. */
fun celebrationPulseCount(milestone: Int): Int = tierFor(milestone).pulseCount

private data class CelebrationTier(
    val pulseCount: Int,
    val lottieSize: Dp,
    val subtitle: String
)

private fun tierFor(milestone: Int): CelebrationTier = when {
    milestone >= 10 -> CelebrationTier(
        pulseCount = 3,
        lottieSize = 180.dp,
        subtitle = "Incredible! You're unstoppable!"
    )
    milestone >= 5 -> CelebrationTier(
        pulseCount = 2,
        lottieSize = 150.dp,
        subtitle = "Amazing streak! Keep pushing!"
    )
    else -> CelebrationTier(
        pulseCount = 1,
        lottieSize = 120.dp,
        subtitle = "You're on fire. Keep the momentum going!"
    )
}

/**
 * Streak milestone celebration. Sits on top of the question as a dimmed
 * backdrop with a centered card; the caller (QuizScreen) times how long it
 * stays up before advancing to the next question. The badge, glow and copy
 * all scale up with [milestone] so a 10-streak feels bigger than a 3-streak.
 */
@Composable
fun CelebrationOverlay(
    milestone: Int,
    modifier: Modifier = Modifier
) {
    val tier = tierFor(milestone)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "$milestone in a row!"
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StreakLottie(modifier = Modifier.size(tier.lottieSize))

                Text(
                    text = "$milestone in a row!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 20.dp)
                )
                Text(
                    text = tier.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StreakLottie(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.streak_anim)
    )
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1016)
@Composable
private fun CelebrationOverlaySmallPreview() {
    QuizingoTheme {
        CelebrationOverlay(milestone = 3)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1016)
@Composable
private fun CelebrationOverlayMidPreview() {
    QuizingoTheme {
        CelebrationOverlay(milestone = 5)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1016)
@Composable
private fun CelebrationOverlayGrandPreview() {
    QuizingoTheme {
        CelebrationOverlay(milestone = 10)
    }
}
