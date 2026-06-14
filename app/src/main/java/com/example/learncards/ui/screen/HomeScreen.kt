package com.prplegryn.quizcat.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.prplegryn.quizcat.data.CardEntity
import com.prplegryn.quizcat.data.DailyCardProgressEntity
import com.prplegryn.quizcat.domain.AnswerAction
import com.prplegryn.quizcat.domain.CardType
import com.prplegryn.quizcat.domain.ClozeContent
import com.prplegryn.quizcat.domain.FirstAnswerResult
import com.prplegryn.quizcat.domain.FlashCardContent
import com.prplegryn.quizcat.domain.LearningContent
import com.prplegryn.quizcat.domain.McqContent
import com.prplegryn.quizcat.domain.ProgressStatus
import com.prplegryn.quizcat.domain.SessionStatus
import com.prplegryn.quizcat.domain.TypedAnswerContent
import com.prplegryn.quizcat.ui.theme.QuizcatMono
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private enum class Sheet {
    Import,
    Today,
    OldKnowledge,
    Settings,
}

private val cardJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onGenerateToday: () -> Unit,
    onCompleteLearning: (String) -> Unit,
    onAnswer: (AnswerAction, String) -> Unit,
    onImportTextChange: (String) -> Unit,
    onPreviewImport: () -> Unit,
    onCommitImport: () -> Unit,
    onClearImportMessage: () -> Unit,
    onThemeChange: (String) -> Unit,
    onAnimationsEnabledChange: (Boolean) -> Unit,
    onHapticsEnabledChange: (Boolean) -> Unit,
    onResetSettings: () -> Unit,
) {
    var sheet by rememberSaveable { mutableStateOf<Sheet?>(null) }
    var showImportConfirm by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            onImportTextChange(text)
            onPreviewImport()
        }
    }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomActions(
                onImport = { sheet = Sheet.Import },
                onToday = { sheet = Sheet.Today },
                onOldKnowledge = { sheet = Sheet.OldKnowledge },
                onSettings = { sheet = Sheet.Settings },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TopStatus(state)

            when {
                state.itemCount == 0 -> EmptyState(onImport = { sheet = Sheet.Import })
                state.session == null -> ReadyToGenerateState(
                    itemCount = state.itemCount,
                    isBusy = state.isBusy,
                    onGenerateToday = onGenerateToday,
                )
                state.session.status == SessionStatus.Completed -> CompletedState(state)
                else -> ActiveSession(
                    state = state,
                    onCompleteLearning = onCompleteLearning,
                    onAnswer = onAnswer,
                )
            }

            QueueSection(state)

            if (state.errorMessage.isNotBlank()) {
                MessageSurface(
                    text = state.errorMessage,
                    isError = true,
                    onDismiss = onClearImportMessage,
                )
            }
            if (state.importMessage.isNotBlank()) {
                MessageSurface(
                    text = state.importMessage,
                    isError = false,
                    onDismiss = onClearImportMessage,
                )
            }
        }
    }

    when (sheet) {
        Sheet.Import -> ImportSheet(
            state = state,
            onDismiss = { sheet = null },
            onPickFile = {
                fileLauncher.launch(arrayOf("application/json", "text/json", "text/*"))
            },
            onTextChange = onImportTextChange,
            onPreview = onPreviewImport,
            onAskCommit = { showImportConfirm = true },
        )
        Sheet.Today -> TodaySheet(state = state, onDismiss = { sheet = null }, onGenerateToday = onGenerateToday)
        Sheet.OldKnowledge -> OldKnowledgeSheet(state = state, onDismiss = { sheet = null })
        Sheet.Settings -> SettingsSheet(
            state = state,
            onDismiss = { sheet = null },
            onThemeChange = onThemeChange,
            onAnimationsEnabledChange = onAnimationsEnabledChange,
            onHapticsEnabledChange = onHapticsEnabledChange,
            onResetSettings = onResetSettings,
        )
        null -> Unit
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("确认重新导入") },
            text = {
                Text("导入会清空当前所有知识、今日任务、旧知识复习记录和学习进度，是否继续？")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirm = false
                        onCommitImport()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("清空并导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun TopStatus(state: HomeUiState) {
    val score = state.session?.displayScore ?: 0.0
    val status = when (state.session?.status) {
        SessionStatus.InProgress -> "进行中"
        SessionStatus.Completed -> "已完成"
        else -> "未开始"
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = formatDate(state.today),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "今日学习",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "$status · 计分卡 ${state.session?.n ?: 0}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                tonalElevation = 0.dp,
            ) {
                Text(
                    text = scoreDisplay(score),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onImport: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "导入 JSON 后开始今天的学习",
                style = MaterialTheme.typography.titleLarge,
            )
            Button(
                onClick = onImport,
                modifier = Modifier.height(52.dp),
            ) {
                Icon(Icons.Rounded.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("导入卡片")
            }
        }
    }
}

@Composable
private fun ReadyToGenerateState(
    itemCount: Int,
    isBusy: Boolean,
    onGenerateToday: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("已导入 $itemCount 个知识点", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "今天还没有冻结任务。生成后会保留当天队列，直到跨天后才会生成下一天。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onGenerateToday,
                enabled = !isBusy,
                modifier = Modifier.height(52.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("生成今日任务")
            }
        }
    }
}

