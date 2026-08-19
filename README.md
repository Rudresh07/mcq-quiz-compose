# Quizingo

A native Android MCQ quiz app built with Kotlin and Jetpack Compose.

## Demo

https://github.com/user-attachments/assets/3df783d0-7536-45f3-b942-b80ec84e6df1

*Full flow: splash -> module -> question -> answer reveal -> streak celebration -> results -> restart/go to module.*

*(To add: record a short screen capture, convert to MP4, then drag-and-drop the
file directly into this README while editing it on GitHub's web UI - GitHub
will upload it and generate the `user-attachments/assets/...` link
automatically. Paste that link in place of the line above.)*

## Architecture

MVVM with Koin for DI and Room for local persistence:

```
data/
  model/                       - QuizModule, Question, ModuleStatus (API/DB shapes)
  remote/QuizApiService.kt     - Retrofit interface: module list + per-module questions (gist-backed)
  local/                       - Room: QuizingoDatabase, ModuleProgressDao, ModuleProgressEntity
  ModuleRepository.kt          - single source of truth; combines the API module list with
                                  Room progress into ModuleWithProgress, caches fetched questions
  NetworkErrors.kt             - Throwable -> user-facing message mapping

di/
  AppModule.kt                 - Koin modules: network, database, repository, viewmodel

ui/
  theme/                       - dark-only Material 3 color scheme + type scale
  modules/                     - Module List screen (entry point)
    ModuleListUiState.kt / ModuleListViewModel.kt / ModuleListScreen.kt
    components/ModuleCard.kt   - status dot/label, expandable stats, Start/Resume/Review/Restart
  quiz/
    QuizUiState.kt             - single flat state data class for one quiz session
    QuizViewModel.kt           - StateFlow<QuizUiState>, owns quiz logic + progress persistence
    QuizScreen.kt              - collects state, owns timing (reveal delay, celebration, swipe)
    components/                - QuestionContent, AnswerOption, CelebrationOverlay,
                                  ResultsScreen, LoadingSpinner, ErrorContent
  results/                     - ResultsUiState.kt / ResultsViewModel.kt (reads persisted progress)
  components/RestartConfirmationDialog.kt
  AppNavHost.kt                - 3-destination NavHost: moduleList -> quiz/{moduleId} -> results/{moduleId}
```

Koin resolves each ViewModel (`ModuleListViewModel`, `QuizViewModel(moduleId)`,
`ResultsViewModel(moduleId)`) via `koinViewModel()`/`viewModel()`, wired in
`AppModule.kt`. `MainActivity` and `AppNavHost` both scope `ModuleListViewModel`
to the Activity's `ViewModelStore`, so the module list loads exactly once and
the splash screen can wait on that same load. `ModuleRepository` is the only
place either the quiz API or the local progress DB gets touched - screens and
ViewModels always go through it, never directly to `QuizApiService` or
`ModuleProgressDao`.

## Key decisions

- **Streak milestones**: 3, 5, 10 consecutive correct answers, as specified.
  Hitting one sets `celebrationMilestone` in the ViewModel; the *timing* of
  when to show/hide the overlay lives in `QuizScreen`'s `LaunchedEffect`, not
  the ViewModel, so it stays cancelable and tied to composition.
- **Skip semantics**: skipping is neutral - it doesn't reveal the answer,
  doesn't affect the streak (no reset, no increment), and doesn't count as
  correct. It advances immediately with no delay. Skipped count is tracked
  separately and shown on the results screen.
- **Answer reveal / auto-advance**: tapping an option locks in the answer,
  reveals correct (green) / incorrect (red) inline, then auto-advances after
  a cancelable 2s `delay()` scoped to the question's composition. If a streak
  milestone was hit, the sequence is reveal (2s) -> full-screen celebration
  (~1.3s) -> advance, never overlapping.
