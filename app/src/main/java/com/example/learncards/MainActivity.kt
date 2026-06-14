package com.prplegryn.quizcat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prplegryn.quizcat.ui.screen.HomeScreen
import com.prplegryn.quizcat.ui.screen.HomeViewModel
import com.prplegryn.quizcat.ui.theme.QuizcatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as QuizcatApp
        setContent {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(app.repository),
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            QuizcatTheme(themeMode = state.settings.theme) {
                HomeScreen(
                    state = state,
                    onRefresh = viewModel::refresh,
                    onGenerateToday = viewModel::generateToday,
                    onCompleteLearning = viewModel::completeLearning,
                    onAnswer = viewModel::answer,
                    onImportTextChange = viewModel::setImportText,
                    onPreviewImport = viewModel::previewImport,
                    onCommitImport = viewModel::commitImport,
                    onClearImportMessage = viewModel::clearImportMessage,
                    onThemeChange = viewModel::setTheme,
                    onAnimationsEnabledChange = viewModel::setAnimationsEnabled,
                    onHapticsEnabledChange = viewModel::setHapticsEnabled,
                    onResetSettings = viewModel::resetSettings,
                )
            }
        }
    }
}
