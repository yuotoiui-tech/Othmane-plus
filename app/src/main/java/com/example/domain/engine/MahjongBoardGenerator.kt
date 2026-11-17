package com.example.domain.engine

import com.example.domain.model.BoardTile
import com.example.domain.model.Difficulty
import com.example.domain.model.TileCatalog
import com.example.domain.model.TilePack
import com.example.domain.model.TileSymbol
import kotlin.random.Random

object MahjongBoardGenerator {

  fun generateCampaignBoard(
    difficulty: Difficulty,
    levelNumber: Int,
    pack: TilePack,
  ): List<BoardTile> {
    val safeLevel = levelNumber.coerceAtLeast(1)
    val seed = (difficulty.ordinal.toLong() + 1L) * 1_000_003L +
      safeLevel.toLong() * 97_991L +
      pack.ordinal.toLong() * 13_337L
    val random = Random(seed)

    val effectivePack = if (pack == TilePack.ALL && safeLevel <= 3) {
      when (safeLevel) {
        1 -> TilePack.NUMBERS_ONLY
        2 -> TilePack.LATIN_ONLY
        else -> TilePack.ARABIC_ONLY
      }
    } else {
      pack
    }

    val targetPairs = when (difficulty) {
      Difficulty.EASY -> (6 + ((safeLevel - 1) % 5)).coerceIn(difficulty.minPairs, difficulty.maxPairs)
      Difficulty.MEDIUM -> (12 + ((safeLevel - 1) % 7)).coerceIn(difficulty.minPairs, difficulty.maxPairs)
      Difficulty.HARD -> (20 + ((safeLevel - 1) % 9)).coerceIn(difficulty.minPairs, difficulty.maxPairs)
    }

    return generateSolvableBoard(
      difficulty = difficulty,
      targetPairs = targetPairs,
      pack = effectivePack,
      variantIndex = safeLevel - 1,
      random = random,
    )
  }

  fun generateEndlessBoard(
    difficulty: Difficulty,
    pack: TilePack,
    boardIndex: Int,
  ): List<BoardTile> {
    val random = Random(System.nanoTime() xor (boardIndex.toLong() * 31_337L))
    val targetPairs = random.nextInt(difficulty.minPairs, difficulty.maxPairs + 1)
    return generateSolvableBoard(
      difficulty = difficulty,
      targetPairs = targetPairs,
      pack = pack,
      variantIndex = random.nextInt(100),
      random = random,
    )
  }

  private fun generateSolvableBoard(
    difficulty: Difficulty,
    targetPairs: Int,
    pack: TilePack,
    variantIndex: Int,
    random: Random,
  ): List<BoardTile> {
    val pool = TileCatalog.symbolsForPack(pack)
    val slots = buildSlotsForDifficulty(difficulty, targetPairs * 2, variantIndex, random)
    val pairCount = slots.size / 2
    val chosenPairs = selectBalancedPairs(pool, pairCount, random)

    val assigned = MahjongRules.assignPairsByReversePeel(slots, chosenPairs, random)
      ?: fallbackAssign(slots, chosenPairs, random)

    val tiles = assigned.mapIndexed { idx, (slot, symbol) ->
      BoardTile(
        instanceId = idx + 1,
        symbol = symbol,
        x = slot.x,
        y = slot.y,
        z = slot.z,
      )
    }
    return MahjongRules.recomputeFreeStates(tiles)
  }

  private fun selectBalancedPairs(
    pool: List<TileSymbol>,
    pairCount: Int,
    random: Random,
  ): List<TileSymbol> {
    val result = mutableListOf<TileSymbol>()
    while (result.size < pairCount) {
      val batch = pool.shuffled(random)
      for (sym in batch) {
        if (result.size >= pairCount) break
        result.add(sym)
      }
    }
    return result
  }