- **Swipe gesture**: a rightward drag past a small threshold on the question
  content advances the quiz - it calls the same "advance" path if the
  question is already answered, or the same "skip" path if it isn't. Detected
  via `pointerInput`/`detectHorizontalDragGestures` on the question container,
  not on individual option rows, so it doesn't compete with tap targets.
- **Restart**: gated behind `RestartConfirmationDialog` since it discards a
  module's saved score/streak. Confirming clears that module's persisted Room
  row via `ModuleRepository.clearProgress()`, then navigates into a fresh
  `QuizViewModel` for the same module - the question list itself is usually
  still warm in `ModuleRepository`'s in-memory cache, so restart rarely means
  a real network re-fetch.
- **Progress persistence**: every answer/skip writes to Room immediately
  (not batched), so a process death mid-quiz doesn't lose that answer's
  contribution to the score. Status is `PAUSED` while mid-quiz and flips to
  `FINISHED` on the last question - `advanceToNext()` awaits that final write
  *before* setting `isQuizFinished`, since `AppNavHost` navigates to Results
  the instant that flag flips and Results reads the same row back.
- **Splash screen wait**: `MainActivity` keeps the splash on-screen until the
  module list's first load completes, capped at a 3s timeout
  (`SPLASH_MAX_WAIT_MS`) - fast connections skip straight past any in-app
  spinner, slow/dead ones still fall through instead of hanging on a static
  splash with no way back.
- **DI (Koin)**: four small modules in `AppModule.kt` (network, database,
  repository, viewmodel) instead of manual factories or Hilt - kept lightweight
  for this app's size, with `QuizViewModel`/`ResultsViewModel` resolved via a
  parameterized `viewModel { (moduleId: String) -> ... }`.
- **Crashlytics**: collection is only enabled on non-debug builds
  (`QuizingoApplication.onCreate`), so local debugging on a developer's own
  device doesn't pollute real crash reports.
- **Theme**: dark-only by design (no light/system toggle, no Material You
  dynamic color) with a custom indigo/violet + amber palette, so the streak
  and answer-reveal colors stay consistent regardless of device wallpaper.
- **Celebration Lottie asset**: `app/src/main/res/raw/celebration_placeholder.json`
  is currently a placeholder animation used for all three milestones tiers.
  Milestone 10 renders it larger with a slower playback speed to feel
  bigger than 3/5. Swap in real confetti/fire/star files per tier in
  `CelebrationOverlay.kt` (`R.raw.celebration_placeholder` reference) once
  final assets are provided.

## Running the app

1. Open the project root in Android Studio.
2. Let Gradle sync (Retrofit, OkHttp, Navigation Compose, Lottie, Room, Koin,
   Firebase Crashlytics, and coroutines are all declared in
   `gradle/libs.versions.toml`).
3. `app/google-services.json` is checked in for this project's Firebase app,
   so Crashlytics initializes out of the box - no extra setup needed.
4. Run the `app` configuration on a device/emulator (minSdk 26).

The app needs network access on first launch (and whenever the module list is
refreshed) to fetch the module list and each module's questions from the
configured gist URLs. Per-module progress is persisted locally in a Room
database, so a paused module resumes where you left off even after the app
process is killed.

## What I'd improve with more time

- **Module API: return each module's question count.** The `GET /modules`
  response has no count field today, so `ModuleRepository` falls back to a
  hardcoded `DEFAULT_QUESTION_COUNT = 10` for the "X questions" label on any
  module that hasn't been attempted yet - the real count is only known once
  `getQuestions()` has actually fetched and cached that module's questions.
  I'd ask backend to add a `question_count` field to the module list payload
  so the module list can show accurate counts up front and the hardcoded
  default can go away entirely.
- **Pagination**: worth doing if the module catalog grows - `GET /modules`
  currently returns the entire list in one response, which is fine at the
  current small catalog size but wouldn't scale indefinitely. Not a priority
  right now since it isn't the actual bottleneck yet.

