package ru.professionals.combats.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.professionals.combats.platform.Analytics
import ru.professionals.combats.platform.GameReminders
import ru.professionals.combats.platform.readProfilePhoto
import ru.professionals.domain.*

/** Presentation state is retained across rotation; only non-secret drafts are persisted to disk. */
internal class CombatViewModel(private val context: Context, val repository: CombatRepository) : ViewModel() {
    private val preferences = context.getSharedPreferences("combat_ui", Context.MODE_PRIVATE)
    var screen by mutableStateOf("splash"); private set
    var error by mutableStateOf<String?>(null); private set
    var busy by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var player by mutableStateOf<Player?>(null); private set
    var viewedPlayer by mutableStateOf<Player?>(null); private set
    var popular by mutableStateOf<List<Player>>(emptyList()); private set
    var games by mutableStateOf<List<Combat>>(emptyList()); private set
    var combat by mutableStateOf<Combat?>(null); private set
    var statistics by mutableStateOf<PlayerStatistics?>(null); private set
    var moves by mutableStateOf<List<Int>>(emptyList()); private set
    var editing by mutableStateOf(preferences.getBoolean("profile.editing", false)); private set
    var selectedGameId by mutableStateOf(preferences.getString("selectedGame", "") ?: ""); private set
    var selectedPlayerId by mutableStateOf(preferences.getString("selectedPlayer", "") ?: ""); private set
    var onboardingPage by mutableIntStateOf(preferences.getInt("onboardingPage", 0)); private set
    var query by mutableStateOf(preferences.getString("query", "") ?: ""); private set
    var category by mutableStateOf(preferences.getString("category", "All") ?: "All"); private set
    var filter by mutableStateOf(preferences.getString("filter", "Upcoming") ?: "Upcoming"); private set
    var authDraft by mutableStateOf(AuthDraft(text("auth.name"), text("auth.username"), text("auth.phone"), text("auth.email"), text("auth.countryCode").ifBlank { "+7" })); private set
    var scheduleDraft by mutableStateOf(ScheduleDraft(text("schedule.title"),
        runCatching { GameCategory.valueOf(text("schedule.category")) }.getOrDefault(GameCategory.IMAGE),
        preferences.getLong("schedule.start", System.currentTimeMillis() + 3_600_000),
        preferences.getLong("schedule.end", System.currentTimeMillis() + 7_200_000),
        text("schedule.prize").ifEmpty { "100" }, text("schedule.description"), preferences.getBoolean("schedule.remind", false))); private set
    var profileDraft by mutableStateOf(ProfileDraft(text("profile.name"), text("profile.location"))); private set
    private var errorJob: Job? = null
    private var pollJob: Job? = null
    private var leaving = false
    private var foreground = true
    private var activeLoads = 0
    private val backgroundJobs = mutableSetOf<Job>()
    private val history = mutableListOf<String>()
    val userId: String? get() = repository.session.value?.userId

    init {
        viewModelScope.launch {
            delay(750)
            val authenticated = userId != null
            val last = text("screen")
            screen = when {
                !preferences.getBoolean("onboarded", false) -> "onboarding"
                !authenticated -> if (last == "register") "register" else "login"
                last in setOf("home", "schedule", "success", "discover", "statistics", "profile", "player", "players", "combat", "result", "departed") -> last
                else -> "home"
            }
            if (authenticated) {
                val unfinished = when {
                    text("activeCombatUser") == userId -> text("activeCombat")
                    pendingId().isNotBlank() -> pendingId()
                    else -> ""
                }
                if (unfinished.isNotBlank()) {
                    savePending(unfinished, requireNotNull(userId))
                    selectedGameId = unfinished
                    screen = "departed"
                    syncForfeit()
                }
                refreshForScreen()
                loadPlayer()
            }
        }
        viewModelScope.launch {
            var previouslyAuthenticated = userId != null
            repository.session.collect { session ->
                if (previouslyAuthenticated && session == null) resetToLogin()
                previouslyAuthenticated = session != null
            }
        }
    }

