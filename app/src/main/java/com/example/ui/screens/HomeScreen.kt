package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.Difficulty
import com.example.domain.model.GamePreferences
import com.example.domain.model.TilePack
import com.example.ui.components.EmeraldFeltBackground
import com.example.ui.components.MiniTileBadge
import com.example.ui.components.WoodCircleButton
import com.example.ui.theme.EmeraldSurfaceCard
import com.example.ui.theme.EmeraldSurfaceCardBorder
import com.example.ui.theme.EmeraldTableEdge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MintLabel
import com.example.ui.theme.SymbolArabicColor
import com.example.ui.theme.SymbolLatinColor
import com.example.ui.theme.SymbolNumberColor
import com.example.ui.theme.TextLightPrimary
import com.example.ui.theme.TextLightSecondary
import com.example.ui.theme.WoodButtonLight

@Composable
fun HomeScreen(
  preferences: GamePreferences,
  selectedDifficulty: Difficulty,
  selectedPack: TilePack,
  onSelectDifficulty: (Difficulty) -> Unit,
  onSelectPack: (TilePack) -> Unit,
  onPlayCampaignLevel: (Difficulty, Int, TilePack) -> Unit,
  onOpenLevelMap: (Difficulty, TilePack) -> Unit,
  onPlayEndless: (Difficulty, TilePack) -> Unit,
  onToggleMusic: (Boolean) -> Unit,
  onOpenSettings: () -> Unit,
) {
  EmeraldFeltBackground {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top utility row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 560.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color = EmeraldSurfaceCard.copy(alpha = 0.9f),
          border = BorderStroke(1.dp, EmeraldSurfaceCardBorder),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Star,
              contentDescription = null,
              tint = GoldAccent,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "${preferences.totalScore} pts",
              style = MaterialTheme.typography.labelLarge,
              color = TextLightPrimary,
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          WoodCircleButton(
            icon = if (preferences.musicEnabled) {
              Icons.AutoMirrored.Rounded.VolumeUp
            } else {
              Icons.AutoMirrored.Rounded.VolumeOff
            },
            contentDescription = stringResource(R.string.settings_music),
            onClick = { onToggleMusic(!preferences.musicEnabled) },
            testTag = "home_music_toggle",
          )
          WoodCircleButton(
            icon = Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.action_settings),
            onClick = onOpenSettings,
            testTag = "home_settings_button",
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Playful 3D Tile Hero Trio (A, 1, أ)
      Row(
        horizontalArrangement = Arrangement.spacedBy((-6).dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        MiniTileBadge(
          glyph = "A",
          color = SymbolLatinColor,
          modifier = Modifier.rotate(-8f),
        )
        MiniTileBadge(
          glyph = "1",
          color = SymbolNumberColor,
          modifier = Modifier.padding(bottom = 8.dp),
        )
        MiniTileBadge(
          glyph = "أ",
          color = SymbolArabicColor,
          isArabic = true,
          modifier = Modifier.rotate(8f),
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp),
        color = TextLightPrimary,
        textAlign = TextAlign.Center,
      )
      Text(
        text = stringResource(R.string.home_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = TextLightSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Tile Pack Selector (Tout, A-Z, 0-10, أ-ي)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 560.dp),
      ) {
        Text(
          text = "Contenu des tuiles",
          style = MaterialTheme.typography.labelLarge,
          color = MintLabel,
          modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          TilePack.entries.forEach { pack ->
            val selected = pack == selectedPack
            FilterChip(
              selected = selected,
              onClick = { onSelectPack(pack) },
              modifier = Modifier.testTag("pack_chip_${pack.name}"),
              label = {
                Text(
                  text = pack.titleFr,
                  style = MaterialTheme.typography.labelLarge,
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                containerColor = EmeraldSurfaceCard,
                labelColor = TextLightSecondary,
                selectedContainerColor = GoldAccent,
                selectedLabelColor = EmeraldTableEdge,
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = EmeraldSurfaceCardBorder,
                selectedBorderColor = GoldAccent,
              ),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Card 1: Infinite Campaign Levels (Facile, Moyen, Difficile)
      val currentLevel = preferences.unlockedLevelFor(selectedDifficulty)
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 560.dp),
        shape = RoundedCornerShape(24.dp),
        color = EmeraldSurfaceCard.copy(alpha = 0.94f),
        border = BorderStroke(1.5.dp, EmeraldSurfaceCardBorder),
        shadowElevation = 6.dp,
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.mode_levels_title),
                style = MaterialTheme.typography.titleLarge,
                color = TextLightPrimary,
              )
              Text(
                text = stringResource(R.string.mode_levels_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextLightSecondary,
              )
            }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = GoldAccent.copy(alpha = 0.16f),
              border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
            ) {
              Text(
                text = "Niv. $currentLevel / ∞",
                style = MaterialTheme.typography.labelLarge,
                color = GoldAccent,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }

          // Difficulty selector pills
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Difficulty.entries.forEach { diff ->
              val selected = diff == selectedDifficulty
              Surface(
                onClick = { onSelectDifficulty(diff) },
                modifier = Modifier
                  .weight(1f)
                  .testTag("difficulty_${diff.name}"),
                shape = RoundedCornerShape(14.dp),
                color = if (selected) WoodButtonLight else EmeraldTableEdge.copy(alpha = 0.6f),
                border = BorderStroke(
                  width = if (selected) 2.dp else 1.dp,
                  color = if (selected) GoldAccent else EmeraldSurfaceCardBorder,
                ),
              ) {
                Column(
                  modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(
                    text = diff.labelFr,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextLightPrimary,
                    maxLines = 1,
                  )
                  Text(
                    text = diff.labelAr,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) GoldAccent else TextLightSecondary,
                    maxLines = 1,
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = {
                onPlayCampaignLevel(selectedDifficulty, currentLevel, selectedPack)
              },
              modifier = Modifier
                .weight(1.4f)
                .height(52.dp)
                .testTag("play_campaign_button"),
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = EmeraldTableEdge,
              ),
            ) {
              Icon(Icons.Rounded.PlayArrow, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text(
                text = "Niveau $currentLevel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }

            OutlinedButton(
              onClick = { onOpenLevelMap(selectedDifficulty, selectedPack) },
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("open_levels_map_button"),
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.5.dp, EmeraldSurfaceCardBorder),
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextLightPrimary,
              ),
            ) {
              Icon(Icons.Rounded.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(
                text = "Niveaux",
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Card 2: Endless Random Mode (Jeu sans fin avec niveaux générés aléatoirement)
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 560.dp),
        shape = RoundedCornerShape(24.dp),
        color = EmeraldSurfaceCard.copy(alpha = 0.94f),
        border = BorderStroke(1.5.dp, EmeraldSurfaceCardBorder),
        shadowElevation = 6.dp,
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.AllInclusive,
                  contentDescription = null,
                  tint = MintLabel,
                  modifier = Modifier.size(22.dp),
                )
                Text(
                  text = stringResource(R.string.mode_endless_title),
                  style = MaterialTheme.typography.titleLarge,
                  color = TextLightPrimary,
                )
              }
              Text(
                text = stringResource(R.string.mode_endless_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextLightSecondary,
              )
            }
            if (preferences.bestEndlessStreak > 0) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MintLabel.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, MintLabel.copy(alpha = 0.45f)),
              ) {
                Text(
                  text = "Série : ${preferences.bestEndlessStreak}",
                  style = MaterialTheme.typography.labelLarge,
                  color = MintLabel,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
              }
            }
          }

          Button(
            onClick = { onPlayEndless(selectedDifficulty, selectedPack) },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("play_endless_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = WoodButtonLight,
              contentColor = TextLightPrimary,
            ),
          ) {
            Icon(Icons.Rounded.AllInclusive, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
              text = "Jouer Sans Fin (${selectedDifficulty.labelFr})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
