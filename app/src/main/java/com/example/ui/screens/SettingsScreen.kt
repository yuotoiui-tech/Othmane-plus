package com.example.ui.screens

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.TtsDiagnostics
import com.example.domain.model.ArabicVoiceMode
import com.example.domain.model.GamePreferences
import com.example.ui.components.EmeraldFeltBackground
import com.example.ui.components.WoodCircleButton
import com.example.ui.theme.EmeraldSurfaceCard
import com.example.ui.theme.EmeraldSurfaceCardBorder
import com.example.ui.theme.EmeraldTableEdge
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MintLabel
import com.example.ui.theme.TextLightPrimary
import com.example.ui.theme.TextLightSecondary

@Composable
fun SettingsScreen(
  preferences: GamePreferences,
  ttsDiagnostics: TtsDiagnostics,
  onToggleMusic: (Boolean) -> Unit,
  onToggleSfx: (Boolean) -> Unit,
  onToggleVoice: (Boolean) -> Unit,
  onToggleSpeakOnTap: (Boolean) -> Unit,
  onToggleHighlightFreeTiles: (Boolean) -> Unit,
  onSelectArabicVoiceMode: (ArabicVoiceMode) -> Unit,
  onTestAudio: () -> Unit,
  onTestArabicAudio: () -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current

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
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 560.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        WoodCircleButton(
          icon = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.action_back),
          onClick = onBack,
          testTag = "settings_back_button",
        )
        Text(
          text = stringResource(R.string.settings_title),
          style = MaterialTheme.typography.titleLarge,
          color = TextLightPrimary,
        )
        Spacer(modifier = Modifier.widthIn(min = 48.dp))
      }

      Spacer(modifier = Modifier.height(18.dp))

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 560.dp),
        shape = RoundedCornerShape(22.dp),
        color = EmeraldSurfaceCard.copy(alpha = 0.94f),
        border = BorderStroke(1.5.dp, EmeraldSurfaceCardBorder),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = "Audio & Prononciation",
            style = MaterialTheme.typography.titleMedium,
            color = MintLabel,
          )

          SettingToggleRow(
            icon = Icons.Rounded.MusicNote,
            title = stringResource(R.string.settings_music),
            subtitle = "Mélodie douce et apaisante pendant le jeu",
            checked = preferences.musicEnabled,
            onCheckedChange = onToggleMusic,
            testTag = "switch_music",
          )

          SettingToggleRow(
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            title = stringResource(R.string.settings_sfx),
            subtitle = "Sons en bois et carillon de réussite",
            checked = preferences.sfxEnabled,
            onCheckedChange = onToggleSfx,
            testTag = "switch_sfx",
          )

          SettingToggleRow(
            icon = Icons.Rounded.RecordVoiceOver,
            title = stringResource(R.string.settings_voice),
            subtitle = "Prononce la lettre ou le chiffre lors d'une paire",
            checked = preferences.voiceEnabled,
            onCheckedChange = onToggleVoice,
            testTag = "switch_voice",
          )

          SettingToggleRow(
            icon = Icons.Rounded.TouchApp,
            title = stringResource(R.string.settings_speak_on_tap),
            subtitle = "Prononce aussi quand l'enfant touche une seule tuile",
            checked = preferences.speakOnTap,
            onCheckedChange = onToggleSpeakOnTap,
            testTag = "switch_speak_tap",
          )

          SettingToggleRow(
            icon = Icons.Rounded.Visibility,
            title = stringResource(R.string.settings_show_hint_badge),
            subtitle = "Assombrit légèrement les tuiles bloquées",
            checked = preferences.highlightFreeTiles,
            onCheckedChange = onToggleHighlightFreeTiles,
            testTag = "switch_highlight_free",
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = onTestAudio,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("test_audio_button"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = EmeraldTableEdge,
              ),
            ) {
              Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text(
                text = "Tester (1 • Un)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            }

            Button(
              onClick = onTestArabicAudio,
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("test_arabic_audio_button"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MintLabel,
                contentColor = EmeraldTableEdge,
              ),
            ) {
              Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text(
                text = "Tester (أ • ألف)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          if (ttsDiagnostics.initialized && (!ttsDiagnostics.frenchVoiceReady || !ttsDiagnostics.arabicVoiceReady)) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = EmeraldTableEdge.copy(alpha = 0.7f),
              border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                val missing = buildList {
                  if (!ttsDiagnostics.frenchVoiceReady) add("Français")
                  if (!ttsDiagnostics.arabicVoiceReady) add("Arabe")
                }.joinToString(" & ")
                Text(
                  text = "Voix $missing non détectées dans les paramètres Android TTS.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = GoldAccent,
                )
                OutlinedButton(
                  onClick = {
                    runCatching {
                      context.startActivity(
                        Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                      )
                    }
                  },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, GoldAccent),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightPrimary),
                ) {
                  Icon(Icons.Rounded.Download, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text("Installer les voix TTS (FR / AR)")
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 560.dp),
        shape = RoundedCornerShape(22.dp),
        color = EmeraldSurfaceCard.copy(alpha = 0.94f),
        border = BorderStroke(1.5.dp, EmeraldSurfaceCardBorder),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Translate,
              contentDescription = null,
              tint = MintLabel,
            )
            Text(
              text = stringResource(R.string.settings_arabic_voice),
              style = MaterialTheme.typography.titleMedium,
              color = MintLabel,
            )
          }

          ArabicVoiceMode.entries.forEach { mode ->
            Surface(
              onClick = { onSelectArabicVoiceMode(mode) },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("arabic_voice_${mode.name}"),
              shape = RoundedCornerShape(14.dp),
              color = if (preferences.arabicVoiceMode == mode) {
                EmeraldTableEdge.copy(alpha = 0.7f)
              } else {
                EmeraldSurfaceCard
              },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                RadioButton(
                  selected = preferences.arabicVoiceMode == mode,
                  onClick = { onSelectArabicVoiceMode(mode) },
                  colors = RadioButtonDefaults.colors(
                    selectedColor = GoldAccent,
                    unselectedColor = TextLightSecondary,
                  ),
                )
                Text(
                  text = mode.label,
                  style = MaterialTheme.typography.bodyLarge,
                  color = TextLightPrimary,
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun SettingToggleRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier = Modifier
        .weight(1f)
        .padding(end = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = GoldAccent,
      )
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = TextLightPrimary,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = TextLightSecondary,
        )
      }
    }

    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
      colors = SwitchDefaults.colors(
        checkedThumbColor = EmeraldTableEdge,
        checkedTrackColor = GoldAccent,
        uncheckedThumbColor = TextLightSecondary,
        uncheckedTrackColor = EmeraldTableEdge,
      ),
    )
  }
}
