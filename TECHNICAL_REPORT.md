# Enlighten Technical Architecture Report

**Assessment date:** 2026-07-18  
**Assessed workspace:** `C:\Users\User\OneDrive\Documents\college app\StudyReader`  
**Application:** Enlighten for Android  
**Package:** `com.example.studyreader`  
**Current version:** `1.0` (`versionCode 1`)  
**Report audience:** Software architects and AI engineers who do not have access to the source code

## 1. Executive Summary

Enlighten is a functional Android study assistant MVP. A student can paste text, extract text from screenshots, camera photos, PDFs, DOCX files, and TXT files, listen to the material through Android text-to-speech, generate a simplified explanation with a local language model, create flashcards, run a self-check quiz, ask passage-grounded tutor questions, and save study sets locally.

The Android application performs document selection, OCR, storage, interface rendering, and speech on the phone. It does not contain an LLM and it does not use a paid cloud API. AI requests are sent directly over the local Wi-Fi network to Ollama running on the user's Windows computer. Ollama serves `llama3.2:3b`, a 3.2 billion parameter, Q4_K_M quantized model occupying approximately 2.02 GB on disk. This design provides private, no-per-call-cost inference with good performance on the current RTX 3070 computer, but it means the phone must be able to reach that computer whenever AI features are used.

The product is beyond a proof of concept: its core study workflow is implemented, the APK builds, unit and instrumentation tests exist, lint passes without errors, and key workflows have been smoke-tested on a Pixel 9. It is not yet production-ready. The most important blockers are the mismatch between the application's 250,000-character import limit and Ollama's current 4,096-token runtime context, stale asynchronous AI responses that can be applied to the wrong active study set, unauthenticated cleartext Ollama traffic, potentially unbounded PDF bitmap allocation, incomplete backup/privacy controls, and the absence of release signing and Play Store deployment configuration.

The architecture is appropriate for one student and one computer on a trusted home network. It does not scale as a shared service because there is no central backend, identity system, request queue, authentication, or tenant isolation. Aggregate installations can scale independently if every user supplies their own computer. A production path should first harden the current local architecture, then introduce a provider abstraction that supports on-device AI on compatible phones and keeps Ollama as an optional higher-quality provider.

## 2. Assessment Method and Confidence

This report is based on a full inspection of the Kotlin source, Android resources, Gradle configuration, manifests, tests, generated APK, local Ollama installation, installed model metadata, and runtime behavior on the development computer. Build, test, lint, APK, model, CPU, RAM, and GPU values are measured. Latency ranges for OCR, long PDFs, and unsupported user volumes are engineering estimates and are labeled as such.

The workspace is not currently a Git repository, so the assessment cannot identify a commit SHA or determine change history. The report describes the files present on 2026-07-18.

## 3. Current Product Status

**Lifecycle stage:** Feature-complete local MVP, suitable for supervised personal testing.

**Operational model:** One Android phone communicates with one Windows computer on the same reachable network.

**Build status:** `testDebugUnitTest`, `lintDebug`, and `assembleDebug` pass. Lint reports 0 errors and 26 warnings.

**Test inventory:** 9 local unit tests and 7 Android instrumentation tests. Instrumentation tests compile. The latest feature set was smoke-tested on a Pixel 9, but the full instrumentation suite was not rerun after the latest changes because doing so would clear or alter the user's application data.

**Distribution status:** Debug APK only. There is no production application ID, release signing, Play App Bundle, Play Store listing, CI/CD workflow, or formal versioning process.

**Current artifact:** `artifacts/Enlighten-debug.apk`

| Artifact property | Value |
|---|---:|
| File size | 64,258,664 bytes |
| Approximate size | 61.3 MiB |
| SHA-256 | `46B8AAD8C3EDE6F73D0F18DBC59F56F03C9353D588277CFFD9997866691DDC99` |

## 4. Completed Features

| Area | Implemented behavior | Status |
|---|---|---|
| Study input | Paste and edit text in a large study text field | Complete |
| Screenshot OCR | Select up to 10 images at once and append recognized text | Complete |
| Camera OCR | Capture a full-resolution image through the system camera and recognize its text | Complete |
| Document import | Import PDF, DOCX, and UTF-8 TXT files through the Android system picker | Complete |
| Read aloud | Speak study text using Android `TextToSpeech` | Complete |
| Playback controls | Previous sentence, next sentence, pause/resume, stop, speed, and progress | Complete |
| Live highlighting | Highlight the spoken word when the TTS engine provides range callbacks | Complete |
| Automatic explanation | Generate an AI explanation while reading and begin reading it after the source finishes | Complete |
| AI explanation | Return a simple explanation, important terms, key points, and quiz questions | Complete |
| Flashcards | Generate six cards, edit/delete cards, flip cards, and mark self-check mastery | Complete |
| Quiz mode | Show flashcard prompts as a manual self-check quiz | Complete with limitations |
| Ask Tutor | Ask passage-grounded follow-up questions with recent chat history | Complete |
| Saved study sets | Create, open, rename, update, and delete local study sets | Complete |
| Profile photo | Select and persist a profile image in private application storage | Complete |
| Appearance | Teal, Forest, Rose, and Blue palettes with system light/dark mode | Complete |
| Voice settings | Select an installed offline voice and save speech rate | Complete |
| Connection settings | Edit, save, and test the Ollama server address | Complete |
| Privacy baseline | No analytics, account, API key, or cloud application backend | Complete |

The quiz is a self-assessment interface, not an automatically graded quiz. Mastery marks are transient and are not persisted as spaced-repetition history.

## 5. Planned and Candidate Features

No formal issue tracker or product requirements document exists. The following items have been discussed or are natural next steps, but are not implemented:

