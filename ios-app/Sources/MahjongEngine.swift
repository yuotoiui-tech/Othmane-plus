import Foundation

struct TileSlot: Hashable {
  let x: Int
  let y: Int
  let z: Int
}

struct SeededRNG: RandomNumberGenerator {
  private var state: UInt64

  init(seed: UInt64) {
    self.state = seed == 0 ? 0x4D595DF4D0F33173 : seed
  }

  mutating func next() -> UInt64 {
    state = state &* 6364136223846793005 &+ 1442695040888963407
    return state
  }
}

enum MahjongEngine {

  static func isSlotFree(_ slot: TileSlot, in active: [TileSlot]) -> Bool {
    var blockedLeft = false
    var blockedRight = false
    for other in active {
      if other == slot { continue }
      if other.z > slot.z && abs(other.x - slot.x) < 2 && abs(other.y - slot.y) < 2 {
        return false
      }
      if other.z == slot.z && abs(other.y - slot.y) < 2 {
        if other.x == slot.x - 2 { blockedLeft = true }
        if other.x == slot.x + 2 { blockedRight = true }
      }
    }
    return !blockedLeft || !blockedRight
  }

  static func isTileFree(_ tile: BoardTile, in all: [BoardTile]) -> Bool {
    var blockedLeft = false
    var blockedRight = false
    for other in all where other.id != tile.id {
      if other.z > tile.z && abs(other.x - tile.x) < 2 && abs(other.y - tile.y) < 2 {
        return false
      }
      if other.z == tile.z && abs(other.y - tile.y) < 2 {
        if other.x == tile.x - 2 { blockedLeft = true }
        if other.x == tile.x + 2 { blockedRight = true }
      }
    }
    return !blockedLeft || !blockedRight
  }

  static func recomputeFree(_ tiles: [BoardTile]) -> [BoardTile] {
    tiles.map { t in
      var copy = t
      copy.isFree = isTileFree(t, in: tiles)
      return copy
    }
  }

  static func findAvailablePairs(_ tiles: [BoardTile]) -> [(BoardTile, BoardTile)] {
    let free = tiles.filter { $0.isFree }
    var res: [(BoardTile, BoardTile)] = []
    for i in 0..<free.count {
      for j in (i + 1)..<free.count {
        if free[i].symbol.id == free[j].symbol.id {
          res.append((free[i], free[j]))
        }
      }
    }
    return res
  }

  static func reshuffleSolvable(_ tiles: [BoardTile]) -> [BoardTile] {
    guard tiles.count >= 2 else { return recomputeFree(tiles) }
    var rng = SeededRNG(seed: UInt64(Date().timeIntervalSince1970 * 1000))
    let slots = tiles.map { TileSlot(x: $0.x, y: $0.y, z: $0.z) }
    var pairSymbols: [TileSymbol] = []
    let grouped = Dictionary(grouping: tiles, by: { $0.symbol.id })
    for (_, grp) in grouped {
      for _ in 0..<(grp.count / 2) {
        if let first = grp.first { pairSymbols.append(first.symbol) }
      }
    }
    if let assigned = assignReversePeel(slots: slots, pairSymbols: pairSymbols, rng: &rng) {
      let mapped = assigned.enumerated().map { idx, pair in
        BoardTile(id: tiles[idx].id, symbol: pair.1, x: pair.0.x, y: pair.0.y, z: pair.0.z)
      }
      return recomputeFree(mapped)
    }
    let syms = tiles.map { $0.symbol }.shuffled(using: &rng)
    return recomputeFree(tiles.enumerated().map { idx, t in
      BoardTile(id: t.id, symbol: syms[idx], x: t.x, y: t.y, z: t.z)
    })
  }

  static func generateCampaignBoard(difficulty: Difficulty, level: Int, pack: TilePack) -> [BoardTile] {
    let safeLevel = max(1, level)
    let diffIdx: UInt64 = difficulty == .easy ? 1 : (difficulty == .medium ? 2 : 3)
    let seed = diffIdx &* 1_000_003 &+ UInt64(safeLevel) &* 97_991
    var rng = SeededRNG(seed: seed)
    let effectivePack: TilePack
    if pack == .all && safeLevel <= 3 {
      effectivePack = safeLevel == 1 ? .numbersOnly : (safeLevel == 2 ? .latinOnly : .arabicOnly)
    } else {
      effectivePack = pack
    }
    return buildBoard(difficulty: difficulty, pack: effectivePack, variant: safeLevel - 1, rng: &rng)
  }

