package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.GameViewModel
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelMapScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MahjongAbcTheme

class MainActivity : ComponentActivity() {

  private val viewModel: GameViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MahjongAbcTheme {
        val screenStack by viewModel.screenStack.collectAsStateWithLifecycle()
        val preferences by viewModel.preferences.collectAsStateWithLifecycle()
        val ttsDiagnostics by viewModel.ttsDiagnostics.collectAsStateWithLifecycle()
        val selectedDifficulty by viewModel.selectedHomeDifficulty.collectAsStateWithLifecycle()
        val selectedPack by viewModel.selectedHomePack.collectAsStateWithLifecycle()
        val gameState by viewModel.gameState.collectAsStateWithLifecycle()

        val currentScreen = screenStack.lastOrNull() ?: AppScreen.Home

        AnimatedContent(
          targetState = currentScreen,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "screenTransition",
        ) { screen ->
          when (screen) {
            AppScreen.Home -> {
              HomeScreen(
                preferences = preferences,
                selectedDifficulty = selectedDifficulty,
                selectedPack = selectedPack,
                onSelectDifficulty = viewModel::setHomeDifficulty,
                onSelectPack = viewModel::setHomePack,
                onPlayCampaignLevel = viewModel::startCampaignLevel,
                onOpenLevelMap = viewModel::openLevelMap,
                onPlayEndless = { diff, pack ->
                  viewModel.startEndlessRandom(difficulty = diff, pack = pack)
                },
                onToggleMusic = viewModel::toggleMusic,
                onOpenSettings = viewModel::openSettings,
              )
            }

            is AppScreen.LevelMap -> {
              LevelMapScreen(
                initialDifficulty = screen.difficulty,
                pack = screen.pack,
                preferences = preferences,
                onSelectLevel = viewModel::startCampaignLevel,
                onOpenSettings = viewModel::openSettings,
                onBack = viewModel::navigateBack,
              )
            }

            AppScreen.Game -> {
              GameScreen(
                gameState = gameState,
                preferences = preferences,
                onTileClick = viewModel::onTileClicked,
                onShuffle = viewModel::shuffleBoard,
                onHint = viewModel::showHint,
                onRepeatSound = viewModel::repeatLastPronunciation,
                onNextBoard = viewModel::playNextBoard,
                onReplayBoard = viewModel::replayCurrentBoard,
                onOpenSettings = viewModel::openSettings,
                onBack = viewModel::navigateBack,
                onBackHome = viewModel::navigateHome,
              )
            }

            AppScreen.Settings -> {
              SettingsScreen(
                preferences = preferences,
                ttsDiagnostics = ttsDiagnostics,
                onToggleMusic = viewModel::toggleMusic,
                onToggleSfx = viewModel::toggleSfx,
                onToggleVoice = viewModel::toggleVoice,
                onToggleSpeakOnTap = viewModel::toggleSpeakOnTap,
                onToggleHighlightFreeTiles = viewModel::toggleHighlightFreeTiles,
                onSelectArabicVoiceMode = viewModel::setArabicVoiceMode,
                onTestAudio = viewModel::testSoundAndVoice,
                onTestArabicAudio = viewModel::testArabicVoice,
                onBack = viewModel::navigateBack,
              )
            }
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.onAppForegroundChanged(true)
  }

  override fun onPause() {
    viewModel.onAppForegroundChanged(false)
    super.onPause()
  }
}
