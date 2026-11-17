package com.example.domain.model

enum class TileCategory {
  LATIN_LETTER,
  NUMBER,
  ARABIC_LETTER,
}

enum class TilePack(
  val titleFr: String,
  val subtitle: String,
) {
  ALL("Tout", "A-Z • 0-10 • أ-ي"),
  LATIN_ONLY("Lettres A-Z", "Alphabet français"),
  NUMBERS_ONLY("Chiffres 0-10", "Compter 0 à 10"),
  ARABIC_ONLY("الحروف أ-ي", "Alphabet arabe"),
}

enum class Difficulty(
  val labelFr: String,
  val labelAr: String,
  val minPairs: Int,
  val maxPairs: Int,
  val layers: Int,
) {
  EASY("Facile", "سهل", 6, 10, 2),
  MEDIUM("Moyen", "متوسط", 12, 18, 3),
  HARD("Difficile", "صعب", 20, 28, 4),
}

enum class ArabicVoiceMode(
  val label: String,
) {
  BILINGUAL_AR_AND_FR("العربية + Français"),
  ARABIC_ONLY("العربية uniquement"),
  FRENCH_ONLY("Français uniquement"),
}

data class TileSymbol(
  val id: String,
  val glyph: String,
  val category: TileCategory,
  val frenchSpeech: String,
  val arabicSpeech: String,
  val frenchDisplayName: String,
  val arabicDisplayName: String? = null,
)

data class BoardTile(
  val instanceId: Int,
  val symbol: TileSymbol,
  val x: Int, // Half-tile units horizontally (width = 2 units)
  val y: Int, // Half-tile units vertically (height = 2 units)
  val z: Int, // Stack layer (0 = bottom)
  val isFree: Boolean = false,
)

data class GamePreferences(
  val musicEnabled: Boolean = true,
  val sfxEnabled: Boolean = true,
  val voiceEnabled: Boolean = true,
  val speakOnTap: Boolean = true,
  val highlightFreeTiles: Boolean = true,
  val arabicVoiceMode: ArabicVoiceMode = ArabicVoiceMode.BILINGUAL_AR_AND_FR,
  val unlockedEasyLevel: Int = 1,
  val unlockedMediumLevel: Int = 1,
  val unlockedHardLevel: Int = 1,
  val totalScore: Int = 0,
  val endlessBoardsCleared: Int = 0,
  val bestEndlessStreak: Int = 0,
) {
  fun unlockedLevelFor(difficulty: Difficulty): Int = when (difficulty) {
    Difficulty.EASY -> unlockedEasyLevel
    Difficulty.MEDIUM -> unlockedMediumLevel
    Difficulty.HARD -> unlockedHardLevel
  }
}

sealed interface GameMode {
  val difficulty: Difficulty
  val tilePack: TilePack

  data class CampaignLevel(
    override val difficulty: Difficulty,
    val levelNumber: Int,
    override val tilePack: TilePack = TilePack.ALL,
  ) : GameMode

  data class EndlessRandom(
    override val difficulty: Difficulty,
    override val tilePack: TilePack,
    val boardIndex: Int = 1,
    val streak: Int = 0,
  ) : GameMode
}