| Feature | Current state | Recommended disposition |
|---|---|---|
| Fully on-device AI | Not implemented | Highest-value product direction after correctness fixes |
| Gemini Nano / ML Kit Prompt API | Not implemented | Add behind a `StudyAiProvider` abstraction for supported devices |
| Small bundled LLM | Not implemented | Investigate only after APK, memory, thermal, and license testing |
| External AI app sharing | Not implemented | Optional fallback using Android share intents; cannot provide seamless API behavior |
| Spaced repetition | Not implemented | Add persistent scheduling after migrating storage to Room |
| Automatic quiz grading | Not implemented | Add structured answers and deterministic grading rules |
| Cloud sync and accounts | Not implemented | Defer until multi-device demand justifies privacy and infrastructure cost |
| Search, tags, folders | Not implemented | Add once saved-set storage is migrated to a database |
| Native PDF text extraction | Not implemented | Replace OCR for digitally generated PDFs where text is available |
| Multi-language OCR | Not implemented | Add optional ML Kit script packages and language selection |
| Streaming AI output | Not implemented | Add to reduce perceived latency and allow cancellation |
| Citations / retrieval | Not implemented | Add passage references and chunk identifiers before presenting answers as grounded |
| Play Store release | Not implemented | Requires production identity, signing, policy, privacy, and QA work |

## 6. Technology Stack

### 6.1 Build and Language

| Component | Version / configuration |
|---|---|
| Language | Kotlin 2.3.20 |
| Java source compatibility | Java 17 |
| Kotlin JVM toolchain | 17 |
| Gradle wrapper | 9.1.0 |
| Android Gradle Plugin | 9.0.1 |
| Compose compiler plugin | Kotlin Compose plugin 2.3.20 |
| Kotlin serialization plugin | 2.3.20, used for navigation keys |
| Build JDK observed | Android Studio JBR 21.0.10 |
| Gradle maximum heap | 2,048 MB |
| Build caches | Build cache and configuration cache enabled |

The build directory is redirected to `%USERPROFILE%\.gradle\studyreader-build\StudyReader` to avoid file-locking problems caused by building inside a OneDrive-synchronized directory.

### 6.2 Android Platform

| Component | Version / configuration |
|---|---|
| Minimum Android API | 26, Android 8.0 |
| Compile SDK | 36 |
| Target SDK | 36 |
| Application type | Single-activity native Android application |
| UI toolkit | Jetpack Compose |
| Design system | Material 3 |
| Navigation | AndroidX Navigation 3 |
| Supported theme | System light/dark plus four user palettes |

### 6.3 Frameworks and Libraries

| Library | Declared or resolved version | Purpose |
|---|---:|---|
| AndroidX Core KTX | 1.18.0 | Android platform utilities |
| Activity Compose | 1.13.0 | Compose activity integration |
| Lifecycle | 2.10.0 | ViewModel and lifecycle-aware state |
| Navigation 3 | 1.0.1 | Back stack and route rendering |
| Lifecycle Navigation 3 | 2.10.0 | Navigation lifecycle integration |
| Compose BOM | 2026.03.01 | Compose dependency alignment |
| Compose UI / runtime | 1.10.6 resolved | Declarative UI and state runtime |
| Material 3 | 1.4.0 resolved | UI components and theme |
| Material icons extended | 1.7.8 | Interface icons |
| ML Kit Text Recognition | 16.0.1 bundled | On-device Latin-script OCR |
| Kotlin coroutines | 1.9.0 resolved transitively | Background work and flows |
| `org.json` | Android platform | Ollama payload and local JSON handling |
| JUnit | 4.13.2 | Local tests |
| AndroidX Test Core | 1.7.0 | Android tests |
| AndroidX Test Ext JUnit | 1.3.0 | Android JUnit integration |
| AndroidX Test Runner | 1.7.0 | Instrumentation runner |
| Espresso | 3.7.0 | UI test support |
| Coroutines Test | 1.10.2 | Coroutine test support |

There is no Retrofit, OkHttp, Room, DataStore, Hilt, Koin, image-loading framework, analytics SDK, crash-reporting SDK, or cloud service SDK.

## 7. Project Structure

```text
StudyReader/
|-- app/
|   |-- build.gradle.kts
|   `-- src/
|       |-- main/
|       |   |-- AndroidManifest.xml
|       |   |-- java/com/example/studyreader/
|       |   |   |-- MainActivity.kt
|       |   |   |-- Navigation.kt
|       |   |   |-- NavigationKeys.kt
|       |   |   |-- data/
|       |   |   |   |-- CameraImageStore.kt
|       |   |   |   |-- DocumentTextExtractor.kt
|       |   |   |   |-- OllamaClient.kt
|       |   |   |   |-- ProfileImageStore.kt
|       |   |   |   |-- ScreenshotTextExtractor.kt
|       |   |   |   `-- StudySetStore.kt
|       |   |   |-- ui/main/
|       |   |   |   |-- FlashcardStudyTools.kt
|       |   |   |   |-- MainScreen.kt
|       |   |   |   |-- MainScreenViewModel.kt
|       |   |   |   |-- SettingsContent.kt
|       |   |   |   |-- SpeechText.kt
|       |   |   |   |-- StudySetLibrary.kt
|       |   |   |   `-- TutorChat.kt
|       |   |   `-- ui/theme/
|       |   |       |-- Color.kt
|       |   |       |-- Theme.kt
|       |   |       `-- Type.kt
|       |   `-- res/
|       |       |-- drawable and mipmap launcher assets
|       |       |-- values and values-night resources
|       |       `-- xml FileProvider and backup rule files
|       |-- test/
|       `-- androidTest/
|-- artifacts/Enlighten-debug.apk
|-- build.gradle.kts
|-- gradle.properties
|-- settings.gradle.kts
`-- start-ollama.cmd
```

There are 20 production Kotlin files totaling approximately 3,873 lines and two test source files totaling approximately 350 lines. `MainScreen.kt` is approximately 1,377 lines and `MainScreenViewModel.kt` approximately 547 lines. These two files contain most product behavior and are the largest maintainability hotspots.

## 8. System Architecture

Enlighten is a two-node local system. The Android client is the application and orchestration layer. Ollama is an independently installed local inference server. There is no Enlighten-owned backend process.

```mermaid
flowchart LR
    User["Student"] --> App["Enlighten Android app"]
    Picker["Android photo, camera, and document pickers"] --> App
    App --> OCR["Bundled ML Kit OCR"]
    App --> TTS["Android TextToSpeech engine"]
    App --> Files["Private app files and SharedPreferences"]
    App -->|"HTTP over private Wi-Fi"| Ollama["Ollama on Windows PC"]
    Ollama --> Model["llama3.2:3b Q4_K_M"]
