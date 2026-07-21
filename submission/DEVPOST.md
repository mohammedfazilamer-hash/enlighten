# OpenAI Build Week Submission Copy

## Project Name

Enlighten

## Tagline

Turn screenshots, documents, and notes into spoken lessons, clear explanations, flashcards, quizzes, and a private local tutor.

## Category

Education

## One-Sentence Pitch

Enlighten is a local-first Android study companion that captures almost any learning material, reads it aloud with live highlighting, and transforms it into an explanation and active-recall tools without a paid AI API.

## Inspiration

Study material rarely arrives in the format a student needs. It may be a photo of a classroom board, several screenshots, a scanned PDF, or a dense textbook paragraph. A student who is tired, has difficulty focusing, prefers audio, or simply needs a concept explained must jump between OCR tools, reading apps, chatbots, notes, and flashcard products.

I wanted one private workflow that starts with the material already in front of the student. The goal was not just to read words from a screen. It was to help the student move from capture, to listening, to understanding, to active recall without requiring a subscription or sending study files to a cloud OCR service.

## What It Does

Enlighten accepts pasted text, up to ten screenshots, a camera photo, a PDF, DOCX, or TXT file. It extracts the text on the Android phone and reads the passage aloud using an installed offline voice. As the speech engine reports progress, Enlighten highlights the current word and gives the student sentence-level playback controls.

When automatic explanation is enabled, the app begins preparing a structured AI explanation while the source is still being read. When the source ends, it continues directly into the explanation. The student can then generate editable flashcards, switch to a self-check quiz, ask passage-grounded tutor questions, and save the entire study set locally.

The AI runs through Ollama on the student's own computer using `llama3.2:3b`. This keeps the product free of paid inference APIs and makes the privacy boundary understandable: files and OCR stay on the phone, while only text prompts travel over the student's private local network to their own model.

## How I Built It

The Android application is written in Kotlin with Jetpack Compose and Material 3. Core state is exposed through `StateFlow` from a ViewModel. Bundled ML Kit performs image OCR. Android `PdfRenderer` converts scanned PDF pages for recognition, while DOCX content is parsed from its XML package. Android `TextToSpeech` provides offline speech, sentence controls, and word-range callbacks. Private app files store study sets and the profile image with atomic writes.

The app communicates directly with Ollama through its local `/api/tags` and `/api/generate` endpoints. Explanation and tutor prompts use low temperatures for stable educational output. Flashcards request structured JSON. The selected 3.2B quantized model occupies about 2 GB and generated approximately 133 tokens per second on the development RTX 3070 during a measured warm benchmark.

Codex was the engineering collaborator throughout the build. It inspected the codebase, implemented feature slices, wired Android platform APIs, debugged real Pixel 9 behavior, ran tests and lint, and performed a complete architecture and security audit. I directed the learning experience, privacy constraints, product priorities, and real-device acceptance tests.

**Submission blocker:** Before pasting this section into Devpost or recording narration, verify the exact Codex model shown in the Build Week task. Replace this sentence with a truthful description of how Codex with the confirmed model was used. Do not claim GPT-5.6 until the UI or `/feedback` details confirm it.

## Challenges

### Coordinating speech and AI

The most interesting interaction problem was making local AI latency disappear into the reading experience. Enlighten launches explanation generation while Android is speaking the original passage. A narration state machine tracks whether the app is reading the source, waiting for AI, or reading the explanation. Generation IDs stop old TTS callbacks from advancing a newer narration session.

### Reliable text extraction

Different inputs need different handling. Screenshots and photos can go directly to ML Kit. PDFs must be rendered page by page without exhausting phone memory. DOCX files are ZIP packages and require safe XML parsing. Every path must return clean text to the same study session while giving the student useful progress and errors.

### Local-network inference

Running AI privately avoids API cost but introduces setup, discovery, and security trade-offs. The app validates the server address, tests for the expected model, applies timeouts, and explains that the computer and phone must be reachable. The repository includes a Windows helper that starts Ollama on the LAN, plus clear warnings not to expose the port publicly.

## Accomplishments

- Built a coherent native Android product rather than a single AI prompt demo.
- Combined multi-image OCR, camera capture, and three document formats in one study flow.
- Added live speech highlighting and automatic continuation into the AI explanation.
- Built saved study sets, editable flashcards, quiz mode, and grounded tutor chat.
- Kept OCR, files, settings, and speech local to the student's devices.
- Tested the experience on a real Pixel 9 and produced repeatable Gradle tests and lint checks.
- Measured the local model instead of presenting guessed performance claims.

## What I Learned

Local-first AI is not simply a cloud request with a different URL. The product must explain availability, secure the network boundary, handle model warm-up, constrain document context, and remain useful when the model computer is unavailable.

I also learned that accessibility features can become the main product experience. Word highlighting, voice selection, adjustable speed, and automatic handoff are not secondary polish. Together they change dense source material into a guided lesson.

Finally, working with Codex was most effective when product decisions remained explicit. Codex could move quickly through implementation and verification once the learning goal and privacy constraint were clear.

## What's Next

The immediate engineering roadmap is token-aware chunking for long documents, source citations, cancellation and stale-result protection, and database-backed study history. The next product milestone is an on-device AI provider for compatible phones, with Ollama retained as an optional higher-quality local provider. Persistent mastery and spaced repetition will turn generated flashcards into a longer-term learning system.

## Built With

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Lifecycle and Navigation 3
- ML Kit Text Recognition
- Android TextToSpeech
- Android PdfRenderer and Storage Access Framework
- Kotlin coroutines and StateFlow
- Ollama
- Llama 3.2 3B Q4_K_M
- Codex

## Links To Fill Before Submission

- Repository: https://github.com/mohammedfazilamer-hash/enlighten
- Public YouTube demo: https://youtu.be/YXYXkX5M_H4
- Promotional site: https://enlighten-study.mohammedfazilamer.chatgpt.site
- Codex `/feedback` session ID: `ADD_FEEDBACK_SESSION_ID`
