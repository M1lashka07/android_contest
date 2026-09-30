package ru.professionals.combats.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test
import ru.professionals.domain.CombatException
import ru.professionals.domain.Session

/** Regression coverage for refresh/logout ordering and stale asynchronous responses. */
class AuthSessionControllerTest {
    private fun session(id: String, token: String, expires: Long = 5000) = Session(id,"$id@b.ru",token,"refresh-$token",expires)

    @Test fun logoutWaitsForRefreshThenRevokesAndClearsLatestSession() = runBlocking {
        val old = session("a","old",0)
        var persisted: Session? = old
        val controller = AuthSessionController(old,{ persisted=it },{ false },{ 1000 })
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val refreshJob = launch { controller.access { entered.complete(Unit); release.await(); session("a","new") } }
        entered.await()
        var revoked: String? = null
        val logoutJob = launch { controller.signOut { revoked=it?.accessToken } }
        yield()
        assertNull(revoked)
        release.complete(Unit)
        refreshJob.join(); logoutJob.join()
        assertEquals("new",revoked)
        assertNull(controller.state.value)
        assertNull(persisted)
    }

    @Test fun oldUnauthorizedResponseCannotRefreshOrEraseNewAccount() = runBlocking {
        val controller = AuthSessionController(session("a","a-token"),{}, { true },{ 1000 })
        val oldRequest = controller.access { error("not expired") }
        controller.authenticate { session("b","b-token") }
        var refreshCalled = false
        val failure = runCatching {
            controller.access(oldRequest.generation,oldRequest.token) { refreshCalled=true; error("old credentials revoked") }
        }.exceptionOrNull()
        assertTrue(failure is CombatException)
        assertFalse(refreshCalled)
        assertEquals("b",controller.state.value?.userId)
    }

    @Test fun cancelledCallerKeepsRotatedCredentialsUntilExplicitLogout() = runBlocking {
        val old = session("a","old",0)
        var persisted: Session? = old
        val controller = AuthSessionController(old,{ persisted=it },{ false },{ 1000 })
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        // Simulate a blocking transport returning a response even after its calling coroutine is cancelled.
        val job = launch { controller.access { withContext(NonCancellable) { entered.complete(Unit); release.await(); session("a","new") } } }
        entered.await(); job.cancel(); release.complete(Unit); job.join()
        assertEquals(session("a","new"),persisted)
        assertEquals(session("a","new"),controller.state.value)
        controller.signOut { }
        assertNull(persisted)
        assertNull(controller.state.value)
    }

    @Test fun expiredRefreshClearsCredentialsButTransientFailureKeepsThem() = runBlocking {
        val old = session("a","old",0)
        var persisted: Session? = old
        val controller = AuthSessionController(old,{ persisted=it },{ it.message == "expired" },{ 1000 })
        runCatching { controller.access { error("offline") } }
        assertEquals(old,persisted)
        runCatching { controller.access { error("expired") } }
        assertNull(persisted)
        assertNull(controller.state.value)
    }
}
