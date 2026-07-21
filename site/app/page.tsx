const inputs = ["Screenshots", "Camera", "PDF", "DOCX", "Pasted text"];

const studyTools = [
  {
    title: "Hear the source",
    copy: "Offline Android voices, speed control, sentence navigation, and live word highlighting keep attention anchored to the passage.",
    accent: "teal",
  },
  {
    title: "Understand the idea",
    copy: "A private local model turns the material into a simple explanation, important terms, key points, and practice questions.",
    accent: "gold",
  },
  {
    title: "Practice immediately",
    copy: "Create editable flashcards, switch into a self-check quiz, and ask follow-up questions grounded in the same source.",
    accent: "coral",
  },
  {
    title: "Return without rebuilding",
    copy: "Save the source, explanation, cards, and tutor conversation together as a private study set on the phone.",
    accent: "blue",
  },
];

export default function Home() {
  return (
    <main>
      <section className="hero" id="top">
        <img
          className="hero-media"
          src="/enlighten-hero.png"
          alt="A student looking toward a glowing open book in the sky"
        />
        <div className="hero-shade" aria-hidden="true" />
        <nav className="site-nav" aria-label="Primary navigation">
          <a className="wordmark" href="#top" aria-label="Enlighten home">
            Enlighten
          </a>
          <div className="nav-links">
            <a href="#workflow">How it works</a>
            <a href="#privacy">Privacy</a>
            <a className="nav-action" href="#build-story">Build story</a>
          </div>
        </nav>

        <div className="hero-content">
          <p className="eyebrow">OpenAI Build Week 2026 · Education</p>
          <h1>Enlighten</h1>
          <p className="hero-lede">
            Turn the material in front of you into something you can hear,
            understand, and study.
          </p>
          <div className="hero-actions">
            <a className="button button-primary" href="#workflow">
              See the learning loop
            </a>
            <a className="button button-secondary" href="#build-story">
              Built with Codex
            </a>
          </div>
          <dl className="hero-proof" aria-label="Product highlights">
            <div>
              <dt>5</dt>
              <dd>ways to capture</dd>
            </div>
            <div>
              <dt>4</dt>
              <dd>active study tools</dd>
            </div>
            <div>
              <dt>$0</dt>
              <dd>paid AI APIs</dd>
            </div>
          </dl>
        </div>
      </section>

      <section className="intro-band" id="workflow">
        <div className="section-shell intro-grid">
          <div>
            <p className="section-kicker">One source. One continuous lesson.</p>
            <h2>From capture to active recall without losing your place.</h2>
          </div>
          <p className="intro-copy">
            Notes arrive as photos, screenshots, scanned pages, and dense
            documents. Enlighten brings extraction, listening, explanation,
            and practice into one calm Android workflow.
          </p>
        </div>
      </section>

      <section className="process-section" aria-labelledby="process-title">
        <div className="section-shell">
          <div className="section-heading">
            <p className="section-kicker">The learning loop</p>
            <h2 id="process-title">Capture. Listen. Understand. Practice.</h2>
          </div>
          <ol className="process-list">
            <li>
              <span>01</span>
              <h3>Bring the material</h3>
              <p>
                Paste text or import screenshots, a camera photo, PDF, DOCX,
                or TXT file. OCR and parsing happen on the phone.
              </p>
            </li>
            <li>
              <span>02</span>
              <h3>Follow every word</h3>
              <p>
                Listen with an installed offline voice while Enlighten tracks
                the current word and gives sentence-level controls.
              </p>
            </li>
            <li>
              <span>03</span>
              <h3>Move into meaning</h3>
              <p>
                The local tutor prepares an explanation while the source is
                read, then continues into cards, quiz mode, and tutor questions.
              </p>
            </li>
          </ol>
        </div>
      </section>

      <section className="product-section" aria-labelledby="product-title">
        <div className="section-shell product-grid">
          <figure className="phone-figure">
            <div className="phone-frame">
              <img
                src="/live-reading.png"
                alt="Enlighten reading a photosynthesis passage with the current word highlighted"
              />
            </div>
            <figcaption>Real Pixel 9 capture · live reading state</figcaption>
          </figure>
          <div className="product-copy">
            <p className="section-kicker">Designed for attention</p>
            <h2 id="product-title">Reading support that becomes the interface.</h2>
            <p>
              Enlighten does not hide accessibility behind a settings page.
              Highlighting, voice, pace, pause, and sentence navigation shape
              the main study experience.
            </p>
            <div className="input-strip" aria-label="Supported source types">
              {inputs.map((input) => (
                <span key={input}>{input}</span>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="tools-section" aria-labelledby="tools-title">
        <div className="section-shell">
          <div className="section-heading compact-heading">
            <p className="section-kicker">A complete study session</p>
            <h2 id="tools-title">More than a summary button.</h2>
          </div>
          <div className="tool-grid">
            {studyTools.map((tool) => (
              <article className={`tool-card ${tool.accent}`} key={tool.title}>
                <span className="tool-mark" aria-hidden="true" />
                <h3>{tool.title}</h3>
                <p>{tool.copy}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="privacy-section" id="privacy" aria-labelledby="privacy-title">
        <div className="section-shell privacy-grid">
          <div>
            <p className="section-kicker light-kicker">Local-first by design</p>
            <h2 id="privacy-title">The privacy boundary is visible.</h2>
            <p className="privacy-copy">
              Images, documents, OCR, speech, and saved sets stay on the phone.
              AI text goes to Ollama on the student&apos;s own computer over a
              trusted private network. There is no paid inference API, account,
              analytics SDK, or cloud OCR upload.
            </p>
          </div>
          <div className="architecture" aria-label="Local system architecture">
            <div className="architecture-node phone-node">
              <span>Android phone</span>
              <strong>Capture · OCR · speech · storage</strong>
            </div>
            <div className="architecture-link">
              <span>Private Wi-Fi</span>
            </div>
            <div className="architecture-node computer-node">
              <span>Personal computer</span>
              <strong>Ollama · Llama 3.2 3B</strong>
            </div>
          </div>
        </div>
      </section>

      <section className="build-section" id="build-story" aria-labelledby="build-title">
        <div className="section-shell build-grid">
          <div className="build-art" aria-hidden="true">
            <img src="/enlighten-hero.png" alt="" />
          </div>
          <div className="build-copy">
            <p className="section-kicker">Built with Codex</p>
            <h2 id="build-title">Product direction by a student. Engineering momentum from Codex.</h2>
            <p>
              The human builder defined the learning flow, privacy model, and
              feature priorities. Codex inspected the codebase, implemented the
              Android architecture, connected OCR and local inference, designed
              the speech state machine, fixed real-device issues, and verified
              the result with tests, lint, and a full technical audit.
            </p>
            <ul className="build-list">
              <li>Native Kotlin and Jetpack Compose</li>
              <li>Bundled ML Kit text recognition</li>
              <li>Android offline text-to-speech</li>
              <li>Ollama with Llama 3.2 3B</li>
            </ul>
          </div>
        </div>
      </section>

      <section className="closing-section">
        <div className="section-shell closing-grid">
          <div>
            <p className="section-kicker">Enlighten</p>
            <h2>Make almost any material teachable.</h2>
          </div>
          <a className="button button-dark" href="#top">
            Back to the beginning
          </a>
        </div>
      </section>

      <footer>
        <div className="section-shell footer-inner">
          <strong>Enlighten</strong>
          <span>OpenAI Build Week 2026 · Education entry</span>
        </div>
      </footer>
    </main>
  );
}
