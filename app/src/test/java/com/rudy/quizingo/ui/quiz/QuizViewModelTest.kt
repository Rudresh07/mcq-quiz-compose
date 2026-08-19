package com.rudy.quizingo.ui.quiz

import com.rudy.quizingo.data.ModuleRepository
import com.rudy.quizingo.data.local.ModuleProgressEntity
import com.rudy.quizingo.data.model.ModuleStatus
import com.rudy.quizingo.data.model.Question
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val MODULE_ID = "module-1"

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun question(id: Int, correctOptionIndex: Int = 0) = Question(
        id = id,
        question = "Question $id",
        options = listOf("A", "B", "C", "D"),
        correctOptionIndex = correctOptionIndex
    )

    /** Builds a [ModuleRepository] mock stubbed for a successful load. */
    private fun fakeRepo(
        questions: List<Question> = listOf(question(1), question(2), question(3)),
        progress: ModuleProgressEntity? = null
    ): ModuleRepository {
        val repo = mockk<ModuleRepository>()
        coEvery { repo.getQuestions(MODULE_ID) } returns Result.success(questions)
        coEvery { repo.getProgress(MODULE_ID) } returns progress
        coEvery {
            repo.saveProgress(
                moduleId = any(),
                status = any(),
                correctCount = any(),
                totalQuestions = any(),
                bestStreak = any(),
                skippedCount = any(),
                currentQuestionIndex = any(),
                currentStreak = any()
            )
        } returns Unit
        return repo
    }

    @Test
    fun `loadQuestions on init populates state on success`() = runTest(testDispatcher) {
        val questions = listOf(question(1), question(2))
        val repo = fakeRepo(questions = questions)

        val viewModel = QuizViewModel(MODULE_ID, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(questions, state.questions)
        assertEquals(0, state.currentQuestionIndex)
    }

    @Test
    fun `loadQuestions restores paused progress`() = runTest(testDispatcher) {
        val questions = listOf(question(1), question(2), question(3))
        val progress = ModuleProgressEntity(
            moduleId = MODULE_ID,
            status = ModuleStatus.PAUSED,
            correctCount = 2,
            totalQuestions = 3,
            bestStreak = 4,
            skippedCount = 1,
            currentQuestionIndex = 2,
            currentStreak = 2,
            lastAttemptTimestamp = 123L
        )
        val repo = fakeRepo(questions = questions, progress = progress)

        val viewModel = QuizViewModel(MODULE_ID, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.currentQuestionIndex)
        assertEquals(2, state.streak)
        assertEquals(4, state.longestStreak)
        assertEquals(2, state.correctCount)
        assertEquals(1, state.skippedCount)
    }

    @Test
    fun `loadQuestions does not restore a finished progress row`() = runTest(testDispatcher) {
        val questions = listOf(question(1), question(2))
        val progress = ModuleProgressEntity(
            moduleId = MODULE_ID,
            status = ModuleStatus.FINISHED,
            correctCount = 2,
            totalQuestions = 2,
            bestStreak = 5,
            skippedCount = 0,
            currentQuestionIndex = 2,
            currentStreak = 5,
            lastAttemptTimestamp = 123L
        )
        val repo = fakeRepo(questions = questions, progress = progress)

        val viewModel = QuizViewModel(MODULE_ID, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.currentQuestionIndex)
        assertEquals(0, state.streak)
        assertEquals(0, state.correctCount)
    }

    @Test
    fun `loadQuestions failure surfaces error message and stops loading`() = runTest(testDispatcher) {
        val repo = mockk<ModuleRepository>()
        coEvery { repo.getQuestions(MODULE_ID) } returns Result.failure(RuntimeException("boom"))
        coEvery { repo.getProgress(MODULE_ID) } returns null

        val viewModel = QuizViewModel(MODULE_ID, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.questions.isEmpty())
        assertTrue(state.error != null)
    }

    @Test
    fun `retrying loadQuestions cancels the stale in-flight request`() = runTest(testDispatcher) {
        val staleQuestions = listOf(question(1))
        val freshQuestions = listOf(question(2), question(3))
        var callCount = 0
        val repo = mockk<ModuleRepository>()
        coEvery { repo.getProgress(MODULE_ID) } returns null
        coEvery { repo.getQuestions(MODULE_ID) } coAnswers {
            callCount++
            if (callCount == 1) {
                delay(1_000)
                Result.success(staleQuestions)
            } else {
                Result.success(freshQuestions)
            }
        }
        coEvery {
            repo.saveProgress(
                moduleId = any(), status = any(), correctCount = any(), totalQuestions = any(),
                bestStreak = any(), skippedCount = any(), currentQuestionIndex = any(), currentStreak = any()
            )
        } returns Unit

        val viewModel = QuizViewModel(MODULE_ID, repo)
        // Let init's loadQuestions() start and suspend on the slow first response.
        testDispatcher.scheduler.runCurrent()

        viewModel.loadQuestions()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(freshQuestions, state.questions)
        assertFalse(state.isLoading)
    }

    @Test
    fun `selectAnswer with correct option increments streak and correctCount`() = runTest(testDispatcher) {
        val repo = fakeRepo(questions = listOf(question(1, correctOptionIndex = 2), question(2)))
        val viewModel = QuizViewModel(MODULE_ID, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectAnswer(2)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAnswered)
        assertEquals(2, state.selectedOptionIndex)
        assertEquals(1, state.streak)
        assertEquals(1, state.longestStreak)
        assertEquals(1, state.correctCount)
    }

    @Test
    fun `selectAnswer with wrong option resets streak to zero`() = runTest(testDispatcher) {
        val repo = fakeRepo(questions = listOf(question(1, correctOptionIndex = 2), question(2)))
        val viewModel = QuizViewModel(MODULE_ID, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectAnswer(0)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAnswered)
        assertEquals(0, state.streak)
        assertEquals(0, state.correctCount)
    }

    @Test
    fun `selectAnswer hitting a milestone streak sets celebrationMilestone`() = runTest(testDispatcher) {
        val questions = (1..3).map { question(it, correctOptionIndex = 0) }
        val repo = fakeRepo(questions = questions)
        val viewModel = QuizViewModel(MODULE_ID, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectAnswer(0)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.advanceToNext()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectAnswer(0)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.advanceToNext()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectAnswer(0)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.streak)
        assertEquals(3, state.celebrationMilestone)
    }

    @Test
    fun `selectAnswer is a no-op once the question is already answered`() = runTest(testDispatcher) {
        val repo = fakeRepo(questions = listOf(question(1, correctOptionIndex = 0), question(2)))
        val viewModel = QuizViewModel(MODULE_ID, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectAnswer(0)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectAnswer(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.selectedOptionIndex)
        assertEquals(1, state.correctCount)
    }

    @Test
    fun `selectAnswer persists progress with the not-yet-applied next index`() = runTest(testDispatcher) {
        val repo = fakeRepo(questions = listOf(question(1, correctOptionIndex = 0), question(2)))
        val viewModel = QuizViewModel(MODULE_ID, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectAnswer(0)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repo.saveProgress(
                moduleId = MODULE_ID,
                status = ModuleStatus.PAUSED,
                correctCount = 1,
                totalQuestions = 2,
                bestStreak = 1,
                skippedCount = 0,
                currentQuestionIndex = 1,
                currentStreak = 1
            )
        }
    }

    @Test
    fun `skip advances without revealing an answer and increments skippedCount`() = runTest(testDispatcher) {
        val repo = fakeRepo(questions = listOf(question(1), question(2)))
        val viewModel = QuizViewModel(MODULE_ID, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.skip()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.skippedCount)
        assertEquals(1, state.currentQuestionIndex)
        assertFalse(state.isAnswered)
        assertNull(state.selectedOptionIndex)
    }

    @Test
    fun `advanceToNext mid-quiz resets per-question state`() = runTest(testDispatcher) {
        val repo = fakeRepo(questions = listOf(question(1, correctOptionIndex = 0), question(2), question(3)))
        val viewModel = QuizViewModel(MODULE_ID, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectAnswer(0)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.advanceToNext()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.currentQuestionIndex)
        assertFalse(state.isAnswered)
        assertNull(state.selectedOptionIndex)
        assertNull(state.celebrationMilestone)
        assertFalse(state.isQuizFinished)
    }

    @Test
    fun `advanceToNext past the last question finishes the quiz and saves FINISHED status`() = runTest(testDispatcher) {
        val repo = fakeRepo(questions = listOf(question(1, correctOptionIndex = 0)))
        val viewModel = QuizViewModel(MODULE_ID, repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectAnswer(0)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.advanceToNext()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isQuizFinished)
        coVerify {
            repo.saveProgress(
                moduleId = MODULE_ID,
                status = ModuleStatus.FINISHED,
                correctCount = 1,
                totalQuestions = 1,
                bestStreak = 1,
                skippedCount = 0,
                currentQuestionIndex = 1,
                currentStreak = 1
            )
        }
    }
}