  private fun fallbackAssign(
    slots: List<TileSlot>,
    chosenPairs: List<TileSymbol>,
    random: Random,
  ): List<Pair<TileSlot, TileSymbol>> {
    // Simple 2-layer grid fallback that is always 100% peelable
    val safeSlots = buildGridPyramid(cols = 4, rows = (slots.size / 8).coerceAtLeast(2), layers = 2)
      .take(chosenPairs.size * 2)
    val peel = MahjongRules.assignPairsByReversePeel(safeSlots, chosenPairs, random)
    if (peel != null) return peel
    val flat = chosenPairs.flatMap { listOf(it, it) }.shuffled(random)
    return slots.zip(flat)
  }

  private fun buildSlotsForDifficulty(
    difficulty: Difficulty,
    targetTileCount: Int,
    variantIndex: Int,
    random: Random,
  ): List<TileSlot> {
    val candidates = when (difficulty) {
      Difficulty.EASY -> listOf(
        ::layoutEasyPyramid,
        ::layoutEasyTwinStacks,
        ::layoutEasyButterfly,
        ::layoutEasyBridge,
      )
      Difficulty.MEDIUM -> listOf(
        ::layoutMediumTurtle,
        ::layoutMediumTemple,
        ::layoutMediumHourglass,
        ::layoutMediumCourtyard,
      )
      Difficulty.HARD -> listOf(
        ::layoutHardFortress,
        ::layoutHardPagoda,
        ::layoutHardCrown,
        ::layoutHardClassicMaster,
      )
    }

    val builder = candidates[variantIndex % candidates.size]
    val raw = builder(targetTileCount, random).distinct()
    // Ensure even count
    val evenCount = if (raw.size % 2 == 0) raw.size else raw.size - 1
    // Trim from top-most / outermost slots if needed so layout stays valid & peelable
    val sortedForTrim = raw.sortedWith(compareByDescending<TileSlot> { it.z }.thenBy { it.x }.thenBy { it.y })
    val trimmed = if (raw.size > evenCount) {
      raw.toMutableList().apply { remove(sortedForTrim.first()) }
    } else {
      raw
    }
    return normalizeToOrigin(trimmed)
  }

  private fun normalizeToOrigin(slots: List<TileSlot>): List<TileSlot> {
    if (slots.isEmpty()) return slots
    val minX = slots.minOf { it.x }
    val minY = slots.minOf { it.y }
    return slots.map { TileSlot(it.x - minX, it.y - minY, it.z) }
  }

  // --- EASY LAYOUTS (12 to 20 tiles, max 2 layers, portrait-friendly) ---

  private fun layoutEasyPyramid(targetCount: Int, random: Random): List<TileSlot> {
    val slots = mutableListOf<TileSlot>()
    // Base 4x3 = 12 tiles
    for (r in 0..2) {
      for (c in 0..3) {
        slots.add(TileSlot(c * 2, r * 2, 0))
      }
    }
    // Top layer staggered 2x2 or 3x2
    val topCount = ((targetCount - 12).coerceAtLeast(2) / 2) * 2
    val topCandidates = listOf(
      TileSlot(1, 1, 1), TileSlot(5, 1, 1),
      TileSlot(1, 3, 1), TileSlot(5, 3, 1),
      TileSlot(3, 1, 1), TileSlot(3, 3, 1),
    )
    slots.addAll(topCandidates.take(topCount.coerceAtMost(4)))
    return slots
  }

  private fun layoutEasyTwinStacks(targetCount: Int, random: Random): List<TileSlot> {
    val slots = mutableListOf<TileSlot>()
    for (r in 0..2) {
      for (c in listOf(0, 2, 5, 7)) {
        slots.add(TileSlot(c, r * 2, 0))
      }
    }
    val extras = listOf(
      TileSlot(1, 2, 1), TileSlot(6, 2, 1),
      TileSlot(1, 0, 1), TileSlot(6, 0, 1),
      TileSlot(1, 4, 1), TileSlot(6, 4, 1),
    )
    val needed = (targetCount - slots.size).coerceIn(2, extras.size)
    slots.addAll(extras.take((needed / 2) * 2))
    return slots
  }