```

### 8.1 Architectural Boundaries

| Boundary | Responsibility |
|---|---|
| Compose UI | Input, rendering, dialogs, launchers, speech controls, and transient interaction state |
| `MainScreenViewModel` | Core screen state, AI operations, OCR/document imports, study-set operations, and error status |
| Data helpers | File storage, OCR, document extraction, profile image persistence, and HTTP requests |
| Android services | Camera, Storage Access Framework, photo picker, URI grants, and TTS engine |
| Ollama | Model loading, tokenization, prompt execution, and text generation |
| `llama3.2:3b` | Explanation, flashcard generation, and tutor answers |

The code uses interfaces around important data helpers, which helps testing, but it does not have formal domain, repository, and use-case layers. Dependencies are manually constructed from Compose when the ViewModel is created.

## 9. End-to-End Data Flow

### 9.1 Text Acquisition

```mermaid
sequenceDiagram
    actor Student
    participant UI as Compose UI
    participant VM as MainScreenViewModel
    participant Extractor as OCR or document extractor
    participant State as StateFlow
    Student->>UI: Paste text or choose media/document
    UI->>VM: Import selected URI values
    VM->>Extractor: Read and extract text on Dispatchers.IO
    Extractor-->>VM: Recognized or parsed text
    VM->>State: Append study text and clear derived AI data
    State-->>UI: Recompose text and status
```

Image OCR is performed entirely on the phone using the bundled Latin-script ML Kit model. PDF pages are rendered to bitmaps and then OCRed. DOCX files are opened as ZIP containers and text is read from `word/document.xml`. TXT files are decoded as UTF-8.

### 9.2 Read and Explain

```mermaid
sequenceDiagram
    actor Student
    participant UI as Compose and TTS controller
    participant VM as MainScreenViewModel
    participant O as OllamaClient
    participant L as Ollama and Llama model
    Student->>UI: Tap Read aloud
    UI->>UI: Split source into speech segments
    UI->>VM: Request explanation if automatic mode is enabled
    VM->>O: Generate explanation from source snapshot
    O->>L: POST /api/generate
    UI->>UI: Speak source one segment at a time
    L-->>O: Complete generated response
    O-->>VM: Explanation text
    VM-->>UI: Update StateFlow
    UI->>UI: Wait if source ended before AI response
    UI->>UI: Speak explanation automatically
```

The AI request and source narration run concurrently. This reduces total wait time. Because the AI response is non-streaming, the explanation cannot be displayed or spoken until generation is complete.

### 9.3 Save and Reload

The current title, source, explanation, flashcards, and tutor messages are serialized into one JSON file in private application storage. Saving or deleting acquires a mutex, reads the complete file, updates the in-memory list, and atomically rewrites the complete file. Opening a saved set replaces the ViewModel's current state.

## 10. AI Model and Runtime

### 10.1 Model Details

| Property | Measured value |
|---|---|
| Ollama model name | `llama3.2:3b` |
| Model ID | `a80c4f17acd5` |
| Digest | `a80c4f17acd55265feec403c7aef86be0c25983ab279d83f3bcd3abbcb5b8b72` |
| Architecture | Llama |
| Parameter count | 3.2 billion |
| Quantization | Q4_K_M |
| Embedding length | 3,072 |
| Model maximum context | 131,072 tokens |
| Current Ollama runtime context | 4,096 tokens |
| Stored model size | 2,019,393,189 bytes, about 2.02 GB or 1.88 GiB |
| Observed loaded runtime size | About 2.6 GB GPU memory |
| Observed execution device | 100% GPU |
| Keep-alive requested by app | 10 minutes |

### 10.2 Why This Model Was Chosen

`llama3.2:3b` is a sensible MVP compromise. It is small enough to fit comfortably in the current 8 GB RTX 3070 VRAM while leaving room for context and runtime overhead. It produces materially better explanations and structured study content than sub-one-billion-parameter models, runs fully offline after download, requires no API key, and has no per-request cost.

The trade-off is quality and availability. A 3B model is weaker at reasoning, factual reliability, instruction hierarchy, and long-document synthesis than larger local or frontier cloud models. Running it through a PC makes the phone dependent on that computer and network. The model file is relatively small, but the current effective context is only 4,096 tokens because Ollama was observed using its default runtime context rather than the model's theoretical maximum.

### 10.3 Current Prompt Strategies

The explanation prompt requests four sections: a simple explanation, important terms, key points, and three quiz questions. Temperature is `0.2`.

The flashcard prompt requests exactly six question-and-answer cards. It uses Ollama JSON output mode and temperature `0.15`. The client parses the returned object and tolerates a fenced JSON response. It accepts up to ten returned cards.

The tutor prompt contains the active passage, the last six tutor conversation messages, and the new question. Temperature is `0.2`. The answer is intended to remain grounded in the passage, but this is prompt guidance rather than a verified retrieval or citation mechanism.

### 10.4 Critical Context Limitation

The Android import layer accepts up to 250,000 characters. A 4,096-token context often holds roughly 12,000 to 18,000 English characters after prompt overhead, depending on vocabulary and formatting. A large imported document can therefore exceed the current AI context by more than an order of magnitude.

The application does not count tokens, split text into semantic chunks, summarize chunks, retrieve relevant chunks for tutor questions, or reserve output tokens. Long passages can be truncated by the runtime, lose important sections, produce incomplete explanations, or fail. Increasing `num_ctx` alone is not a complete fix because context memory and prompt-evaluation latency grow significantly. The recommended solution is token-aware chunking plus hierarchical synthesis and retrieval.

## 11. Backend Architecture and API Design

### 11.1 Backend Status

There is no custom Enlighten backend. The Android app calls Ollama directly using Java `HttpURLConnection` and `org.json`. The configured base URL defaults to the example `http://192.168.1.100:11434`, can be edited in settings, and is persisted in `SharedPreferences`.

