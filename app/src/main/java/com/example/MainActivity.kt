package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.StudyScreen
import com.example.ui.StudyViewModel
import com.example.ui.theme.GemmaStudyTheme

class MainActivity : ComponentActivity() {
    private val studyViewModel: StudyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GemmaStudyTheme {
                StudyScreen(viewModel = studyViewModel)
            }
        }
    }
}