    private fun text(key: String): String = preferences.getString(key, "") ?: ""
    private fun pendingId(): String {
        val owner = userId ?: return ""
        return text("pendingForfeit.$owner").ifBlank { if (text("pendingForfeitUser") == owner) text("pendingForfeit") else "" }
    }
    private fun savePending(id: String, owner: String) {
        preferences.edit().putString("pendingForfeit.$owner", id).remove("activeCombat").remove("activeCombatUser").apply()
    }
    private fun clearPending(owner: String) {
        val editor = preferences.edit().remove("pendingForfeit.$owner")
        if (text("pendingForfeitUser") == owner) editor.remove("pendingForfeit").remove("pendingForfeitUser")
        editor.apply()
    }
    private suspend fun flushPending() {
        val owner = userId ?: return
        val pending = pendingId()
        if (pending.isBlank()) return
        check(repository.forfeitCombat(pending).status == GameStatus.FINISHED) { "The server has not confirmed the defeat yet. Please retry." }
        clearPending(owner)
    }
    fun showError(message: String) {
        errorJob?.cancel(); error = message
        errorJob = viewModelScope.launch { delay(5000); error = null }
    }
    fun dismissError() { errorJob?.cancel(); error = null }
    private fun request(block: suspend () -> Unit) {
        if (busy) return
        viewModelScope.launch {
            busy = true
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                Log.e("CombatViewModel", "[CombatViewModel]: Ошибка — User request failed", failure)
                showError(failure.message ?: "The request failed. Check your internet connection.")
            }
            finally { busy = false }
        }
    }
    private fun background(block: suspend () -> Unit) {
        val job = viewModelScope.launch {
            activeLoads++; loading = true
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                Log.e("CombatViewModel", "[CombatViewModel]: Ошибка — Data refresh failed", failure)
                showError(failure.message ?: "Unable to load data.")
            }
            finally { activeLoads--; loading = activeLoads > 0 }
        }
        backgroundJobs.add(job)
        job.invokeOnCompletion { backgroundJobs.remove(job) }
    }

    fun navigate(destination: String, remember: Boolean = true) {
        if (destination == screen) return
        if (screen == "game" && destination != "result" && destination != "departed") { leaveGame(); return }
        if (remember && screen !in setOf("splash", "onboarding", "login", "register", "success", "game", "result")) history.add(screen)
        screen = destination
        preferences.edit().putString("screen", destination).apply()
        Analytics.event("screen_$destination")
        refreshForScreen()
    }
    fun back() {
        if (screen == "game") { leaveGame(); return }
        val destination = history.removeLastOrNull() ?: if (userId != null) "home" else "login"
        navigate(destination, false)
    }
    fun finishOnboarding() {
        preferences.edit().putBoolean("onboarded", true).apply()
        navigate("register", false)
    }
    fun updateOnboarding(page: Int) { onboardingPage = page; preferences.edit().putInt("onboardingPage", page).apply() }
    fun updateAuth(value: AuthDraft) {
        authDraft = value
        preferences.edit().putString("auth.name", value.fullName).putString("auth.username", value.userName)
            .putString("auth.phone", value.phone).putString("auth.email", value.email).putString("auth.countryCode", value.countryCode).apply()
    }
    fun updateSchedule(value: ScheduleDraft) {
        scheduleDraft = value
        preferences.edit().putString("schedule.title", value.title).putString("schedule.category", value.category.name)
            .putLong("schedule.start", value.start).putLong("schedule.end", value.end).putString("schedule.prize", value.prize)
            .putString("schedule.description", value.description).putBoolean("schedule.remind", value.remind).apply()
    }
    fun updateProfileDraft(value: ProfileDraft) {
        profileDraft = value
        preferences.edit().putString("profile.name", value.name).putString("profile.location", value.location).apply()
    }
    fun updateEditing(value: Boolean) {
        editing = value; preferences.edit().putBoolean("profile.editing", value).apply()
        if (value) player?.let { updateProfileDraft(ProfileDraft(it.name, it.location)) }
    }
    fun updateQuery(value: String) { query = value; preferences.edit().putString("query", value).apply() }
    fun updateCategory(value: String) { category = value; preferences.edit().putString("category", value).apply() }
    fun updateFilter(value: String) { filter = value; preferences.edit().putString("filter", value).apply() }

    fun authenticate(register: Boolean, email: String, password: String) = request {
        val phone = authDraft.phone.trim().let { if (it.isBlank() || it.startsWith("+")) it else authDraft.countryCode.trim() + it }
        val session = if (register) repository.signUp(authDraft.fullName.trim(), email, password, phone, authDraft.userName.trim()) else repository.signIn(email, password)
        Analytics.event(if (register) "sign_up" else "sign_in")
        if (session == null) {
            navigate("login", false)
            showError("Account created. Confirm your email using the message sent by the server, then sign in.")
        } else {
            history.clear(); navigate("home", false); loadPlayer()
        }
    }
    private fun resetToLogin() {
        val active = text("activeCombat")
        val owner = text("activeCombatUser")
        if (active.isNotBlank() && owner.isNotBlank()) savePending(active, owner)
        backgroundJobs.toList().forEach(Job::cancel)
        pollJob?.cancel(); history.clear(); player = null; viewedPlayer = null; popular = emptyList(); games = emptyList(); combat = null; statistics = null
        moves = emptyList(); selectedGameId = ""; selectedPlayerId = ""; editing = false
        preferences.edit().putString("screen", "login").remove("selectedGame").remove("selectedPlayer").remove("profile.name").remove("profile.location").putBoolean("profile.editing", false).apply()
        screen = "login"
    }
    fun signOut() = request {
        // A pending defeat belongs to this account and must be acknowledged before we discard its credentials.
        flushPending()
        repository.signOut(); Analytics.event("sign_out"); resetToLogin()
    }
    private fun loadPlayer() {
        val id = userId ?: return
        background { player = repository.getProfile(id) }
    }
    fun refreshForScreen() {
        if (userId == null || !foreground) return
        when (screen) {
            "home", "players" -> background { popular = repository.popularPlayers() }
            "discover" -> background { games = repository.listCombats() }
            "statistics" -> background { statistics = repository.statistics() }
            "profile" -> loadPlayer()
            "player" -> if (selectedPlayerId.isNotBlank()) background { viewedPlayer = repository.getProfile(selectedPlayerId) }
            "combat", "result" -> if (selectedGameId.isNotBlank()) background { combat = repository.getCombat(selectedGameId) }
        }
        if (screen == "combat" || screen == "game") startPolling() else pollJob?.cancel()
    }
    fun publish() = request {
        flushPending()
        val draft = scheduleDraft
        require(draft.start > System.currentTimeMillis()) { "Choose a start date in the future." }
        val created = ScheduleCombat(repository)(ScheduleRequest(draft.title.trim(), draft.category, draft.start, draft.remind, draft.end, draft.prize.toIntOrNull() ?: -1, draft.description.trim()))
        if (draft.remind) GameReminders(context).schedule(created.id, created.title, created.startsAtEpochMillis)
        Analytics.event("combat_created")
        selectedGameId = created.id; combat = created
        updateSchedule(ScheduleDraft())
        navigate("success", false)
    }
    fun openCombat(id: String) {
        selectedGameId = id; combat = games.firstOrNull { it.id == id }
        preferences.edit().putString("selectedGame", id).apply()
        navigate("combat")
    }
    fun openPlayer(id: String) {
        if (id == userId) { navigate("profile"); return }
        selectedPlayerId = id; viewedPlayer = popular.firstOrNull { it.id == id }
        preferences.edit().putString("selectedPlayer", id).apply()
        navigate("player")
    }
    fun join() = request {
        flushPending()
        combat = repository.joinCombat(selectedGameId)
        Analytics.event("combat_joined")
        showError("You joined the combat. The game can start once its scheduled time arrives.")
    }
    fun start() = request {
        flushPending()
        val started = repository.startCombat(selectedGameId)
        check(started.status == GameStatus.ACTIVE) { "The game has not started yet." }
        enterGame(started)
        startPolling()
    }
    private fun enterGame(started: Combat) {
        if (!foreground || pendingId() == started.id) {
            combat = started
            userId?.let { savePending(started.id, it) }
            screen = "departed"
            preferences.edit().putString("screen", "departed").apply()
            syncForfeit()
            return
        }
        combat = started; moves = emptyList(); leaving = false
        preferences.edit().putString("activeCombat", started.id).putString("activeCombatUser", userId).putString("selectedGame", started.id).putString("screen", "game").apply()
        screen = "game"
        Analytics.event("combat_started")
    }
    private fun startPolling() {
        pollJob?.cancel()
        if (!foreground) return
        val id = selectedGameId
        if (id.isBlank()) return
        pollJob = viewModelScope.launch {
            var reported = false
            while (foreground && (screen == "combat" || screen == "game")) {
                try {
                    val next = repository.getCombat(id)
                    combat = next; reported = false
                    if (screen == "combat" && next.status == GameStatus.ACTIVE && (userId == next.hostId || userId == next.guestId)) enterGame(next)
                    if (screen == "game" && next.status == GameStatus.FINISHED) {
                        preferences.edit().remove("activeCombat").remove("activeCombatUser").apply()
                        userId?.let(::clearPending)
                        // Keep this coroutine alive until navigation has finished; refreshForScreen cancels it afterwards.
                        screen = "result"; preferences.edit().putString("screen", "result").apply()
                        return@launch
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) {
                    Log.e("CombatViewModel", "[CombatViewModel]: Ошибка — Combat polling failed", failure)
                    if (!reported) showError(failure.message ?: "Connection lost. Reconnecting…")
                    reported = true
                }
                delay(2000)
            }
        }
    }
    fun makeMove(value: Int) {
        val game = combat ?: return
        if (screen != "game" || busy || game.status != GameStatus.ACTIVE) return
        if (game.category == GameCategory.CIRCLE) {
            if (value != circleTargetOrder().getOrNull(moves.size)) { showError("Choose the largest remaining circle."); return }
        } else {
            var board = imageInitialBoard(game.imageSeed)
            moves.forEach { board = applyImageMove(board, it) ?: board }
            if (applyImageMove(board, value) == null) return
        }
        moves = moves + value
        Analytics.event(if (game.category == GameCategory.IMAGE) "image_tile_moved" else "circle_placed")
    }
    fun finishGame() = request {
        Analytics.event(if (combat?.category == GameCategory.IMAGE) "image_result_submitted" else "circle_result_submitted")
        val result = repository.submitResult(selectedGameId, moves)
        combat = result
        check(result.status == GameStatus.FINISHED) { "The server has not confirmed the game result yet. Please retry." }
        Analytics.event("combat_completed")
        preferences.edit().remove("activeCombat").remove("activeCombatUser").apply()
        userId?.let(::clearPending)
        navigate("result", false)
    }
    fun leaveGame() {
        if (screen != "game" || leaving) return
        leaving = true; pollJob?.cancel()
        userId?.let { savePending(selectedGameId, it) }
        navigate("departed", false)
        Analytics.event("combat_forfeited")
        syncForfeit()
    }
    fun onBackground() {
        foreground = false
        pollJob?.cancel()
        leaveGame()
    }
    fun onForeground() {
        foreground = true
        if (pendingId().isNotBlank()) syncForfeit()
        refreshForScreen()
    }
    fun syncForfeit() {
        val owner = userId ?: return
        val pending = pendingId()
        if (pending.isBlank()) return
        background {
            val ended = repository.forfeitCombat(pending)
            check(ended.status == GameStatus.FINISHED) { "The server has not confirmed the defeat yet. Please retry." }
            clearPending(owner)
            combat = ended
            if (screen == "departed") navigate("result", false)
        }
    }
    fun saveProfile() = request {
        require(profileDraft.name.trim().isNotEmpty()) { "Enter your name." }
        player = repository.updateProfile(profileDraft.name.trim(), profileDraft.location.trim())
        updateEditing(false); Analytics.event("profile_updated")
    }
    fun changePhoto(uri: Uri) = request {
        val current = player ?: throw IllegalStateException("Load your profile before changing the photo.")
        val bytes = readProfilePhoto(context, uri)
        player = repository.updateProfile(current.name, current.location, bytes)
        Analytics.event("profile_photo_updated")
    }
}