The base URL validator accepts HTTP or HTTPS, a host, and an optional port. Paths, query strings, and fragments are rejected. The HTTP connect timeout is 10 seconds and the read timeout is 180 seconds.

### 11.2 Endpoints

| Method | Endpoint | Purpose | App behavior |
|---|---|---|---|
| `GET` | `/api/tags` | Connection and model availability check | Confirms Ollama is reachable and inspects installed model names |
| `POST` | `/api/generate` | Explanation generation | Sends source prompt, `stream:false`, temperature `0.2`, keep-alive `10m` |
| `POST` | `/api/generate` | Flashcard generation | Sends structured prompt, `format:"json"`, `stream:false`, temperature `0.15` |
| `POST` | `/api/generate` | Tutor answer | Sends passage, recent chat, and question with temperature `0.2` |

Example explanation request:

```json
{
  "model": "llama3.2:3b",
  "prompt": "You are a study tutor...\n\nText:\n<student text>",
  "stream": false,
  "keep_alive": "10m",
  "options": {
    "temperature": 0.2
  }
}
```

The client reads the final `response` field. It does not consume streaming newline-delimited JSON, expose token metrics, retry failed requests, apply exponential backoff, cancel an in-flight socket operation, or attach a request identifier.

### 11.3 API Design Assessment

Direct Ollama access is simple and appropriate for a private prototype. It minimizes code, infrastructure, cost, and data processors. It also exposes a low-level inference service directly to the network and tightly couples the Android client to Ollama's API and a specific model name.

A stronger local design would place a small authenticated gateway on the PC. The gateway would own prompt versions, model selection, chunking, request IDs, queues, rate limits, health checks, and TLS or a secure tunnel. An alternative is to keep direct Ollama access but add a provider interface in the app and secure network access through Tailscale, WireGuard, or a private reverse proxy.

## 12. Android Application Architecture

### 12.1 Activity and Navigation

`MainActivity` enables edge-to-edge rendering, loads the stored palette, applies the Material 3 theme, and renders `MainNavigation`.

Navigation 3 has one actual route, `Main`. Inside that route, an application drawer switches among three sections represented by local state:

| Section | Function |
|---|---|
| Study | Input, imports, narration, AI explanation, flashcards, quiz, and tutor |
| Library | Saved study-set list and management dialogs |
| Settings | Color palette and offline voice selection |

Navigation 3 is currently more infrastructure than the app requires, but it is a reasonable base if Library, Settings, flashcard sessions, and tutor chat become true routes later.

### 12.2 Screen Composition

The Study section is a vertically scrolling Compose layout. It contains title and save actions, source import controls, the study text editor, TTS controls, Ollama connection state, explanation actions and output, flashcard/quiz controls, and tutor chat.

The Library uses a lazy list and modal dialogs for rename and delete operations. Settings displays palette swatches and a voice selection dialog. Photo, camera, and document selection are handled through Android Activity Result APIs and system interfaces rather than custom file browsers.

### 12.3 State Management

Core content state is held in one immutable `MainScreenUiState` exposed as `StateFlow` from `MainScreenViewModel`. The ViewModel uses `viewModelScope` and switches file, OCR, and network work to background dispatchers.

Transient UI and speech state is held with Compose `remember` and `rememberSaveable`. Examples include drawer section, narration phase, current speech segment, highlighted range, flashcard index, quiz mode, and dialog state.

This design survives ordinary recomposition and the ViewModel survives configuration changes. It does not use `SavedStateHandle`, so unsaved study text and active derived state can be lost after process death, force stop, or memory reclamation. Several important asynchronous operation flags also live in a screen-wide state object, which makes unrelated operations difficult to coordinate safely.

### 12.4 Dependency Management

The ViewModel is manually created with concrete OCR, file, store, and Ollama implementations. No dependency injection framework is used. Manual construction is adequate at this size, but provider switching and comprehensive tests would benefit from a small dependency container or Hilt once the app grows.

## 13. Text Extraction Architecture

### 13.1 Screenshots and Camera

The application uses bundled ML Kit Text Recognition 16.0.1 for Latin-script OCR. Bundling allows recognition without downloading a model at first use. Multiple selected screenshots are processed sequentially, which controls peak memory but makes total time proportional to image count.

Camera capture writes to a temporary private cache file exposed through a non-exported `FileProvider`. Old camera images are cleaned after approximately 24 hours. Extracted text is appended to the source, and stale explanation, cards, and tutor data are cleared.

### 13.2 PDF

PDFs are opened with Android `PdfRenderer`. Up to 60 pages are rendered sequentially into ARGB bitmaps at a target width near 1,600 pixels and passed through OCR. Extracted text is capped at 250,000 characters.

This approach works for scanned PDFs but is inefficient and less accurate for digital PDFs that already contain selectable text. Page width scaling is constrained, but page height and total pixel area are not strictly capped. An unusually tall or malformed page can request a very large bitmap and cause an out-of-memory crash.

### 13.3 DOCX and TXT

DOCX is parsed without a third-party Office library. The file is treated as ZIP, `word/document.xml` is read, and paragraphs, text nodes, and tabs are extracted. The parser disables document type declarations and external entities and limits decompressed XML to 12 MB, reducing ZIP expansion and XML external entity risk.

TXT is decoded as UTF-8. Both formats are capped at 250,000 extracted characters. DOCX headers, footers, footnotes, comments, equations, images, tables with complex structure, and alternate content may not be represented fully.

## 14. Audio Playback and Text-to-Speech

### 14.1 TTS Strategy

