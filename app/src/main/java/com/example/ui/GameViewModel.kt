package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundAndVoiceManager
import com.example.audio.TtsDiagnostics
import com.example.data.GamePreferencesRepository
import com.example.domain.engine.MahjongBoardGenerator
import com.example.domain.engine.MahjongRules
import com.example.domain.model.ArabicVoiceMode
import com.example.domain.model.BoardTile
import com.example.domain.model.Difficulty
import com.example.domain.model.GameMode
import com.example.domain.model.GamePreferences
import com.example.domain.model.TilePack
import com.example.domain.model.TileSymbol
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppScreen {
  data object Home : AppScreen
  data class LevelMap(
    val difficulty: Difficulty = Difficulty.EASY,
    val pack: TilePack = TilePack.ALL,
  ) : AppScreen

  data object Game : AppScreen
  data object Settings : AppScreen
}

data class GameBoardState(
  val mode: GameMode = GameMode.CampaignLevel(Difficulty.EASY, 1, TilePack.ALL),
  val tiles: List<BoardTile> = emptyList(),
  val selectedTileId: Int? = null,
  val hintTileIds: Set<Int> = emptySet(),
  val blockedShakeTileId: Int? = null,
  val lastMatchedSymbol: TileSymbol? = null,
  val score: Int = 0,
  val availablePairsCount: Int = 0,
  val isBoardComplete: Boolean = false,
  val autoShuffledNotice: Boolean = false,
  val burstNonce: Int = 0,
  val burstGridX: Int = 0,
  val burstGridY: Int = 0,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = GamePreferencesRepository(application)
  private val audioManager = SoundAndVoiceManager(application)

  val preferences: StateFlow<GamePreferences> = repository.preferencesFlow.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = GamePreferences(),
  )

  val ttsDiagnostics: StateFlow<TtsDiagnostics> = audioManager.ttsDiagnostics

  private val _screenStack = MutableStateFlow<List<AppScreen>>(listOf(AppScreen.Home))
  val screenStack: StateFlow<List<AppScreen>> = _screenStack.asStateFlow()

  private val _selectedHomeDifficulty = MutableStateFlow(Difficulty.EASY)
  val selectedHomeDifficulty: StateFlow<Difficulty> = _selectedHomeDifficulty.asStateFlow()

  private val _selectedHomePack = MutableStateFlow(TilePack.ALL)
  val selectedHomePack: StateFlow<TilePack> = _selectedHomePack.asStateFlow()

  private val _gameState = MutableStateFlow(GameBoardState())
  val gameState: StateFlow<GameBoardState> = _gameState.asStateFlow()

  init {
    viewModelScope.launch {
      preferences.collect { prefs ->
        audioManager.updateSettings(
          musicEnabled = prefs.musicEnabled,
          sfxEnabled = prefs.sfxEnabled,
          voiceEnabled = prefs.voiceEnabled,
          arabicVoiceMode = prefs.arabicVoiceMode,
        )
      }
    }
  }

  fun onAppForegroundChanged(inForeground: Boolean) {
    audioManager.setMusicActive(active = inForeground, musicEnabled = preferences.value.musicEnabled)
  }

  fun setHomeDifficulty(difficulty: Difficulty) {
    _selectedHomeDifficulty.value = difficulty
  }

  fun setHomePack(pack: TilePack) {
    _selectedHomePack.value = pack
  }

  fun openLevelMap(difficulty: Difficulty, pack: TilePack) {
    _screenStack.update { stack -> stack + AppScreen.LevelMap(difficulty, pack) }
  }

  fun openSettings() {
    _screenStack.update { stack ->
      if (stack.lastOrNull() == AppScreen.Settings) stack else stack + AppScreen.Settings
    }
  }

  fun navigateBack() {
    _screenStack.update { stack ->
      if (stack.size > 1) stack.dropLast(1) else stack
    }
  }

  fun navigateHome() {
    _screenStack.value = listOf(AppScreen.Home)
  }

  fun startCampaignLevel(
    difficulty: Difficulty,
    levelNumber: Int,
    pack: TilePack,
  ) {
    val mode = GameMode.CampaignLevel(difficulty, levelNumber, pack)
    val board = MahjongBoardGenerator.generateCampaignBoard(difficulty, levelNumber, pack)
    setNewBoard(mode, board, carryScore = 0)
    _screenStack.update { stack ->
      if (stack.lastOrNull() == AppScreen.Game) stack else stack + AppScreen.Game
    }
  }

  fun startEndlessRandom(
    difficulty: Difficulty,
    pack: TilePack,
    boardIndex: Int = 1,
    streak: Int = 0,
    carryScore: Int = 0,
  ) {
    val mode = GameMode.EndlessRandom(difficulty, pack, boardIndex, streak)
    val board = MahjongBoardGenerator.generateEndlessBoard(difficulty, pack, boardIndex)
    setNewBoard(mode, board, carryScore = carryScore)
    _screenStack.update { stack ->
      if (stack.lastOrNull() == AppScreen.Game) stack else stack + AppScreen.Game
    }
  }

  private fun setNewBoard(mode: GameMode, initialTiles: List<BoardTile>, carryScore: Int) {
    var tiles = initialTiles
    if (tiles.isNotEmpty() && MahjongRules.findAvailablePairs(tiles).isEmpty()) {
      tiles = MahjongRules.reshuffleSolvable(tiles)
    }
    _gameState.value = GameBoardState(
      mode = mode,
      tiles = tiles,
      selectedTileId = null,
      hintTileIds = emptySet(),
      blockedShakeTileId = null,
      lastMatchedSymbol = null,
      score = carryScore,
      availablePairsCount = MahjongRules.findAvailablePairs(tiles).size,
      isBoardComplete = false,
      autoShuffledNotice = false,
    )
  }

  fun onTileClicked(tile: BoardTile) {
    val current = _gameState.value
    if (current.isBoardComplete) return

    if (!tile.isFree) {
      audioManager.playTileBlocked()
      _gameState.update { it.copy(blockedShakeTileId = tile.instanceId) }
      viewModelScope.launch {
        delay(360)
        _gameState.update { state ->
          if (state.blockedShakeTileId == tile.instanceId) {
            state.copy(blockedShakeTileId = null)
          } else {
            state
          }
        }
      }
      return
    }

    val selectedId = current.selectedTileId
    if (selectedId == null) {
      audioManager.playTileTap()
      if (preferences.value.speakOnTap) {
        audioManager.speakSymbol(tile.symbol)
      }
      _gameState.update {
        it.copy(
          selectedTileId = tile.instanceId,
          hintTileIds = emptySet(),
          lastMatchedSymbol = tile.symbol,
        )
      }
      return
    }

    if (selectedId == tile.instanceId) {
      audioManager.playTileTap()
      if (preferences.value.speakOnTap) {
        audioManager.speakSymbol(tile.symbol)
      }
      _gameState.update { it.copy(selectedTileId = null) }
      return
    }

    val selectedTile = current.tiles.firstOrNull { it.instanceId == selectedId }
    if (selectedTile != null && MahjongRules.canMatch(selectedTile, tile)) {
      handleMatch(selectedTile, tile)
    } else {
      audioManager.playTileTap()
      if (preferences.value.speakOnTap) {
        audioManager.speakSymbol(tile.symbol)
      }
      _gameState.update {
        it.copy(
          selectedTileId = tile.instanceId,
          hintTileIds = emptySet(),
          lastMatchedSymbol = tile.symbol,
        )
      }
    }
  }

  private fun handleMatch(first: BoardTile, second: BoardTile) {
    val current = _gameState.value
    val remaining = current.tiles.filter {
      it.instanceId != first.instanceId && it.instanceId != second.instanceId
    }
    var updatedTiles = MahjongRules.recomputeFreeStates(remaining)
    var pairsCount = MahjongRules.findAvailablePairs(updatedTiles).size
    var autoShuffled = false

    if (updatedTiles.isNotEmpty() && pairsCount == 0) {
      updatedTiles = MahjongRules.reshuffleSolvable(updatedTiles, Random(System.nanoTime()))
      pairsCount = MahjongRules.findAvailablePairs(updatedTiles).size
      autoShuffled = true
    }

    val newScore = current.score + 100
    val completed = updatedTiles.isEmpty()

    _gameState.update {
      it.copy(
        tiles = updatedTiles,
        selectedTileId = null,
        hintTileIds = emptySet(),
        lastMatchedSymbol = second.symbol,
        score = newScore,
        availablePairsCount = pairsCount,
        isBoardComplete = completed,
        autoShuffledNotice = autoShuffled,
        burstNonce = it.burstNonce + 1,
        burstGridX = second.x,
        burstGridY = second.y,
      )
    }

    audioManager.playMatchSuccess()
    viewModelScope.launch {
      delay(140)
      audioManager.speakSymbol(second.symbol)
    }

    if (autoShuffled) {
      viewModelScope.launch {
        delay(1800)
        _gameState.update { it.copy(autoShuffledNotice = false) }
      }
    }

    if (completed) {
      onBoardCleared(current.mode, newScore)
    }
  }

  private fun onBoardCleared(mode: GameMode, finalScore: Int) {
    viewModelScope.launch {
      delay(650)
      audioManager.playVictoryFanfare()
      when (mode) {
        is GameMode.CampaignLevel -> {
          repository.recordCampaignLevelCleared(
            difficulty = mode.difficulty,
            clearedLevel = mode.levelNumber,
            earnedScore = finalScore,
          )
        }

        is GameMode.EndlessRandom -> {
          repository.recordEndlessBoardCleared(
            newStreak = mode.streak + 1,
            earnedScore = finalScore,
          )
        }
      }
    }
  }

  fun shuffleBoard() {
    val current = _gameState.value
    if (current.tiles.size < 2 || current.isBoardComplete) return
    audioManager.playTileTap()
    val reshuffled = MahjongRules.reshuffleSolvable(current.tiles, Random(System.nanoTime()))
    _gameState.update {
      it.copy(
        tiles = reshuffled,
        selectedTileId = null,
        hintTileIds = emptySet(),
        availablePairsCount = MahjongRules.findAvailablePairs(reshuffled).size,
      )
    }
  }

  fun showHint() {
    val current = _gameState.value
    if (current.isBoardComplete) return
    val pairs = MahjongRules.findAvailablePairs(current.tiles)
    if (pairs.isNotEmpty()) {
      audioManager.playTileTap()
      val chosen = pairs.first()
      _gameState.update {
        it.copy(
          hintTileIds = setOf(chosen.first.instanceId, chosen.second.instanceId),
          selectedTileId = null,
        )
      }
    } else {
      shuffleBoard()
    }
  }

  fun repeatLastPronunciation() {
    val symbol = _gameState.value.lastMatchedSymbol ?: return
    audioManager.speakSymbol(symbol)
  }

  fun testSoundAndVoice() {
    audioManager.playMatchSuccess()
    val sampleOne = com.example.domain.model.TileCatalog.numbers[1]
    viewModelScope.launch {
      delay(180)
      audioManager.speakSymbol(sampleOne)
    }
  }

  fun testArabicVoice() {
    audioManager.playMatchSuccess()
    val sampleAlif = com.example.domain.model.TileCatalog.arabicLetters[0]
    viewModelScope.launch {
      delay(180)
      audioManager.speakSymbol(sampleAlif)
    }
  }

  fun refreshTts() {
    audioManager.reinitializeTts()
  }

  fun playNextBoard() {
    when (val mode = _gameState.value.mode) {
      is GameMode.CampaignLevel -> {
        startCampaignLevel(
          difficulty = mode.difficulty,
          levelNumber = mode.levelNumber + 1,
          pack = mode.tilePack,
        )
      }

      is GameMode.EndlessRandom -> {
        startEndlessRandom(
          difficulty = mode.difficulty,
          pack = mode.tilePack,
          boardIndex = mode.boardIndex + 1,
          streak = mode.streak + 1,
          carryScore = _gameState.value.score,
        )
      }
    }
  }

  fun replayCurrentBoard() {
    when (val mode = _gameState.value.mode) {
      is GameMode.CampaignLevel -> {
        startCampaignLevel(mode.difficulty, mode.levelNumber, mode.tilePack)
      }

      is GameMode.EndlessRandom -> {
        startEndlessRandom(mode.difficulty, mode.tilePack, mode.boardIndex, mode.streak, 0)
      }
    }
  }

  // --- Settings Updates ---

  fun toggleMusic(enabled: Boolean) {
    viewModelScope.launch { repository.setMusicEnabled(enabled) }
  }

  fun toggleSfx(enabled: Boolean) {
    viewModelScope.launch { repository.setSfxEnabled(enabled) }
  }

  fun toggleVoice(enabled: Boolean) {
    viewModelScope.launch { repository.setVoiceEnabled(enabled) }
  }

  fun toggleSpeakOnTap(enabled: Boolean) {
    viewModelScope.launch { repository.setSpeakOnTap(enabled) }
  }

  fun toggleHighlightFreeTiles(enabled: Boolean) {
    viewModelScope.launch { repository.setHighlightFreeTiles(enabled) }
  }

  fun setArabicVoiceMode(mode: ArabicVoiceMode) {
    viewModelScope.launch { repository.setArabicVoiceMode(mode) }
  }

  override fun onCleared() {
    audioManager.release()
    super.onCleared()
  }
}
