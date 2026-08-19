# Implementation Notes

Snapshot of what's actually built on `app_improvement_r1`. This branch replaced
the original single-session quiz with a persistent, multi-module app — the
notes below describe the current implementation, not the original one.

## What the app does

A module-based MCQ quiz app with local progress tracking:

- **Module List** (home screen): fetches the list of quiz modules from a
  remote gist, shown as expandable cards with a status dot/label
  (`NOT_STARTED` / `PAUSED` / `FINISHED`). Tapping a card reveals saved stats
  (best streak, skipped count, last attempt); Start/Resume/Review/Restart are
  always visible on the card, not gated behind the expand.
- **Quiz**: fetches the selected module's questions, presents one at a time
  with 4 tappable options. Tapping an option locks the answer, reveals
  correct (green) / incorrect (red) inline, then auto-advances after a short
  delay (faster for correct answers). Tracks current streak, longest streak,
  correct count, and skipped count. Progress is written to Room after every
  answer, skip, and advance, so a killed process or a back-tap mid-quiz never
  loses more than the current unanswered question — "Resume" on the Module
  List picks a `PAUSED` module back up at the right question.
- Hits a full-screen Lottie celebration at streak milestones of 3, 5, and 10
  in a row, with animation size and copy scaling up per tier, plus escalating
  haptic pulses.
- Skipping a question is neutral: no reveal, no streak change, immediate
  advance.
- Supports a left swipe gesture on the question card as an alternate way to
  advance/skip.
- **Results**: reads the module's *persisted* progress rather than live quiz
  state, so the same screen serves both "just finished" and "Review" (tapped
  later on an already-`FINISHED` module). Shows correct/total, longest
  streak, and skipped count, with a full-screen Lottie success animation
  when the score is 7+ out of 10. Restart and Finish both require the user
  to confirm before Restart clears the module's saved score/streak.
- Module cards don't show a question count until a module has been started
  at least once — the count is only known once the module's real question
  list has been fetched, so nothing is guessed up front.
- Module List lays out as an adaptive grid (`GridCells.Adaptive(minSize =
  340.dp)`): one column on a phone in portrait, two or more on a wider
  screen or landscape, with no manual orientation branching.
- Dark-only theme — no light/system toggle, no Material You dynamic color.
- Branded launch splash (native `SplashScreen` API, using the app's own
  icon) stays up through the Module List's first load, capped at 3 seconds
  so a slow/dead connection falls through to the in-app spinner instead of
  looking frozen.

## Tech stack

- **Kotlin** + **Jetpack Compose** (BOM `2026.02.01`), Material 3.
- **Navigation Compose** — 3-destination `NavHost`: `moduleList` →
  `quiz/{moduleId}` → `results/{moduleId}`, each screen with its own
  `ViewModel` (Quiz and Results are separate routes/ViewModels; Results
  reads from Room rather than sharing the live Quiz state).
- **Koin** (BOM `4.2.2`) for DI — `networkModule`, `databaseModule`,
  `repositoryModule`, `viewModelModule`, started in `QuizingoApplication`;
  screens resolve dependencies via `koinViewModel()`.
- **Retrofit 3** + **Gson converter** for the network calls (module list +
  per-module questions); **OkHttp** with an `HttpLoggingInterceptor` (BASIC).
- **Room** (`2.8.4`, via KSP) for local per-module progress persistence.
- **Kotlin Coroutines** (`viewModelScope`, `StateFlow`, `Flow.combine`) for
  async loading and state.
- **Lottie for Compose** (`lottie-compose` 6.4.0) for the streak-milestone
  and results-screen celebration animations.
- **AndroidX Lifecycle** (`lifecycle-viewmodel-compose`,
  `lifecycle-runtime-ktx`) for `collectAsStateWithLifecycle`.
- **AndroidX Core SplashScreen** (`1.0.1`) for the branded launch splash.
- minSdk 26, targetSdk/compileSdk 36.

## Architecture

Koin-based MVVM — dependencies are wired declaratively in `di/AppModule.kt`
and started once from `QuizingoApplication.onCreate()`.

```
data/
  model/
    QuizModule.kt          - id, title, description, questionsUrl (one gist module entry)
    Question.kt            - id, question, options, correctOptionIndex
    ModuleStatus.kt         - NOT_STARTED / PAUSED / FINISHED
  remote/
    QuizApiService.kt      - Retrofit interface: getModules(), getQuestions(url)
  local/
    QuizingoDatabase.kt     - Room database (schema v2)
    ModuleProgressEntity.kt - one row per module; only the latest attempt is kept
    ModuleProgressDao.kt    - observeAll / getProgress / upsert / clear
    Converters.kt            - ModuleStatus <-> String
  ModuleRepository.kt       - single source of truth: combines the API module list with
                              Room progress into ModuleWithProgress; caches fetched
                              question lists in memory per module
  NetworkErrors.kt          - maps exceptions (no host, timeout, HTTP, IO) to
                              user-facing copy instead of surfacing raw messages

di/
  AppModule.kt              - Koin modules: network, database, repository, viewModel

ui/
  theme/                    - dark-only Material 3 color scheme + type scale
  AppNavHost.kt              - moduleList -> quiz/{moduleId} -> results/{moduleId}
  modules/
    ModuleListScreen.kt      - loading/error/empty/grid states, restart confirmation dialog
    ModuleListUiState.kt
    ModuleListViewModel.kt
    components/ModuleCard.kt - expandable card; primary actions always visible
  quiz/
    QuizUiState.kt            - single flat state data class for the whole session
    QuizViewModel.kt          - owns quiz logic, incl. persisting progress after every
                                answer/skip/advance
    QuizScreen.kt             - collects state, owns timing (reveal delay, celebration, swipe)
    components/                - QuestionContent, AnswerOption, CelebrationOverlay,
                                  ResultsScreen, ErrorContent, LoadingSpinner
  results/
    ResultsUiState.kt
    ResultsViewModel.kt       - reads persisted progress; also backs "Review"

QuizingoApplication.kt     - startKoin { modules(...) }
MainActivity.kt            - installSplashScreen(); holds the splash open until the
                              Module List's first load finishes or 3s pass, whichever
                              is first (reads the same ModuleListViewModel instance
                              the Compose screen uses)
```

`QuizUiState` (and the other screen states) stay flat data classes rather
than a `Loading/Error/Success` sealed hierarchy — the UI just branches on
the fields it cares about (`if (state.isLoading)`, etc.), which is simpler
for an app this size than the ceremony of a sealed wrapper.

## App icon / splash

The launcher icon and the native splash icon both come from the same
adaptive-icon foreground asset (`mipmap-*/ic_launcher_foreground.png`),
generated from the source logo with a matching solid background color
(`quiz_launcher_background`) so a launcher's mask (circle, squircle, etc.)
never reveals a seam. `Theme.Quizingo.Starting` is the manifest activity
theme shown by the platform for the brief window before Compose's first
frame; it hands off to `Theme.Quizingo` via `postSplashScreenTheme`.

## Not yet included

- Unit tests for `QuizViewModel`/`ModuleListViewModel`/`ResultsViewModel`
  logic (streak/milestone/skip/restart/persistence).
- Per-question answer review on the Results screen — "Review" currently
  shows the same aggregate score as a just-finished quiz, not a breakdown
  of which questions were missed.
- Onboarding/first-run explainer for streak milestones, Skip being
  score-neutral, or the swipe-to-advance gesture.
- Search/filter on the Module List (not needed yet at the current module
  count).
