package ru.professionals.domain

/** Puzzle categories. Created: 30-09-2026. Author: participant number pending. */
enum class GameCategory { IMAGE, CIRCLE }
/** Server-controlled match lifecycle. Created: 30-09-2026. Author: participant number pending. */
enum class GameStatus { SCHEDULED, ACTIVE, FINISHED }
/** Public player information, excluding credentials. Created: 30-09-2026. Author: participant number pending. */
data class Player(val id: String, val name: String, val avatarUrl: String? = null,
    val location: String = "", val points: Int = 0, val username: String = "")
/** Authenticated session; never log or expose the token values. Created: 30-09-2026. Author: participant number pending. */
data class Session(val userId: String, val email: String, val accessToken: String,
    val refreshToken: String, val expiresAtEpochSeconds: Long)
/** Immutable server match snapshot. Created: 30-09-2026. Author: participant number pending. */
data class Combat(val id: String, val title: String, val category: GameCategory,
    val startsAtEpochMillis: Long, val hostId: String, val guestId: String? = null,
    val status: GameStatus = GameStatus.SCHEDULED, val imageSeed: Int = 0,
    val winnerId: String? = null, val startedAtEpochMillis: Long? = null,
    val endsAtEpochMillis: Long? = null, val winningPoints: Int = 0,
    val description: String = "")
/** Weekly statistics use Europe/Moscow and Monday as week start. Created: 30-09-2026. Author: participant number pending. */
data class PlayerStatistics(val weeklyPoints: Int = 0, val imageWins: Int = 0,
    val circleWins: Int = 0, val scheduledThisWeek: Int = 0,
    val dailyPoints: List<Int> = List(7) { 0 })
/** User input for scheduling a match. Created: 30-09-2026. Author: participant number pending. */
data class ScheduleRequest(val title: String, val category: GameCategory,
    val startsAtEpochMillis: Long, val remind: Boolean = false,
    val endsAtEpochMillis: Long? = null, val winningPoints: Int = 0,
    val description: String = "")
/** A readable domain failure, optionally retaining its technical cause. Created: 30-09-2026. Author: participant number pending. */
class CombatException(message: String, cause: Throwable? = null) : Exception(message, cause)