@Composable
private fun ActiveSession(
    state: HomeUiState,
    onCompleteLearning: (String) -> Unit,
    onAnswer: (AnswerAction, String) -> Unit,
) {
    val card = state.currentCard
    val scale = remember { Animatable(1f) }
    val shake = remember { Animatable(0f) }
    val density = LocalDensity.current

    LaunchedEffect(state.feedbackNonce) {
        if (state.feedbackNonce == 0 || !state.settings.animationsEnabled) return@LaunchedEffect
        if (state.feedbackResult == FirstAnswerResult.Correct) {
            scale.snapTo(1f)
            scale.animateTo(1.02f, tween(75))
            scale.animateTo(1f, tween(75))
        } else {
            val px = with(density) { 8.dp.toPx() }
            shake.snapTo(0f)
            listOf(-px, px, -px * 0.6f, px * 0.6f, 0f).forEach {
                shake.animateTo(it, tween(36))
            }
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(shake.value.roundToInt(), 0) }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    ) {
        if (state.settings.animationsEnabled) {
            AnimatedContent(
                targetState = card?.cardId,
                transitionSpec = {
                    (slideInHorizontally(animationSpec = tween(220)) { width -> width / 4 } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(120)) { width -> -width / 4 } + fadeOut(tween(120)))
                },
                label = "card_switch",
            ) { targetId ->
                CardBody(
                    card = card?.takeIf { it.cardId == targetId },
                    progress = state.currentProgress,
                    onCompleteLearning = onCompleteLearning,
                    onAnswer = onAnswer,
                )
            }
        } else {
            CardBody(
                card = card,
                progress = state.currentProgress,
                onCompleteLearning = onCompleteLearning,
                onAnswer = onAnswer,
            )
        }
    }
}

