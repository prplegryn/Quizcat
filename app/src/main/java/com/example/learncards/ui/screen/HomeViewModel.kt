package com.prplegryn.quizcat.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.prplegryn.quizcat.data.CardEntity
import com.prplegryn.quizcat.data.DailyCardProgressEntity
import com.prplegryn.quizcat.data.DailySessionEntity
import com.prplegryn.quizcat.domain.AnswerAction
import com.prplegryn.quizcat.domain.AppSettings
import com.prplegryn.quizcat.domain.HomeSnapshot
import com.prplegryn.quizcat.domain.ImportPreview
import com.prplegryn.quizcat.domain.QuizcatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val settings: AppSettings = AppSettings(),
    val today: String = "",
    val itemCount: Int = 0,
    val session: DailySessionEntity? = null,
    val currentCard: CardEntity? = null,
    val currentProgress: DailyCardProgressEntity? = null,
    val progress: List<DailyCardProgressEntity> = emptyList(),
    val completedCount: Int = 0,
    val remainingCount: Int = 0,
    val failedCount: Int = 0,
    val activeOldKnowledgeCount: Int = 0,
    val tomorrowDueOldKnowledgeCount: Int = 0,
    val importText: String = "",
    val importPreview: ImportPreview? = null,
    val importMessage: String = "",
    val errorMessage: String = "",
    val isBusy: Boolean = false,
    val feedbackNonce: Int = 0,
    val feedbackResult: String = "",
)

class HomeViewModel(
    private val repository: QuizcatRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settingsFlow().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.loadHomeSnapshot() }
                .onSuccess { snapshot -> applySnapshot(snapshot) }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "读取数据失败") }
                }
        }
    }

    fun generateToday() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = "") }
            runCatching { repository.generateTodaySession() }
                .onSuccess { refresh() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "生成今日任务失败") }
                }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    fun completeLearning(cardId: String) {
        val sessionId = _uiState.value.session?.sessionId ?: return
        viewModelScope.launch {
            runCatching { repository.completeLearningCard(sessionId, cardId) }
                .onSuccess { refresh() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "学习卡片更新失败") }
                }
        }
    }

    fun answer(action: AnswerAction, value: String = "") {
        val state = _uiState.value
        val sessionId = state.session?.sessionId ?: return
        val cardId = state.currentCard?.cardId ?: return
        viewModelScope.launch {
            runCatching { repository.answer(sessionId, cardId, action, value) }
                .onSuccess { outcome ->
                    val snapshot = repository.loadHomeSnapshot()
                    applySnapshot(snapshot)
                    _uiState.update {
                        it.copy(
                            feedbackNonce = it.feedbackNonce + 1,
                            feedbackResult = outcome.result,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "答题更新失败") }
                }
        }
    }

    fun setImportText(text: String) {
        _uiState.update { it.copy(importText = text, importPreview = null, importMessage = "") }
    }

    fun previewImport() {
        val text = _uiState.value.importText
        val preview = repository.previewImport(text)
        _uiState.update { it.copy(importPreview = preview, importMessage = "") }
    }

    fun commitImport() {
        val preview = _uiState.value.importPreview ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = "") }
            runCatching { repository.rebuildFromImport(preview) }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            importText = "",
                            importPreview = null,
                            importMessage = "导入完成：成功 ${result.successCount}，错误 ${result.errorCount}",
                        )
                    }
                    refresh()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "导入失败") }
                }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    fun clearImportMessage() {
        _uiState.update { it.copy(importMessage = "", errorMessage = "") }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { repository.setTheme(theme) }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setAnimationsEnabled(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticsEnabled(enabled) }
    }

    fun resetSettings() {
        viewModelScope.launch { repository.resetSettings() }
    }

    private fun applySnapshot(snapshot: HomeSnapshot) {
        _uiState.update {
            it.copy(
                today = snapshot.today,
                itemCount = snapshot.itemCount,
                session = snapshot.session,
                currentCard = snapshot.currentCard,
                currentProgress = snapshot.currentProgress,
                progress = snapshot.progress,
                completedCount = snapshot.completedCount,
                remainingCount = snapshot.remainingCount,
                failedCount = snapshot.failedCount,
                activeOldKnowledgeCount = snapshot.activeOldKnowledgeCount,
                tomorrowDueOldKnowledgeCount = snapshot.tomorrowDueOldKnowledgeCount,
            )
        }
    }

    companion object {
        fun factory(repository: QuizcatRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    return HomeViewModel(repository) as T
                }
            }
        }
    }
}
