package ru.professionals.domain

import kotlinx.coroutines.flow.StateFlow

/** Replaceable data boundary used by presentation and use cases. Created: 30-09-2026. Author: participant number pending. */
interface CombatRepository {
    val session: StateFlow<Session?>
    /** Starts a session from the supplied credentials. */
    suspend fun signIn(email: String, password: String): Session
    /** Registers a player; null means email confirmation is required. */
    suspend fun signUp(name: String, email: String, password: String, phone: String = "", username: String = ""): Session?
    /** Revokes the remote session and removes locally stored credentials. */
    suspend fun signOut()
    /** Reads a public player by identifier. */
    suspend fun getProfile(id: String): Player
    /** Changes current player's profile; image bytes must be JPEG/PNG. */
    suspend fun updateProfile(name: String, location: String, avatarBytes: ByteArray? = null): Player
    /** Returns highest-scoring public players. */
    suspend fun popularPlayers(): List<Player>
    /** Schedules a server-owned match with a shared puzzle seed. */
    suspend fun createCombat(request: ScheduleRequest): Combat
    /** Lists visible scheduled, active and finished matches. */
    suspend fun listCombats(): List<Combat>
    /** Refreshes a match and its participant heartbeat; call every two seconds during play. */
    suspend fun getCombat(id: String): Combat
    /** Atomically claims the single vacant opponent position. */
    suspend fun joinCombat(id: String): Combat
    /** Starts a due match once both players have joined. */
    suspend fun startCombat(id: String): Combat
    /** Submits a replay; image moves are tile indexes, circle moves are descending values. */
    suspend fun submitResult(id: String, moves: List<Int>): Combat
    /** Records an explicit departure as a defeat. */
    suspend fun forfeitCombat(id: String): Combat
    /** Reads current user's server-calculated statistics. */
    suspend fun statistics(): PlayerStatistics
}