  private fun layoutEasyButterfly(targetCount: Int, random: Random): List<TileSlot> {
    val slots = mutableListOf<TileSlot>()
    val base = listOf(
      TileSlot(0, 0, 0), TileSlot(2, 1, 0), TileSlot(4, 1, 0), TileSlot(6, 0, 0),
      TileSlot(0, 2, 0), TileSlot(2, 3, 0), TileSlot(4, 3, 0), TileSlot(6, 2, 0),
      TileSlot(0, 4, 0), TileSlot(6, 4, 0),
    )
    slots.addAll(base)
    val top = listOf(
      TileSlot(2, 2, 1), TileSlot(4, 2, 1),
      TileSlot(0, 1, 1), TileSlot(6, 1, 1),
      TileSlot(0, 3, 1), TileSlot(6, 3, 1),
    )
    val needed = (targetCount - slots.size).coerceIn(2, top.size)
    slots.addAll(top.take((needed / 2) * 2))
    return slots
  }

  private fun layoutEasyBridge(targetCount: Int, random: Random): List<TileSlot> {
    val slots = mutableListOf<TileSlot>()
    for (c in 0..4) {
      slots.add(TileSlot(c * 2, 0, 0))
      slots.add(TileSlot(c * 2, 4, 0))
    }
    val mid = listOf(
      TileSlot(2, 2, 0), TileSlot(4, 2, 0), TileSlot(6, 2, 0),
      TileSlot(2, 1, 1), TileSlot(6, 1, 1),
      TileSlot(2, 3, 1), TileSlot(6, 3, 1),
    )
    val needed = (targetCount - slots.size).coerceIn(2, mid.size)
    slots.addAll(mid.take((needed / 2) * 2))
    return slots
  }

  // --- MEDIUM LAYOUTS (24 to 36 tiles, up to 3 layers) ---

  private fun layoutMediumTurtle(targetCount: Int, random: Random): List<TileSlot> {
    val slots = mutableListOf<TileSlot>()
    // Layer 0: 5x4 = 20 tiles
    for (r in 0..3) {
      for (c in 0..4) {
        slots.add(TileSlot(c * 2, r * 2, 0))
      }
    }
    // Layer 1: 3x2 = 6 tiles centered
    for (r in 1..2) {
      for (c in 1..3) {
        slots.add(TileSlot(c * 2, r * 2, 1))
      }
    }
    // Layer 2: 2 or 4 tiles on top
    val top = listOf(
      TileSlot(3, 3, 2), TileSlot(5, 3, 2),
      TileSlot(3, 1, 2), TileSlot(5, 1, 2),
    )
    val extra = (targetCount - slots.size).coerceIn(2, top.size)
    slots.addAll(top.take((extra / 2) * 2))
    return slots
  }

  private fun layoutMediumTemple(targetCount: Int, random: Random): List<TileSlot> {
    val slots = mutableListOf<TileSlot>()
    // Outer frame 6x4 (hollow middle)
    for (r in 0..4) {
      for (c in 0..4) {
        val isMiddle = (c in 1..3 && r == 2)
        if (!isMiddle) slots.add(TileSlot(c * 2, r * 2, 0))
      }
    }
    // Layer 1 pillars
    val layer1 = listOf(
      TileSlot(0, 0, 1), TileSlot(8, 0, 1),
      TileSlot(0, 8, 1), TileSlot(8, 8, 1),
      TileSlot(2, 2, 1), TileSlot(6, 2, 1),
      TileSlot(2, 6, 1), TileSlot(6, 6, 1),
    )
    slots.addAll(layer1.take(((targetCount - slots.size).coerceIn(2, layer1.size) / 2) * 2))
    if (slots.size + 2 <= targetCount) {
      slots.add(TileSlot(2, 4, 2))
      slots.add(TileSlot(6, 4, 2))
    }
    return slots
  }

