package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessageEntity
import com.example.data.local.StudyDatabase
import com.example.data.local.StudyNoteEntity
import com.example.data.model.ChatMessage
import com.example.data.model.EngineStatus
import com.example.data.model.StudyMode
import com.example.data.model.StudyNote
import com.example.data.model.SubjectType
import com.example.engine.InferenceModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class StudyUiState(
    val selectedSubject: SubjectType = SubjectType.CODING,
    val studyMode: StudyMode = StudyMode.TEACH,
    val messages: List<ChatMessage> = emptyList(),
    val currentStreamingText: String = "",
    val isGenerating: Boolean = false,
    val engineStatus: EngineStatus = EngineStatus.Checking,
    val studyNotes: List<StudyNote> = emptyList(),
    val isNotesSheetOpen: Boolean = false,
    val isAdbDialogOpen: Boolean = false,
    val isTimerDialogOpen: Boolean = false,
    val timerSecondsRemaining: Int = 25 * 60,
    val isTimerRunning: Boolean = false,
    val isBreakTimer: Boolean = false
)

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StudyDatabase.getInstance(application)
    private val dao = db.studyDao()
    private val inferenceModel = InferenceModel(application)

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null
    private var timerJob: Job? = null

    init {
        // Collect messages from local Room database
        viewModelScope.launch {
            dao.getAllMessages().collect { entities ->
                _uiState.update { it.copy(messages = entities.map { e -> e.toModel() }) }
            }
        }

        // Collect study notes from local Room database
        viewModelScope.launch {
            dao.getAllNotes().collect { noteEntities ->
                _uiState.update { it.copy(studyNotes = noteEntities.map { e -> e.toModel() }) }
            }
        }

        // Initialize Inference Model with default path
        checkAndInitModel()
    }

    fun checkAndInitModel(customPath: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(engineStatus = EngineStatus.Checking) }
            val status = inferenceModel.initialize(customPath)
            _uiState.update { it.copy(engineStatus = status) }
        }
    }

    fun toggleDemoMode() {
        val nextDemo = !inferenceModel.isDemoMode
        inferenceModel.setDemoModeEnabled(nextDemo)
        checkAndInitModel()
    }

    fun selectSubject(subject: SubjectType) {
        _uiState.update { it.copy(selectedSubject = subject) }
    }

    fun setStudyMode(mode: StudyMode) {
        _uiState.update { it.copy(studyMode = mode) }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _uiState.value.isGenerating) return

        val currentSubject = _uiState.value.selectedSubject
        val currentMode = _uiState.value.studyMode

        val userMessage = ChatMessage(
            text = trimmed,
            isUser = true,
            subject = currentSubject,
            mode = currentMode
        )

        viewModelScope.launch {
            // Save user message to database
            dao.insertMessage(ChatMessageEntity.fromModel(userMessage))

            _uiState.update {
                it.copy(
                    isGenerating = true,
                    currentStreamingText = ""
                )
            }

            generationJob = launch {
                val responseBuilder = StringBuilder()
                try {
                    inferenceModel.generateStream(trimmed, currentSubject, currentMode)
                        .collect { chunk ->
                            responseBuilder.append(chunk)
                            _uiState.update {
                                it.copy(currentStreamingText = responseBuilder.toString())
                            }
                        }

                    val finalResponse = responseBuilder.toString()
                    if (finalResponse.isNotEmpty()) {
                        val assistantMsg = ChatMessage(
                            text = finalResponse,
                            isUser = false,
                            subject = currentSubject,
                            mode = currentMode
                        )
                        dao.insertMessage(ChatMessageEntity.fromModel(assistantMsg))
                    }
                } catch (e: Exception) {
                    Log.e("StudyViewModel", "Generation error", e)
                    val errorMsg = ChatMessage(
                        text = "⚠️ Generation interrupted: ${e.localizedMessage ?: "Unknown error"}",
                        isUser = false,
                        subject = currentSubject,
                        mode = currentMode
                    )
                    dao.insertMessage(ChatMessageEntity.fromModel(errorMsg))
                } finally {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            currentStreamingText = ""
                        )
                    }
                }
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        val partial = _uiState.value.currentStreamingText
        val currentSubject = _uiState.value.selectedSubject
        val currentMode = _uiState.value.studyMode

        if (partial.isNotBlank()) {
            viewModelScope.launch {
                val assistantMsg = ChatMessage(
                    text = "$partial\n\n*(Generation stopped by user)*",
                    isUser = false,
                    subject = currentSubject,
                    mode = currentMode
                )
                dao.insertMessage(ChatMessageEntity.fromModel(assistantMsg))
            }
        }

        _uiState.update {
            it.copy(
                isGenerating = false,
                currentStreamingText = ""
            )
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            dao.clearMessages()
        }
    }

    fun onModelFilePicked(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val fileName = "selected_gemma_${System.currentTimeMillis()}.task"
                val destFile = File(context.filesDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (destFile.exists() && destFile.length() > 0) {
                    Log.i("StudyViewModel", "Model file successfully copied to: ${destFile.absolutePath} (${destFile.length()} bytes)")
                    checkAndInitModel(destFile.absolutePath)
                } else {
                    _uiState.update {
                        it.copy(engineStatus = EngineStatus.Error("Selected file is empty or unreadable."))
                    }
                }
            } catch (e: Exception) {
                Log.e("StudyViewModel", "Error loading model from URI", e)
                _uiState.update {
                    it.copy(engineStatus = EngineStatus.Error("Failed to import model: ${e.localizedMessage}"))
                }
            }
        }
    }

    // Pomodoro Timer Controls
    fun startTimer() {
        if (_uiState.value.isTimerRunning) return
        _uiState.update { it.copy(isTimerRunning = true) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.isTimerRunning && _uiState.value.timerSecondsRemaining > 0) {
                delay(1000)
                _uiState.update { state ->
                    val next = state.timerSecondsRemaining - 1
                    if (next <= 0) {
                        // Switch between focus and break
                        val nextBreak = !state.isBreakTimer
                        val nextDuration = if (nextBreak) 5 * 60 else 25 * 60
                        state.copy(
                            timerSecondsRemaining = nextDuration,
                            isBreakTimer = nextBreak,
                            isTimerRunning = false
                        )
                    } else {
                        state.copy(timerSecondsRemaining = next)
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isTimerRunning = false) }
    }

    fun resetTimer(isBreak: Boolean = _uiState.value.isBreakTimer) {
        timerJob?.cancel()
        val duration = if (isBreak) 5 * 60 else 25 * 60
        _uiState.update {
            it.copy(
                isTimerRunning = false,
                isBreakTimer = isBreak,
                timerSecondsRemaining = duration
            )
        }
    }

    // Study Notes
    fun addNote(title: String, content: String, subject: SubjectType) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val entity = StudyNoteEntity(
                title = title.ifBlank { "Untitled Note" },
                content = content,
                subject = subject.name,
                timestamp = System.currentTimeMillis()
            )
            dao.insertNote(entity)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            dao.deleteNoteById(id)
        }
    }

    // Sheet / Dialog Visibility
    fun setNotesSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isNotesSheetOpen = open) }
    }

    fun setAdbDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isAdbDialogOpen = open) }
    }

    fun setTimerDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isTimerDialogOpen = open) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        generationJob?.cancel()
        inferenceModel.close()
    }
}