Enlighten uses Android's platform `TextToSpeech` engine. It does not synthesize audio itself and does not store generated audio files. Only installed voices whose metadata reports that no network connection is required are offered. The selected voice and rate are persisted.

Source and explanation text are segmented with `BreakIterator.getSentenceInstance(Locale.getDefault())`. Segments are capped near 900 characters, with whitespace fallback splitting. One segment is submitted at a time with `QUEUE_FLUSH`.

### 14.2 Narration State Machine

The speech workflow has four phases:

| Phase | Meaning |
|---|---|
| `Idle` | No active narration |
| `ReadingStudyText` | Source passage is being spoken |
| `WaitingForExplanation` | Source ended before AI generation completed |
| `ReadingExplanation` | AI explanation is being spoken |

Utterance IDs encode the content type, narration generation, and segment index. A generation counter prevents completion callbacks from a previous TTS run from advancing a newer run.

### 14.3 Highlighting and Controls

`UtteranceProgressListener.onRangeStart` supplies character offsets when supported by the installed engine. Callbacks are posted to the main thread and mapped to the active source or explanation so the current word can be highlighted.

Previous and next move by speech segment. Stop invalidates the narration generation and stops the engine. Pause also calls `TextToSpeech.stop()`, so resume starts the current sentence again rather than continuing at the exact audio position. Changing speed affects subsequent synthesis; it does not restart the currently speaking segment.

The current design is robust enough for an MVP but should be extracted into a lifecycle-aware `SpeechController` with explicit events, unit tests, and a single state flow. TTS range callbacks vary by engine and voice, so highlighting must remain a progressive enhancement.

## 15. Persistence Architecture

| Data | Storage | Lifetime |
|---|---|---|
| Study sets | `filesDir/study_sets.json` through `AtomicFile` | Persistent until deletion/uninstall |
| Profile image | `filesDir/profile/profile.jpg` through `AtomicFile` | Persistent until replacement/uninstall |
| Server URL | `SharedPreferences` | Persistent |
| Palette | `SharedPreferences` | Persistent |
| Voice and speech rate | `SharedPreferences` | Persistent |
| Auto-read setting | `SharedPreferences` | Persistent |
| Camera captures | Private cache through `FileProvider` | Temporary, cleanup after about 24 hours |
| Unsaved active state | ViewModel and Compose memory | Lost after process death |

The profile image is decoded to at most 1,024 pixels for input handling and stored at at most 512 pixels as JPEG quality 90. This fixes the earlier issue where a picker URI grant could disappear after reopening the app.

Study-set writes are atomic and mutex-protected, which protects against partial writes and concurrent in-process modification. However, all sets share one JSON document. Every mutation is O(total library size), all records are loaded into memory, one unrecoverable file affects the entire library, and the nominal schema version does not have a formal migration framework. Room is the appropriate next storage layer.

## 16. Performance and Resource Estimates

### 16.1 Development Computer

| Resource | Measured configuration |
|---|---|
| CPU | Intel Core i7-12700KF, 12 cores / 20 threads |
| RAM | 15.83 GiB |
| GPU | NVIDIA GeForce RTX 3070, 8 GiB VRAM |
| Model execution | 100% GPU during observation |

### 16.2 AI Latency Benchmark

A local benchmark used a prompt of approximately 1,561 tokens.

| Condition | Wall time | Load time | Output | Generation rate |
|---|---:|---:|---:|---:|
| Cold model | 8.05 s | 6.17 s | 197 tokens | 133.4 tokens/s |
| Warm model | 2.19 s | 0.29 s | 251 tokens | 133.5 tokens/s |

The app requests a ten-minute keep-alive, so repeated work within a study session is normally warm. Wi-Fi transport usually adds only milliseconds on a healthy LAN. Because responses are non-streaming, the user sees no partial answer. Short requests should commonly finish in roughly 2 to 8 seconds on this computer; long inputs or slower computers can take tens of seconds or exceed the 180-second read timeout.

These benchmark numbers describe this exact machine and prompt. They are not service-level guarantees.

### 16.3 Phone-Side Estimates

| Workload | Estimated behavior |
|---|---|
| One typical screenshot | Approximately 0.2 to 1.5 seconds depending on resolution and device state |
| Ten screenshots | Approximately 2 to 15 seconds because processing is sequential |
| 60-page PDF | Approximately 20 to 90 seconds; scanned complexity and device thermals matter |
| Profile bitmap | Roughly 1 MiB decoded at 512 x 512 ARGB |
| 1,600 x 2,070 PDF page bitmap | Roughly 12.6 MiB ARGB before OCR overhead |
| 250,000-character text | At least about 0.5 MiB as UTF-16, but several copies can exist during UI, prompt, JSON, and save work |

OCR and parsing run away from the main thread, so the interface should remain responsive. Long jobs are not scheduled through WorkManager and do not survive process termination. PDF pages are recycled sequentially, limiting normal peak memory, but unbounded page aspect ratios remain an OOM risk.

### 16.4 APK and Build Performance

The 61.3 MiB debug APK is large for the feature count. The bundled OCR model and debug packaging contribute materially. Release minification is currently disabled, and no release size measurement exists. Production should enable R8/resource shrinking after testing and distribute an Android App Bundle so Play can optimize delivery.

## 17. Security and Privacy

### 17.1 Positive Properties

| Property | Benefit |
|---|---|
| No paid or cloud AI API | No API key leakage or per-call billing |
| OCR and document parsing on phone | Images and files are not uploaded to a third-party OCR service |
| Private internal file storage | Other ordinary apps cannot directly read saved study data |
| Atomic file writes | Reduced corruption risk on interrupted writes |
| System file and photo pickers | No broad storage permission is required |
| Non-exported FileProvider | Camera cache is exposed only through temporary URI grants |
| Secure DOCX XML settings | DOCTYPE and external entity processing are disabled |
| No analytics or crash SDK | No automatic behavioral telemetry leaves the device |

### 17.2 High-Risk Findings

