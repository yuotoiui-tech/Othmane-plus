import SwiftUI

final class GameViewModel: ObservableObject {
  enum Screen {
    case home
    case levels(Difficulty)
    case game
    case settings
  }

  @Published var screen: Screen = .home
  private var backStack: [Screen] = [.home]

  @Published var selectedDifficulty: Difficulty = .easy
  @Published var selectedPack: TilePack = .all

  @Published var musicEnabled: Bool { didSet { persistAndSync() } }
  @Published var sfxEnabled: Bool { didSet { persistAndSync() } }
  @Published var voiceEnabled: Bool { didSet { persistAndSync() } }
  @Published var speakOnTap: Bool { didSet { persistAndSync() } }
  @Published var highlightFreeTiles: Bool { didSet { persistAndSync() } }
  @Published var arabicVoiceMode: ArabicVoiceMode { didSet { persistAndSync() } }

  @Published var unlockedEasy: Int
  @Published var unlockedMedium: Int
  @Published var unlockedHard: Int
  @Published var totalScore: Int
  @Published var bestEndlessStreak: Int

  @Published var tiles: [BoardTile] = []
  @Published var selectedTileId: Int?
  @Published var hintTileIds: Set<Int> = []
  @Published var lastSpokenSymbol: TileSymbol?
  @Published var score: Int = 0
  @Published var availablePairs: Int = 0
  @Published var isBoardComplete: Bool = false
  @Published var isEndlessMode: Bool = false
  @Published var currentLevel: Int = 1
  @Published var endlessStreak: Int = 0

  let audio = SoundAndVoiceManager()
  private let defaults = UserDefaults.standard

  init() {
    musicEnabled = defaults.object(forKey: "music") as? Bool ?? true
    sfxEnabled = defaults.object(forKey: "sfx") as? Bool ?? true
    voiceEnabled = defaults.object(forKey: "voice") as? Bool ?? true
    speakOnTap = defaults.object(forKey: "speakOnTap") as? Bool ?? true
    highlightFreeTiles = defaults.object(forKey: "highlightFree") as? Bool ?? true
    let modeRaw = defaults.string(forKey: "arMode") ?? ArabicVoiceMode.bilingual.rawValue
    arabicVoiceMode = ArabicVoiceMode(rawValue: modeRaw) ?? .bilingual

    unlockedEasy = max(1, defaults.integer(forKey: "unlockedEasy"))
    unlockedMedium = max(1, defaults.integer(forKey: "unlockedMedium"))
    unlockedHard = max(1, defaults.integer(forKey: "unlockedHard"))
    totalScore = defaults.integer(forKey: "totalScore")
    bestEndlessStreak = defaults.integer(forKey: "bestStreak")

    audio.updateSettings(music: musicEnabled, sfx: sfxEnabled, voice: voiceEnabled, arabicMode: arabicVoiceMode)
  }

  private func persistAndSync() {
    defaults.set(musicEnabled, forKey: "music")
    defaults.set(sfxEnabled, forKey: "sfx")
    defaults.set(voiceEnabled, forKey: "voice")
    defaults.set(speakOnTap, forKey: "speakOnTap")
    defaults.set(highlightFreeTiles, forKey: "highlightFree")
    defaults.set(arabicVoiceMode.rawValue, forKey: "arMode")
    audio.updateSettings(music: musicEnabled, sfx: sfxEnabled, voice: voiceEnabled, arabicMode: arabicVoiceMode)
  }

  func unlockedLevel(for diff: Difficulty) -> Int {
    switch diff {
    case .easy: return unlockedEasy
    case .medium: return unlockedMedium
    case .hard: return unlockedHard
    }
  }

  func push(_ next: Screen) {
    backStack.append(next)
    screen = next
  }

  func goBack() {
    if backStack.count > 1 { backStack.removeLast() }
    screen = backStack.last ?? .home
  }

  func goHome() {
    backStack = [.home]
    screen = .home
  }

  func startCampaign(difficulty: Difficulty, level: Int, pack: TilePack) {
    isEndlessMode = false
    selectedDifficulty = difficulty
    selectedPack = pack
    currentLevel = level
    score = 0
    loadBoard(MahjongEngine.generateCampaignBoard(difficulty: difficulty, level: level, pack: pack))
    push(.game)
  }

  func startEndless(difficulty: Difficulty, pack: TilePack, index: Int = 1, streak: Int = 0, carryScore: Int = 0) {
    isEndlessMode = true
    selectedDifficulty = difficulty
    selectedPack = pack
    currentLevel = index
    endlessStreak = streak
    score = carryScore
    loadBoard(MahjongEngine.generateEndlessBoard(difficulty: difficulty, pack: pack))
    push(.game)
  }

