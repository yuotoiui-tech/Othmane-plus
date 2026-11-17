package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.BoardTile
import com.example.domain.model.GameMode
import com.example.domain.model.GamePreferences
import com.example.domain.model.TileCategory
import com.example.domain.model.TileSymbol
import com.example.ui.GameBoardState
import com.example.ui.components.EmeraldFeltBackground
import com.example.ui.components.MahjongTileView
import com.example.ui.components.StatBadgeRow
import com.example.ui.components.WoodCircleButton
import com.example.ui.components.WoodDiamondActionButton
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.EmeraldSurfaceCard
import com.example.ui.theme.EmeraldSurfaceCardBorder
import com.example.ui.theme.EmeraldTableEdge
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MintLabel
import com.example.ui.theme.TextLightPrimary
import com.example.ui.theme.TextLightSecondary
import com.example.ui.theme.WoodButtonLight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun GameScreen(
  gameState: GameBoardState,
  preferences: GamePreferences,
  onTileClick: (BoardTile) -> Unit,
  onShuffle: () -> Unit,
  onHint: () -> Unit,
  onRepeatSound: () -> Unit,
  onNextBoard: () -> Unit,
  onReplayBoard: () -> Unit,
  onOpenSettings: () -> Unit,
  onBack: () -> Unit,
  onBackHome: () -> Unit,
) {
  BackHandler(onBack = onBack)

  EmeraldFeltBackground {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top HUD matching Mahjong Master layout
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 680.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        WoodCircleButton(
          icon = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.action_back),
          onClick = onBack,
          testTag = "game_back_button",
        )

        StatBadgeRow(
          score = gameState.score,
          pairsCount = gameState.availablePairsCount,
        )

        WoodCircleButton(
          icon = Icons.Rounded.Settings,
          contentDescription = stringResource(R.string.action_settings),
          onClick = onOpenSettings,
          testTag = "game_settings_button",
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Mode pill + Live Pronunciation Pill
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 680.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ModeStatusPill(mode = gameState.mode)

        AnimatedVisibility(
          visible = gameState.lastMatchedSymbol != null,
          enter = fadeIn() + scaleIn(initialScale = 0.85f),
          exit = fadeOut(),
        ) {
          gameState.lastMatchedSymbol?.let { sym ->
            Spacer(modifier = Modifier.width(8.dp))
            PronunciationPill(
              symbol = sym,
              onClick = onRepeatSound,
            )
          }
        }
      }

      // Auto-shuffle info notice
      AnimatedVisibility(visible = gameState.autoShuffledNotice) {
        Surface(
          modifier = Modifier.padding(top = 6.dp),
          shape = RoundedCornerShape(50),
          color = GoldAccent,
        ) {
          Text(
            text = "Nouvelles paires mélangées !",
            style = MaterialTheme.typography.labelLarge,
            color = EmeraldTableEdge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
          )
        }
      }

      // Center 3D Mahjong Board
      BoxWithConstraints(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
      ) {
        val tiles = gameState.tiles
        if (tiles.isNotEmpty()) {
          val minX = tiles.minOf { it.x }
          val maxX = tiles.maxOf { it.x }
          val minY = tiles.minOf { it.y }
          val maxY = tiles.maxOf { it.y }
          val maxZ = tiles.maxOf { it.z }

          val spanCols = ((maxX - minX) / 2f) + 1f
          val spanRows = ((maxY - minY) / 2f) + 1f

          val availW = maxWidth.value - 18f
          val availH = maxHeight.value - 18f

          val tileWidthByW = availW / (spanCols + 0.35f)
          val tileWidthByH = availH / ((spanRows * 1.24f) + 0.35f)
          val tileW = min(tileWidthByW, tileWidthByH).coerceIn(36f, 84f)
          val tileH = tileW * 1.24f

          val layerShiftX = 4.dp
          val layerShiftY = 5.dp

          val boardTotalW = (tileW * spanCols).dp + (layerShiftX * maxZ) + 10.dp
          val boardTotalH = (tileH * spanRows).dp + (layerShiftY * maxZ) + 12.dp

          val sortedTiles = remember(tiles) {
            tiles.sortedWith(compareBy<BoardTile>({ it.z }, { it.y }, { it.x }))
          }

          Box(
            modifier = Modifier.size(width = boardTotalW, height = boardTotalH),
          ) {
            sortedTiles.forEach { tile ->
              val colFrac = (tile.x - minX) / 2f
              val rowFrac = (tile.y - minY) / 2f
              val offsetX = (colFrac * tileW).dp + (layerShiftX * (maxZ - tile.z))
              val offsetY = (rowFrac * tileH).dp + (layerShiftY * (maxZ - tile.z))

              MahjongTileView(
                tile = tile,
                tileWidth = tileW.dp,
                tileHeight = tileH.dp,
                isSelected = gameState.selectedTileId == tile.instanceId,
                isHinted = tile.instanceId in gameState.hintTileIds,
                isShaking = gameState.blockedShakeTileId == tile.instanceId,
                highlightFreeTiles = preferences.highlightFreeTiles,
                onClick = { onTileClick(tile) },
                modifier = Modifier.offset(x = offsetX, y = offsetY),
              )
            }

            // Golden burst effect on matched tile location
            if (gameState.burstNonce > 0) {
              val burstCol = (gameState.burstGridX - minX) / 2f
              val burstRow = (gameState.burstGridY - minY) / 2f
              MatchSparkleBurst(
                nonce = gameState.burstNonce,
                centerX = ((burstCol + 0.5f) * tileW).dp,
                centerY = ((burstRow + 0.5f) * tileH).dp,
              )
            }
          }
        }
      }

      // Bottom HUD matching screenshot (Shuffle & Hint wooden diamond buttons)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 520.dp)
          .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        WoodDiamondActionButton(
          icon = Icons.Rounded.Sync,
          label = stringResource(R.string.action_shuffle),
          badgeText = "∞",
          onClick = onShuffle,
          testTag = "shuffle_button",
        )

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = EmeraldSurfaceCard.copy(alpha = 0.85f),
          border = BorderStroke(1.dp, EmeraldSurfaceCardBorder),
        ) {
          Text(
            text = "${gameState.tiles.size} tuiles",
            style = MaterialTheme.typography.labelLarge,
            color = TextLightSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          )
        }

        WoodDiamondActionButton(
          icon = Icons.Rounded.Lightbulb,
          label = stringResource(R.string.action_hint),
          badgeText = gameState.availablePairsCount.takeIf { it > 0 }?.toString(),
          onClick = onHint,
          testTag = "hint_button",
        )
      }
    }

    // Celebration overlay when board is cleared
    AnimatedVisibility(
      visible = gameState.isBoardComplete,
      enter = fadeIn() + scaleIn(initialScale = 0.9f),
      exit = fadeOut() + scaleOut(),
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.58f))
          .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        BoardCompleteDialog(
          mode = gameState.mode,
          score = gameState.score,
          onNextBoard = onNextBoard,
          onReplayBoard = onReplayBoard,
          onBackHome = onBackHome,
        )
      }
    }
  }
}

