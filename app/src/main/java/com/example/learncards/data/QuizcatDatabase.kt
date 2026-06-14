package com.prplegryn.quizcat.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ItemEntity::class,
        CardEntity::class,
        DailySessionEntity::class,
        DailyCardProgressEntity::class,
        OldKnowledgeItemEntity::class,
        AppSettingEntity::class,
        ImportLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class QuizcatDatabase : RoomDatabase() {
    abstract fun dao(): QuizcatDao
}
