package ru.professionals.combats.presentation

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.professionals.domain.*

/** Presentation integration tests use a test-only repository, never a simulated production server. */
class CombatFlowTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: TestCombatRepository

    @Before fun reset() {
        context.getSharedPreferences("combat_ui", Context.MODE_PRIVATE).edit().clear().commit()
        repository = TestCombatRepository()
    }

    @Test fun firstLaunchSkipOpensRegistrationAndPersistsCompletion() {
        compose.setContent { CombatApp(repository) }
        awaitText("Skip")
        compose.onNodeWithText("Skip").performClick()
        compose.onAllNodesWithText("Create Account").onFirst().assertIsDisplayed()
        assertTrue(context.getSharedPreferences("combat_ui", Context.MODE_PRIVATE).getBoolean("onboarded", false))
        assertEquals(0, repository.signUpCalls)
    }

    @Test fun invalidContestEmailNeverReachesRepository() {
        prepareScreen("register")
        compose.setContent { CombatApp(repository) }
        awaitText("Full Name")
        compose.onNode(hasSetTextAction() and hasText("Email")).performScrollTo().performTextInput("Upper.Name@example.com")
        compose.onNode(hasText("Create Account") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Email must match name@domain.ru using lowercase letters and numbers.").assertExists()
        assertEquals(0, repository.signUpCalls)
        assertEquals(0, repository.signInCalls)
    }

    @Test fun publishingSendsScheduleThroughAbstractionBeforeSuccess() {
        repository.session.value = testSession()
        prepareScreen("schedule")
        compose.setContent { CombatApp(repository) }
        awaitText("Game Name")
        compose.onNode(hasSetTextAction() and hasText("Game Name")).performTextInput("Evening Puzzle")
        compose.onNode(hasText("Publish") and hasClickAction()).performScrollTo().performClick()
        awaitText("Success!")
        val request = requireNotNull(repository.lastSchedule)
        assertEquals("Evening Puzzle", request.title)
        assertEquals(GameCategory.IMAGE, request.category)
        assertTrue(request.startsAtEpochMillis > System.currentTimeMillis())
        assertEquals(1, repository.createCalls)
        compose.onNodeWithText("Your game has been published.").assertIsDisplayed()
    }

    private fun prepareScreen(screen: String) {
        context.getSharedPreferences("combat_ui", Context.MODE_PRIVATE).edit().putBoolean("onboarded", true).putString("screen", screen).commit()
    }

    private fun awaitText(text: String) {
        compose.waitUntil(7000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
}

private fun testSession() = Session("test-player", "player@demo.ru", "test-only-access", "test-only-refresh", System.currentTimeMillis() / 1000 + 3600)

/** This fake lives exclusively in androidTest and cannot be packaged in a release APK. */
private class TestCombatRepository : CombatRepository {
    override val session = MutableStateFlow<Session?>(null)
    var signUpCalls = 0
    var signInCalls = 0
    var createCalls = 0
    var lastSchedule: ScheduleRequest? = null
    override suspend fun signIn(email: String, password: String): Session { signInCalls++; return testSession().also { session.value = it } }
    override suspend fun signUp(name: String, email: String, password: String, phone: String, username: String): Session? { signUpCalls++; return testSession().also { session.value = it } }
    override suspend fun signOut() { session.value = null }
    override suspend fun getProfile(id: String) = Player(id, "Test Player")
    override suspend fun updateProfile(name: String, location: String, avatarBytes: ByteArray?) = Player("test-player", name, location = location)
    override suspend fun popularPlayers(): List<Player> = emptyList()
    override suspend fun createCombat(request: ScheduleRequest): Combat {
        createCalls++; lastSchedule = request
        return Combat("test-game", request.title, request.category, request.startsAtEpochMillis, "test-player")
    }
    override suspend fun listCombats(): List<Combat> = emptyList()
    override suspend fun getCombat(id: String) = Combat(id, "Test Game", GameCategory.IMAGE, System.currentTimeMillis(), "test-player")
    override suspend fun joinCombat(id: String) = getCombat(id)
    override suspend fun startCombat(id: String) = getCombat(id).copy(status = GameStatus.ACTIVE)
    override suspend fun submitResult(id: String, moves: List<Int>) = getCombat(id).copy(status = GameStatus.FINISHED, winnerId = "test-player")
    override suspend fun forfeitCombat(id: String) = getCombat(id).copy(status = GameStatus.FINISHED)
    override suspend fun statistics() = PlayerStatistics()
}
