package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.R
import com.example.domain.model.ArabicVoiceMode
import com.example.domain.model.TileCategory
import com.example.domain.model.TileSymbol
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TtsDiagnostics(
  val initialized: Boolean = false,
  val ttsEngineReady: Boolean = false,
  val frenchVoiceReady: Boolean = false,
  val arabicVoiceReady: Boolean = false,
)

class SoundAndVoiceManager(context: Context) {

  private val appContext = context.applicationContext
  private val mainHandler = Handler(Looper.getMainLooper())

  // Dedicated TTS engines for French and Arabic prevent cross-language voice switching glitches
  private val frenchTtsRef = AtomicReference<TextToSpeech?>(null)
  private val arabicTtsRef = AtomicReference<TextToSpeech?>(null)
  private val speechToken = AtomicLong(0L)

  private val _ttsDiagnostics = MutableStateFlow(TtsDiagnostics())
  val ttsDiagnostics: StateFlow<TtsDiagnostics> = _ttsDiagnostics.asStateFlow()

  private var frenchVoice: Voice? = null
  private var arabicVoice: Voice? = null
  private var frenchLocale: Locale = Locale.FRANCE
  private var arabicLocale: Locale = Locale.forLanguageTag("ar-SA")

  @Volatile
  private var frenchTtsReady = false

  @Volatile
  private var arabicTtsReady = false

  @Volatile
  private var frenchAvailable = false

  @Volatile
  private var arabicAvailable = false

  @Volatile
  private var pendingFrenchFollowUp: Pair<Long, String>? = null

  private var mediaPlayer: MediaPlayer? = null
  private var soundPool: SoundPool? = null
  private var tapSoundId: Int = 0
  private var blockedSoundId: Int = 0
  private var matchSoundId: Int = 0
  private var winSoundId: Int = 0
  private val loadedSoundIds = mutableSetOf<Int>()

  @Volatile
  private var musicShouldPlay = false

  @Volatile
  private var musicEnabled = true

  @Volatile
  private var sfxEnabled = true

  @Volatile
  private var voiceEnabled = true

  @Volatile
  private var arabicVoiceMode = ArabicVoiceMode.BILINGUAL_AR_AND_FR

  init {
    initSoundPool()
    mainHandler.post { initTtsEngines() }
  }

  private fun initSoundPool() {
    val attrs = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_MEDIA)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()

    val pool = SoundPool.Builder()
      .setMaxStreams(6)
      .setAudioAttributes(attrs)
      .build()

    pool.setOnLoadCompleteListener { _, sampleId, status ->
      if (status == 0) {
        synchronized(loadedSoundIds) {
          loadedSoundIds.add(sampleId)
        }
      }
    }

