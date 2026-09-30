package ru.professionals.combats.data

import android.content.Context
import android.util.Log
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import ru.professionals.domain.*
import ru.professionals.network.*

/** Maps the independent REST client to domain operations and securely rotates sessions. Created: 30-09-2026. Author: participant number pending. */
class SupabaseCombatRepository(context: Context, private val baseUrl: String, publishableKey: String) : CombatRepository {
    private val store = EncryptedSessionStore(context)
    private val sessions = AuthSessionController(store.load(),
        persist = { value -> if (value == null) store.clear() else store.save(value) },
        isExpired = { it is ApiException && it.status in listOf(400, 401, 403) })
    override val session: StateFlow<Session?> = sessions.state
    private val api = SupabaseClient(ApiExecutor(UrlConnectionClient(baseUrl, NetworkLogger { level, tag, message ->
        when (level) { "ERROR" -> Log.e(tag, "[$tag]: Ошибка — $message"); "DEBUG" -> Log.d(tag, "[$tag]: Ответ — $message"); else -> Log.i(tag, "[$tag]: Запрос — $message") }
    }), publishableKey))

    override suspend fun signIn(email: String, password: String): Session = withContext(Dispatchers.IO) {
        require(isContestEmail(email)) { "Email: только строчные латинские буквы, цифры и домен .ru" }
        requireNotNull(sessions.authenticate {
            val value = api.signIn(email, password).domain()
            api.ensureProfile(value.accessToken)
            value
        })
    }
    override suspend fun signUp(name: String, email: String, password: String, phone: String, username: String): Session? = withContext(Dispatchers.IO) {
        require(isContestEmail(email)) { "Email: только строчные латинские буквы, цифры и домен .ru" }
        require(name.trim().length in 2..80) { "Укажите имя от 2 до 80 символов" }
        require(password.length >= 8) { "Пароль должен содержать не менее 8 символов" }
        sessions.authenticate {
            api.signUp(name.trim(), email, password, phone, username).domainOrNull()?.also {
                api.ensureProfile(it.accessToken)
            }
        }
    }
    override suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        sessions.signOut { value -> value?.let { api.signOut(it.accessToken) }; Unit }
    }
    override suspend fun getProfile(id: String): Player = authenticated { api.getProfile(it, id).domain() }
    override suspend fun updateProfile(name: String, location: String, avatarBytes: ByteArray?): Player = authenticated { token ->
        require(name.trim().length in 2..80) { "Укажите имя от 2 до 80 символов" }
        val userId = requireNotNull(session.value).userId
        val avatarUrl = avatarBytes?.let { bytes ->
            require(bytes.size <= 5 * 1024 * 1024) { "Фото должно быть меньше 5 МБ" }
            val png = bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte()
            val jpeg = bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
            require(png || jpeg) { "Выберите изображение JPEG или PNG" }
            val path = api.uploadAvatar(token, userId, bytes, png)
            "${baseUrl.trimEnd('/')}/storage/v1/object/public/contest-avatars/$path?v=${System.currentTimeMillis()}"
        }
        api.updateProfile(token, userId, name.trim(), location.take(120), avatarUrl).domain()
    }
    override suspend fun popularPlayers(): List<Player> = authenticated { api.popularPlayers(it).map(ProfileDto::domain) }
    override suspend fun createCombat(request: ScheduleRequest): Combat = authenticated { token ->
        api.createGame(token, request.title.trim(), request.category.name, Instant.ofEpochMilli(request.startsAtEpochMillis).toString(),
            request.endsAtEpochMillis?.let { Instant.ofEpochMilli(it).toString() }, request.winningPoints, request.description).domain()
    }
    override suspend fun listCombats(): List<Combat> = authenticated { token ->
        val result = mutableListOf<Combat>()
        // Fetch every page rather than silently omitting older scheduled/completed matches.
        var offset = 0
        do {
            val page = api.listGames(token, offset)
            result.addAll(page.map(GameDto::domain))
            offset += page.size
        } while (page.size == 100)
        result
    }
    override suspend fun getCombat(id: String): Combat = authenticated { api.getGame(it, id).domain() }
    override suspend fun joinCombat(id: String): Combat = authenticated { api.joinGame(it, id).domain() }
    override suspend fun startCombat(id: String): Combat = authenticated { api.startGame(it, id).domain() }
    override suspend fun submitResult(id: String, moves: List<Int>): Combat = authenticated { api.saveResult(it, id, moves).domain() }
    override suspend fun forfeitCombat(id: String): Combat = authenticated { api.forfeitGame(it, id).domain() }
    override suspend fun statistics(): PlayerStatistics = authenticated { api.statistics(it).let { dto ->
        PlayerStatistics(dto.weeklyPoints, dto.imageWins, dto.circleWins, dto.scheduledThisWeek, dto.dailyPoints)
    } }

    private suspend fun <T> authenticated(block: (String) -> T): T = withContext(Dispatchers.IO) {
        val access = validToken()
        try { block(access.token) }
        catch (error: ApiException) {
            Log.e("Repository", "[Repository]: Ошибка — HTTP ${error.status}")
            // Retry only an authorization rejection, once, after rotating the session.
            if (error.status == 401) block(validToken(access).token) else throw error
        }
    }
    private suspend fun validToken(rejected: SessionAccess? = null): SessionAccess = sessions.access(rejected?.generation, rejected?.token) { refreshToken ->
        try {
            api.refresh(refreshToken).domain()
        } catch (error: ApiException) {
            Log.e("Repository", "[Repository]: Ошибка — Обновление сессии (${error.status})")
            throw error
        }
    }
}

private fun SessionDto.domain() = Session(userId, email, accessToken, refreshToken, expiresAt)
private fun SessionDto?.domainOrNull() = this?.domain()
private fun ProfileDto.domain() = Player(id, name, avatarUrl, location, points, username)
private fun GameDto.domain() = Combat(id, title, GameCategory.valueOf(category), Instant.parse(startsAt).toEpochMilli(), hostId, guestId,
    GameStatus.valueOf(status), imageSeed, winnerId, startedAt?.let { Instant.parse(it).toEpochMilli() },
    endsAt?.let { Instant.parse(it).toEpochMilli() }, winningPoints, description)