  private fun layoutMediumHourglass(targetCount: Int, random: Random): List<TileSlot> {
    val slots = mutableListOf<TileSlot>()
    val rows = listOf(
      0 to listOf(0, 2, 4, 6, 8),
      2 to listOf(1, 3, 5, 7),
      4 to listOf(2, 4, 6),
      6 to listOf(1, 3, 5, 7),
      8 to listOf(0, 2, 4, 6, 8),
    )
    for ((y, xs) in rows) {
      for (x in xs) slots.add(TileSlot(x, y, 0))
    }
    val layer1 = listOf(
      TileSlot(2, 0, 1), TileSlot(6, 0, 1),
      TileSlot(2, 8, 1), TileSlot(6, 8, 1),
      TileSlot(3, 4, 1), TileSlot(5, 4, 1),
      TileSlot(4, 2, 1), TileSlot(4, 6, 1),
    )
    val needed = (targetCount - slots.size).coerceIn(2, layer1.size)
    slots.addAll(layer1.take((needed / 2) * 2))
    if (slots.size + 2 <= targetCount) {
      slots.add(TileSlot(3, 4, 2))
      slots.add(TileSlot(5, 4, 2))
    }
    return slots
  }

  private fun layoutMediumCourtyard(targetCount: Int, random: Random): List<TileSlot> {
    val slots = mutableListOf<TileSlot>()
    for (r in 0..4) {
      for (c in 0..4) {
        if (r == 2 && c == 2) continue
        slots.add(TileSlot(c * 2, r * 2, 0))
      }
    }
    val layer1 = listOf(
      TileSlot(2, 2, 1), TileSlot(6, 2, 1),
      TileSlot(2, 6, 1), TileSlot(6, 6, 1),
      TileSlot(4, 0, 1), TileSlot(4, 8, 1),
    )
    val needed = (targetCount - slots.size).coerceIn(2, layer1.size)
    slots.addAll(layer1.take((needed / 2) * 2))
    if (slots.size + 2 <= targetCount) {
      slots.add(TileSlot(2, 2, 2))
      slots.add(TileSlot(6, 6, 2))
    }
    return slots
  }

  // --- HARD LAYOUTS (40 to 56 tiles, up to 4 layers, matching Mahjong Master feel!) ---

  private fun layoutHardFortress(targetCount: Int, random: Random): List<TileSlot> {
    val slots = mutableListOf<TileSlot>()
    // 6x5 base = 30 tiles
    for (r in 0..4) {
      for (c in 0..5) {
        slots.add(TileSlot(c * 2, r * 2, 0))
      }
    }
    // Layer 1: 4x3 = 12 tiles
    for (r in 1..3) {
      for (c in 1..4) {
        slots.add(TileSlot(c * 2, r * 2, 1))
      }
    }
    // Layer 2: 2x2 = 4 tiles
    for (r in 1..2) {
      for (c in 2..3) {
        slots.add(TileSlot(c * 2, r * 2 + 1, 2))
      }
    }
    // Layer 3: crown 2..6 tiles
    val crown = listOf(
      TileSlot(4, 3, 3), TileSlot(6, 3, 3),
      TileSlot(4, 5, 3), TileSlot(6, 5, 3),
      TileSlot(0, 4, 1), TileSlot(10, 4, 1),
    )
    val needed = (targetCount - slots.size).coerceIn(2, crown.size)
    slots.addAll(crown.take((needed / 2) * 2))
    return slots
  }

  private fun layoutHardPagoda(targetCount: Int, random: Random): List<TileSlot> {
    val slots = mutableListOf<TileSlot>()
    // Classic portrait twin-pillar pagoda like the user's screenshot!
    for (r in 0..5) {
      for (c in 0..5) {
        val gap = (r == 3 && (c == 0 || c == 5))
        if (!gap) slots.add(TileSlot(c * 2, r * 2, 0))
      }
    }
    // Layer 1: left and right towers
    for (r in 1..4) {
      slots.add(TileSlot(2, r * 2, 1))
      slots.add(TileSlot(8, r * 2, 1))
    }
    // Layer 2: upper towers + center bridge
    val layer2 = listOf(
      TileSlot(2, 2, 2), TileSlot(8, 2, 2),
      TileSlot(2, 6, 2), TileSlot(8, 6, 2),
      TileSlot(4, 5, 1), TileSlot(6, 5, 1),
    )
    slots.addAll(layer2)
    // Layer 3
    val layer3 = listOf(
      TileSlot(2, 4, 3), TileSlot(8, 4, 3),
      TileSlot(4, 5, 2), TileSlot(6, 5, 2),
    )
    val needed = (targetCount - slots.size).coerceIn(2, layer3.size)
    slots.addAll(layer3.take((needed / 2) * 2))
    return slots
  }