    tapSoundId = pool.load(appContext, R.raw.sfx_tap, 1)
    blockedSoundId = pool.load(appContext, R.raw.sfx_blocked, 1)
    matchSoundId = pool.load(appContext, R.raw.sfx_match, 1)
    winSoundId = pool.load(appContext, R.raw.sfx_win, 1)
    soundPool = pool
  }

  private fun initTtsEngines() {
    val speechAttrs = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_MEDIA)
      .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
      .build()

    val createdFrench = TextToSpeech(appContext) { status ->
      mainHandler.post {
        val engine = frenchTtsRef.get()
        if (status == TextToSpeech.SUCCESS && engine != null) {
          engine.setAudioAttributes(speechAttrs)
          engine.setSpeechRate(0.88f)
          engine.setPitch(1.04f)

          val frCandidates = listOf(Locale.FRANCE, Locale.FRENCH, Locale.CANADA_FRENCH)
          for (loc in frCandidates) {
            if (engine.isLanguageAvailable(loc) >= TextToSpeech.LANG_AVAILABLE) {
              frenchLocale = loc
              frenchAvailable = true
              break
            }
          }
          val voices = runCatching { engine.voices }.getOrNull().orEmpty()
          if (voices.isNotEmpty()) {
            frenchVoice = voices.firstOrNull {
              it.locale.language == "fr" && !it.isNetworkConnectionRequired
            } ?: voices.firstOrNull { it.locale.language == "fr" }
            if (frenchVoice != null) {
              frenchAvailable = true
              frenchLocale = frenchVoice!!.locale
            }
          }
          frenchVoice?.let { runCatching { engine.voice = it } }
            ?: runCatching { engine.language = frenchLocale }
          frenchTtsReady = true
        }
        publishDiagnostics()
      }
    }
    frenchTtsRef.set(createdFrench)

    val createdArabic = TextToSpeech(appContext) { status ->
      mainHandler.post {
        val engine = arabicTtsRef.get()
        if (status == TextToSpeech.SUCCESS && engine != null) {
          engine.setAudioAttributes(speechAttrs)
          // Slightly slower articulation makes Arabic letter names super clear for kids
          engine.setSpeechRate(0.80f)
          engine.setPitch(1.02f)

          // Prefer Standard / Gulf / Moroccan Arabic over Egyptian (where ج is pronounced hard G)
          val arCandidates = listOf(
            Locale.forLanguageTag("ar-SA"),
            Locale.forLanguageTag("ar-XA"),
            Locale.forLanguageTag("ar"),
            Locale.forLanguageTag("ar-MA"),
            Locale.forLanguageTag("ar-AE"),
          )
          for (loc in arCandidates) {
            if (engine.isLanguageAvailable(loc) >= TextToSpeech.LANG_AVAILABLE) {
              arabicLocale = loc
              arabicAvailable = true
              break
            }
          }

          val voices = runCatching { engine.voices }.getOrNull().orEmpty()
          if (voices.isNotEmpty()) {
            val arVoices = voices.filter { it.locale.language == "ar" }
            arabicVoice = selectBestArabicVoice(arVoices)
            if (arabicVoice != null) {
              arabicAvailable = true
              arabicLocale = arabicVoice!!.locale
            }
          }

          if (arabicVoice != null) {
            runCatching { engine.voice = arabicVoice }
          } else if (arabicAvailable) {
            runCatching { engine.language = arabicLocale }
          }

          engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
              if (utteranceId == UTTERANCE_AR_THEN_FR) {
                val pending = pendingFrenchFollowUp
                pendingFrenchFollowUp = null
                if (pending != null && pending.first == speechToken.get()) {
                  mainHandler.postDelayed({
                    if (pending.first == speechToken.get()) {
                      speakFrenchInternal(pending.second, TextToSpeech.QUEUE_FLUSH)
                    }
                  }, 220L)
                }
              }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
              pendingFrenchFollowUp = null
            }
          })

          arabicTtsReady = true
        }
        publishDiagnostics()
      }
    }
    arabicTtsRef.set(createdArabic)
  }

  private fun selectBestArabicVoice(arVoices: List<Voice>): Voice? {
    if (arVoices.isEmpty()) return null
    val offline = arVoices.filter { !it.isNetworkConnectionRequired }
    val pool = offline.ifEmpty { arVoices }

    val preferredCountries = listOf("SA", "XA", "", "MA", "AE", "JO")
    for (country in preferredCountries) {
      val match = pool.firstOrNull { it.locale.country.equals(country, ignoreCase = true) }
      if (match != null) return match
    }
    // Avoid Egyptian if any other Arabic voice exists
    return pool.firstOrNull { !it.locale.country.equals("EG", ignoreCase = true) }
      ?: pool.firstOrNull()
  }

  private fun publishDiagnostics() {
    _ttsDiagnostics.value = TtsDiagnostics(
      initialized = true,
      ttsEngineReady = frenchTtsReady || arabicTtsReady,
      frenchVoiceReady = frenchAvailable,
      arabicVoiceReady = arabicAvailable,
    )
    Log.d(
      TAG,
      "TTS status: frReady=$frenchTtsReady ($frenchAvailable, $frenchLocale), " +
        "arReady=$arabicTtsReady ($arabicAvailable, $arabicLocale)",
    )
  }

  fun reinitializeTts() {
    mainHandler.post {
      frenchTtsReady = false
      arabicTtsReady = false
      runCatching {
        frenchTtsRef.getAndSet(null)?.apply {
          stop()
          shutdown()
        }
        arabicTtsRef.getAndSet(null)?.apply {
          stop()
          shutdown()
        }
      }
      initTtsEngines()
    }
  }

  fun updateSettings(
    musicEnabled: Boolean,
    sfxEnabled: Boolean,
    voiceEnabled: Boolean,
    arabicVoiceMode: ArabicVoiceMode,
  ) {
    this.musicEnabled = musicEnabled
    this.sfxEnabled = sfxEnabled
    this.voiceEnabled = voiceEnabled
    this.arabicVoiceMode = arabicVoiceMode
    syncMusicPlayback()
  }

  fun setMusicActive(active: Boolean, musicEnabled: Boolean) {
    this.musicShouldPlay = active
    this.musicEnabled = musicEnabled
    syncMusicPlayback()
  }

  private fun syncMusicPlayback() {
    mainHandler.post {
      if (musicShouldPlay && musicEnabled) {
        startOrResumeMusic()
      } else {
        pauseMusic()
      }
    }
  }

  private fun startOrResumeMusic() {
    val existing = mediaPlayer
    if (existing != null) {
      if (!existing.isPlaying) {
        runCatching { existing.start() }
      }
      return
    }

    val mp = runCatching {
      MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build(),
        )
        val afd = appContext.resources.openRawResourceFd(R.raw.calm_music)
        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()
        isLooping = true
        setVolume(0.42f, 0.42f)
        prepare()
        start()
      }
    }.getOrElse { err ->
      Log.e(TAG, "Failed to start calm music MediaPlayer", err)
      null
    }
    mediaPlayer = mp
  }

  private fun pauseMusic() {
    val mp = mediaPlayer ?: return
    runCatching {
      if (mp.isPlaying) mp.pause()
    }
  }

  fun speakSymbol(symbol: TileSymbol) {
    if (!voiceEnabled) return
    mainHandler.post {
      val token = speechToken.incrementAndGet()
      pendingFrenchFollowUp = null
      runCatching { frenchTtsRef.get()?.stop() }
      runCatching { arabicTtsRef.get()?.stop() }

      when (symbol.category) {
        TileCategory.NUMBER, TileCategory.LATIN_LETTER -> {
          if (frenchTtsReady) {
            speakFrenchInternal(symbol.frenchSpeech, TextToSpeech.QUEUE_FLUSH)
          }
        }

        TileCategory.ARABIC_LETTER -> {
          when (arabicVoiceMode) {
            ArabicVoiceMode.FRENCH_ONLY -> {
              if (frenchTtsReady) {
                speakFrenchInternal(symbol.frenchSpeech, TextToSpeech.QUEUE_FLUSH)
              }
            }

            ArabicVoiceMode.ARABIC_ONLY -> {
              if (arabicTtsReady && arabicAvailable) {
                speakArabicInternal(symbol.arabicSpeech, UTTERANCE_SINGLE)
              } else if (frenchTtsReady) {
                speakFrenchInternal(symbol.frenchSpeech, TextToSpeech.QUEUE_FLUSH)
              }
            }

            ArabicVoiceMode.BILINGUAL_AR_AND_FR -> {
              if (arabicTtsReady && arabicAvailable) {
                pendingFrenchFollowUp = token to symbol.frenchSpeech
                speakArabicInternal(symbol.arabicSpeech, UTTERANCE_AR_THEN_FR)
              } else if (frenchTtsReady) {
                speakFrenchInternal(symbol.frenchSpeech, TextToSpeech.QUEUE_FLUSH)
              }
            }
          }
        }
      }
    }
  }

  private fun speakFrenchInternal(text: String, queueMode: Int) {
    val engine = frenchTtsRef.get() ?: return
    val voice = frenchVoice
    if (voice != null) {
      runCatching { engine.voice = voice }
    } else if (frenchAvailable) {
      runCatching { engine.language = frenchLocale }
    }
    val params = Bundle().apply {
      putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
      putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
    }
    engine.speak(text, queueMode, params, UTTERANCE_SINGLE)
  }

  private fun speakArabicInternal(text: String, utteranceId: String) {
    val engine = arabicTtsRef.get() ?: return
    val voice = arabicVoice
    if (voice != null) {
      runCatching { engine.voice = voice }
    } else {
      runCatching { engine.language = arabicLocale }
    }
    val params = Bundle().apply {
      putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
      putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
    }
    engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
  }

  fun playTileTap() {
    playPoolSound(tapSoundId, volume = 0.85f)
  }

  fun playTileBlocked() {
    playPoolSound(blockedSoundId, volume = 0.80f)
  }

  fun playMatchSuccess() {
    playPoolSound(matchSoundId, volume = 0.95f)
  }

  fun playVictoryFanfare() {
    playPoolSound(winSoundId, volume = 0.95f)
  }

  private fun playPoolSound(soundId: Int, volume: Float) {
    if (!sfxEnabled || soundId == 0) return
    val pool = soundPool ?: return
    val isLoaded = synchronized(loadedSoundIds) { soundId in loadedSoundIds }
    if (isLoaded) {
      pool.play(soundId, volume, volume, 1, 0, 1.0f)
    }
  }

  fun release() {
    mainHandler.removeCallbacksAndMessages(null)
    runCatching {
      mediaPlayer?.stop()
      mediaPlayer?.release()
    }
    mediaPlayer = null
    runCatching {
      soundPool?.release()
    }
    soundPool = null
    runCatching {
      frenchTtsRef.getAndSet(null)?.apply {
        stop()
        shutdown()
      }
      arabicTtsRef.getAndSet(null)?.apply {
        stop()
        shutdown()
      }
    }
  }

  private companion object {
    const val TAG = "SoundAndVoiceManager"
    const val UTTERANCE_SINGLE = "mahjong_abc_single"
    const val UTTERANCE_AR_THEN_FR = "mahjong_abc_ar_then_fr"
  }
}
