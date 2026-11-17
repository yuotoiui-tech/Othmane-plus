import AVFoundation
import Foundation

final class SoundAndVoiceManager: NSObject, AVSpeechSynthesizerDelegate {

  private let synthesizer = AVSpeechSynthesizer()
  private var musicPlayer: AVAudioPlayer?
  private var sfxPlayers: [String: AVAudioPlayer] = [:]

  private var musicEnabled = true
  private var sfxEnabled = true
  private var voiceEnabled = true
  private var arabicVoiceMode: ArabicVoiceMode = .bilingual
  private var pendingFrenchFollowUp: String?

  override init() {
    super.init()
    synthesizer.delegate = self
    configureAudioSession()
    prepareAudioBuffers()
  }

  private func configureAudioSession() {
    let session = AVAudioSession.sharedInstance()
    try? session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
    try? session.setActive(true)
  }

  private func prepareAudioBuffers() {
    let melody: [Double] = [
      261.63, 329.63, 392.00, 523.25, 440.00, 392.00, 329.63, 293.66,
      261.63, 293.66, 392.00, 440.00, 523.25, 440.00, 392.00, 329.63
    ]
    let musicData = makeLoopWav(melody: melody, beatDur: 0.8)
    if let mp = try? AVAudioPlayer(data: musicData) {
      mp.numberOfLoops = -1
      mp.volume = 0.35
      mp.prepareToPlay()
      musicPlayer = mp
    }

    sfxPlayers["tap"] = try? AVAudioPlayer(data: makeToneWav(notes: [(620, 0.0, 0.075, 0.65, 50)]))
    sfxPlayers["blocked"] = try? AVAudioPlayer(data: makeToneWav(notes: [(195, 0.0, 0.09, 0.55, 40)]))
    sfxPlayers["match"] = try? AVAudioPlayer(data: makeToneWav(notes: [(523.25, 0.0, 0.16, 0.55, 11), (659.25, 0.095, 0.28, 0.58, 11)]))
    sfxPlayers["win"] = try? AVAudioPlayer(data: makeToneWav(notes: [
      (523.25, 0.0, 0.3, 0.45, 7), (659.25, 0.1, 0.3, 0.45, 7),
      (783.99, 0.2, 0.3, 0.45, 7), (1046.5, 0.3, 0.42, 0.5, 6)
    ]))
    sfxPlayers.values.forEach { $0.prepareToPlay() }
  }

  func updateSettings(music: Bool, sfx: Bool, voice: Bool, arabicMode: ArabicVoiceMode) {
    musicEnabled = music
    sfxEnabled = sfx
    voiceEnabled = voice
    arabicVoiceMode = arabicMode
    setMusicActive(music)
  }

  func setMusicActive(_ active: Bool) {
    guard let mp = musicPlayer else { return }
    if active && musicEnabled {
      if !mp.isPlaying { mp.play() }
    } else {
      if mp.isPlaying { mp.pause() }
    }
  }

  func playTap() { playSfx("tap") }
  func playBlocked() { playSfx("blocked") }
  func playMatch() { playSfx("match") }
  func playWin() { playSfx("win") }

  private func playSfx(_ key: String) {
    guard sfxEnabled, let p = sfxPlayers[key] else { return }
    p.currentTime = 0
    p.play()
  }

  func speakSymbol(_ symbol: TileSymbol) {
    guard voiceEnabled else { return }
    pendingFrenchFollowUp = nil
    synthesizer.stopSpeaking(at: .immediate)

    if symbol.category != .arabicLetter || arabicVoiceMode == .frenchOnly {
      speakFrench(symbol.frenchSpeech)
      return
    }

    if arabicVoiceMode == .bilingual {
      pendingFrenchFollowUp = symbol.frenchSpeech
    }
    speakArabic(symbol.arabicSpeech)
  }

