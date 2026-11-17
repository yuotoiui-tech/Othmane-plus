package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.BoardTile
import com.example.domain.model.TileCategory
import com.example.ui.theme.BadgeRed
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.EmeraldTableCenter
import com.example.ui.theme.EmeraldTableEdge
import com.example.ui.theme.EmeraldTableMid
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SymbolArabicColor
import com.example.ui.theme.SymbolLatinColor
import com.example.ui.theme.SymbolNumberColor
import com.example.ui.theme.TextLightPrimary
import com.example.ui.theme.TileBlockedOverlay
import com.example.ui.theme.TileHintBorder
import com.example.ui.theme.TileIvoryBottom
import com.example.ui.theme.TileIvoryTop
import com.example.ui.theme.TileRimGreen
import com.example.ui.theme.TileRimGreenDark
import com.example.ui.theme.TileSelectedBorder
import com.example.ui.theme.WoodButtonDark
import com.example.ui.theme.WoodButtonLight
import com.example.ui.theme.WoodButtonRim
import kotlin.math.roundToInt

@Composable
fun EmeraldFeltBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        brush = Brush.radialGradient(
          colors = listOf(
            EmeraldTableCenter,
            EmeraldTableMid,
            EmeraldTableEdge,
          ),
          radius = 1200f,
        ),
      ),
  ) {
    // Subtle organic felt texture speckles & corner warmth
    Canvas(modifier = Modifier.fillMaxSize()) {
      val stepX = size.width / 8f
      val stepY = size.height / 12f
      for (ix in 0..8) {
        for (iy in 0..12) {
          val x = ix * stepX + ((iy % 2) * stepX * 0.45f)
          val y = iy * stepY
          drawCircle(
            color = Color.White.copy(alpha = 0.025f),
            radius = 2.2f,
            center = Offset(x, y),
          )
        }
      }
    }
    content()
  }
}

@Composable
fun WoodCircleButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
  testTag: String? = null,
) {
  Box(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .size(size)
      .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
      .shadow(elevation = 6.dp, shape = CircleShape)
      .clip(CircleShape)
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(WoodButtonLight, WoodButtonDark),
        ),
      )
      .border(width = 2.dp, color = WoodButtonRim, shape = CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = GoldAccent,
      modifier = Modifier.size(size * 0.54f),
    )
  }
}

@Composable
fun WoodDiamondActionButton(
  icon: ImageVector,
  label: String,
  badgeText: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String? = null,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier,
  ) {
    Box(
      modifier = Modifier
        .size(66.dp)
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onClick,
        ),
      contentAlignment = Alignment.Center,
    ) {
      // Rotated wooden diamond backdrop matching the reference screenshot
      Box(
        modifier = Modifier
          .size(50.dp)
          .rotate(45f)
          .shadow(8.dp, RoundedCornerShape(13.dp))
          .clip(RoundedCornerShape(13.dp))
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(WoodButtonLight, WoodButtonDark),
            ),
          )
          .border(2.5.dp, WoodButtonRim, RoundedCornerShape(13.dp)),
      )
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = GoldAccent,
        modifier = Modifier.size(28.dp),
      )
      if (!badgeText.isNullOrEmpty()) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = (-2).dp, y = 2.dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(BadgeRed)
            .border(1.5.dp, TextLightPrimary, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Color.White,
            maxLines = 1,
          )
        }
      }
    }
    Spacer(modifier = Modifier.size(2.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = TextLightPrimary.copy(alpha = 0.9f),
      maxLines = 1,
    )
  }
}

