package com.example

import com.example.domain.engine.MahjongBoardGenerator
import com.example.domain.engine.MahjongRules
import com.example.domain.model.BoardTile
import com.example.domain.model.Difficulty
import com.example.domain.model.TileCatalog
import com.example.domain.model.TilePack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MahjongEngineTest {

  @Test
  fun catalogContainsCompleteSets() {
    assertEquals(26, TileCatalog.latinLetters.size)
    assertEquals("A", TileCatalog.latinLetters.first().glyph)
    assertEquals("Z", TileCatalog.latinLetters.last().glyph)

    assertEquals(11, TileCatalog.numbers.size)
    assertEquals("0", TileCatalog.numbers.first().glyph)
    assertEquals("10", TileCatalog.numbers.last().glyph)
    assertEquals("un", TileCatalog.numbers[1].frenchSpeech)

    assertEquals(28, TileCatalog.arabicLetters.size)
    assertEquals("أ", TileCatalog.arabicLetters.first().glyph)
    assertEquals("Alif", TileCatalog.arabicLetters.first().frenchSpeech)
    assertEquals("ي", TileCatalog.arabicLetters.last().glyph)
  }

  @Test
  fun generatedBoardsAcrossDifficultiesAreEvenAndPlayable() {
    for (diff in Difficulty.entries) {
      for (level in 1..15) {
        val board = MahjongBoardGenerator.generateCampaignBoard(diff, level, TilePack.ALL)
        assertTrue("Board empty for $diff $level", board.isNotEmpty())
        assertEquals("Odd tile count for $diff $level", 0, board.size % 2)
        val pairs = MahjongRules.findAvailablePairs(board)
        assertTrue("No initial available pairs for $diff $level", pairs.isNotEmpty())
        assertTrue("Board should be solvable via reshuffle fallback", canClearBoard(board))
      }
    }
  }

  @Test
  fun endlessBoardsArePlayableAcrossAllPacks() {
    for (pack in TilePack.entries) {
      for (diff in Difficulty.entries) {
        val board = MahjongBoardGenerator.generateEndlessBoard(diff, pack, 1)
        assertTrue(board.isNotEmpty())
        assertEquals(0, board.size % 2)
        assertTrue(MahjongRules.findAvailablePairs(board).isNotEmpty())
      }
    }
  }

  private fun canClearBoard(initial: List<BoardTile>): Boolean {
    var current = initial
    var safety = 0
    while (current.isNotEmpty() && safety < 200) {
      safety++
      var pairs = MahjongRules.findAvailablePairs(current)
      if (pairs.isEmpty()) {
        current = MahjongRules.reshuffleSolvable(current)
        pairs = MahjongRules.findAvailablePairs(current)
      }
      if (pairs.isEmpty()) return false
      val (a, b) = pairs.first()
      current = MahjongRules.recomputeFreeStates(
        current.filter { it.instanceId != a.instanceId && it.instanceId != b.instanceId },
      )
    }
    return current.isEmpty()
  }
}