| Risk | Severity | Impact | Recommended mitigation |
|---|---|---|---|
| Cleartext HTTP to Ollama | High | Anyone able to observe the LAN may read study text, questions, and generated answers | Use a VPN tunnel, HTTPS reverse proxy, or authenticated local gateway; restrict cleartext with a network security config |
| Ollama binds to all interfaces without app authentication | High | Other LAN clients can submit prompts, consume GPU resources, or access the service | Bind to a trusted interface, firewall private profile only, add authentication, never port-forward 11434 |
| Backup behavior is not explicitly constrained | High | Study sets, profile data, and preferences may enter Android cloud/device backup despite local-only expectations | Wire `dataExtractionRules` and `fullBackupContent` into the manifest or disable backup for sensitive data |
| Long documents exceed context silently | High | Incorrect or incomplete AI output may be presented as a complete explanation | Token-aware chunking, visible source coverage, citations, and hard input limits |
| AI prompt injection through source text | Medium | Imported text can direct the model to ignore tutor instructions | Delimit untrusted content, use structured roles where possible, validate outputs, and communicate limitations |
| No encryption or app lock | Medium | Anyone with an unlocked phone or accessible backup can read study data | Optional biometric lock and encrypted storage for sensitive use cases |
| No model output verification | Medium | Hallucinations may teach incorrect material | Ground answers to passage chunks, cite passages, add uncertainty language, and expose source comparison |
| No production privacy policy | Medium | Store release and user trust requirements are unmet | Document local processing, LAN transfer, retention, deletion, and optional future providers |

The current explicit model name is local, but a hardened installation should also disable Ollama cloud functionality where appropriate and document the setting. Llama model license and attribution requirements must be reviewed before public distribution.

## 18. Reliability, Bugs, Risks, and Technical Debt

### 18.1 Priority Findings

| Priority | Finding | Failure mode |
|---|---|---|
| P0 | 250,000-character input versus 4,096-token runtime context | Large documents are truncated, misunderstood, or rejected without clear coverage feedback |
| P0 | AI jobs are not correlated to the source/set version | A response started for one passage can be written into a different study set opened while the request is running |
| P1 | In-flight AI work is not cancelable from the UI | Stop narration does not stop automatic explanation generation; stale work consumes resources and can update state later |
| P1 | Cleartext, unauthenticated Ollama network exposure | Private content and GPU service are exposed to the reachable LAN |
| P1 | PDF bitmap pixel area is not bounded | A malformed or extreme page can exhaust phone memory |
| P1 | Backup XML resources are not referenced by the manifest | Sensitive local data may be backed up under platform defaults |
| P1 | No process-death recovery or autosave | Unsaved work is lost when Android kills the process |
| P2 | Model readiness check accepts any `llama3.2:*` tag | Status can say ready while generation still fails because exact `llama3.2:3b` is missing |
| P2 | Whole-library JSON persistence | Save/delete cost and corruption blast radius grow with every set |
| P2 | Non-streaming API with 180-second read | Long waits provide limited feedback and cannot be interrupted cleanly |
| P2 | Failed tutor messages remain as normal student messages | Conversation history can contain a question with no explicit failed/retry state |
| P2 | PDF always uses OCR | Digital PDFs are slower and less accurate than native text extraction |
| P2 | Latin OCR only | Other scripts are unsupported or inaccurate |
| P2 | Transient mastery and manual quiz grading | Learning progress is lost and quiz correctness is not measured |
| P3 | One-route Navigation 3 integration | Extra complexity without current navigation benefit |
| P3 | `MainScreen.kt` and ViewModel are oversized | UI, lifecycle, TTS, launchers, and orchestration are difficult to reason about and test |
| P3 | Package remains `com.example` | Not suitable as a stable production identity |
| P3 | README is behind current behavior | New engineers receive an incomplete product description |

### 18.2 Async Race Detail

Explanation, flashcard, tutor, and import work launches in `viewModelScope`, but job handles and content version identifiers are not retained. If the student starts an explanation, opens another set, or starts a new set before completion, the old result can update the newly active state. TTS generations protect only against stale speech callbacks; they do not protect AI or OCR state.

Each operation should capture an immutable request containing `studySetId`, `sourceHash`, and `revision`. The ViewModel should cancel superseded jobs and apply a result only if those identifiers still match active state.

### 18.3 Test Gaps

The project has useful parser and persistence tests, but lacks comprehensive coverage for ViewModel concurrency, request cancellation, TTS state transitions, process death, PDF dimension limits, large libraries, corrupt or migrated data, model-context behavior, network security, accessibility, and complete device workflows. There is no CI workflow or coverage report.

## 19. Deployment and Operations

### 19.1 Current Developer Build

Prerequisites are Android Studio/JBR, Android SDK 36, and a connected or wirelessly paired Android device.

```powershell
./gradlew testDebugUnitTest lintDebug assembleDebug
adb install -r artifacts/Enlighten-debug.apk
```

The build output redirection under the user's Gradle directory is intentional because OneDrive can lock generated files.

### 19.2 Ollama Computer Setup

```powershell
ollama pull llama3.2:3b
ollama run llama3.2:3b
```

`start-ollama.cmd` locates Ollama under `%LOCALAPPDATA%\Programs\Ollama`, checks port 11434, sets `OLLAMA_HOST=0.0.0.0:11434`, and starts the service minimized. The phone must use the computer's current LAN address and be able to reach TCP port 11434.

Production-like local setup should use a stable DHCP reservation, Windows Firewall limited to the private profile and trusted subnet, no router port forwarding, and a secure tunnel or authenticated gateway. A connection check in the app should verify the exact model, effective context, and server capabilities.

### 19.3 Android Installation

The current workflow uses ADB and a debug-signed APK. The phone requires Android 8.0 or newer. USB or wireless debugging is needed only for developer installation, not for ordinary use after installation. The phone and computer must be on mutually reachable networks for Ollama AI features. OCR, saved sets, and TTS remain usable without the computer.

### 19.4 Production Release Requirements