@Composable
private fun ModeStatusPill(mode: GameMode) {
  Surface(
    shape = RoundedCornerShape(50),
    color = EmeraldSurfaceCard.copy(alpha = 0.9f),
    border = BorderStroke(1.dp, EmeraldSurfaceCardBorder),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (mode is GameMode.EndlessRandom) {
        Icon(
          imageVector = Icons.Rounded.AllInclusive,
          contentDescription = null,
          tint = MintLabel,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = "Sans fin #${mode.boardIndex} • ${mode.difficulty.labelFr}",
          style = MaterialTheme.typography.labelLarge,
          color = TextLightPrimary,
        )
      } else if (mode is GameMode.CampaignLevel) {
        Text(
          text = "${mode.difficulty.labelFr} • Niveau ${mode.levelNumber}",
          style = MaterialTheme.typography.labelLarge,
          color = TextLightPrimary,
        )
      }
    }
  }
}

@Composable
private fun PronunciationPill(
  symbol: TileSymbol,
  onClick: () -> Unit,
) {
  val labelText = when (symbol.category) {
    TileCategory.ARABIC_LETTER -> {
      val ar = symbol.arabicDisplayName ?: symbol.glyph
      "${symbol.glyph}  •  $ar / ${symbol.frenchDisplayName}"
    }
    TileCategory.NUMBER -> "${symbol.glyph}  •  ${symbol.frenchDisplayName}"
    TileCategory.LATIN_LETTER -> "${symbol.glyph}  •  ${symbol.frenchDisplayName}"
  }

  Surface(
    onClick = onClick,
    modifier = Modifier.testTag("pronunciation_pill"),
    shape = RoundedCornerShape(50),
    color = GoldAccent,
    shadowElevation = 4.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
        contentDescription = stringResource(R.string.action_repeat_sound),
        tint = EmeraldTableEdge,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = labelText,
        fontFamily = if (symbol.category == TileCategory.ARABIC_LETTER) {
          CairoFontFamily
        } else {
          FredokaFontFamily
        },
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = EmeraldTableEdge,
      )
    }
  }
}

