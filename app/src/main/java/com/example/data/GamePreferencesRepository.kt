package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ArabicVoiceMode
import com.example.domain.model.Difficulty
import com.example.domain.model.GamePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mahjong_abc_prefs")

class GamePreferencesRepository(private val context: Context) {

  private object Keys {
    val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
    val SFX_ENABLED = booleanPreferencesKey("sfx_enabled")
    val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
    val SPEAK_ON_TAP = booleanPreferencesKey("speak_on_tap")
    val HIGHLIGHT_FREE_TILES = booleanPreferencesKey("highlight_free_tiles")
    val ARABIC_VOICE_MODE = stringPreferencesKey("arabic_voice_mode")
    val UNLOCKED_EASY = intPreferencesKey("unlocked_easy")
    val UNLOCKED_MEDIUM = intPreferencesKey("unlocked_medium")
    val UNLOCKED_HARD = intPreferencesKey("unlocked_hard")
    val TOTAL_SCORE = intPreferencesKey("total_score")
    val ENDLESS_CLEARED = intPreferencesKey("endless_cleared")
    val BEST_ENDLESS_STREAK = intPreferencesKey("best_endless_streak")
  }

  val preferencesFlow: Flow<GamePreferences> = context.dataStore.data.map { prefs ->
    val modeName = prefs[Keys.ARABIC_VOICE_MODE] ?: ArabicVoiceMode.BILINGUAL_AR_AND_FR.name
    val voiceMode = runCatching { ArabicVoiceMode.valueOf(modeName) }
      .getOrDefault(ArabicVoiceMode.BILINGUAL_AR_AND_FR)

    GamePreferences(
      musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: true,
      sfxEnabled = prefs[Keys.SFX_ENABLED] ?: true,
      voiceEnabled = prefs[Keys.VOICE_ENABLED] ?: true,
      speakOnTap = prefs[Keys.SPEAK_ON_TAP] ?: true,
      highlightFreeTiles = prefs[Keys.HIGHLIGHT_FREE_TILES] ?: true,
      arabicVoiceMode = voiceMode,
      unlockedEasyLevel = prefs[Keys.UNLOCKED_EASY] ?: 1,
      unlockedMediumLevel = prefs[Keys.UNLOCKED_MEDIUM] ?: 1,
      unlockedHardLevel = prefs[Keys.UNLOCKED_HARD] ?: 1,
      totalScore = prefs[Keys.TOTAL_SCORE] ?: 0,
      endlessBoardsCleared = prefs[Keys.ENDLESS_CLEARED] ?: 0,
      bestEndlessStreak = prefs[Keys.BEST_ENDLESS_STREAK] ?: 0,
    )
  }

  suspend fun setMusicEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.MUSIC_ENABLED] = enabled }
  }

  suspend fun setSfxEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.SFX_ENABLED] = enabled }
  }

  suspend fun setVoiceEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.VOICE_ENABLED] = enabled }
  }

  suspend fun setSpeakOnTap(enabled: Boolean) {
    context.dataStore.edit { it[Keys.SPEAK_ON_TAP] = enabled }
  }

  suspend fun setHighlightFreeTiles(enabled: Boolean) {
    context.dataStore.edit { it[Keys.HIGHLIGHT_FREE_TILES] = enabled }
  }

  suspend fun setArabicVoiceMode(mode: ArabicVoiceMode) {
    context.dataStore.edit { it[Keys.ARABIC_VOICE_MODE] = mode.name }
  }

  suspend fun recordCampaignLevelCleared(
    difficulty: Difficulty,
    clearedLevel: Int,
    earnedScore: Int,
  ) {
    context.dataStore.edit { prefs ->
      val key = when (difficulty) {
        Difficulty.EASY -> Keys.UNLOCKED_EASY
        Difficulty.MEDIUM -> Keys.UNLOCKED_MEDIUM
        Difficulty.HARD -> Keys.UNLOCKED_HARD
      }
      val current = prefs[key] ?: 1
      if (clearedLevel >= current) {
        prefs[key] = clearedLevel + 1
      }
      prefs[Keys.TOTAL_SCORE] = (prefs[Keys.TOTAL_SCORE] ?: 0) + earnedScore
    }
  }

  suspend fun recordEndlessBoardCleared(
    newStreak: Int,
    earnedScore: Int,
  ) {
    context.dataStore.edit { prefs ->
      prefs[Keys.ENDLESS_CLEARED] = (prefs[Keys.ENDLESS_CLEARED] ?: 0) + 1
      val best = prefs[Keys.BEST_ENDLESS_STREAK] ?: 0
      if (newStreak > best) {
        prefs[Keys.BEST_ENDLESS_STREAK] = newStreak
      }
      prefs[Keys.TOTAL_SCORE] = (prefs[Keys.TOTAL_SCORE] ?: 0) + earnedScore
    }
  }
}
