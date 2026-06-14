package com.prplegryn.quizcat.domain

import androidx.room.withTransaction
import com.prplegryn.quizcat.data.AppSettingsStore
import com.prplegryn.quizcat.data.CardEntity
import com.prplegryn.quizcat.data.DailyCardProgressEntity
import com.prplegryn.quizcat.data.DailySessionEntity
import com.prplegryn.quizcat.data.ImportLogEntity
import com.prplegryn.quizcat.data.OldKnowledgeItemEntity
import com.prplegryn.quizcat.data.QuizcatDatabase
import com.prplegryn.quizcat.data.QuizcatImporter
import java.time.LocalDate
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class HomeSnapshot(
    val itemCount: Int,
    val today: String,
    val session: DailySessionEntity?,
    val currentCard: CardEntity?,
    val currentProgress: DailyCardProgressEntity?,
    val progress: List<DailyCardProgressEntity>,
    val completedCount: Int,
    val remainingCount: Int,
    val failedCount: Int,
    val activeOldKnowledgeCount: Int,
    val tomorrowDueOldKnowledgeCount: Int,
    val latestImportLog: ImportLogEntity?,
)

class QuizcatRepository(
    private val database: QuizcatDatabase,
    private val settingsStore: AppSettingsStore,
) {
    private val dao = database.dao()
    private val importer = QuizcatImporter()
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun previewImport(rawJson: String): ImportPreview = importer.preview(rawJson)

    suspend fun rebuildFromImport(preview: ImportPreview): ImportCommitResult {
        return database.withTransaction {
            dao.deleteDailyCardProgress()
            dao.deleteDailySessions()
            dao.deleteOldKnowledge()
            dao.deleteCards()
            dao.deleteItems()
            dao.deleteImportLogs()

            if (preview.items.isNotEmpty()) dao.insertItems(preview.items)
            if (preview.cards.isNotEmpty()) dao.insertCards(preview.cards)

            dao.insertImportLog(
                ImportLogEntity(
                    logId = UUID.randomUUID().toString(),
                    successCount = preview.validItemCount,
                    errorCount = preview.errorCount,
                    errorsJson = json.encodeToString(preview.errors),
                    createdAt = System.currentTimeMillis(),
                ),
            )

            ImportCommitResult(
                successCount = preview.validItemCount,
                errorCount = preview.errorCount,
            )
        }
    }

    suspend fun loadHomeSnapshot(): HomeSnapshot {
        val today = LocalDate.now().toString()
        val session = dao.getSessionByDate(today)
        val progress = session?.let { dao.getProgressForSession(it.sessionId) }.orEmpty()
        val learningCompleted = session?.learningCompletedCardIdsJson.decodeIds().toSet()
        val currentLearningId = session?.learningCardIdsJson
            ?.decodeIds()
            ?.firstOrNull { it !in learningCompleted }
        val currentProgress = if (currentLearningId == null) {
            chooseNextProgress(progress)
        } else {
            null
        }
        val currentCardId = currentLearningId ?: currentProgress?.cardId
        val currentCard = currentCardId?.let { dao.getCard(it) }
        val tomorrow = LocalDate.now().plusDays(1).toString()

        return HomeSnapshot(
            itemCount = dao.countActiveItems(),
            today = today,
            session = session,
            currentCard = currentCard,
            currentProgress = currentProgress,
            progress = progress,
            completedCount = progress.count { it.status == ProgressStatus.Completed },
            remainingCount = session?.let { it.n - progress.count { item -> item.status == ProgressStatus.Completed } } ?: 0,
            failedCount = progress.count { it.status == ProgressStatus.Failed },
            activeOldKnowledgeCount = dao.countActiveOldKnowledge(),
            tomorrowDueOldKnowledgeCount = dao.countOldKnowledgeDueOnOrBefore(tomorrow),
            latestImportLog = dao.getLatestImportLog(),
        )
    }

    suspend fun generateTodaySession(): DailySessionEntity? {
        val settings = settingsStore.settings.first()
        val today = LocalDate.now()
        val todayText = today.toString()
        return database.withTransaction {
            dao.getSessionByDate(todayText)?.let { return@withTransaction it }

            val allItems = dao.getActiveItems()
            if (allItems.isEmpty()) return@withTransaction null

            val oldDue = dao.getDueOldKnowledge(todayText)
            val oldSources = linkedMapOf<String, String>()
            oldDue.forEach { old ->
                oldSources[old.itemId] = if (old.status == OldKnowledgeStatus.Old) {
                    ProgressSource.OldDue
                } else {
                    ProgressSource.OldNew
                }
                if (old.status == OldKnowledgeStatus.Old) {
                    dao.upsertOldKnowledge(
                        old.copy(
                            status = OldKnowledgeStatus.New,
                            unfamiliarityRemaining = max(1, old.unfamiliarityInitial),
                            dueDate = todayText,
                            consecutiveCorrectDays = 0,
                        ),
                    )
                }
            }

            val introduced = dao.getIntroducedItemIds().toSet()
            val oldItemIds = oldSources.keys
            val sessionCount = dao.countSessions()
            val newLimit = if (sessionCount == 0) {
                settings.firstDayNewItemCount
            } else {
                settings.dailyNewItemCountAfterFirstDay
            }
            val newItems = allItems
                .filter { it.itemId !in introduced && it.itemId !in oldItemIds }
                .take(newLimit)

            val learningIds = mutableListOf<String>()
            val scoringSources = linkedMapOf<String, String>()
            val cardsById = linkedMapOf<String, CardEntity>()

            oldSources.forEach { (itemId, source) ->
                dao.getCardsForItem(itemId)
                    .filter { it.type != CardType.Learning && it.isScoring }
                    .forEach { card ->
                        cardsById[card.cardId] = card
                        scoringSources.putIfAbsent(card.cardId, source)
                    }
            }

            newItems.forEach { item ->
                dao.getCardsForItem(item.itemId).forEach { card ->
                    cardsById[card.cardId] = card
                    if (card.type == CardType.Learning) {
                        learningIds += card.cardId
                    } else if (card.isScoring) {
                        scoringSources.putIfAbsent(card.cardId, ProgressSource.New)
                    }
                }
            }

            val scoringIds = buildScoringOrder(cardsById, scoringSources)
            val n = scoringIds.size
            if (n == 0) return@withTransaction null

            val internalTotal = getInternalTotal(n)
            val scorePerCard = internalTotal / n
            val now = System.currentTimeMillis()
            val session = DailySessionEntity(
                sessionId = UUID.randomUUID().toString(),
                date = todayText,
                learningCardIdsJson = json.encodeToString(learningIds.distinct()),
                learningCompletedCardIdsJson = json.encodeToString(emptyList<String>()),
                scoringCardIdsJson = json.encodeToString(scoringIds),
                n = n,
                internalTotal = internalTotal,
                displayTotal = 100,
                earnedTotal = 0,
                displayScore = 0.0,
                status = SessionStatus.NotStarted,
                frozen = true,
                createdAt = now,
            )
            dao.insertSession(session)
            dao.insertProgress(
                scoringIds.mapIndexedNotNull { index, cardId ->
                    val card = cardsById[cardId] ?: return@mapIndexedNotNull null
                    val rowTime = now + index
                    DailyCardProgressEntity(
                        progressId = UUID.randomUUID().toString(),
                        sessionId = session.sessionId,
                        cardId = card.cardId,
                        itemId = card.itemId,
                        cardType = card.type,
                        s = scorePerCard,
                        e = 0,
                        r = scorePerCard,
                        status = ProgressStatus.Unanswered,
                        streak = 0,
                        attemptCount = 0,
                        wrongCount = 0,
                        skipCount = 0,
                        firstAnswerResult = FirstAnswerResult.None,
                        lastFeedback = "",
                        source = scoringSources.getValue(cardId),
                        createdAt = rowTime,
                        updatedAt = rowTime,
                    )
                },
            )
            session
        }
    }

    suspend fun completeLearningCard(sessionId: String, cardId: String) {
        database.withTransaction {
            val session = dao.getSessionById(sessionId) ?: return@withTransaction
            if (session.status == SessionStatus.Completed) return@withTransaction
            val completed = session.learningCompletedCardIdsJson.decodeIds().toMutableList()
            if (cardId !in completed) completed += cardId
            dao.updateSession(
                session.copy(
                    learningCompletedCardIdsJson = json.encodeToString(completed),
                    status = SessionStatus.InProgress,
                ),
            )
        }
    }

    suspend fun answer(
        sessionId: String,
        cardId: String,
        action: AnswerAction,
        value: String = "",
    ): AnswerOutcome {
        val settings = settingsStore.settings.first()
        return database.withTransaction {
            val session = dao.getSessionById(sessionId) ?: return@withTransaction AnswerOutcome("", "none")
            val progress = dao.getProgress(sessionId, cardId) ?: return@withTransaction AnswerOutcome("", "none")
            val card = dao.getCard(cardId) ?: return@withTransaction AnswerOutcome("", "none")
            if (progress.status == ProgressStatus.Completed || session.status == SessionStatus.Completed) {
                return@withTransaction AnswerOutcome(progress.lastFeedback, "completed")
            }

            val evaluation = evaluate(card, action, value)
            val now = System.currentTimeMillis()
            val updated = applyStateMachine(
                progress = progress,
                correct = evaluation.correct,
                skipped = evaluation.skipped,
                feedback = evaluation.feedback,
                now = now,
                requiredFailedStreak = settings.failedRequiredStreak,
            )
            dao.updateProgress(updated)
            refreshSessionScoreLocked(session, now, settings)
            AnswerOutcome(evaluation.feedback, evaluation.result)
        }
    }

    suspend fun setTheme(theme: String) {
        settingsStore.setTheme(theme)
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        settingsStore.setAnimationsEnabled(enabled)
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        settingsStore.setHapticsEnabled(enabled)
    }

    suspend fun resetSettings() {
        settingsStore.reset()
    }

    fun settingsFlow() = settingsStore.settings

    private fun chooseNextProgress(progress: List<DailyCardProgressEntity>): DailyCardProgressEntity? {
        return progress
            .asSequence()
            .filter { it.status != ProgressStatus.Completed }
            .sortedWith(
                compareBy<DailyCardProgressEntity> { it.updatedAt }
                    .thenBy { scoringTypeRank(it.cardType) }
                    .thenBy { it.createdAt }
                    .thenBy { it.itemId.toLongOrNull() ?: Long.MAX_VALUE }
                    .thenBy { it.itemId }
                    .thenBy { it.cardId },
            )
            .firstOrNull()
    }

    private fun buildScoringOrder(
        cardsById: Map<String, CardEntity>,
        scoringSources: Map<String, String>,
    ): List<String> {
        val scoringCards = scoringSources.keys.mapNotNull { cardsById[it] }
        val orderedTypes = listOf(
            CardType.FlashCard,
            CardType.Mcq,
            CardType.Cloze,
            CardType.TypedAnswer,
        )
        val known = scoringCards
            .filter { it.type in orderedTypes }
            .sortedWith(
                compareBy<CardEntity> { sourceRank(scoringSources[it.cardId]) }
                    .thenBy { scoringTypeRank(it.type) }
                    .thenBy { it.createdAt }
                    .thenBy { it.itemId.toLongOrNull() ?: Long.MAX_VALUE }
                    .thenBy { it.itemId }
                    .thenBy { it.cardId },
            )
            .map { it.cardId }
        val remaining = scoringCards
            .filter { it.type !in orderedTypes }
            .sortedWith(
                compareBy<CardEntity> { sourceRank(scoringSources[it.cardId]) }
                    .thenBy { it.createdAt }
                    .thenBy { it.itemId.toLongOrNull() ?: Long.MAX_VALUE }
                    .thenBy { it.itemId }
                    .thenBy { it.cardId },
            )
            .map { it.cardId }
        return (known + remaining).distinct()
    }

    private fun sourceRank(source: String?): Int = when (source) {
        ProgressSource.OldNew -> 0
        ProgressSource.OldDue -> 1
        ProgressSource.New -> 2
        else -> 3
    }

    private fun scoringTypeRank(type: String): Int = when (type) {
        CardType.FlashCard -> 0
        CardType.Mcq -> 1
        CardType.Cloze -> 2
        CardType.TypedAnswer -> 3
        else -> 4
    }

    private suspend fun refreshSessionScoreLocked(
        session: DailySessionEntity,
        now: Long,
        settings: AppSettings,
    ) {
        val progress = dao.getProgressForSession(session.sessionId)
        val allCompleted = progress.isNotEmpty() && progress.all { it.status == ProgressStatus.Completed }
        if (allCompleted) {
            dao.updateSession(
                session.copy(
                    earnedTotal = session.internalTotal,
                    displayScore = 100.0,
                    status = SessionStatus.Completed,
                    completedAt = now,
                ),
            )
            if (session.status != SessionStatus.Completed) {
                settleOldKnowledgeLocked(session, progress, settings)
            }
        } else {
            val earned = progress.sumOf { it.e }.coerceIn(0, session.internalTotal)
            val displayScore = if (session.internalTotal == 0) {
                0.0
            } else {
                (earned.toDouble() / session.internalTotal.toDouble() * 100.0).coerceAtMost(99.99)
            }
            dao.updateSession(
                session.copy(
                    earnedTotal = earned,
                    displayScore = displayScore,
                    status = SessionStatus.InProgress,
                ),
            )
        }
    }

    private suspend fun settleOldKnowledgeLocked(
        session: DailySessionEntity,
        progress: List<DailyCardProgressEntity>,
        settings: AppSettings,
    ) {
        val today = LocalDate.parse(session.date)
        val tomorrow = today.plusDays(1).toString()
        progress.groupBy { it.itemId }.forEach { (itemId, rows) ->
            val source = rows.firstOrNull { it.source != ProgressSource.New }?.source ?: ProgressSource.New
            val failureTotalToday = rows.sumOf { it.wrongCount }
            val anyFirstFailed = rows.any {
                it.firstAnswerResult == FirstAnswerResult.Wrong || it.firstAnswerResult == FirstAnswerResult.Skip
            }
            val anyFailure = failureTotalToday > 0
            val (unfamiliarityAdd, sourceType) = when {
                !anyFailure && !anyFirstFailed -> 1 to SourceResultType.FirstPass
                !anyFirstFailed && anyFailure -> 2 to SourceResultType.PassThenFail
                failureTotalToday < 5 -> 4 to SourceResultType.FailThenPass
                else -> 6 to SourceResultType.HeavyFailThenPass
            }
            val existing = dao.getOldKnowledge(itemId)

            if (source == ProgressSource.New || existing == null) {
                val created = existing?.createdAt ?: today.toString()
                dao.upsertOldKnowledge(
                    OldKnowledgeItemEntity(
                        itemId = itemId,
                        status = OldKnowledgeStatus.New,
                        unfamiliarityInitial = (existing?.unfamiliarityInitial ?: 0) + unfamiliarityAdd,
                        unfamiliarityRemaining = (existing?.unfamiliarityRemaining ?: 0) + unfamiliarityAdd,
                        failureTotal = (existing?.failureTotal ?: 0) + failureTotalToday,
                        sourceResultType = sourceType,
                        createdAt = created,
                        lastReviewedAt = today.toString(),
                        dueDate = tomorrow,
                        consecutiveCorrectDays = if (failureTotalToday == 0) {
                            existing?.consecutiveCorrectDays ?: 0
                        } else {
                            0
                        },
                        hasEnteredOld = existing?.hasEnteredOld ?: false,
                        isActive = true,
                        removedAt = null,
                    ),
                )
                return@forEach
            }

            if (failureTotalToday > 0) {
                dao.upsertOldKnowledge(
                    existing.copy(
                        status = OldKnowledgeStatus.New,
                        unfamiliarityInitial = existing.unfamiliarityInitial + unfamiliarityAdd,
                        unfamiliarityRemaining = existing.unfamiliarityRemaining + unfamiliarityAdd,
                        failureTotal = existing.failureTotal + failureTotalToday,
                        sourceResultType = sourceType,
                        lastReviewedAt = today.toString(),
                        dueDate = tomorrow,
                        consecutiveCorrectDays = 0,
                        isActive = true,
                        removedAt = null,
                    ),
                )
            } else {
                val remaining = existing.unfamiliarityRemaining - 1
                val enteredOld = existing.hasEnteredOld
                val next = when {
                    remaining > 0 -> existing.copy(
                        status = OldKnowledgeStatus.New,
                        unfamiliarityRemaining = remaining,
                        failureTotal = existing.failureTotal + failureTotalToday,
                        sourceResultType = sourceType,
                        lastReviewedAt = today.toString(),
                        dueDate = tomorrow,
                        consecutiveCorrectDays = existing.consecutiveCorrectDays + 1,
                        isActive = true,
                        removedAt = null,
                    )
                    !enteredOld -> existing.copy(
                        status = OldKnowledgeStatus.Old,
                        unfamiliarityRemaining = 0,
                        failureTotal = existing.failureTotal + failureTotalToday,
                        sourceResultType = sourceType,
                        lastReviewedAt = today.toString(),
                        dueDate = today.plusDays(settings.oldDueDelayDays.toLong()).toString(),
                        consecutiveCorrectDays = 0,
                        hasEnteredOld = true,
                        isActive = true,
                        removedAt = null,
                    )
                    else -> existing.copy(
                        unfamiliarityRemaining = 0,
                        failureTotal = existing.failureTotal + failureTotalToday,
                        sourceResultType = sourceType,
                        lastReviewedAt = today.toString(),
                        consecutiveCorrectDays = existing.consecutiveCorrectDays + 1,
                        isActive = false,
                        removedAt = today.toString(),
                    )
                }
                dao.upsertOldKnowledge(next)
            }
        }
    }

    private fun applyStateMachine(
        progress: DailyCardProgressEntity,
        correct: Boolean,
        skipped: Boolean,
        feedback: String,
        now: Long,
        requiredFailedStreak: Int,
    ): DailyCardProgressEntity {
        val wrong = !correct
        val baseAttempts = progress.attemptCount + 1
        val baseWrong = progress.wrongCount + if (wrong) 1 else 0
        val baseSkip = progress.skipCount + if (skipped) 1 else 0

        fun completed(e: Int): DailyCardProgressEntity = progress.copy(
            e = e.coerceAtMost(progress.s),
            r = 0,
            status = ProgressStatus.Completed,
            attemptCount = baseAttempts,
            wrongCount = baseWrong,
            skipCount = baseSkip,
            lastFeedback = feedback,
            updatedAt = now,
            completedAt = now,
        )

        return when (progress.status) {
            ProgressStatus.Unanswered -> {
                if (correct) {
                    val add = minOf(progress.s / 2, progress.r)
                    val earned = (progress.e + add).coerceAtMost(progress.s)
                    progress.copy(
                        e = earned,
                        r = (progress.s - earned).coerceAtLeast(0),
                        status = ProgressStatus.WaitingConfirm,
                        firstAnswerResult = FirstAnswerResult.Correct,
                        attemptCount = baseAttempts,
                        wrongCount = baseWrong,
                        skipCount = baseSkip,
                        lastFeedback = feedback,
                        updatedAt = now,
                    )
                } else {
                    progress.copy(
                        e = 0,
                        r = progress.s,
                        status = ProgressStatus.Failed,
                        streak = 0,
                        firstAnswerResult = if (skipped) FirstAnswerResult.Skip else FirstAnswerResult.Wrong,
                        attemptCount = baseAttempts,
                        wrongCount = baseWrong,
                        skipCount = baseSkip,
                        lastFeedback = feedback,
                        updatedAt = now,
                    )
                }
            }
            ProgressStatus.WaitingConfirm -> {
                if (correct) {
                    completed(progress.s)
                } else {
                    progress.copy(
                        status = ProgressStatus.Failed,
                        streak = 0,
                        attemptCount = baseAttempts,
                        wrongCount = baseWrong,
                        skipCount = baseSkip,
                        lastFeedback = feedback,
                        updatedAt = now,
                    )
                }
            }
            ProgressStatus.Failed -> {
                if (correct) {
                    val nextStreak = progress.streak + 1
                    if (nextStreak >= requiredFailedStreak) {
                        completed(progress.s)
                    } else {
                        progress.copy(
                            streak = nextStreak,
                            attemptCount = baseAttempts,
                            wrongCount = baseWrong,
                            skipCount = baseSkip,
                            lastFeedback = feedback,
                            updatedAt = now,
                        )
                    }
                } else {
                    progress.copy(
                        streak = 0,
                        status = ProgressStatus.Failed,
                        attemptCount = baseAttempts,
                        wrongCount = baseWrong,
                        skipCount = baseSkip,
                        lastFeedback = feedback,
                        updatedAt = now,
                    )
                }
            }
            else -> progress
        }
    }

    private fun evaluate(card: CardEntity, action: AnswerAction, value: String): Evaluation {
        if (action == AnswerAction.Skip) {
            return Evaluation(correct = false, skipped = true, feedback = wrongFeedback(card), result = FirstAnswerResult.Skip)
        }
        return when (card.type) {
            CardType.FlashCard -> {
                val correct = action == AnswerAction.Correct
                Evaluation(
                    correct = correct,
                    skipped = false,
                    feedback = if (correct) DefaultTips.FlashCorrect else DefaultTips.FlashWrong,
                    result = if (correct) FirstAnswerResult.Correct else FirstAnswerResult.Wrong,
                )
            }
            CardType.Mcq -> {
                val content = json.decodeFromString<McqContent>(card.contentJson)
                val correct = value == content.rightChoice
                Evaluation(
                    correct = correct,
                    skipped = false,
                    feedback = if (correct) content.rightTips else content.wrongTips,
                    result = if (correct) FirstAnswerResult.Correct else FirstAnswerResult.Wrong,
                )
            }
            CardType.Cloze -> {
                val content = json.decodeFromString<ClozeContent>(card.contentJson)
                strictTextEvaluation(value, content.fillAnswer, content.wrongTips)
            }
            CardType.TypedAnswer -> {
                val content = json.decodeFromString<TypedAnswerContent>(card.contentJson)
                strictTextEvaluation(value, content.answer, content.wrongTips, content.rightTips)
            }
            else -> Evaluation(false, skipped = false, feedback = "", result = FirstAnswerResult.Wrong)
        }
    }

    private fun strictTextEvaluation(
        rawInput: String,
        answer: String,
        wrongTips: String,
        rightTips: String = "回答正确。",
    ): Evaluation {
        val input = rawInput.trim()
        val expected = answer.trim()
        return when {
            input == expected -> Evaluation(true, skipped = false, feedback = rightTips, result = FirstAnswerResult.Correct)
            input.equals(expected, ignoreCase = true) -> Evaluation(
                false,
                skipped = false,
                feedback = DefaultTips.CaseMismatch,
                result = FirstAnswerResult.Wrong,
            )
            else -> Evaluation(false, skipped = false, feedback = wrongTips.ifBlank { DefaultTips.FlashWrong }, result = FirstAnswerResult.Wrong)
        }
    }

    private fun wrongFeedback(card: CardEntity): String = when (card.type) {
        CardType.FlashCard -> DefaultTips.FlashWrong
        CardType.Mcq -> json.decodeFromString<McqContent>(card.contentJson).wrongTips.ifBlank { DefaultTips.FlashWrong }
        CardType.Cloze -> json.decodeFromString<ClozeContent>(card.contentJson).wrongTips.ifBlank { DefaultTips.FlashWrong }
        CardType.TypedAnswer -> json.decodeFromString<TypedAnswerContent>(card.contentJson).wrongTips.ifBlank { DefaultTips.FlashWrong }
        else -> DefaultTips.FlashWrong
    }

    private fun getInternalTotal(n: Int): Int {
        val divisor = 2 * n
        val lower = floor(100.0 / divisor).toInt() * divisor
        val upper = ceil(100.0 / divisor).toInt() * divisor
        if (lower <= 0) return upper
        return if (abs(100 - lower) <= abs(upper - 100)) lower else upper
    }

    private fun String?.decodeIds(): List<String> {
        if (this.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(this) }.getOrDefault(emptyList())
    }

    private data class Evaluation(
        val correct: Boolean,
        val skipped: Boolean,
        val feedback: String,
        val result: String,
    )
}
