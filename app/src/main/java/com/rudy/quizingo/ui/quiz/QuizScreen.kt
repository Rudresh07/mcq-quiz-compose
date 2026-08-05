package com.rudy.quizingo.ui.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rudy.quizingo.data.model.Question
import com.rudy.quizingo.ui.quiz.components.CelebrationOverlay
import com.rudy.quizingo.ui.quiz.components.ErrorContent
import com.rudy.quizingo.ui.quiz.components.LoadingContent
import com.rudy.quizingo.ui.quiz.components.QuestionContent
import com.rudy.quizingo.ui.quiz.components.celebrationPulseCount
import kotlinx.coroutines.delay

/** Snapshot of everything a single frame of [QuestionContent] needs, so the
 *  question sliding out during a transition keeps showing its own answered
 *  state instead of picking up the next question's reset state. */
private data class QuestionFrame(
    val question: Question,
    val questionNumber: Int,
    val totalQuestions: Int,
    val streak: Int,
    val selectedOptionIndex: Int?,
    val isAnswered: Boolean
)

/** Minimum leftward drag distance to count as a swipe-to-advance gesture. */
private val SWIPE_THRESHOLD = 96.dp

/** Correct answers auto-advance quickly - there's nothing new to read. */
private const val CORRECT_REVEAL_DELAY_MS = 500L

/** Wrong answers stay revealed longer so the correct answer registers. */
private const val INCORRECT_REVEAL_DELAY_MS = 2000L

/** How long the full-screen milestone celebration stays up before dismissing. */
private const val CELEBRATION_DURATION_MS = 1300L

/** Gap between the repeated haptic pulses that mark a milestone - more pulses
 *  at higher tiers reads as more emphatic without needing raw vibration amplitude. */
private const val CELEBRATION_PULSE_GAP_MS = 120L

@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onQuizFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCelebration by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Cancelable per composition: re-keyed on question index + answered flag,
    // so leaving the question (advance, skip, restart) cancels any pending delay.
    // Sequenced as: haptic + reveal -> celebration (if milestone hit) -> advance.
    LaunchedEffect(state.currentQuestionIndex, state.isAnswered) {
        if (state.isAnswered) {
            val isCorrect = state.selectedOptionIndex == state.currentQuestion?.correctOptionIndex
            haptic.performHapticFeedback(if (isCorrect) HapticFeedbackType.Confirm else HapticFeedbackType.Reject)
            delay(if (isCorrect) CORRECT_REVEAL_DELAY_MS else INCORRECT_REVEAL_DELAY_MS)
            val milestone = state.celebrationMilestone
            if (milestone != null) {
                showCelebration = true
                val pulseCount = celebrationPulseCount(milestone)
                repeat(pulseCount) { pulseIndex ->
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    if (pulseIndex < pulseCount - 1) delay(CELEBRATION_PULSE_GAP_MS)
                }
                delay(CELEBRATION_DURATION_MS)
                showCelebration = false
            }
            viewModel.advanceToNext()
        } else {
            showCelebration = false
        }
    }

    LaunchedEffect(state.isQuizFinished) {
        if (state.isQuizFinished) onQuizFinished()
    }

    val swipeThresholdPx = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }

    fun onSwipeAdvance() {
        if (state.isAnswered) viewModel.advanceToNext() else viewModel.skip()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val question = state.currentQuestion
        when {
            state.isLoading -> LoadingContent(modifier = Modifier.align(Alignment.Center))
            state.error != null -> ErrorContent(
                message = state.error.orEmpty(),
                onRetry = viewModel::loadQuestions,
                modifier = Modifier.align(Alignment.Center)
            )
            question != null -> AnimatedContent(
                targetState = QuestionFrame(
                    question = question,
                    questionNumber = state.currentQuestionIndex + 1,
                    totalQuestions = state.totalQuestions,
                    streak = state.streak,
                    selectedOptionIndex = state.selectedOptionIndex,
                    isAnswered = state.isAnswered
                ),
                contentKey = { it.question.id },
                transitionSpec = {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn()) togetherWith
                        (slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                },
                modifier = Modifier.pointerInput(state.currentQuestionIndex) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragEnd = {
                            if (totalDrag < -swipeThresholdPx) onSwipeAdvance()
                        }
                    ) { _, dragAmount ->
                        totalDrag += dragAmount
                    }
                },
                label = "questionTransition"
            ) { frame ->
                QuestionContent(
                    question = frame.question,
                    questionNumber = frame.questionNumber,
                    totalQuestions = frame.totalQuestions,
                    streak = frame.streak,
                    selectedOptionIndex = frame.selectedOptionIndex,
                    isAnswered = frame.isAnswered,
                    onOptionSelected = viewModel::selectAnswer,
                    onSkip = viewModel::skip
                )
            }
        }

        val milestone = state.celebrationMilestone
        if (showCelebration && milestone != null) {
            CelebrationOverlay(milestone = milestone, modifier = Modifier.fillMaxSize())
        }
    }
}
