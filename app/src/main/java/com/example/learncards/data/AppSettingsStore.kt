package com.prplegryn.quizcat.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prplegryn.quizcat.domain.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.quizcatSettings by preferencesDataStore(name = "quizcat_settings")

class AppSettingsStore(private val context: Context) {
    private object Keys {
        val Theme = stringPreferencesKey("theme")
        val FirstDayNewItemCount = intPreferencesKey("first_day_new_item_count")
        val DailyNewItemCountAfterFirstDay = intPreferencesKey("daily_new_item_count_after_first_day")
        val OldDueDelayDays = intPreferencesKey("old_due_delay_days")
        val FailedRequiredStreak = intPreferencesKey("failed_required_streak")
        val AnimationsEnabled = booleanPreferencesKey("animations_enabled")
        val HapticsEnabled = booleanPreferencesKey("haptics_enabled")
    }

    val settings: Flow<AppSettings> = context.quizcatSettings.data.map { prefs ->
        AppSettings(
            theme = prefs[Keys.Theme] ?: "system",
            firstDayNewItemCount = prefs[Keys.FirstDayNewItemCount] ?: 20,
            dailyNewItemCountAfterFirstDay = prefs[Keys.DailyNewItemCountAfterFirstDay] ?: 5,
            oldDueDelayDays = prefs[Keys.OldDueDelayDays] ?: 7,
            failedRequiredStreak = prefs[Keys.FailedRequiredStreak] ?: 3,
            animationsEnabled = prefs[Keys.AnimationsEnabled] ?: true,
            hapticsEnabled = prefs[Keys.HapticsEnabled] ?: true,
        )
    }

    suspend fun setTheme(theme: String) {
        context.quizcatSettings.edit { prefs ->
            prefs[Keys.Theme] = theme
        }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.quizcatSettings.edit { prefs ->
            prefs[Keys.AnimationsEnabled] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.quizcatSettings.edit { prefs ->
            prefs[Keys.HapticsEnabled] = enabled
        }
    }

    suspend fun reset() {
        context.quizcatSettings.edit { it.clear() }
    }
}
