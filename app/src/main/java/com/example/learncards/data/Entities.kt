package com.prplegryn.quizcat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: String,
    val term: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
)

@Entity(
    tableName = "cards",
    indices = [Index(value = ["item_id"]), Index(value = ["type"])],
)
data class CardEntity(
    @PrimaryKey
    @ColumnInfo(name = "card_id")
    val cardId: String,
    @ColumnInfo(name = "item_id")
    val itemId: String,
    val type: String,
    @ColumnInfo(name = "is_scoring")
    val isScoring: Boolean,
    @ColumnInfo(name = "content_json")
    val contentJson: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
)

@Entity(
    tableName = "daily_sessions",
    indices = [Index(value = ["date"], unique = true)],
)
data class DailySessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    val date: String,
    @ColumnInfo(name = "learning_card_ids")
    val learningCardIdsJson: String,
    @ColumnInfo(name = "learning_completed_card_ids")
    val learningCompletedCardIdsJson: String = "[]",
    @ColumnInfo(name = "scoring_card_ids")
    val scoringCardIdsJson: String,
    @ColumnInfo(name = "N")
    val n: Int,
    @ColumnInfo(name = "internal_total")
    val internalTotal: Int,
    @ColumnInfo(name = "display_total")
    val displayTotal: Int,
    @ColumnInfo(name = "earned_total")
    val earnedTotal: Int,
    @ColumnInfo(name = "display_score")
    val displayScore: Double,
    val status: String,
    val frozen: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
)

@Entity(
    tableName = "daily_card_progress",
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["card_id"]),
        Index(value = ["item_id"]),
        Index(value = ["session_id", "card_id"], unique = true),
    ],
)
data class DailyCardProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "progress_id")
    val progressId: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "card_id")
    val cardId: String,
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "card_type")
    val cardType: String,
    @ColumnInfo(name = "S")
    val s: Int,
    @ColumnInfo(name = "E")
    val e: Int,
    @ColumnInfo(name = "R")
    val r: Int,
    val status: String,
    val streak: Int,
    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int,
    @ColumnInfo(name = "wrong_count")
    val wrongCount: Int,
    @ColumnInfo(name = "skip_count")
    val skipCount: Int,
    @ColumnInfo(name = "first_answer_result")
    val firstAnswerResult: String,
    @ColumnInfo(name = "last_feedback")
    val lastFeedback: String,
    val source: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
)

@Entity(tableName = "old_knowledge_items")
data class OldKnowledgeItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "item_id")
    val itemId: String,
    val status: String,
    @ColumnInfo(name = "unfamiliarity_initial")
    val unfamiliarityInitial: Int,
    @ColumnInfo(name = "unfamiliarity_remaining")
    val unfamiliarityRemaining: Int,
    @ColumnInfo(name = "failure_total")
    val failureTotal: Int,
    @ColumnInfo(name = "source_result_type")
    val sourceResultType: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "last_reviewed_at")
    val lastReviewedAt: String?,
    @ColumnInfo(name = "due_date")
    val dueDate: String,
    @ColumnInfo(name = "consecutive_correct_days")
    val consecutiveCorrectDays: Int,
    @ColumnInfo(name = "has_entered_old")
    val hasEnteredOld: Boolean,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    @ColumnInfo(name = "removed_at")
    val removedAt: String?,
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(tableName = "import_logs")
data class ImportLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "log_id")
    val logId: String,
    @ColumnInfo(name = "success_count")
    val successCount: Int,
    @ColumnInfo(name = "error_count")
    val errorCount: Int,
    @ColumnInfo(name = "errors_json")
    val errorsJson: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
