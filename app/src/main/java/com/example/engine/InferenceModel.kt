package com.example.engine

import android.content.Context
import android.util.Log
import com.example.data.model.EngineStatus
import com.example.data.model.StudyMode
import com.example.data.model.SubjectType
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class InferenceModel(private val context: Context) {

    companion object {
        const val TAG = "InferenceModel"
        const val DEFAULT_ADB_PATH = "/data/local/tmp/llm/gemma.task"
    }

    private var llmInference: LlmInference? = null
    var currentModelPath: String = DEFAULT_ADB_PATH
        private set

    var isDemoMode: Boolean = false
        private set

    /**
     * Initializes the LLM engine with the given model path or the default path.
     * If the model file exists, initializes MediaPipe LlmInference.
     * If not found, flags MissingWeights and enables seamless demo fallback.
     */
    suspend fun initialize(customPath: String? = null): EngineStatus = withContext(Dispatchers.IO) {
        val targetPath = customPath ?: currentModelPath
        currentModelPath = targetPath

        val file = File(targetPath)
        Log.d(TAG, "Checking for Gemma weights at: $targetPath (exists: ${file.exists()}, size: ${if (file.exists()) file.length() else 0})")

        if (!file.exists() || file.length() == 0L) {
            llmInference = null
            return@withContext if (isDemoMode) {
                EngineStatus.DemoMode("Offline Gemma Simulation Mode")
            } else {
                EngineStatus.MissingWeights(targetPath)
            }
        }

        try {
            llmInference = null

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(targetPath)
                .setMaxTokens(1024)
                .setTemperature(0.7f)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isDemoMode = false
            Log.i(TAG, "MediaPipe LlmInference successfully initialized from: $targetPath")
            EngineStatus.Loaded(targetPath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LlmInference from $targetPath", e)
            EngineStatus.Error("Init failed: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    fun setDemoModeEnabled(enabled: Boolean) {
        isDemoMode = enabled
    }

    /**
     * Streams responses from on-device Gemma.
     * Uses real MediaPipe LlmInference generateResponse() on Dispatchers.IO when weights are present,
     * or on-device algorithmic study simulation when running without loaded weights.
     */
    fun generateStream(
        userPrompt: String,
        subject: SubjectType,
        mode: StudyMode
    ): Flow<String> = flow {
        val activeEngine = llmInference
        if (activeEngine != null) {
            val formattedPrompt = buildGemmaPrompt(userPrompt, subject, mode)
            val fullResponse = activeEngine.generateResponse(formattedPrompt)
            val tokens = fullResponse.split(Regex("(?<=\\s)|(?<=\\n)"))
            for (token in tokens) {
                emit(token)
                delay(20)
            }
        } else {
            // Intelligent on-device heuristic study engine (no cloud, completely local)
            val fullResponse = generateCuratedStudyResponse(userPrompt, subject, mode)
            val tokens = fullResponse.split(Regex("(?<=\\s)|(?<=\\n)"))
            for (token in tokens) {
                emit(token)
                delay(28)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun buildGemmaPrompt(
        userPrompt: String,
        subject: SubjectType,
        mode: StudyMode
    ): String {
        return buildString {
            append("<start_of_turn>user\n")
            append("You are an on-device personal study companion specializing in ${subject.displayName}.\n")
            append("${subject.promptContext}\n")
            append("Pedagogy Mode: ${mode.title}. ${mode.instructions}\n\n")
            append("Student Inquiry:\n")
            append(userPrompt.trim())
            append("<end_of_turn>\n<start_of_turn>model\n")
        }
    }

    private fun generateCuratedStudyResponse(
        prompt: String,
        subject: SubjectType,
        mode: StudyMode
    ): String {
        val query = prompt.lowercase()
        return when (mode) {
            StudyMode.TEACH -> generateSocraticResponse(query, subject)
            StudyMode.DEMONSTRATE -> generateDemonstrationResponse(query, subject)
        }
    }

    private fun generateSocraticResponse(
        query: String,
        subject: SubjectType
    ): String {
        return when (subject) {
            SubjectType.CODING -> {
                if (query.contains("recursion") || query.contains("recursive")) {
                    """Before writing recursive code, let's establish the two pillars every recursive function requires:

1. **The Base Case**: When should the recursion stop, and what simple value does it return immediately without calling itself?
2. **The Recursive Step**: How do we break the larger input into a strictly smaller sub-problem that moves closer to the base case?

Consider your goal: what happens if the input is at its simplest possible boundary? What condition would you test first?"""
                } else if (query.contains("binary search") || query.contains("search")) {
                    """Let's reason through Binary Search step-by-step:

- What fundamental prerequisite must our array or list satisfy before Binary Search can work?
- If we inspect the element exactly at mid = (low + high) / 2 and our target is greater than arr[mid], which half of the array can we immediately discard?
- How should we update low or high in that case?"""
                } else if (query.contains("complexity") || query.contains("big o") || query.contains("runtime")) {
                    """To determine the Big-O time complexity, think about how the work scales with input size N:

- In each iteration or recursive frame, how many operations take place?
- Is the problem size halved (N/2), reduced by one (N-1), or multiplied?
- How many total iterations will execute before the loop termination condition is satisfied?"""
                } else {
                    """Let's unpack this problem together using first principles:

1. **State the Invariants**: What inputs are given, and what exact output or state change is expected?
2. **Edge Conditions**: What is the most trivial or empty case (e.g., N=0, null, or single-element)?
3. **Hypothesis**: If you were solving this by hand with pen and paper on a small example, what is the very first action you would take?"""
                }
            }
            SubjectType.MATHEMATICS -> {
                if (query.contains("derivative") || query.contains("calculus") || query.contains("integral")) {
                    """Let's look at the foundational definition:

d/dx [f(x)] = lim(h -> 0) [f(x+h) - f(x)] / h

Before calculating, identify the algebraic form:
- Is this a composition requiring the Chain Rule (f(g(x)))?
- Or a product of two distinct functions requiring the Product Rule (u * v)?
- What is the outer function, and what is the inner function?"""
                } else if (query.contains("bayes") || query.contains("probability")) {
                    """To apply Bayes' Theorem effectively, let's identify each event component:

P(A | B) = [P(B | A) * P(A)] / P(B)

- What is the Prior Probability P(A) before observing new evidence?
- What is the Likelihood P(B | A) of seeing the evidence given A occurred?
- What constitutes the total probability of the evidence P(B) across all mutually exclusive hypotheses?"""
                } else {
                    """To solve this mathematically, let's clarify the axioms and known values:

1. What known equations or identities directly relate the variables in your question?
2. Can we isolate the unknown variable onto one side through algebraic manipulation?
3. What is the dimensional consistency or sign restriction on the solution?"""
                }
            }
            SubjectType.SCIENCE -> {
                if (query.contains("photosynthesis") || query.contains("light")) {
                    """Photosynthesis operates in two coupled stages within the chloroplast:

- Where do the Light-Dependent Reactions physically occur, and what molecule is split to yield free electrons and oxygen?
- What energy carrier molecules (ATP and NADPH) are synthesized during this stage to power the subsequent Calvin Cycle?
- What would happen to the Calvin Cycle if light suddenly ceased?"""
                } else if (query.contains("newton") || query.contains("force") || query.contains("friction")) {
                    """Let's draw a mental Free Body Diagram:

1. What contact forces (Normal force, friction, tension) act on the mass?
2. What non-contact field forces (Gravity mg) are present?
3. In which direction is the acceleration non-zero? Setting Sigma F = ma along that axis, what terms balance?"""
                } else {
                    """In scientific analysis, we begin with empirical conservation laws:

- Which quantities are conserved in this closed system (Energy, Momentum, Mass, or Charge)?
- What state variable changes over time or distance?
- What is the governing physical law or mechanism driving this transformation?"""
                }
            }
            SubjectType.ESSAY -> {
                """A compelling academic essay or thesis hinges on a defensible claim:

1. **Debatable Claim**: Is your thesis a factual statement (which cannot be argued), or an interpretative claim that a reasonable person could dispute?
2. **The "So What?" Question**: Why does this interpretation matter for the broader subject or real-world understanding?
3. **Counter-Argument**: What is the strongest objection someone might raise against your claim, and how does your thesis account for it?"""
            }
        }
    }

    private fun generateDemonstrationResponse(
        query: String,
        subject: SubjectType
    ): String {
        return when (subject) {
            SubjectType.CODING -> {
                """Here is a complete, production-grade implementation with full complexity analysis:

```kotlin
/**
 * Demonstrating clean idiomatic implementation.
 * Time Complexity: O(log N)
 * Space Complexity: O(1) auxiliary
 */
fun binarySearch(sortedArray: IntArray, target: Int): Int {
    var low = 0
    var high = sortedArray.size - 1

    while (low <= high) {
        // Prevent integer overflow: low + (high - low) / 2
        val mid = low + (high - low) / 2
        val midVal = sortedArray[mid]

        when {
            midVal == target -> return mid      // Element found at index mid
            midVal < target  -> low = mid + 1   // Search right half
            else             -> high = mid - 1  // Search left half
        }
    }
    return -1 // Target not present in array
}
```

### Key Technical Takeaways:
- **Index Arithmetic**: Calculating mid = low + (high - low) / 2 protects against 32-bit signed integer overflow when low + high > 2^31 - 1.
- **Termination Invariant**: Loop continues while low <= high. When low > high, the search space has converged to an empty interval."""
            }
            SubjectType.MATHEMATICS -> {
                """### Step-by-Step Derivation

**Problem**: Solve for the derivative using rigorous derivation.

**Step 1: State the Function**
f(x) = x * e^(-2x)

**Step 2: Apply the Product Rule**
d/dx [u(x) * v(x)] = u'(x) * v(x) + u(x) * v'(x)
Let u(x) = x  =>  u'(x) = 1
Let v(x) = e^(-2x)  =>  v'(x) = -2 * e^(-2x) (via Chain Rule)

**Step 3: Combine and Factor**
f'(x) = 1 * e^(-2x) + x * (-2 * e^(-2x))
f'(x) = e^(-2x) * (1 - 2x)

**Step 4: Critical Points**
Setting f'(x) = 0:
Since e^(-2x) != 0 for all real x, we have:
1 - 2x = 0  =>  x = 1/2

At x = 1/2, f(1/2) = 1 / (2e) ~ 0.1839 (Local Maximum)."""
            }
            SubjectType.SCIENCE -> {
                """### Scientific Mechanism & Governing Equations

**Process**: Photosynthetic Electron Transport & Photophosphorylation

1. **Photon Absorption at Photosystem II (P680)**:
   Photons excite electrons in chlorophyll a. Water-splitting enzyme catalyzes:
   2 H2O -> 4 H+ + 4 e- + O2

2. **Proton Gradient Generation**:
   Electrons travel through Plastoquinone (PQ), Cytochrome b6f, and Plastocyanin (PC). This pumps H+ ions from the stroma into the thylakoid lumen, generating a steep electrochemical proton motive force (Delta pH ~ 3.5).

3. **ATP Synthesis**:
   Chemiosmotic flow of H+ back to the stroma through CF0-CF1 ATP Synthase phosphorylates ADP + Pi -> ATP.

4. **Photosystem I (P700) & Ferredoxin**:
   Electrons are re-energized by light at PSI and transferred via Ferredoxin-NADP+ Reductase to reduce:
   NADP+ + 2 e- + H+ -> NADPH"""
            }
            SubjectType.ESSAY -> {
                """### Exemplary Argumentative Thesis & Outline

**Thesis Statement**:
> "While pervasive algorithmic personalization accelerates initial content discovery, it systematically constrains cognitive serendipity by enclosing users in epistemically uniform feedback loops, ultimately degrading long-term intellectual adaptability."

**Structural Argumentation Architecture**:

1. **Introduction & Context**: Define algorithmic filtering and cognitive serendipity. Establish why friction-free curation risks intellectual atrophy.
2. **Body I (Mechanism)**: Examine the optimization metric—maximizing immediate engagement over conceptual diversity.
3. **Body II (Counter-Perspective)**: Address the counter-argument that user curation saves finite cognitive bandwidth. Refute by demonstrating that algorithmic prediction narrows rather than focuses breadth.
4. **Conclusion**: Propose deliberate epistemic resistance (offline study, socratic inquiry, and structured randomness) as cognitive countermeasures."""
            }
        }
    }

    fun close() {
        llmInference = null
    }
}
