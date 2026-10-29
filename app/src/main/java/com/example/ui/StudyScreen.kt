package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EngineStatus
import com.example.data.model.StudyMode
import com.example.data.model.SubjectType
import com.example.ui.components.AdbSetupDialog
import com.example.ui.components.MessageBubble
import com.example.ui.components.ModeSwitcherSegmentedButton
import com.example.ui.components.ModelStatusBadge
import com.example.ui.components.PomodoroTimerDialog
import com.example.ui.components.StreamingMessageBubble
import com.example.ui.components.StudyNotesSheet
import com.example.ui.components.SubjectChipsRow
import com.example.ui.theme.BrightAmber
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepDarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StopCoral
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividViolet
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // File Picker for Gemma weights (.task or .bin)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onModelFilePicked(uri)
        }
    }

    // Scroll to bottom when new messages arrive or when streaming begins
    LaunchedEffect(uiState.messages.size, uiState.currentStreamingText.isNotEmpty()) {
        val totalItems = uiState.messages.size + (if (uiState.isGenerating) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        containerColor = ObsidianBlack,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricCyan.copy(alpha = 0.15f))
                                .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gemma Study",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            ModelStatusBadge(
                                status = uiState.engineStatus,
                                onStatusClick = { viewModel.setAdbDialogOpen(true) }
                            )
                        }
                    }
                },
                actions = {
                    // Pomodoro Timer pill
                    val timerMinutes = uiState.timerSecondsRemaining / 60
                    val timerSecs = uiState.timerSecondsRemaining % 60
                    val timerText = String.format(Locale.getDefault(), "%02d:%02d", timerMinutes, timerSecs)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.isTimerRunning) NeonEmerald.copy(alpha = 0.2f) else DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (uiState.isTimerRunning) NeonEmerald else DarkSurfaceElevated
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.setTimerDialogOpen(true) }
                            .padding(end = 4.dp)
                            .testTag("timer_appbar_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Focus Timer",
                                tint = if (uiState.isTimerRunning) NeonEmerald else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = timerText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isTimerRunning) NeonEmerald else TextSecondary
                            )
                        }
                    }

                    // Study Notes Drawer Icon with Badge
                    IconButton(
                        onClick = { viewModel.setNotesSheetOpen(true) },
                        modifier = Modifier.testTag("notes_drawer_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (uiState.studyNotes.isNotEmpty()) {
                                    Badge(
                                        containerColor = VividViolet,
                                        contentColor = Color.White
                                    ) {
                                        Text("${uiState.studyNotes.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.NoteAlt,
                                contentDescription = "Study Notes",
                                tint = TextSecondary
                            )
                        }
                    }

                    // Model Folder Picker Icon
                    IconButton(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.testTag("pick_model_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Pick Gemma Weights File",
                            tint = ElectricCyan
                        )
                    }

                    // Overflow Menu
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = TextSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(DeepDarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear Chat", color = TextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = StopCoral)
                            },
                            onClick = {
                                showMenu = false
                                viewModel.clearChat()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ADB Setup Guide", color = TextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null, tint = ElectricCyan)
                            },
                            onClick = {
                                showMenu = false
                                viewModel.setAdbDialogOpen(true)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (uiState.engineStatus is EngineStatus.DemoMode) "Disable Demo Mode" else "Enable Demo Mode",
                                    color = TextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonEmerald)
                            },
                            onClick = {
                                showMenu = false
                                viewModel.toggleDemoMode()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBlack,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Top Controls: Subject Chips Row
            SubjectChipsRow(
                selectedSubject = uiState.selectedSubject,
                onSubjectSelected = { viewModel.selectSubject(it) }
            )

            // Socratic (Teach) vs. Direct (Demonstrate) Mode Switcher
            ModeSwitcherSegmentedButton(
                selectedMode = uiState.studyMode,
                onModeChanged = { viewModel.setStudyMode(it) }
            )

            // Missing Weights Warning Notice Banner (Dismissable or clickable to enable demo mode)
            AnimatedVisibility(
                visible = uiState.engineStatus is EngineStatus.MissingWeights,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = BrightAmber.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrightAmber.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = BrightAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Weights missing at /data/local/tmp/llm/gemma.task",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrightAmber,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrightAmber.copy(alpha = 0.25f))
                                .clickable { viewModel.toggleDemoMode() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Use Demo Mode",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrightAmber
                            )
                        }
                    }
                }
            }

            // Message Feed
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.messages.isEmpty() && !uiState.isGenerating) {
                    EmptyChatState(
                        subject = uiState.selectedSubject,
                        mode = uiState.studyMode,
                        onPromptSelected = { prompt ->
                            inputText = prompt
                            viewModel.sendMessage(prompt)
                            inputText = ""
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            MessageBubble(message = message)
                        }

                        if (uiState.isGenerating) {
                            item {
                                StreamingMessageBubble(
                                    streamingText = uiState.currentStreamingText,
                                    mode = uiState.studyMode
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            // Clean Input Bar
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                isGenerating = uiState.isGenerating,
                onStop = { viewModel.stopGeneration() },
                subject = uiState.selectedSubject
            )
        }
    }

    // Dialogs & Sheets
    AdbSetupDialog(
        isOpen = uiState.isAdbDialogOpen,
        onDismiss = { viewModel.setAdbDialogOpen(false) },
        onPickFileClick = { filePickerLauncher.launch(arrayOf("*/*")) },
        onToggleDemoMode = { viewModel.toggleDemoMode() },
        isDemoMode = uiState.engineStatus is EngineStatus.DemoMode
    )

    PomodoroTimerDialog(
        isOpen = uiState.isTimerDialogOpen,
        onDismiss = { viewModel.setTimerDialogOpen(false) },
        remainingSeconds = uiState.timerSecondsRemaining,
        isRunning = uiState.isTimerRunning,
        isBreak = uiState.isBreakTimer,
        onStart = { viewModel.startTimer() },
        onPause = { viewModel.pauseTimer() },
        onReset = { viewModel.resetTimer() },
        onToggleBreak = { viewModel.resetTimer(it) }
    )

    StudyNotesSheet(
        isOpen = uiState.isNotesSheetOpen,
        onDismiss = { viewModel.setNotesSheetOpen(false) },
        notes = uiState.studyNotes,
        currentSubject = uiState.selectedSubject,
        onAddNote = { title, content, subject ->
            viewModel.addNote(title, content, subject)
        },
        onDeleteNote = { viewModel.deleteNote(it) }
    )
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isGenerating: Boolean,
    onStop: () -> Unit,
    subject: SubjectType,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = DeepDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        text = "Ask a ${subject.displayName} question...",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                maxLines = 4,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = DarkSurfaceElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = ElectricCyan
                ),
                shape = RoundedCornerShape(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isGenerating) {
                // Stop Button
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(StopCoral.copy(alpha = 0.2f))
                        .border(1.dp, StopCoral, CircleShape)
                        .testTag("stop_generation_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Generation",
                        tint = StopCoral,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // Send Button
                val canSend = text.isNotBlank()
                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (canSend) ElectricCyan else DarkSurfaceVariant)
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) Color.Black else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatState(
    subject: SubjectType,
    mode: StudyMode,
    onPromptSelected: (String) -> Unit
) {
    val samplePrompts = when (subject) {
        SubjectType.CODING -> listOf(
            "Explain binary search invariants and edge cases",
            "How does recursive stack frame unwinding work?",
            "Design an LRU cache step-by-step",
            "What makes quicksort O(N^2) in worst case?"
        )
        SubjectType.MATHEMATICS -> listOf(
            "Derive Bayes Theorem from conditional probability",
            "Why is the derivative of e^x equal to e^x?",
            "Explain eigenvalue and eigenvector intuition",
            "Prove why square root of 2 is irrational"
        )
        SubjectType.SCIENCE -> listOf(
            "Trace the electron transport chain in photosynthesis",
            "Explain how the proton gradient drives ATP synthase",
            "Why does friction not depend on surface area?",
            "Explain wave-particle duality and the de Broglie wavelength"
        )
        SubjectType.ESSAY -> listOf(
            "How do I craft a strong counter-argument in my thesis?",
            "Evaluate the epistemic impact of algorithmic personalization",
            "Structure a 5-paragraph comparative literature essay",
            "Techniques for eliminating passive voice in academic papers"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan.copy(alpha = 0.12f))
                    .border(1.dp, ElectricCyan.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${subject.displayName} Companion",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mode: ${mode.title} (${if (mode == StudyMode.TEACH) "Socratic Inquiries" else "Direct Derivations"})",
                style = MaterialTheme.typography.bodySmall,
                color = if (mode == StudyMode.TEACH) NeonEmerald else ElectricCyan
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Starter Inquiries:",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(samplePrompts) { prompt ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DeepDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onPromptSelected(prompt) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ElectricCyan.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
