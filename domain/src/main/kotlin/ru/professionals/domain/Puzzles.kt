package ru.professionals.domain

import kotlin.math.abs

/** Constructs a solvable 3×3 board with the algorithm shared by PostgreSQL. Zero is the empty tile. */
fun imageInitialBoard(seed: Int): List<Int> {
    val board = (1..8).toMutableList().apply { add(0) }
    var random = seed.toLong().coerceAtLeast(1)
    var empty = 8
    var previous = -1
    // Walk from the solved board using valid moves, excluding an immediate reversal.
    repeat(80) {
        val candidates = (0..8).filter { index ->
            index != previous && abs(index / 3 - empty / 3) + abs(index % 3 - empty % 3) == 1
        }
        random = (random * 48271L) % 2147483647L
        val next = candidates[(random % candidates.size).toInt()]
        board[empty] = board[next]
        board[next] = 0
        previous = empty
        empty = next
    }
    // Rare solved shuffles still need a real move before completion.
    if (board == listOf(1, 2, 3, 4, 5, 6, 7, 8, 0)) {
        board[8] = board[7]; board[7] = 0
    }
    return board
}

/** Returns a seeded shuffled circle arrangement. The target order is independent of the seed. */
fun circleInitialOrder(seed: Int): List<Int> {
    val result = (1..5).toMutableList()
    var random = seed.toLong().coerceAtLeast(1)
    for (i in 4 downTo 1) {
        random = (random * 48271L) % 2147483647L
        val j = (random % (i + 1)).toInt()
        val old = result[i]; result[i] = result[j]; result[j] = old
    }
    return result
}

/** Required largest-to-smallest circle sequence. */
fun circleTargetOrder(): List<Int> = listOf(5, 4, 3, 2, 1)

/** Validates and replays an image move without changing the caller's board. */
fun applyImageMove(board: List<Int>, index: Int): List<Int>? {
    if (board.size != 9 || index !in 0..8) return null
    val empty = board.indexOf(0)
    if (empty < 0 || abs(index / 3 - empty / 3) + abs(index % 3 - empty % 3) != 1) return null
    return board.toMutableList().apply { this[empty] = this[index]; this[index] = 0 }
}

/** The competition intentionally accepts a narrower email format than general email standards. */
fun isContestEmail(email: String): Boolean = Regex("^[a-z0-9]+@[a-z0-9]+\\.ru$").matches(email)

/** Validates scheduling before sending it; server repeats these checks authoritatively. Created: 30-09-2026. Author: participant number pending. */
class ScheduleCombat(private val repository: CombatRepository) {
    /** Creates a match after checking its title, date range and prize. */
    suspend operator fun invoke(request: ScheduleRequest): Combat {
        require(request.title.trim().length in 3..80) { "Название должно содержать от 3 до 80 символов" }
        require(request.endsAtEpochMillis == null || request.endsAtEpochMillis > request.startsAtEpochMillis) { "Окончание должно быть позже начала" }
        require(request.winningPoints in 0..1000) { "Приз должен быть от 0 до 1000 баллов" }
        return repository.createCombat(request)
    }
}