@Composable
private fun MatchSparkleBurst(
  nonce: Int,
  centerX: androidx.compose.ui.unit.Dp,
  centerY: androidx.compose.ui.unit.Dp,
) {
  val progress = remember(nonce) { Animatable(0f) }
  LaunchedEffect(nonce) {
    progress.snapTo(0f)
    progress.animateTo(1f, animationSpec = tween(480, easing = FastOutSlowInEasing))
  }

  val p = progress.value
  if (p < 0.99f) {
    Canvas(
      modifier = Modifier
        .offset(x = centerX - 40.dp, y = centerY - 40.dp)
        .size(80.dp),
    ) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val maxDist = size.width * 0.45f * p
      val alpha = (1f - p).coerceIn(0f, 1f)
      for (i in 0 until 8) {
        val angle = (i * PI / 4.0).toFloat()
        val px = cx + cos(angle) * maxDist
        val py = cy + sin(angle) * maxDist
        drawCircle(
          color = if (i % 2 == 0) GoldAccent.copy(alpha = alpha) else MintLabel.copy(alpha = alpha),
          radius = (6f * (1f - p * 0.5f)),
          center = Offset(px, py),
        )
      }
    }
  }
}

@Composable
private fun BoardCompleteDialog(
  mode: GameMode,
  score: Int,
  onNextBoard: () -> Unit,
  onReplayBoard: () -> Unit,
  onBackHome: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 420.dp)
      .testTag("board_complete_dialog"),
    shape = RoundedCornerShape(28.dp),
    color = EmeraldSurfaceCard,
    border = BorderStroke(2.dp, GoldAccent),
    shadowElevation = 12.dp,
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(34.dp))
        Icon(Icons.Rounded.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(44.dp))
        Icon(Icons.Rounded.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(34.dp))
      }

      Text(
        text = "Bravo ! • أحسنت !",
        style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
        color = GoldAccent,
        textAlign = TextAlign.Center,
      )

      val subtitle = when (mode) {
        is GameMode.CampaignLevel -> "Niveau ${mode.levelNumber} (${mode.difficulty.labelFr}) terminé !"
        is GameMode.EndlessRandom -> "Plateau #${mode.boardIndex} terminé ! Série : ${mode.streak + 1}"
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.titleMedium,
        color = TextLightPrimary,
        textAlign = TextAlign.Center,
      )

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = EmeraldTableEdge.copy(alpha = 0.65f),
      ) {
        Text(
          text = "Score : $score",
          style = MaterialTheme.typography.titleLarge,
          color = MintLabel,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = onNextBoard,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("next_level_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = GoldAccent,
          contentColor = EmeraldTableEdge,
        ),
      ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(
          text = if (mode is GameMode.EndlessRandom) "Plateau suivant" else stringResource(R.string.next_level),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onReplayBoard,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("replay_button"),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.5.dp, EmeraldSurfaceCardBorder),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightPrimary),
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(stringResource(R.string.replay_level))
        }

        OutlinedButton(
          onClick = onBackHome,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("complete_home_button"),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.5.dp, WoodButtonLight),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightPrimary),
        ) {
          Icon(Icons.Rounded.Home, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(stringResource(R.string.back_home))
        }
      }
    }
  }
}
