package com.prplegryn.quizcat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface QuizcatDao {
    @Query("SELECT COUNT(*) FROM items WHERE is_active = 1")
    suspend fun countActiveItems(): Int

    @Query("SELECT * FROM items WHERE is_active = 1 ORDER BY created_at ASC, item_id ASC")
    suspend fun getActiveItems(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Query("SELECT * FROM cards WHERE item_id = :itemId AND is_active = 1 ORDER BY type ASC, card_id ASC")
    suspend fun getCardsForItem(itemId: String): List<CardEntity>

    @Query("SELECT * FROM cards WHERE card_id = :cardId LIMIT 1")
    suspend fun getCard(cardId: String): CardEntity?

    @Query("SELECT * FROM daily_sessions WHERE date = :date LIMIT 1")
    suspend fun getSessionByDate(date: String): DailySessionEntity?

    @Query("SELECT * FROM daily_sessions WHERE session_id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): DailySessionEntity?

    @Query("SELECT COUNT(*) FROM daily_sessions")
    suspend fun countSessions(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: DailySessionEntity)

    @Update
    suspend fun updateSession(session: DailySessionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProgress(progress: List<DailyCardProgressEntity>)

    @Update
    suspend fun updateProgress(progress: DailyCardProgressEntity)

    @Query("SELECT * FROM daily_card_progress WHERE session_id = :sessionId ORDER BY created_at ASC, card_id ASC")
    suspend fun getProgressForSession(sessionId: String): List<DailyCardProgressEntity>

    @Query("SELECT * FROM daily_card_progress WHERE session_id = :sessionId AND card_id = :cardId LIMIT 1")
    suspend fun getProgress(sessionId: String, cardId: String): DailyCardProgressEntity?

    @Query("SELECT DISTINCT item_id FROM daily_card_progress")
    suspend fun getIntroducedItemIds(): List<String>

    @Query(
        """
        SELECT * FROM old_knowledge_items
        WHERE is_active = 1 AND due_date <= :today
        ORDER BY
            CASE status WHEN 'new' THEN 0 ELSE 1 END ASC,
            unfamiliarity_remaining DESC,
            failure_total DESC,
            COALESCE(last_reviewed_at, '0000-00-00') ASC,
            item_id ASC
        """,
    )
    suspend fun getDueOldKnowledge(today: String): List<OldKnowledgeItemEntity>

    @Query("SELECT * FROM old_knowledge_items WHERE item_id = :itemId LIMIT 1")
    suspend fun getOldKnowledge(itemId: String): OldKnowledgeItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOldKnowledge(item: OldKnowledgeItemEntity)

    @Query("SELECT COUNT(*) FROM old_knowledge_items WHERE is_active = 1")
    suspend fun countActiveOldKnowledge(): Int

    @Query("SELECT COUNT(*) FROM old_knowledge_items WHERE is_active = 1 AND due_date <= :date")
    suspend fun countOldKnowledgeDueOnOrBefore(date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportLog(log: ImportLogEntity)

    @Query("SELECT * FROM import_logs ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestImportLog(): ImportLogEntity?

    @Query("DELETE FROM daily_card_progress")
    suspend fun deleteDailyCardProgress()

    @Query("DELETE FROM daily_sessions")
    suspend fun deleteDailySessions()

    @Query("DELETE FROM old_knowledge_items")
    suspend fun deleteOldKnowledge()

    @Query("DELETE FROM cards")
    suspend fun deleteCards()

    @Query("DELETE FROM items")
    suspend fun deleteItems()

    @Query("DELETE FROM import_logs")
    suspend fun deleteImportLogs()
}