  private fun layoutHardCrown(targetCount: Int, random: Random): List<TileSlot> {
    val slots = mutableListOf<TileSlot>()
    for (r in 0..5) {
      for (c in 0..4) {
        slots.add(TileSlot(c * 2, r * 2, 0))
      }
    }
    // Layer 1: 3x4 = 12 tiles
    for (r in 1..4) {
      for (c in 1..3) {
        slots.add(TileSlot(c * 2, r * 2, 1))
      }
    }
    // Layer 2: 2x3 = 6 tiles
    for (r in 1..3) {
      slots.add(TileSlot(3, r * 2, 2))
      slots.add(TileSlot(5, r * 2, 2))
    }
    // Layer 3: 2..4 crown tiles
    val crown = listOf(
      TileSlot(3, 4, 3), TileSlot(5, 4, 3),
      TileSlot(3, 2, 3), TileSlot(5, 2, 3),
    )
    val needed = (targetCount - slots.size).coerceIn(2, crown.size)
    slots.addAll(crown.take((needed / 2) * 2))
    return slots
  }

  private fun layoutHardClassicMaster(targetCount: Int, random: Random): List<TileSlot> {
    val slots = mutableListOf<TileSlot>()
    // Mirrors the silhouette in IMG_3146: wide upper shoulders, waist, lower skirt + stacked columns
    val baseRows = listOf(
      0 to listOf(0, 2, 4, 6, 8, 10),
      2 to listOf(2, 4, 6, 8),
      4 to listOf(2, 4, 6, 8),
      6 to listOf(0, 2, 4, 6, 8, 10),
      8 to listOf(0, 2, 4, 6, 8, 10),
      10 to listOf(2, 4, 6, 8),
    )
    for ((y, xs) in baseRows) {
      for (x in xs) slots.add(TileSlot(x, y, 0))
    }
    val layer1 = listOf(
      TileSlot(2, 0, 1), TileSlot(8, 0, 1),
      TileSlot(2, 2, 1), TileSlot(8, 2, 1),
      TileSlot(4, 4, 1), TileSlot(6, 4, 1),
      TileSlot(0, 7, 1), TileSlot(10, 7, 1),
      TileSlot(4, 8, 1), TileSlot(6, 8, 1),
      TileSlot(4, 10, 1), TileSlot(6, 10, 1),
    )
    slots.addAll(layer1)
    val layer2 = listOf(
      TileSlot(2, 1, 2), TileSlot(8, 1, 2),
      TileSlot(4, 7, 2), TileSlot(6, 7, 2),
      TileSlot(4, 9, 2), TileSlot(6, 9, 2),
    )
    val needed = (targetCount - slots.size).coerceIn(2, layer2.size)
    slots.addAll(layer2.take((needed / 2) * 2))
    return slots
  }

  private fun buildGridPyramid(cols: Int, rows: Int, layers: Int): List<TileSlot> {
    val slots = mutableListOf<TileSlot>()
    for (r in 0 until rows) {
      for (c in 0 until cols) {
        slots.add(TileSlot(c * 2, r * 2, 0))
      }
    }
    if (layers >= 2 && cols >= 2 && rows >= 2) {
      for (r in 0 until rows - 1) {
        for (c in 0 until cols - 1) {
          slots.add(TileSlot(c * 2 + 1, r * 2 + 1, 1))
        }
      }
    }
    return slots
  }
}