1. Replace `com.example.studyreader` with a controlled application ID.
2. Create and securely manage a release signing key.
3. Configure release build types, R8, resource shrinking, and reproducible versioning.
4. Produce and test a signed Android App Bundle.
5. Add explicit backup, data deletion, privacy, and network security policies.
6. Add CI for tests, lint, release build, dependency review, and artifact checksums.
7. Run instrumentation, accessibility, orientation, low-memory, offline, and supported-device test matrices.
8. Review the Llama/Ollama and bundled library licenses and provide required notices.
9. Prepare Play Console internal testing, privacy disclosures, screenshots, and support contact.

## 20. Scalability Assessment

### 20.1 Important Interpretation

The current product is decentralized. If 100 users each have a separate phone and separate computer, there is no shared Enlighten server to overload. The aggregate system scales by duplicating hardware, but setup, compatibility, updates, and support become difficult. If many users share one Ollama computer, inference and network capacity fail quickly.

### 20.2 User-Level Breakdown

| User count | Current architecture behavior | First things that break | Required evolution |
|---:|---|---|---|
| 100 | Feasible only as 100 isolated personal installations; poor as one shared server | Manual IP setup, firewall variability, support load, model/version drift; shared GPU requests queue for minutes | Signed distribution, automated diagnostics, provider abstraction, secure pairing, per-device on-device AI where possible |
| 1,000 | Operationally impractical to manage manually | No identity, update channel, telemetry, compatibility matrix, remote reachability, tenant isolation, or service queue | Play distribution, CI/CD, crash/health telemetry with consent, configuration management, on-device inference or authenticated gateway |
| 10,000 | Direct Ollama architecture is not a manageable product platform | Security exposure, support burden, model licensing/updates, inconsistent hardware, no data migration strategy | On-device-first architecture or managed multi-tenant inference, Room migrations, observability, policy/compliance, staged rollouts |
| 100,000 | Current architecture fails as a service design | No autoscaling, authentication, abuse control, quotas, billing controls, data layer, SLOs, disaster recovery, or global routing | On-device inference at the edge or OAuth-secured backend, autoscaled GPU pools, queues, rate limits, streaming, databases, observability, regional privacy controls |

### 20.3 Shared GPU Capacity Illustration

The measured model generated approximately 133 tokens per second with a warm model. A 200-token answer consumes roughly 1.5 seconds of generation time before prompt evaluation and scheduling. One hundred simultaneous requests cannot all receive that latency on one RTX 3070. Without an explicit queue and concurrency controls, contexts compete for VRAM, latency becomes unpredictable, and requests can time out. A simple serialized queue would put the last request several minutes behind the first.

### 20.4 Recommended Scale Strategy

An on-device model shifts inference cost and capacity to each phone and is the best fit for the application's privacy and no-subscription goal. Compatible devices can use Gemini Nano through ML Kit Prompt API or another supported mobile runtime. The application must handle model availability, feature download, foreground execution, token limits, device quotas, thermal constraints, and quality differences.

Ollama should remain an optional `High quality on computer` provider. If a shared cloud tier is ever introduced, it should be a separate opt-in provider behind the same interface rather than changing core study workflows.

## 21. Recommended Target Architecture

```mermaid
flowchart LR
    UI["Compose feature screens"] --> Coordinator["Study session coordinator"]
    Coordinator --> Extract["Text extraction providers"]
    Coordinator --> Speech["SpeechController"]
    Coordinator --> Repo["Room repositories"]
    Coordinator --> AI["StudyAiProvider interface"]
    AI --> Nano["On-device provider"]
    AI --> Local["Secure Ollama provider"]
    AI --> Share["External-app share fallback"]
    Repo --> Room["Room database"]
    Settings["DataStore settings"] --> Coordinator
```

Recommended core contracts:

```kotlin
interface StudyAiProvider {
    suspend fun explain(request: ExplainRequest): Flow<AiEvent>
    suspend fun createFlashcards(request: FlashcardRequest): Flow<AiEvent>
    suspend fun askTutor(request: TutorRequest): Flow<AiEvent>
    suspend fun capabilities(): AiCapabilities
}
```

Every request should include a set ID, source revision, selected chunks, prompt version, and cancellation semantics. Every result should include provider, model, latency, covered chunk IDs, and warnings. This makes model switching explicit and prevents stale results from silently contaminating another study set.

## 22. Recommended Improvements

### 22.1 Immediate Correctness and Security

1. Add token-aware chunking, per-chunk summaries, hierarchical synthesis, and chunk retrieval for tutor questions.
2. Add request IDs, active source revisions, retained `Job` handles, cancellation, and stale-result rejection.
3. Cap PDF width, height, and total pixels before bitmap allocation.
4. Verify exact model name and report effective context and server health.
5. Connect backup rules in the manifest or disable sensitive backup.
6. Restrict Ollama to trusted networking and add authentication or a secure tunnel.
7. Persist unsaved drafts with `SavedStateHandle` and periodic local autosave.

### 22.2 Architecture and Maintainability

1. Move speech lifecycle and callbacks into a tested `SpeechController`.
2. Split the main screen into feature-level routes or coordinators with smaller state models.
3. Replace the one-file JSON store with Room entities, transactions, indexes, and migrations.
4. Replace settings `SharedPreferences` with typed DataStore.
5. Introduce `StudyAiProvider` and `TextExtractor` provider contracts.
6. Use structured response schemas and store prompt/model versions with derived content.
7. Add streaming, progress, retry, and user cancellation.
8. Add CI, dependency updates, license inventory, release checks, and coverage reporting.

### 22.3 Product Quality

1. Add native extraction for digital PDFs and retain OCR as fallback.
2. Persist quiz results, mastery, and spaced-repetition due dates.
3. Add citations that link explanation claims to source chunks.
4. Add multi-language OCR and explicit study/TTS language selection.
5. Add library search, tags, export, import, and corruption recovery.
6. Add accessibility testing for TalkBack, dynamic text, contrast, and touch targets.
7. Bound tutor history and archive old messages to prevent unlimited UI and prompt growth.