  static func generateEndlessBoard(difficulty: Difficulty, pack: TilePack) -> [BoardTile] {
    var rng = SeededRNG(seed: UInt64.random(in: 1...UInt64.max))
    return buildBoard(difficulty: difficulty, pack: pack, variant: Int.random(in: 0..<4, using: &rng), rng: &rng)
  }

  private static func buildBoard<R: RandomNumberGenerator>(
    difficulty: Difficulty,
    pack: TilePack,
    variant: Int,
    rng: inout R
  ) -> [BoardTile] {
    let slots = slotsFor(difficulty: difficulty, variant: variant)
    let pairCount = slots.count / 2
    let pool = TileCatalog.symbols(for: pack)
    var pairs: [TileSymbol] = []
    while pairs.count < pairCount {
      for sym in pool.shuffled(using: &rng) {
        if pairs.count >= pairCount { break }
        pairs.append(sym)
      }
    }
    let assigned = assignReversePeel(slots: slots, pairSymbols: pairs, rng: &rng)
      ?? zip(slots, pairs.flatMap { [$0, $0] }.shuffled(using: &rng)).map { ($0.0, $0.1) }

    let tiles = assigned.enumerated().map { idx, item in
      BoardTile(id: idx + 1, symbol: item.1, x: item.0.x, y: item.0.y, z: item.0.z)
    }
    return recomputeFree(tiles)
  }

  private static func assignReversePeel<R: RandomNumberGenerator>(
    slots: [TileSlot],
    pairSymbols: [TileSymbol],
    rng: inout R
  ) -> [(TileSlot, TileSymbol)]? {
    guard slots.count == pairSymbols.count * 2 else { return nil }
    for _ in 0..<12 {
      var remaining = slots
      var out: [(TileSlot, TileSymbol)] = []
      var failed = false
      for sym in pairSymbols.shuffled(using: &rng) {
        let free = remaining.filter { isSlotFree($0, in: remaining) }
        guard free.count >= 2 else { failed = true; break }
        let a = free.randomElement(using: &rng)!
        let rest = free.filter { $0 != a }
        let b = rest.randomElement(using: &rng)!
        out.append((a, sym))
        out.append((b, sym))
        remaining.removeAll { $0 == a || $0 == b }
      }
      if !failed && remaining.isEmpty { return out }
    }
    return nil
  }

  private static func slotsFor(difficulty: Difficulty, variant: Int) -> [TileSlot] {
    var slots: [TileSlot] = []
    switch difficulty {
    case .easy:
      for r in 0..<3 { for c in 0..<4 { slots.append(TileSlot(x: c * 2, y: r * 2, z: 0)) } }
      let extras = [TileSlot(x: 1, y: 1, z: 1), TileSlot(x: 5, y: 1, z: 1),
                    TileSlot(x: 1, y: 3, z: 1), TileSlot(x: 5, y: 3, z: 1)]
      slots.append(contentsOf: extras.prefix((variant % 2 == 0) ? 4 : 2))
    case .medium:
      for r in 0..<4 { for c in 0..<5 { slots.append(TileSlot(x: c * 2, y: r * 2, z: 0)) } }
      for r in 1..<3 { for c in 1..<4 { slots.append(TileSlot(x: c * 2, y: r * 2, z: 1)) } }
      slots.append(contentsOf: [TileSlot(x: 3, y: 3, z: 2), TileSlot(x: 5, y: 3, z: 2)])
    case .hard:
      for r in 0..<5 { for c in 0..<6 { slots.append(TileSlot(x: c * 2, y: r * 2, z: 0)) } }
      for r in 1..<4 { for c in 1..<5 { slots.append(TileSlot(x: c * 2, y: r * 2, z: 1)) } }
      slots.append(contentsOf: [
        TileSlot(x: 4, y: 3, z: 2), TileSlot(x: 6, y: 3, z: 2),
        TileSlot(x: 4, y: 5, z: 2), TileSlot(x: 6, y: 5, z: 2)
      ])
    }
    return Array(Set(slots)).count % 2 == 0 ? slots : Array(slots.dropLast())
  }
}
