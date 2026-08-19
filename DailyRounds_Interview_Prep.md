# DailyRounds (M3 India) — Android Developer Interview Prep

## Company context worth knowing cold

- **What they do**: DailyRounds organizes "knowledge of practice of medicine" — case-based learning for doctors — because conferences/journals are too slow/expensive, and doctors make ~30 clinical decisions a day that need current knowledge.
- **Scale claims to remember**: 300,000+ doctors served, 500M+ hours of video streamed. If asked "why do you want to join," tying your answer to *scale + real-world impact* (not another CRUD app) will land well since that's literally how they pitch the role.
- **Ownership**: Acquired (majority stake) by **M3 India** in April 2019 — M3 is a Japanese healthtech company listed on the Tokyo Stock Exchange, one of the largest healthcare networks globally. Good to know so you don't look blindsided if they mention "M3" mid-interview.
- **Team size**: ~300 people, Bangalore-based.
- **Stated culture/priorities**: "product-driven," explicitly ranks priorities as *scale > profitability > avoiding fads* — i.e., they want engineers who think about what actually ships and scales, not resume-driven tech adoption.
- **The core technical theme of this specific role**: petabyte-scale VOD streaming (not a generic Android role) — expect real conversation about video delivery, buffering, adaptive bitrate, CDN, offline caching, not just "how do you build a RecyclerView."

## Technical interview questions by topic

### Kotlin (they want 1–1.5 yrs hands-on, not tutorial-level)
- Difference between `val`/`var`, `lateinit` vs `by lazy`, and when each breaks.
- Null safety: `?.`, `!!`, `?:`, and where `!!` in production code becomes a red flag.
- Data classes vs regular classes — what `copy()`, `equals()`, `hashCode()` give you for free.
- Sealed classes vs enums — when you'd reach for one over the other (they may probe this since your CLAUDE memory shows you personally prefer flat data classes over sealed-class UI state for small apps — be ready to defend *why*, not just cite a preference).
- Coroutines: `suspend` functions, structured concurrency, `viewModelScope` vs `GlobalScope`, `Dispatchers.IO` vs `Main`, how you cancel work tied to a lifecycle.
- Flow vs LiveData — why you'd pick one, and how `StateFlow`/`SharedFlow` differ.
- Extension functions and scope functions (`let`, `run`, `apply`, `also`, `with`) — real usage, not just definitions.
- Higher-order functions / lambdas with receivers (relevant since Compose DSLs lean on this heavily).

### Android UI (mobile + tablet, explicitly called out)
- How do you handle different screen sizes/densities — resource qualifiers, `sw<N>dp`, `ConstraintLayout` vs adaptive layouts, `WindowSizeClass` for tablets.
- Compose: state hoisting, `remember` vs `rememberSaveable`, recomposition — what triggers unnecessary recomposition and how you'd debug/fix it (you've been doing exactly this — Lottie overlays, sizing per state — so have a concrete example ready from *your own recent work*, e.g. the results-screen success animation or the streak-tier-scaled Lottie).
- `LazyColumn`/`LazyRow` performance — keys, item reuse, avoiding recomposition storms in lists.
- Jetpack Navigation — nav graphs, passing arguments/back stack management, deep links.
- Animation APIs — you've hands-on used `rememberInfiniteTransition`, `animateFloat`, and Lottie in this very project; be ready to talk through *why* you chose transparent backgrounds / scaled sizing for tiers instead of the more static resource-based approach.

### Architecture (MVVM/MVP required)
- MVVM: what lives in the ViewModel vs the UI layer, how you avoid leaking `Context`/Views into ViewModel.
- How you expose one-time events (navigation, snackbars) from ViewModel without them re-firing on rotation/recomposition.
- Modular app architecture — how you'd split a monolithic app into `:core`, `:feature-x`, `:data` modules, and why (build times, ownership boundaries, testability).
- Repository pattern — single source of truth between network and local DB.

### Persistence
- SharedPreferences / Jetpack `DataStore` (Preferences vs Proto) — why DataStore replaced SharedPreferences and what problem it actually solves (async, no UI-thread blocking, type safety).
- Room: entities, DAOs, migrations (this is where "multi-table databases" from the JD will get probed) — foreign keys, relations (`@Relation`), transactions, and how you handle a schema migration in production without losing user data.
- Caching strategy for offline-first apps (very relevant given the case-based-learning-offline-access angle of this product).

### Networking / APIs
- Retrofit + OkHttp — interceptors (logging, auth headers), error handling, retry logic, cancellation tied to lifecycle/coroutines.
- How you'd design an API layer for **video streaming at scale**: chunked/range requests, adaptive bitrate switching, resumable downloads, handling flaky mobile networks gracefully — this is the part unique to this JD, don't skip prepping it.
- Pagination strategies (Paging 3 library) if discussing large lists of cases/videos.

### Jetpack components (pick the ones you know deepest — Compose, Navigation, WorkManager are named explicitly)
- WorkManager: when you'd use it over a foreground service or coroutine, constraints (network/battery), chaining work, guaranteed execution across process death — likely relevant for background video downloads/sync in this app.
- Compose (see UI section above) and Navigation-Compose specifics.

### Testing
- Unit testing ViewModels — faking/mocking repositories, testing coroutines with `runTest`/`TestDispatcher`.
- What you *don't* unit test (UI rendering) vs what you do (business logic, mappers, ViewModel state transitions).
- Given your own past feedback on this: you've been burned before by mocking a database in integration tests and having it mask a real migration bug — a good answer here is "unit test business logic with fakes, but integration-test persistence against a real DB," which is consistent with how you already work.

### Scale / systems thinking (the "why is this breaking at scale" angle they explicitly want)
- Be ready for scenario questions like: "Video buffers for users on 3G in tier-2 cities — how do you investigate?" or "App crashes only after 6 hours of background playback — where do you start?"
- Security basics: certificate pinning, secure storage of tokens (EncryptedSharedPreferences/Keystore), not logging PII (relevant — this is a *healthcare* app, so PII/PHI handling questions are fair game).
- Stability: ANR causes, memory leaks (LeakCanary), StrictMode, crash-reporting triage (Firebase Crashlytics) — how you'd prioritize a crash affecting 0.1% of a 300k-user base.

## Good questions to ask them (shows you read the JD, not generic)

- "What does the current video pipeline look like — is transcoding/adaptive bitrate handled server-side, or does the Android app do meaningful client-side logic around that?"
- "Since this is a healthcare platform, how does the team think about offline access for doctors with unreliable connectivity — is that a client-side priority?"
- "How is the app modularized today, and what's the biggest pain point in the current architecture?"
- "What does 'own a piece of the streaming system' look like day-to-day — is it app-side only, or do Android engineers get visibility into the backend/CDN decisions?"

## One honest gap to watch for

The JD wants "petabyte-scale streaming" experience framed almost like infra ownership — if your actual hands-on experience is more standard CRUD/API-consumption Android work (which is fine and common), don't oversell depth you don't have. Better answer: be honest about your current level, but show you *understand the shape of the problem* (buffering, bitrate, caching, offline) and are eager to go deep — that matches their own framing of wanting people who "own a piece of it" rather than already being experts.