## 23. Future Roadmap

| Phase | Goal | Deliverables |
|---|---|---|
| Phase 0 | Correctness and privacy | Context-aware chunking, race fixes, cancellation, PDF memory cap, backup policy, exact health checks, secure LAN guidance |
| Phase 1 | Stable architecture | Room, DataStore, speech controller, feature decomposition, provider interfaces, CI, expanded tests |
| Phase 2 | Phone independence | On-device Gemini Nano provider on supported devices, capability detection, Ollama optional fallback, provider selection UI |
| Phase 3 | Learning quality | Spaced repetition, graded quizzes, persistent mastery, citations, native PDF text, multilingual extraction and speech |
| Phase 4 | Distribution | Production ID/signing, app bundle, Play internal testing, privacy policy, license notices, staged releases |
| Phase 5 | Optional scale | Encrypted sync, accounts, multi-device, or managed AI only if user demand justifies cost and privacy trade-offs |

## 24. Explain This Project to Another AI Engineer

Enlighten is not an Android wrapper around a cloud API. It is a local-first Android application whose phone-side responsibilities are text acquisition, document/OCR processing, state, persistence, interface rendering, and speech. Its AI responsibility is delegated to a local Ollama server running `llama3.2:3b` on a Windows PC. The app calls Ollama directly over LAN HTTP and receives one complete response per operation.

The central product workflow is `source -> speak -> explain -> speak explanation -> study tools`. When automatic explanation is enabled, the app starts the model request at the same time it begins source narration. This is a deliberate latency-hiding decision: generation often finishes before narration, so the explanation can start immediately. The trade-off is concurrency complexity. The current implementation protects against stale TTS callbacks but does not correlate asynchronous model results with the passage revision that created them. Fix that before extending the workflow.

Input normalization happens on the phone. Screenshots and camera photos go through bundled Latin ML Kit OCR. PDFs are rendered page by page and OCRed, which supports scans but wastes work on digital documents. DOCX is parsed directly from its XML package, and TXT is read as UTF-8. All extracted text converges into one source string, and changing that source invalidates explanation, flashcards, and tutor history.

The Android state model is a single screen-level `StateFlow` plus Compose-local speech and interaction state. This kept the MVP easy to build but now concentrates too many concerns in `MainScreen.kt` and `MainScreenViewModel.kt`. Avoid adding more flags to those files. Extract a study-session coordinator, speech controller, persistence repositories, and AI provider interface. Use immutable request snapshots with IDs and revisions.

The most important non-obvious defect is context size. The model metadata supports a large theoretical context, but the observed Ollama process uses 4,096 tokens. The app accepts 250,000 characters and sends the text as one prompt. Do not solve this only by increasing context. Build a token-aware document pipeline: normalize, segment by headings/paragraphs, estimate or count tokens, summarize each chunk, synthesize the final explanation, and retrieve top relevant chunks for tutor questions. Preserve chunk IDs so the UI can show coverage and citations.

The direct Ollama design was chosen because it is free, private, and quick to implement. It is good for one user on trusted Wi-Fi. It is not an internet-facing backend and must never be treated as one. It has no application authentication, TLS, rate limiting, tenancy, queue, or stable discovery. For remote use, prefer an authenticated private tunnel. For broad consumer scale, prefer on-device inference and keep Ollama as an optional provider.

Persistence currently optimizes for simplicity: one atomic JSON file for every study set and SharedPreferences for settings. This is safe enough for a small personal library, but it scales linearly and lacks migrations, indexed queries, and partial recovery. Migrate to Room before adding spaced repetition, search, tags, or sync. Store AI provenance, prompt version, model name, source revision, and chunk coverage alongside generated content.

TTS is sentence-oriented. Pause is implemented by stopping the engine and replaying the current sentence on resume. Live highlighting depends on engine range callbacks and is not universally guaranteed. Preserve this fallback behavior, but isolate it from Compose so narration can be tested as a deterministic event/state machine.

The recommended strategic design is a `StudyAiProvider` abstraction with three possible implementations: on-device AI for independence and privacy, secure Ollama for higher local quality, and an external-app share fallback. Capability detection should drive the interface. The UI must communicate whether a provider is available, which data leaves the phone, what document portion was used, and when an answer may be incomplete.

In short: preserve the local-first product promise, fix source/version correlation and context handling first, harden LAN transport, then modularize. The current feature set is valuable and coherent; the next engineering work should improve correctness and trust before increasing feature count.

## 25. Reference Documentation

- [Ollama Generate API](https://docs.ollama.com/api/generate)
- [Ollama List Models API](https://docs.ollama.com/api/tags)
- [Ollama FAQ: networking, context, and local processing](https://docs.ollama.com/faq)
- [Android TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech)
- [Android UtteranceProgressListener](https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener)
- [ML Kit Text Recognition for Android](https://developers.google.com/ml-kit/vision/text-recognition/v2/android)
- [Android PdfRenderer](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer)
- [Android Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)
- [ML Kit GenAI APIs](https://developers.google.com/ml-kit/genai)
- [ML Kit Prompt API for Gemini Nano](https://developers.google.com/ml-kit/genai/prompt/android/get-started)
- [Google AI Edge LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM)
- [Gemma 3n model card](https://ai.google.dev/gemma/docs/gemma-3n/model_card)

## 26. Final Assessment

Enlighten has a strong personal-use MVP foundation and a clear user value proposition. Its current local architecture successfully avoids paid APIs and keeps most data processing on the user's devices. The application should not yet be released broadly because long-document correctness, asynchronous state integrity, LAN security, PDF memory safety, process recovery, and production distribution controls remain unresolved.

The highest-return next milestone is not another visible feature. It is a reliability release that makes every AI result traceable to the correct source revision, makes long-document coverage explicit, secures the computer connection, and protects data through process death and backup. Once that foundation is in place, on-device AI can remove the computer dependency without forcing a redesign of the study experience.