  private func speakFrench(_ text: String) {
    let utt = AVSpeechUtterance(string: text)
    utt.voice = AVSpeechSynthesisVoice(language: "fr-FR")
    utt.rate = 0.46
    utt.pitchMultiplier = 1.04
    synthesizer.speak(utt)
  }

  private func speakArabic(_ text: String) {
    let utt = AVSpeechUtterance(string: text)
    utt.voice = AVSpeechSynthesisVoice(language: "ar-SA") ?? AVSpeechSynthesisVoice(language: "ar")
    utt.rate = 0.42
    utt.pitchMultiplier = 1.02
    synthesizer.speak(utt)
  }

  func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
    if let nextFr = pendingFrenchFollowUp {
      pendingFrenchFollowUp = nil
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.18) { [weak self] in
        self?.speakFrench(nextFr)
      }
    }
  }

  // Synthesize 44.1kHz mono PCM WAV in memory
  private func makeToneWav(notes: [(Double, Double, Double, Double, Double)]) -> Data {
    let sr = 44100.0
    let maxDur = notes.map { $0.1 + $0.2 }.max() ?? 0.2
    let count = Int(sr * maxDur)
    var buf = [Double](repeating: 0, count: count)
    for (freq, start, len, gain, decay) in notes {
      let s0 = Int(start * sr)
      let cnt = Int(len * sr)
      for j in 0..<cnt where s0 + j < count {
        let t = Double(j) / sr
        let att = min(1.0, t / 0.004)
        let env = att * exp(-decay * t)
        let wave = 0.75 * sin(2 * .pi * freq * t) + 0.25 * sin(4 * .pi * freq * t)
        buf[s0 + j] += wave * env * gain
      }
    }
    return encodeWav(samples: buf, sampleRate: Int(sr))
  }

  private func makeLoopWav(melody: [Double], beatDur: Double) -> Data {
    let sr = 44100.0
    let count = Int(sr * Double(melody.count) * beatDur)
    var buf = [Double](repeating: 0, count: count)
    for (idx, freq) in melody.enumerated() {
      let s0 = Int(Double(idx) * beatDur * sr)
      let cnt = Int(beatDur * 1.4 * sr)
      for j in 0..<cnt {
        let pos = (s0 + j) % count
        let t = Double(j) / sr
        let att = min(1.0, t / 0.015)
        let env = att * exp(-3.2 * t)
        let wave = 0.78 * sin(2 * .pi * freq * t) + 0.22 * sin(4 * .pi * freq * t)
        buf[pos] += wave * env * 0.30
      }
    }
    return encodeWav(samples: buf, sampleRate: Int(sr))
  }

  private func encodeWav(samples: [Double], sampleRate: Int) -> Data {
    var data = Data()
    let byteCount = samples.count * 2
    let totalSize = 36 + byteCount
    data.append(contentsOf: "RIFF".utf8)
    data.append(uint32LE(UInt32(totalSize)))
    data.append(contentsOf: "WAVEfmt ".utf8)
    data.append(uint32LE(16))
    data.append(uint16LE(1))
    data.append(uint16LE(1))
    data.append(uint32LE(UInt32(sampleRate)))
    data.append(uint32LE(UInt32(sampleRate * 2)))
    data.append(uint16LE(2))
    data.append(uint16LE(16))
    data.append(contentsOf: "data".utf8)
    data.append(uint32LE(UInt32(byteCount)))
    for s in samples {
      let clamped = max(-1.0, min(1.0, s))
      let i16 = Int16(clamped * 32767.0)
      var le = i16.littleEndian
      data.append(Data(bytes: &le, count: 2))
    }
    return data
  }

  private func uint32LE(_ v: UInt32) -> Data {
    var le = v.littleEndian
    return Data(bytes: &le, count: 4)
  }

  private func uint16LE(_ v: UInt16) -> Data {
    var le = v.littleEndian
    return Data(bytes: &le, count: 2)
  }
}
