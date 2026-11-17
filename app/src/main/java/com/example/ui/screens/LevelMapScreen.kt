package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.Difficulty
import com.example.domain.model.GamePreferences
import com.example.domain.model.TilePack
import com.example.ui.components.EmeraldFeltBackground
import com.example.ui.components.WoodCircleButton
import com.example.ui.theme.EmeraldSurfaceCard
import com.example.ui.theme.EmeraldSurfaceCardBorder
import com.example.ui.theme.EmeraldTableEdge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MintLabel
import com.example.ui.theme.TextLightPrimary
import com.example.ui.theme.TextLightSecondary
import com.example.ui.theme.TileIvoryTop
import com.example.ui.theme.TileRimGreenDark
import com.example.ui.theme.WoodButtonLight
import kotlin.math.max

@Composable
fun LevelMapScreen(
  initialDifficulty: Difficulty,
  pack: TilePack,
  preferences: GamePreferences,
  onSelectLevel: (Difficulty, Int, TilePack) -> Unit,
  onOpenSettings: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  var currentDifficulty by remember { mutableStateOf(initialDifficulty) }
  val unlockedLevel = preferences.unlockedLevelFor(currentDifficulty)
  var loadedLevelCount by remember(currentDifficulty, unlockedLevel) {
    mutableIntStateOf(max(36, unlockedLevel + 18))
  }

  val gridState = rememberLazyGridState()
  val shouldLoadMore by remember {
    derivedStateOf {
      val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      lastVisible >= loadedLevelCount - 8
    }
  }

  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) {
      loadedLevelCount += 18
    }
  }

  EmeraldFeltBackground {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Header bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        WoodCircleButton(
          icon = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.action_back),
          onClick = onBack,
          testTag = "levels_back_button",
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = stringResource(R.string.mode_levels_title),
            style = MaterialTheme.typography.titleLarge,
            color = TextLightPrimary,
          )
          Text(
            text = "${pack.titleFr} • ∞ Niveaux",
            style = MaterialTheme.typography.labelLarge,
            color = MintLabel,
          )
        }
        WoodCircleButton(
          icon = Icons.Rounded.Settings,
          contentDescription = stringResource(R.string.action_settings),
          onClick = onOpenSettings,
          testTag = "levels_settings_button",
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Difficulty Tabs
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 600.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Difficulty.entries.forEach { diff ->
          val selected = diff == currentDifficulty
          Surface(
            onClick = { currentDifficulty = diff },
            modifier = Modifier
              .weight(1f)
              .testTag("map_difficulty_${diff.name}"),
            shape = RoundedCornerShape(14.dp),
            color = if (selected) WoodButtonLight else EmeraldSurfaceCard,
            border = BorderStroke(
              width = if (selected) 2.dp else 1.dp,
              color = if (selected) GoldAccent else EmeraldSurfaceCardBorder,
            ),
          ) {
            Column(
              modifier = Modifier.padding(vertical = 10.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = diff.labelFr,
                style = MaterialTheme.typography.labelLarge,
                color = TextLightPrimary,
              )
              Text(
                text = diff.labelAr,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) GoldAccent else TextLightSecondary,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Infinite level grid
      LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 86.dp),
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 600.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(loadedLevelCount) { index ->
          val levelNum = index + 1
          val isCompleted = levelNum < unlockedLevel
          val isCurrent = levelNum == unlockedLevel
          val isUnlocked = levelNum <= unlockedLevel

          LevelTileNode(
            levelNumber = levelNum,
            isCompleted = isCompleted,
            isCurrent = isCurrent,
            isUnlocked = isUnlocked,
            onClick = {
              if (isUnlocked) {
                onSelectLevel(currentDifficulty, levelNum, pack)
              }
            },
          )
        }
      }
    }
  }
}

@Composable
private fun LevelTileNode(
  levelNumber: Int,
  isCompleted: Boolean,
  isCurrent: Boolean,
  isUnlocked: Boolean,
  onClick: () -> Unit,
) {
  val bgColor = when {
    isCurrent -> GoldAccent
    isCompleted -> TileIvoryTop
    else -> EmeraldSurfaceCard.copy(alpha = 0.72f)
  }
  val borderColor = when {
    isCurrent -> TextLightPrimary
    isCompleted -> TileRimGreenDark
    else -> EmeraldSurfaceCardBorder.copy(alpha = 0.5f)
  }
  val textColor = when {
    isCurrent || isCompleted -> EmeraldTableEdge
    else -> TextLightSecondary.copy(alpha = 0.65f)
  }

  Surface(
    onClick = onClick,
    enabled = isUnlocked,
    modifier = Modifier
      .aspectRatio(0.92f)
      .testTag("level_node_$levelNumber"),
    shape = RoundedCornerShape(18.dp),
    color = bgColor,
    border = BorderStroke(if (isCurrent) 2.5.dp else 1.5.dp, borderColor),
    shadowElevation = if (isUnlocked) 5.dp else 0.dp,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = levelNumber.toString(),
          style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
          ),
          color = textColor,
        )
        Spacer(modifier = Modifier.height(4.dp))
        when {
          isCompleted -> {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              repeat(3) {
                Icon(
                  imageVector = Icons.Rounded.Star,
                  contentDescription = null,
                  tint = WoodButtonLight,
                  modifier = Modifier.size(13.dp),
                )
              }
            }
          }

          isCurrent -> {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = "Jouer niveau $levelNumber",
              tint = EmeraldTableEdge,
              modifier = Modifier.size(18.dp),
            )
          }

          else -> {
            Icon(
              imageVector = Icons.Rounded.Lock,
              contentDescription = "Verrouillé",
              tint = TextLightSecondary.copy(alpha = 0.55f),
              modifier = Modifier.size(15.dp),
            )
          }
        }
      }
    }
  }
}