  private func loadBoard(_ board: [BoardTile]) {
    var b = board
    if !b.isEmpty && MahjongEngine.findAvailablePairs(b).isEmpty {
      b = MahjongEngine.reshuffleSolvable(b)
    }
    tiles = b
    selectedTileId = nil
    hintTileIds = []
    lastSpokenSymbol = nil
    availablePairs = MahjongEngine.findAvailablePairs(b).count
    isBoardComplete = false
  }

  func tapTile(_ tile: BoardTile) {
    guard !isBoardComplete else { return }
    if !tile.isFree {
      audio.playBlocked()
      return
    }

    if selectedTileId == nil {
      audio.playTap()
      if speakOnTap { audio.speakSymbol(tile.symbol) }
      selectedTileId = tile.id
      hintTileIds = []
      lastSpokenSymbol = tile.symbol
      return
    }

    if selectedTileId == tile.id {
      audio.playTap()
      if speakOnTap { audio.speakSymbol(tile.symbol) }
      selectedTileId = nil
      return
    }

    if let first = tiles.first(where: { $0.id == selectedTileId }), first.symbol.id == tile.symbol.id {
      var remaining = MahjongEngine.recomputeFree(tiles.filter { $0.id != first.id && $0.id != tile.id })
      if !remaining.isEmpty && MahjongEngine.findAvailablePairs(remaining).isEmpty {
        remaining = MahjongEngine.reshuffleSolvable(remaining)
      }
      tiles = remaining
      selectedTileId = nil
      hintTileIds = []
      lastSpokenSymbol = tile.symbol
      score += 100
      availablePairs = MahjongEngine.findAvailablePairs(remaining).count

      audio.playMatch()
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.14) { [weak self] in
        self?.audio.speakSymbol(tile.symbol)
      }

      if remaining.isEmpty {
        isBoardComplete = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { [weak self] in
          self?.audio.playWin()
        }
        totalScore += score
        defaults.set(totalScore, forKey: "totalScore")
        if isEndlessMode {
          endlessStreak += 1
          if endlessStreak > bestEndlessStreak {
            bestEndlessStreak = endlessStreak
            defaults.set(bestEndlessStreak, forKey: "bestStreak")
          }
        } else {
          if currentLevel >= unlockedLevel(for: selectedDifficulty) {
            let next = currentLevel + 1
            switch selectedDifficulty {
            case .easy: unlockedEasy = next; defaults.set(next, forKey: "unlockedEasy")
            case .medium: unlockedMedium = next; defaults.set(next, forKey: "unlockedMedium")
            case .hard: unlockedHard = next; defaults.set(next, forKey: "unlockedHard")
            }
          }
        }
      }
    } else {
      audio.playTap()
      if speakOnTap { audio.speakSymbol(tile.symbol) }
      selectedTileId = tile.id
      hintTileIds = []
      lastSpokenSymbol = tile.symbol
    }
  }

  func shuffleBoard() {
    guard tiles.count >= 2 && !isBoardComplete else { return }
    audio.playTap()
    tiles = MahjongEngine.reshuffleSolvable(tiles)
    selectedTileId = nil
    hintTileIds = []
    availablePairs = MahjongEngine.findAvailablePairs(tiles).count
  }

  func showHint() {
    guard !isBoardComplete else { return }
    let pairs = MahjongEngine.findAvailablePairs(tiles)
    if let first = pairs.first {
      audio.playTap()
      hintTileIds = [first.0.id, first.1.id]
      selectedTileId = nil
    } else {
      shuffleBoard()
    }
  }

  func nextBoard() {
    if isEndlessMode {
      startEndless(difficulty: selectedDifficulty, pack: selectedPack, index: currentLevel + 1, streak: endlessStreak, carryScore: score)
    } else {
      startCampaign(difficulty: selectedDifficulty, level: currentLevel + 1, pack: selectedPack)
    }
  }

  func testFrench() {
    audio.playMatch()
    DispatchQueue.main.asyncAfter(deadline: .now() + 0.16) { [weak self] in
      self?.audio.speakSymbol(TileCatalog.numbers[1])
    }
  }

  func testArabic() {
    audio.playMatch()
    DispatchQueue.main.asyncAfter(deadline: .now() + 0.16) { [weak self] in
      self?.audio.speakSymbol(TileCatalog.arabicLetters[0])
    }
  }
}