@Composable
private fun CardBody(
    card: CardEntity?,
    progress: DailyCardProgressEntity?,
    onCompleteLearning: (String) -> Unit,
    onAnswer: (AnswerAction, String) -> Unit,
) {
    if (card == null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Text(
                text = "今日任务已冻结，暂无可做卡片。",
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    when (card.type) {
        CardType.Learning -> LearningCard(card, onCompleteLearning)
        CardType.FlashCard -> FlashCard(card, progress, onAnswer)
        CardType.Mcq -> McqCard(card, progress, onAnswer)
        CardType.Cloze -> ClozeCard(card, progress, onAnswer)
        CardType.TypedAnswer -> TypedAnswerCard(card, progress, onAnswer)
    }
}

@Composable
private fun LearningCard(card: CardEntity, onCompleteLearning: (String) -> Unit) {
    val content = remember(card.cardId, card.contentJson) {
        cardJson.decodeFromString<LearningContent>(card.contentJson)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TypeLabel("learning")
            Text(
                text = content.term,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = if (looksTechnical(content.term)) QuizcatMono else null,
                ),
            )
            if (content.fullName.isNotBlank()) {
                Text(
                    text = content.fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(text = content.definition, style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = { onCompleteLearning(card.cardId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("我已了解")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun FlashCard(
    card: CardEntity,
    progress: DailyCardProgressEntity?,
    onAnswer: (AnswerAction, String) -> Unit,
) {
    val content = remember(card.cardId, card.contentJson) {
        cardJson.decodeFromString<FlashCardContent>(card.contentJson)
    }
    var answerVisible by remember(card.cardId) { mutableStateOf(false) }
    ScoringCardShell(type = "flash_card", progress = progress) {
        Text(
            text = content.term,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = if (looksTechnical(content.term)) QuizcatMono else null,
            ),
        )
        if (answerVisible) {
            if (content.fullName.isNotBlank()) {
                Text(
                    text = content.fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(text = content.definition, style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onAnswer(AnswerAction.Correct, "") },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("答对")
                }
                FilledTonalButton(
                    onClick = { onAnswer(AnswerAction.Wrong, "") },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("答错")
                }
            }
            TextButton(
                onClick = { onAnswer(AnswerAction.Skip, "") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("跳过")
            }
        } else {
            Button(
                onClick = { answerVisible = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("显示答案")
            }
        }
    }
}

@Composable
private fun McqCard(
    card: CardEntity,
    progress: DailyCardProgressEntity?,
    onAnswer: (AnswerAction, String) -> Unit,
) {
    val content = remember(card.cardId, card.contentJson) {
        cardJson.decodeFromString<McqContent>(card.contentJson)
    }
    val choices = listOf(
        "choice_1" to content.choice1,
        "choice_2" to content.choice2,
        "choice_3" to content.choice3,
        "choice_4" to content.choice4,
    )
    ScoringCardShell(type = "MCQ", progress = progress) {
        Text(text = content.question, style = MaterialTheme.typography.titleLarge)
        choices.forEach { (key, text) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(role = Role.Button) { onAnswer(AnswerAction.Submit, key) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = choiceLabel(key),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        TextButton(
            onClick = { onAnswer(AnswerAction.Skip, "") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("跳过")
        }
    }
}

@Composable
private fun ClozeCard(
    card: CardEntity,
    progress: DailyCardProgressEntity?,
    onAnswer: (AnswerAction, String) -> Unit,
) {
    val content = remember(card.cardId, card.contentJson) {
        cardJson.decodeFromString<ClozeContent>(card.contentJson)
    }
    var input by remember(card.cardId) { mutableStateOf("") }
    ScoringCardShell(type = "cloze", progress = progress) {
        Text(text = content.questionSentenceWithEmpty, style = MaterialTheme.typography.titleLarge)
        MonoInput(
            value = input,
            onValueChange = { input = it },
            label = "填空答案",
            imeAction = ImeAction.Done,
        )
        Button(
            onClick = { onAnswer(AnswerAction.Submit, input) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("提交")
        }
        TextButton(
            onClick = { onAnswer(AnswerAction.Skip, "") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("跳过")
        }
    }
}

@Composable
private fun TypedAnswerCard(
    card: CardEntity,
    progress: DailyCardProgressEntity?,
    onAnswer: (AnswerAction, String) -> Unit,
) {
    val content = remember(card.cardId, card.contentJson) {
        cardJson.decodeFromString<TypedAnswerContent>(card.contentJson)
    }
    var input by remember(card.cardId) { mutableStateOf("") }
    ScoringCardShell(type = "typed_answer", progress = progress) {
        Text(text = content.question, style = MaterialTheme.typography.titleLarge)
        MonoInput(
            value = input,
            onValueChange = { input = it },
            label = "输入答案",
            imeAction = ImeAction.Done,
        )
        Button(
            onClick = { onAnswer(AnswerAction.Submit, input) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("提交")
        }
        TextButton(
            onClick = { onAnswer(AnswerAction.Skip, "") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("跳过")
        }
    }
}

@Composable
private fun ScoringCardShell(
    type: String,
    progress: DailyCardProgressEntity?,
    content: @Composable Column.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TypeLabel(type)
                val statusText = when (progress?.status) {
                    ProgressStatus.WaitingConfirm -> "再次确认"
                    ProgressStatus.Failed -> "连续正确 ${progress.streak}/3"
                    ProgressStatus.Completed -> "已完成"
                    else -> "计分卡"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
            val feedback = progress?.lastFeedback.orEmpty()
            AnimatedVisibility(feedback.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = feedback,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonoInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = QuizcatMono),
        shape = RoundedCornerShape(18.dp),
        colors = softTextFieldColors(),
    )
}

@Composable
private fun QueueSection(state: HomeUiState) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("今日队列", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "剩余 ${state.remainingCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                    )
                }
            }
            AnimatedVisibility(expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatPill("已完成", state.completedCount.toString(), Modifier.weight(1f))
                    StatPill("未通过", state.failedCount.toString(), Modifier.weight(1f))
                    StatPill("计分卡", (state.session?.n ?: 0).toString(), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CompletedState(state: HomeUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("今日已完成", style = MaterialTheme.typography.titleLarge)
            Text("完成计分卡 ${state.completedCount} / ${state.session?.n ?: 0}", style = MaterialTheme.typography.bodyLarge)
            Text("旧知识 active ${state.activeOldKnowledgeCount} · 明日预计 ${state.tomorrowDueOldKnowledgeCount}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BottomActions(
    onImport: () -> Unit,
    onToday: () -> Unit,
    onOldKnowledge: () -> Unit,
    onSettings: () -> Unit,
) {
    Surface(
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BottomAction(Icons.Rounded.FileUpload, "导入", onImport, Modifier.weight(1f))
            BottomAction(Icons.Rounded.Today, "今日任务", onToday, Modifier.weight(1f))
            BottomAction(Icons.Rounded.History, "旧知识", onOldKnowledge, Modifier.weight(1f))
            BottomAction(Icons.Rounded.Settings, "设置", onSettings, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BottomAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportSheet(
    state: HomeUiState,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onTextChange: (String) -> Unit,
    onPreview: () -> Unit,
    onAskCommit: () -> Unit,
) {
    var showErrors by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("导入", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = onPickFile,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("选择 JSON")
                }
                OutlinedButton(
                    onClick = { onTextChange(sampleJson) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("示例")
                }
            }
            TextField(
                value = state.importText,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = QuizcatMono),
                placeholder = { Text("粘贴 JSON") },
                shape = RoundedCornerShape(20.dp),
                colors = softTextFieldColors(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onPreview,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = state.importText.isNotBlank() && !state.isBusy,
                ) {
                    Text("预览导入")
                }
                Button(
                    onClick = onAskCommit,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = (state.importPreview?.validItemCount ?: 0) > 0 && !state.isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("确认导入")
                }
            }
            state.importPreview?.let { preview ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("识别 item ${preview.validItemCount} · learning ${preview.learningCount} · 计分卡 ${preview.scoringCount}")
                        Text("错误字段 ${preview.errorCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (preview.errors.isNotEmpty()) {
                            TextButton(onClick = { showErrors = !showErrors }) {
                                Text(if (showErrors) "收起错误" else "查看错误")
                            }
                            AnimatedVisibility(showErrors) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    preview.errors.take(12).forEach { error ->
                                        Text(
                                            text = "${error.path}: ${error.message}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = QuizcatMono),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodaySheet(
    state: HomeUiState,
    onDismiss: () -> Unit,
    onGenerateToday: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("今日任务", style = MaterialTheme.typography.titleLarge)
            val session = state.session
            if (session == null) {
                Text("今天还没有生成任务。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = onGenerateToday,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = state.itemCount > 0 && !state.isBusy,
                ) {
                    Text("生成今日任务")
                }
            } else {
                StatRow("状态", sessionStatusText(session.status))
                StatRow("计分卡 N", session.n.toString())
                StatRow("internal_total", session.internalTotal.toString(), mono = true)
                StatRow("S", if (session.n > 0) (session.internalTotal / session.n).toString() else "0", mono = true)
                StatRow("frozen", session.frozen.toString(), mono = true)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OldKnowledgeSheet(state: HomeUiState, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("旧知识", style = MaterialTheme.typography.titleLarge)
            StatRow("当前 active", state.activeOldKnowledgeCount.toString())
            StatRow("明日预计复习", state.tomorrowDueOldKnowledgeCount.toString())
            Text(
                text = "到期旧知识会在当天任务生成时自动加入，只包含非 learning 卡片。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    state: HomeUiState,
    onDismiss: () -> Unit,
    onThemeChange: (String) -> Unit,
    onAnimationsEnabledChange: (Boolean) -> Unit,
    onHapticsEnabledChange: (Boolean) -> Unit,
    onResetSettings: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("设置", style = MaterialTheme.typography.titleLarge)
            Text("主题", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ThemeButton("system", "系统", state.settings.theme, onThemeChange, Modifier.weight(1f))
                ThemeButton("light", "浅色", state.settings.theme, onThemeChange, Modifier.weight(1f))
                ThemeButton("dark", "深色", state.settings.theme, onThemeChange, Modifier.weight(1f))
            }
            ToggleRow("动画", state.settings.animationsEnabled, onAnimationsEnabledChange)
            ToggleRow("触感反馈", state.settings.hapticsEnabled, onHapticsEnabledChange)
            StatRow("首日新增", state.settings.firstDayNewItemCount.toString())
            StatRow("每日新增", state.settings.dailyNewItemCountAfterFirstDay.toString())
            StatRow("旧知识间隔", "${state.settings.oldDueDelayDays} 天")
            StatRow("未通过连续正确", state.settings.failedRequiredStreak.toString())
            OutlinedButton(
                onClick = onResetSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("重置所有设置")
            }
        }
    }
}

@Composable
private fun ThemeButton(
    value: String,
    label: String,
    selected: String,
    onThemeChange: (String) -> Unit,
    modifier: Modifier,
) {
    val active = selected == value
    val colors = if (active) {
        ButtonDefaults.buttonColors()
    } else {
        ButtonDefaults.filledTonalButtonColors()
    }
    Button(
        onClick = { onThemeChange(value) },
        modifier = modifier.height(48.dp),
        colors = colors,
    ) {
        Text(label)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (mono) QuizcatMono else null,
            ),
        )
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TypeLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = QuizcatMono),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MessageSurface(text: String, isError: Boolean, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "关闭")
            }
        }
    }
}

@Composable
private fun softTextFieldColors() = TextFieldDefaults.colors(
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
)

private fun formatDate(value: String): String {
    return runCatching {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
    }.getOrElse { value.ifBlank { LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")) } }
}

private fun scoreDisplay(score: Double): String {
    return if (score >= 100.0) "100" else score.roundToInt().coerceIn(0, 99).toString()
}

private fun choiceLabel(key: String): String = when (key) {
    "choice_1" -> "A"
    "choice_2" -> "B"
    "choice_3" -> "C"
    else -> "D"
}

private fun sessionStatusText(status: String): String = when (status) {
    SessionStatus.InProgress -> "进行中"
    SessionStatus.Completed -> "已完成"
    else -> "未开始"
}

private fun looksTechnical(text: String): Boolean {
    val trimmed = text.trim()
    return trimmed.contains("_") ||
        trimmed.contains("-") ||
        trimmed.contains("/") ||
        trimmed.contains(".") ||
        trimmed.contains(":") ||
        (trimmed.length <= 18 && trimmed.any { it.isDigit() })
}

private val sampleJson = """
[
  {
    "item": {
      "term": "chmod",
      "item_id": "linux_chmod"
    },
    "learning/flashcard": {
      "item_id": "linux_chmod",
      "term": "chmod",
      "full_name": "change mode",
      "definition": "Linux 中用于修改文件权限的命令。",
      "skip_tips": ""
    },
    "MCQ": {
      "item_id": "linux_chmod",
      "question": "chmod 的主要作用是什么？",
      "right_choice": "choice_1",
      "right_tips": "正确。",
      "wrong_tips": "chmod 用于修改权限。",
      "skip_tips": "",
      "choice_1": "修改文件权限",
      "choice_2": "移动文件",
      "choice_3": "显示目录",
      "choice_4": "查看进程"
    },
    "cloze": {
      "item_id": "linux_chmod",
      "question_sentence_with_empty": "____ 用于修改文件权限。",
      "fill_answer": "chmod",
      "wrong_tips": "正确答案是 chmod。",
      "skip_tips": ""
    },
    "typed_answer_question": {
      "item_id": "linux_chmod",
      "question": "输入 Linux 修改权限命令",
      "answer": "chmod",
      "right_tips": "正确。",
      "wrong_tips": "正确答案是 chmod。",
      "skip_tips": ""
    }
  }
]
""".trimIndent()
