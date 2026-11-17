package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MahjongColorScheme = darkColorScheme(
  primary = WoodButtonLight,
  onPrimary = TextLightPrimary,
  primaryContainer = EmeraldSurfaceCard,
  onPrimaryContainer = TextLightPrimary,
  secondary = GoldAccent,
  onSecondary = EmeraldTableEdge,
  secondaryContainer = WoodButtonDark,
  onSecondaryContainer = TextLightPrimary,
  tertiary = MintLabel,
  onTertiary = EmeraldTableEdge,
  background = EmeraldTableMid,
  onBackground = TextLightPrimary,
  surface = EmeraldSurfaceCard,
  onSurface = TextLightPrimary,
  surfaceVariant = EmeraldTableEdge,
  onSurfaceVariant = TextLightSecondary,
  outline = EmeraldSurfaceCardBorder,
)

private val AppShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(24.dp),
)

@Composable
fun MahjongAbcTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = MahjongColorScheme,
    typography = Typography,
    shapes = AppShapes,
    content = content,
  )
}
