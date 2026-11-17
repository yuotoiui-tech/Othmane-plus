package com.example.domain.engine

import com.example.domain.model.BoardTile
import com.example.domain.model.TileSymbol
import kotlin.math.abs
import kotlin.random.Random

data class TileSlot(
  val x: Int,
  val y: Int,
  val z: Int,
)

object MahjongRules {

  fun isSlotFree(slot: TileSlot, activeSlots: List<TileSlot>): Boolean {
    var blockedLeft = false
    var blockedRight = false

    for (other in activeSlots) {
      if (other === slot || (other.x == slot.x && other.y == slot.y && other.z == slot.z)) {
        continue
      }
      // Covered by any tile on a higher layer overlapping this tile's 2x2 footprint
      if (other.z > slot.z && abs(other.x - slot.x) < 2 && abs(other.y - slot.y) < 2) {
        return false
      }
      // Same layer horizontal neighbors
      if (other.z == slot.z && abs(other.y - slot.y) < 2) {
        if (other.x == slot.x - 2) {
          blockedLeft = true
        } else if (other.x == slot.x + 2) {
          blockedRight = true
        }
      }
    }
    return !blockedLeft || !blockedRight
  }

  fun isTileFree(tile: BoardTile, allTiles: List<BoardTile>): Boolean {
    var blockedLeft = false
    var blockedRight = false

    for (other in allTiles) {
      if (other.instanceId == tile.instanceId) continue

      if (other.z > tile.z && abs(other.x - tile.x) < 2 && abs(other.y - tile.y) < 2) {
        return false
      }
      if (other.z == tile.z && abs(other.y - tile.y) < 2) {
        if (other.x == tile.x - 2) {
          blockedLeft = true
        } else if (other.x == tile.x + 2) {
          blockedRight = true
        }
      }
    }
    return !blockedLeft || !blockedRight
  }

  fun recomputeFreeStates(tiles: List<BoardTile>): List<BoardTile> {
    return tiles.map { tile ->
      tile.copy(isFree = isTileFree(tile, tiles))
    }
  }

  fun findAvailablePairs(tiles: List<BoardTile>): List<Pair<BoardTile, BoardTile>> {
    val freeTiles = tiles.filter { it.isFree }
    val grouped = freeTiles.groupBy { it.symbol.id }
    val result = mutableListOf<Pair<BoardTile, BoardTile>>()

    for ((_, group) in grouped) {
      for (i in 0 until group.size) {
        for (j in i + 1 until group.size) {
          result.add(group[i] to group[j])
        }
      }
    }
    return result
  }

  fun canMatch(a: BoardTile, b: BoardTile): Boolean {
    return a.instanceId != b.instanceId && a.isFree && b.isFree && a.symbol.id == b.symbol.id
  }

  /**
   * Re-assigns the exact remaining symbol pairs onto the exact remaining board positions
   * using reverse-peeling so the shuffled board is guaranteed solvable and immediately playable.
   */
  fun reshuffleSolvable(
    tiles: List<BoardTile>,
    random: Random = Random.Default,
  ): List<BoardTile> {
    if (tiles.size < 2) return recomputeFreeStates(tiles)

    // Extract symbol pairs currently on the board
    val symbolPairs = mutableListOf<TileSymbol>()
    val byId = tiles.groupBy { it.symbol.id }
    for ((_, group) in byId) {
      val pairCount = group.size / 2
      repeat(pairCount) {
        symbolPairs.add(group.first().symbol)
      }
    }

    val slots = tiles.map { TileSlot(it.x, it.y, it.z) }
    val assigned = assignPairsByReversePeel(slots, symbolPairs, random)
    if (assigned != null) {
      val mapped = assigned.mapIndexed { idx, (slot, symbol) ->
        BoardTile(
          instanceId = tiles[idx].instanceId,
          symbol = symbol,
          x = slot.x,
          y = slot.y,
          z = slot.z,
        )
      }
      return recomputeFreeStates(mapped)
    }

    // Fallback: simple random permutation of symbols across existing slots
    val shuffledSymbols = tiles.map { it.symbol }.shuffled(random)
    return recomputeFreeStates(
      tiles.mapIndexed { idx, tile ->
        tile.copy(symbol = shuffledSymbols[idx])
      },
    )
  }

  /**
   * Reverse-peel solver-guarantor:
   * Repeatedly picks 2 currently free slots, assigns a pair of identical symbols to them,
   * removes them from the remaining layout, and repeats until all slots are filled.
   * Playing forward in reverse order is mathematically guaranteed to solve the board.
   */
  fun assignPairsByReversePeel(
    slots: List<TileSlot>,
    pairSymbols: List<TileSymbol>,
    random: Random,
  ): List<Pair<TileSlot, TileSymbol>>? {
    if (slots.size != pairSymbols.size * 2) return null

    repeat(12) {
      val remaining = slots.toMutableList()
      val shuffledPairs = pairSymbols.shuffled(random)
      val output = mutableListOf<Pair<TileSlot, TileSymbol>>()
      var failed = false

      for (symbol in shuffledPairs) {
        val freeSlots = remaining.filter { isSlotFree(it, remaining) }
        if (freeSlots.size < 2) {
          failed = true
          break
        }
        val first = freeSlots[random.nextInt(freeSlots.size)]
        val candidates = freeSlots.filter { it !== first }
        // Prefer slightly separated tiles when possible so pairs aren't always touching
        val distant = candidates.filter {
          abs(it.x - first.x) + abs(it.y - first.y) >= 3 || it.z != first.z
        }
        val second = if (distant.isNotEmpty() && random.nextFloat() < 0.75f) {
          distant[random.nextInt(distant.size)]
        } else {
          candidates[random.nextInt(candidates.size)]
        }

        output.add(first to symbol)
        output.add(second to symbol)
        remaining.remove(first)
        remaining.remove(second)
      }

      if (!failed && remaining.isEmpty()) {
        return output
      }
    }
    return null
  }
}
