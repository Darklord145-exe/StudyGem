# Offline Gemma Study Companion (Jetpack Compose + MediaPipe Tasks GenAI)

A 100% offline, distraction-free Android study companion powered by Google AI Edge (MediaPipe LLM Inference) running an on-device Gemma model, designed with deep OLED dark aesthetics and subtle neon accents.

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural direction incorporates the choices confirmed in Phase 1 interactive clarification:
> - **Visual Theme**: Deep OLED dark (`#0A0D14` obsidian base) with electric cyan (`#00F0FF`) and neon emerald (`#00FFA3`) accents for maximum readability and zero distraction.
> - **Companion Utilities**: Integrated Pomodoro focus timer and quick offline study notes drawer.
> - **Setup & Demo Strategy**: Detailed ADB setup guide dialog with one-tap file picker, alongside an intelligent offline demo mode that simulates on-device Socratic/Demonstration responses if large `.task` weights have not yet been pushed to `/data/local/tmp/llm/gemma.task`.

- **Confirmed Decision 1**: Socratic "Teach" mode (guiding questions, hint ladders, conceptual breakdown) vs. "Demonstrate" mode (instant derivations, runnable code solutions).
- **Confirmed Decision 2**: Zero cloud permissions—no `android.permission.INTERNET`, no cloud SDKs, no API keys.
- **Confirmed Decision 3**: MediaPipe `tasks-genai` integration with `LlmInference` streaming and cancellation support.

---

## 1. Overview & Core Concept

- **What It Does**: A completely offline, private AI study partner for learners studying complex technical subjects (Coding, Mathematics, Science, and Essay/Misc). It provides on-device intelligence via Google AI Edge with zero latency spikes, zero data telemetry, and total focus.
- **Target Audience**: Students, programmers, and self-learners working in flight, during offline study retreats, or in low-connectivity environments requiring zero-distraction focus.
- **Key Value**: Guaranteed privacy, zero subscription or API token costs, customized Socratic pedagogy, and real on-device Gemma inference running straight from device storage.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Launch & Engine Inspection**: The app automatically checks `/data/local/tmp/llm/gemma.task` (or user's selected file path). Status pill indicates "Model Loaded" (emerald), "Generating..." (cyan pulsing), or "Model Missing (Tap for ADB setup)" (amber).
2. **Subject & Mode Configuration**: 
   - User picks from 4 study domains: **Coding**, **Mathematics**, **Science**, **Essay/Misc**.
   - User toggles the segmented button between **"Teach"** (Socratic tutor) and **"Demonstrate"** (direct solution).
3. **Interactive Study Chat**:
   - User submits a prompt (e.g., "Help me understand binary search recursion").
   - Assistant streams response. In Socratic mode, it asks a probing question. In Demonstrate mode, it provides code in a dark syntax box with an instant "Copy Code" action button.
   - User can stop generation at any time with the "Stop" button.
4. **Study Session Drawer & Focus Timer**:
   - Top action bar exposes the Pomodoro timer (25:00 / 05:00 cycles with Start/Pause/Reset).
   - Quick Notes drawer allows jotting down takeaway formulas, flash notes, and key insights saved to local Room persistence.
5. **Model File Picker & ADB Setup Dialog**:
   - Folder icon in AppBar allows picking any `.task` or `.bin` file via Android's system file picker (`ActivityResultContracts.OpenDocument`).
   - ADB dialog shows the exact command: `adb push gemma-2b-it-cpu-int4.task /data/local/tmp/llm/gemma.task`.

### Visual Identity & Theme
- **Background**: Deep OLED black `#06080C` and obsidian card surfaces `#0F141F`.
- **Accents**: Cyber Cyan `#00E5FF` (primary actions), Neon Emerald `#00FF9D` (model ready & success states), Electric Purple `#A78BFA` (user message bubbles), Soft Coral `#FF5370` (stop action).
- **Monospace Code Containers**: Obsidian box with `#161B26` background, thin cyan border `#00E5FF40`, and JetBrains-inspired typography with syntax line indicator and one-click copy.
- **Motion & Feedback**: Smooth spring animations for mode switching, pulsating status badge during token streaming, and fluid drawer transitions.

---

## 3. Key Product Decisions & Trade-Offs

- **MediaPipe `tasks-genai` On-Device Architecture**:
  - *Chosen Approach*: `com.google.mediapipe:tasks-genai:0.10.35` configured with `LlmInferenceOptions`:
    * `maxTokens = 1024`
    * `temperature = 0.7f`
    * `modelPath = /data/local/tmp/llm/gemma.task` (or picked uri/path)
  - *Why*: Direct hardware-accelerated local CPU/GPU execution without network overhead.
- **Graceful Offline Simulation Fallback**:
  - *Chosen Approach*: When a physical `.task` model file is not present on the test device/emulator, the app provides a realistic offline heuristic study engine simulating Socratic and Demonstration replies for the selected subjects, while clearly highlighting the exact ADB push instructions.
  - *Why*: Allows immediate evaluation and testing in any emulator or container environment while maintaining full compatibility with actual Gemma weights.
- **Local Persistence via Room**:
  - *Chosen Approach*: Local SQLite Room database for chat sessions and quick study notes.
  - *Why*: 100% device-local, preserving study history and flashcards across reboots without requiring any cloud synchronization or account sign-in.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   MainActivity                         │
│  - Edge-to-Edge OLED Theme & Navigation Insets        │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                 StudyCompanionScreen                   │
│  - Top AppBar (Model Status Badge, Timer, File Picker) │
│  - Subject Chips (Coding, Math, Science, Essay)        │
│  - Mode Switcher (Teach / Demonstrate)                 │
│  - Message Feed (User Bubble, Markdown/Code Monospace) │
│  - Chat Input Bar (TextField, Send, Stop Button)       │
│  - Focus Timer & Quick Study Notes Drawer              │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                 StudyViewModel                         │
│  - UiState (messages, status, timer, selectedSubject)  │
│  - Socratic & Demonstration Prompt Engineering         │
│  - Coroutine Stream Orchestration & Cancellation       │
└──────────────┬──────────────────────────┬──────────────┘
               │                          │
┌──────────────▼────────────┐  ┌──────────▼──────────────┐
│     InferenceModel        │  │   StudyRepository       │
│ - MediaPipe LlmInference  │  │ - Room Database         │
│ - modelPath resolution    │  │ - Chat history          │
│ - Fallback Study Engine   │  │ - Focus study notes     │
└───────────────────────────┘  └─────────────────────────┘
```

### Component & State Mapping
- `SubjectType`: `CODING`, `MATHEMATICS`, `SCIENCE`, `ESSAY`
- `StudyMode`: `TEACH` (Socratic questioner), `DEMONSTRATE` (Immediate solution & derivations)
- `EngineStatus`: `LOADED`, `MISSING_WEIGHTS`, `GENERATING`, `ERROR`
- `ChatMessage`: `id`, `text`, `sender` (USER/ASSISTANT), `timestamp`, `codeSnippets`
- `StudyNote`: `id`, `title`, `content`, `subject`, `timestamp`
- `PomodoroState`: `remainingSeconds`, `isRunning`, `isBreak`
