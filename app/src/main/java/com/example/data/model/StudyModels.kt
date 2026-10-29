package com.example.data.model

enum class SubjectType(val displayName: String, val iconResName: String, val promptContext: String) {
    CODING(
        displayName = "Coding",
        iconResName = "code",
        promptContext = "Subject: Computer Science & Software Engineering. Focus on algorithms, clean code, time/space complexity, and architecture."
    ),
    MATHEMATICS(
        displayName = "Mathematics",
        iconResName = "functions",
        promptContext = "Subject: Pure and Applied Mathematics. Focus on rigorous derivations, theorems, calculus, algebra, and step-by-step logical proofs."
    ),
    SCIENCE(
        displayName = "Science",
        iconResName = "science",
        promptContext = "Subject: Natural & Physical Sciences (Physics, Chemistry, Biology). Focus on empirical laws, scientific methods, formulas, and phenomena."
    ),
    ESSAY(
        displayName = "Essay/Misc",
        iconResName = "edit_note",
        promptContext = "Subject: Writing, Humanities, and General Analysis. Focus on thesis development, rhetorical structure, coherent argument flow, and critical thinking."
    )
}

enum class StudyMode(val title: String, val subtitle: String, val instructions: String) {
    TEACH(
        title = "Teach",
        subtitle = "Socratic Dialogue",
        instructions = "You are a master Socratic study tutor. NEVER give the final answer or code outright. Instead, ask a thoughtful, probing question that guides the student toward the next logical step. Break complex problems into intuitive pieces. If the student is stuck, provide a hint or analogy, then ask what they think follows."
    ),
    DEMONSTRATE(
        title = "Demonstrate",
        subtitle = "Direct Solution",
        instructions = "You are an expert technical instructor. Deliver an immediate, fully worked-out solution. For code, provide clean, idiomatic, runnable code wrapped in ```language blocks with concise line comments. For math and science, write out full step-by-step derivations."
    )
}

sealed class EngineStatus {
    object Checking : EngineStatus()
    data class Loaded(val modelPath: String) : EngineStatus()
    data class MissingWeights(val checkedPath: String) : EngineStatus()
    data class DemoMode(val simulatedModel: String) : EngineStatus()
    object Generating : EngineStatus()
    data class Error(val message: String) : EngineStatus()
}

data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val subject: SubjectType = SubjectType.CODING,
    val mode: StudyMode = StudyMode.TEACH
)

data class StudyNote(
    val id: Long = 0,
    val title: String,
    val content: String,
    val subject: SubjectType = SubjectType.CODING,
    val timestamp: Long = System.currentTimeMillis()
)
