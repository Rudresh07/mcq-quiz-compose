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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rudy.quizingo.ui.theme.QuizSecondary
import com.rudy.quizingo.ui.theme.QuizingoTheme

/** How many short haptic pulses a milestone celebration fires - escalates with tier
 *  so a 10-streak reads as more emphatic than a 3-streak without needing raw
 *  vibration-amplitude control. Shared with [QuizScreen] so the pulse count used
 *  for haptics always matches what the badge visually represents. */
fun celebrationPulseCount(milestone: Int): Int = tierFor(milestone).pulseCount

private data class CelebrationTier(
    val pulseCount: Int,
    val outerGlowSize: Dp,
    val ringSize: Dp,
    val iconSize: Dp,
    val peakGlowAlpha: Float,
    val subtitle: String,
    val hasBonusRing: Boolean
)

private fun tierFor(milestone: Int): CelebrationTier = when {
    milestone >= 10 -> CelebrationTier(
        pulseCount = 3,
        outerGlowSize = 168.dp,
        ringSize = 100.dp,
        iconSize = 46.dp,
        peakGlowAlpha = 0.8f,
        subtitle = "Incredible! You're unstoppable!",
        hasBonusRing = true
    )
    milestone >= 5 -> CelebrationTier(
        pulseCount = 2,
        outerGlowSize = 148.dp,
        ringSize = 88.dp,
        iconSize = 40.dp,
        peakGlowAlpha = 0.65f,
        subtitle = "Amazing streak! Keep pushing!",
        hasBonusRing = false
    )
    else -> CelebrationTier(
        pulseCount = 1,
        outerGlowSize = 120.dp,
        ringSize = 76.dp,
        iconSize = 36.dp,
        peakGlowAlpha = 0.55f,
        subtitle = "You're on fire. Keep the momentum going!",
        hasBonusRing = false
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
                GlowingStreakBadge(tier)

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
private fun GlowingStreakBadge(tier: CelebrationTier) {
    val glowTransition = rememberInfiniteTransition(label = "streakGlow")
    val glowIntensity by glowTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowIntensity"
    )

    Box(
        modifier = Modifier.size(tier.outerGlowSize),
        contentAlignment = Alignment.Center
    ) {
        // Grand-tier bonus: a second, static ring outside the pulsing glow so a
        // 10-streak reads as visually richer, not just bigger.
        if (tier.hasBonusRing) {
            Box(
                modifier = Modifier
                    .size(tier.outerGlowSize)
                    .border(BorderStroke(1.dp, QuizSecondary.copy(alpha = 0.25f)), CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(tier.outerGlowSize * 0.85f)
                .scale(0.85f + glowIntensity * 0.15f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            QuizSecondary.copy(alpha = tier.peakGlowAlpha * glowIntensity),
                            QuizSecondary.copy(alpha = 0f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(tier.ringSize)
                    .clip(CircleShape)
                    .border(
                        BorderStroke(1.dp + 2.dp * glowIntensity, QuizSecondary.copy(alpha = 0.35f + 0.4f * glowIntensity)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = QuizSecondary.copy(alpha = 0.6f + 0.4f * glowIntensity),
                    modifier = Modifier.size(tier.iconSize)
                )
            }
        }
    }
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
