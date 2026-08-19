package com.rudy.quizingo.data

import com.rudy.quizingo.data.local.ModuleProgressDao
import com.rudy.quizingo.data.local.ModuleProgressEntity
import com.rudy.quizingo.data.model.ModuleStatus
import com.rudy.quizingo.data.model.Question
import com.rudy.quizingo.data.model.QuizModule
import com.rudy.quizingo.data.remote.QuizApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Every module in the current API returns exactly 10 questions - used as the
 *  displayed count before a module has been attempted (and its real count is known). */
private const val DEFAULT_QUESTION_COUNT = 10

data class ModuleWithProgress(
    val module: QuizModule,
    val status: ModuleStatus,
    val correctCount: Int,
    val totalQuestions: Int,
    val bestStreak: Int,
    val skippedCount: Int,
    val lastAttemptTimestamp: Long?,
    val currentQuestionIndex: Int = 0
)

/** Single source of truth for module data - the only place the quiz API and the
 *  local progress DB get touched. Screens/ViewModels only ever go through this. */
class ModuleRepository(
    private val apiService: QuizApiService,
    private val dao: ModuleProgressDao
) {
    private val modules = MutableStateFlow<List<QuizModule>>(emptyList())
    // ConcurrentHashMap, not a plain map: this is a Koin singleton, and getQuestions()
    // mutates it from IO-dispatcher coroutines that can run on different pool threads.
    private val questionsCache = ConcurrentHashMap<String, List<Question>>()

    val modulesWithProgress: Flow<List<ModuleWithProgress>> =
        combine(modules, dao.observeAll()) { moduleList, progressRows ->
            val progressByModuleId = progressRows.associateBy { it.moduleId }
            moduleList.map { module -> module.withProgress(progressByModuleId[module.id]) }
        }

    suspend fun refreshModules(): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            modules.value = apiService.getModules()
        }
    }.rethrowCancellation()

    /** Looks up the module's question URL from the cached module list, refetching
     *  it first if the module list hasn't been loaded yet (e.g. process death). Rejects
     *  (and does not cache) an empty response, so a malformed payload surfaces as a
     *  retryable error instead of a silent "no questions" screen with no way to recover
     *  without an app restart. */
    suspend fun getQuestions(moduleId: String): Result<List<Question>> = runCatching {
        withContext(Dispatchers.IO) {
            questionsCache[moduleId]?.let { return@withContext it }
            val module = resolveModule(moduleId)
            val questions = apiService.getQuestions(module.questionsUrl)
            check(questions.isNotEmpty()) { "This module has no questions available." }
            questionsCache[moduleId] = questions
            questions
        }
    }.rethrowCancellation()

    suspend fun getProgress(moduleId: String): ModuleProgressEntity? = withContext(Dispatchers.IO) {
        dao.getProgress(moduleId)
    }

    suspend fun saveProgress(
        moduleId: String,
        status: ModuleStatus,
        correctCount: Int,
        totalQuestions: Int,
        bestStreak: Int,
        skippedCount: Int,
        currentQuestionIndex: Int,
        currentStreak: Int
    ) = withContext(Dispatchers.IO) {
        dao.upsert(
            ModuleProgressEntity(
                moduleId = moduleId,
                status = status,
                correctCount = correctCount,
                totalQuestions = totalQuestions,
                bestStreak = bestStreak,
                skippedCount = skippedCount,
                currentQuestionIndex = currentQuestionIndex,
                currentStreak = currentStreak,
                lastAttemptTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearProgress(moduleId: String) = withContext(Dispatchers.IO) {
        dao.clear(moduleId)
    }

    private suspend fun resolveModule(moduleId: String): QuizModule {
        modules.value.firstOrNull { it.id == moduleId }?.let { return it }
        val fetched = apiService.getModules()
        modules.value = fetched
        return fetched.first { it.id == moduleId }
    }

    private fun QuizModule.withProgress(entity: ModuleProgressEntity?): ModuleWithProgress =
        ModuleWithProgress(
            module = this,
            status = entity?.status ?: ModuleStatus.NOT_STARTED,
            correctCount = entity?.correctCount ?: 0,
            totalQuestions = entity?.totalQuestions ?: DEFAULT_QUESTION_COUNT,
            bestStreak = entity?.bestStreak ?: 0,
            skippedCount = entity?.skippedCount ?: 0,
            lastAttemptTimestamp = entity?.lastAttemptTimestamp,
            currentQuestionIndex = entity?.currentQuestionIndex ?: 0
        )
}

/** [runCatching] swallows every [Throwable], including [CancellationException] - which would
 *  otherwise stop cancellation from propagating up through this coroutine. Re-throwing it here
 *  keeps runCatching's convenience for real failures without breaking structured concurrency. */
private fun <T> Result<T>.rethrowCancellation(): Result<T> =
    onFailure { if (it is CancellationException) throw it }
