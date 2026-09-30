package ru.professionals.combats.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.professionals.domain.CombatException
import ru.professionals.domain.Session

/** A token paired with its explicit login generation. Created: 30-09-2026. Author: participant number pending. */
internal data class SessionAccess(val token: String, val generation: Long)

/** Serializes authentication and rejects stale-session retries without an Android dependency. Created: 30-09-2026. Author: participant number pending. */
internal class AuthSessionController(
    initial: Session?,
    private val persist: (Session?) -> Unit,
    private val isExpired: (Throwable) -> Boolean,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) {
    private val lock = Mutex()
    private val mutable = MutableStateFlow(initial)
    val state: StateFlow<Session?> = mutable.asStateFlow()
    private var generation = 0L

    /** Completes an explicit login atomically with secure persistence. Confirmation-only signup leaves state unchanged. */
    suspend fun authenticate(operation: suspend () -> Session?): Session? = lock.withLock {
        val result = operation()
        currentCoroutineContext().ensureActive()
        if (result != null) replace(result)
        result
    }

    /** Waits out an in-flight refresh, revokes the resulting current session, then always clears local credentials. */
    suspend fun signOut(operation: suspend (Session?) -> Unit) = lock.withLock {
        try { operation(mutable.value) }
        finally { replace(null) }
    }

    /** Returns a current token; a rejected old login may never refresh or clear a later login. */
    suspend fun access(expectedGeneration: Long? = null, rejectedToken: String? = null,
                       refresh: suspend (String) -> Session): SessionAccess = lock.withLock {
        if (expectedGeneration != null && expectedGeneration != generation) throw CombatException("Сессия изменилась. Повторите действие")
        val value = mutable.value ?: throw CombatException("Войдите в аккаунт")
        val anotherRequestAlreadyRefreshed = rejectedToken != null && rejectedToken != value.accessToken
        if (anotherRequestAlreadyRefreshed || rejectedToken == null && value.expiresAtEpochSeconds > nowSeconds() + 60) {
            return@withLock SessionAccess(value.accessToken, generation)
        }
        try {
            val updated = refresh(value.refreshToken)
            // Persist a rotated pair even if the HTTP caller was cancelled: the previous refresh token may be consumed.
            // Login/logout share this mutex, so logout will clear this pair after the refresh finishes.
            persist(updated)
            mutable.value = updated
            currentCoroutineContext().ensureActive()
            SessionAccess(updated.accessToken, generation)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (isExpired(failure)) replace(null)
            throw failure
        }
    }

    private fun replace(value: Session?) {
        persist(value)
        generation++
        mutable.value = value
    }
}