@Composable
fun MahjongTileView(
  tile: BoardTile,
  tileWidth: Dp,
  tileHeight: Dp,
  isSelected: Boolean,
  isHinted: Boolean,
  isShaking: Boolean,
  highlightFreeTiles: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val scale by animateFloatAsState(
    targetValue = if (isSelected) 1.06f else 1.0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "tileScale",
  )
  val liftY by animateDpAsState(
    targetValue = if (isSelected) (-4).dp else 0.dp,
    animationSpec = spring(stiffness = Spring.StiffnessMedium),
    label = "tileLift",
  )

  val shakeOffset = remember { Animatable(0f) }
  LaunchedEffect(isShaking) {
    if (isShaking) {
      val px = with(density) { 5.dp.toPx() }
      shakeOffset.snapTo(0f)
      shakeOffset.animateTo(px, tween(45))
      shakeOffset.animateTo(-px, tween(55))
      shakeOffset.animateTo(px * 0.6f, tween(50))
      shakeOffset.animateTo(0f, tween(50))
    } else {
      shakeOffset.snapTo(0f)
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "hintPulse")
  val hintAlpha by infiniteTransition.animateFloat(
    initialValue = 0.45f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(650, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "hintAlpha",
  )

  val depthX = (tileWidth * 0.08f).coerceIn(3.dp, 6.dp)
  val depthY = (tileHeight * 0.09f).coerceIn(4.dp, 8.dp)
  val cornerRadius = (tileWidth * 0.18f).coerceIn(6.dp, 13.dp)

  val symbolColor = when (tile.symbol.category) {
    TileCategory.LATIN_LETTER -> SymbolLatinColor
    TileCategory.NUMBER -> SymbolNumberColor
    TileCategory.ARABIC_LETTER -> SymbolArabicColor
  }

  val symbolFont = when (tile.symbol.category) {
    TileCategory.ARABIC_LETTER -> CairoFontFamily
    TileCategory.LATIN_LETTER, TileCategory.NUMBER -> FredokaFontFamily
  }

  val fontSizeSp = with(density) {
    val base = tileWidth.toSp() * when {
      tile.symbol.glyph.length >= 2 -> 0.46f // "10"
      tile.symbol.category == TileCategory.ARABIC_LETTER -> 0.56f
      else -> 0.58f
    }
    base
  }

  Box(
    modifier = modifier
      .size(width = tileWidth + depthX, height = tileHeight + depthY)
      .offset { IntOffset(shakeOffset.value.roundToInt(), liftY.roundToPx()) }
      .scale(scale)
      .testTag("tile_${tile.instanceId}")
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
      ),
  ) {
    // 1. Soft drop shadow beneath the tile
    Box(
      modifier = Modifier
        .offset(x = depthX * 0.65f, y = depthY * 0.9f)
        .size(width = tileWidth, height = tileHeight)
        .clip(RoundedCornerShape(cornerRadius))
        .background(Color.Black.copy(alpha = 0.34f)),
    )

    // 2. Emerald-green 3D extruded tile body
    Box(
      modifier = Modifier
        .offset(x = depthX, y = depthY)
        .size(width = tileWidth, height = tileHeight)
        .clip(RoundedCornerShape(cornerRadius))
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(TileRimGreen, TileRimGreenDark),
          ),
        ),
    )

    // 3. Ivory front tile face
    val borderColor = when {
      isSelected -> TileSelectedBorder
      isHinted -> TileHintBorder.copy(alpha = hintAlpha)
      else -> Color(0xFFE3D8C5)
    }
    val borderWidth = if (isSelected || isHinted) 2.5.dp else 1.dp

    Surface(
      modifier = Modifier
        .size(width = tileWidth, height = tileHeight),
      shape = RoundedCornerShape(cornerRadius),
      border = BorderStroke(borderWidth, borderColor),
      color = Color.Transparent,
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(TileIvoryTop, TileIvoryBottom),
            ),
          ),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = tile.symbol.glyph,
          fontFamily = symbolFont,
          fontWeight = FontWeight.Bold,
          fontSize = fontSizeSp,
          color = symbolColor,
          textAlign = TextAlign.Center,
          maxLines = 1,
        )

        // Subtle blocked veil when highlightFreeTiles is enabled
        if (!tile.isFree && highlightFreeTiles) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(TileBlockedOverlay),
          )
        }
      }
    }
  }
}

@Composable
fun MiniTileBadge(
  glyph: String,
  color: Color,
  isArabic: Boolean = false,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .size(width = 44.dp, height = 54.dp)
      .shadow(6.dp, RoundedCornerShape(10.dp))
      .clip(RoundedCornerShape(10.dp))
      .background(TileRimGreenDark)
      .padding(bottom = 4.dp, end = 3.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(9.dp))
        .background(
          brush = Brush.verticalGradient(
            colors = listOf(TileIvoryTop, TileIvoryBottom),
          ),
        ),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = glyph,
        fontFamily = if (isArabic) CairoFontFamily else FredokaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = color,
      )
    }
  }
}

@Composable
fun StatBadgeRow(
  score: Int,
  pairsCount: Int,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(24.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "Score",
        style = MaterialTheme.typography.labelLarge,
        color = Color(0xFF4BE39E),
      )
      Text(
        text = score.toString(),
        style = MaterialTheme.typography.headlineMedium,
        color = TextLightPrimary,
      )
    }
    Spacer(modifier = Modifier.width(4.dp))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "Paires",
        style = MaterialTheme.typography.labelLarge,
        color = Color(0xFF4BE39E),
      )
      Text(
        text = pairsCount.toString(),
        style = MaterialTheme.typography.headlineMedium,
        color = TextLightPrimary,
      )
    }
  }
}
