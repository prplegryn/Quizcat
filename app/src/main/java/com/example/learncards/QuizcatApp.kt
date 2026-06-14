package com.prplegryn.quizcat

import android.app.Application
import androidx.room.Room
import com.prplegryn.quizcat.data.AppSettingsStore
import com.prplegryn.quizcat.data.QuizcatDatabase
import com.prplegryn.quizcat.domain.QuizcatRepository

class QuizcatApp : Application() {
    val database: QuizcatDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            QuizcatDatabase::class.java,
            "quizcat.db",
        ).build()
    }

    val settingsStore: AppSettingsStore by lazy {
        AppSettingsStore(applicationContext)
    }

    val repository: QuizcatRepository by lazy {
        QuizcatRepository(database, settingsStore)
    }
}
