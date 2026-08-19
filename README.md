# Quizingo

A native Android MCQ quiz app built with Kotlin and Jetpack Compose.

## Demo

https://github.com/user-attachments/assets/3df783d0-7536-45f3-b942-b80ec84e6df1

*Full flow: question -> answer reveal -> streak celebration -> results -> restart.*

*(To add: record a short screen capture, convert to MP4, then drag-and-drop the
file directly into this README while editing it on GitHub's web UI - GitHub
will upload it and generate the `user-attachments/assets/...` link
automatically. Paste that link in place of the line above.)*

## Architecture

Plain MVVM, no DI framework:

```
data/
  model/Question.kt          - matches the gist's JSON shape directly
  remote/QuizApiService.kt   - Retrofit interface (GET against the gist raw URL)
  remote/NetworkModule.kt    - manual singleton: lazy OkHttp + Retrofit + Gson
  QuizRepository.kt          - wraps the network call in Result<List<Question>>

ui/
  theme/                     - dark-only Material 3 color scheme + type scale
  quiz/
    QuizUiState.kt           - single flat state data class for the whole session
    QuizViewModel.kt         - StateFlow<QuizUiState>, owns all quiz logic
    QuizViewModelFactory.kt  - manual ViewModelProvider.Factory
    QuizNavHost.kt           - 2-destination NavHost (quiz -> results) sharing one ViewModel
    QuizScreen.kt            - collects state, owns timing (reveal delay, celebration, swipe)
    components/              - QuestionContent, AnswerOption, CelebrationOverlay,
                                ResultsScreen, Loading/ErrorContent
```

`QuizViewModel` is constructed once in `MainActivity` via `viewModels { QuizViewModelFactory(...) }`
and shared across both nav destinations, so the results screen reads the same
session state the quiz screen built up - no DI framework, no repeated fetches.

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
- **Restart**: reuses the already-fetched question list instead of re-fetching
  - restart resets score/streak/index state but keeps the same 10 questions
    for that app session, so it's instant. A fresh network fetch only happens
    on first launch (or after a manual retry from the error state).
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
2. Let Gradle sync (Retrofit, OkHttp, Navigation Compose, Lottie, coroutines
   are all declared in `gradle/libs.versions.toml`).
3. Run the `app` configuration on a device/emulator (minSdk 26).

The app needs network access on first launch to fetch the 10 questions from
the configured gist URL.

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

