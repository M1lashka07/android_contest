package ru.professionals.domain

import org.junit.Assert.*
import org.junit.Test

class PuzzlesTest {
    @Test fun emailMatchesCompetitionPattern() {
        assertTrue(isContestEmail("player7@domain2.ru"))
        listOf("A@b.ru", "a.b@b.ru", "a@b.com", "a@b.ru.evil", "a@b.ru\n").forEach { assertFalse(it, isContestEmail(it)) }
    }
    @Test fun allGeneratedBoardsAreSolvableAndUnsolved() {
        for (seed in 1..1000) {
            val board = imageInitialBoard(seed)
            assertEquals((0..8).toList(), board.sorted())
            assertNotEquals(listOf(1,2,3,4,5,6,7,8,0), board)
            val tiles = board.filter { it != 0 }
            val inversions = tiles.indices.sumOf { i -> (i + 1 until tiles.size).count { j -> tiles[i] > tiles[j] } }
            assertEquals(0, inversions % 2)
        }
    }
    @Test fun imageMovesCannotWrapRows() {
        val board = listOf(1,2,0,4,5,6,7,8,3)
        assertNull(applyImageMove(board, 3))
        assertEquals(listOf(1,0,2,4,5,6,7,8,3), applyImageMove(board, 1))
        assertNull(applyImageMove(board, -1))
    }
    @Test fun sameSeedGivesSamePuzzle() {
        assertEquals(imageInitialBoard(47), imageInitialBoard(47))
        assertNotEquals(imageInitialBoard(47), imageInitialBoard(48))
        assertEquals((1..5).toList(), circleInitialOrder(47).sorted())
    }
    @Test fun seedFixturesMatchLivePostgresGenerator() {
        assertEquals(listOf(2,1,7,3,0,5,6,8,4),imageInitialBoard(1))
        assertEquals(listOf(0,4,8,1,2,3,6,5,7),imageInitialBoard(42))
        assertEquals(listOf(4,8,0,6,5,1,7,2,3),imageInitialBoard(1234))
        assertEquals(listOf(2,4,5,6,3,1,0,7,8),imageInitialBoard(2147483646))
    }
    @Test fun independentlySolvedReplayFinishesPuzzle() {
        var board = imageInitialBoard(42)
        listOf(1,4,3,6,7,8,5,2,1,4,3,6,7,4,5,8,7,4,3,0,1,2,5,8).forEach { index ->
            board = requireNotNull(applyImageMove(board,index))
        }
        assertEquals(listOf(1,2,3,4,5,6,7,8,0),board)
    }
}
