package ru.professionals.network

import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

/** Authentication DTO; absent session after signup indicates email confirmation. Created: 30-09-2026. Author: participant number pending. */
data class SessionDto(val userId: String, val email: String, val accessToken: String, val refreshToken: String, val expiresAt: Long)
/** Public profile DTO, separate from domain entities. Created: 30-09-2026. Author: participant number pending. */
data class ProfileDto(val id: String, val name: String, val avatarUrl: String?, val location: String, val points: Int, val username: String)
/** Match DTO using ISO-8601 server dates. Created: 30-09-2026. Author: participant number pending. */
data class GameDto(val id: String, val title: String, val category: String, val startsAt: String,
    val hostId: String, val guestId: String?, val status: String, val imageSeed: Int,
    val winnerId: String?, val startedAt: String?, val endsAt: String?, val winningPoints: Int, val description: String)
/** Statistics DTO produced by the authenticated aggregate RPC. Created: 30-09-2026. Author: participant number pending. */
data class StatisticsDto(val weeklyPoints: Int, val imageWins: Int, val circleWins: Int, val scheduledThisWeek: Int, val dailyPoints: List<Int>)

/** Supabase REST contract, independent from Android and application models. Created: 30-09-2026. Author: participant number pending. */
class SupabaseClient(private val api: ApiExecutor) {
    /** Authenticates with Supabase Auth password grant. */
    fun signIn(email: String, password: String): SessionDto = session(json("POST", "/auth/v1/token?grant_type=password", body = JSONObject().put("email", email).put("password", password)))
        ?: throw ApiException(500, "Сервер не вернул сессию")
    /** Registers credentials and public display metadata; confirmation may be required. */
    fun signUp(name: String, email: String, password: String, phone: String = "", username: String = ""): SessionDto? = session(json("POST", "/auth/v1/signup", body = JSONObject()
        .put("email", email).put("password", password).put("data", JSONObject().put("name", name).put("phone", phone).put("username", username))))
    /** Refreshes an expiring session using the opaque refresh token. */
    fun refresh(refreshToken: String): SessionDto = session(json("POST", "/auth/v1/token?grant_type=refresh_token", body = JSONObject().put("refresh_token", refreshToken)))
        ?: throw ApiException(401, "Сессия истекла. Войдите снова")
    /** Invalidates the bearer session. */
    fun signOut(token: String) { api.request("POST", "/auth/v1/logout", token, "{}".toByteArray()) }
    /** Creates only the authenticated user's contest profile on first sign-in. */
    fun ensureProfile(token: String): ProfileDto = profile(rpc(token, "contest_ensure_profile", JSONObject()))
    /** Reads a profile; a missing profile produces a typed 404. */
    fun getProfile(token: String, id: String): ProfileDto {
        val rows = JSONArray(api.request("GET", "/rest/v1/contest_profiles?id=eq.${encode(id)}&select=*", token))
        if (rows.length() == 0) throw ApiException(404, "Профиль не найден")
        return profile(rows.getJSONObject(0))
    }
    /** Updates only user-editable profile columns; points remain server-owned. */
    fun updateProfile(token: String, id: String, name: String, location: String, avatarUrl: String? = null): ProfileDto {
        require(id.isNotBlank()) { "Не указан профиль" }
        val fields = JSONObject().put("p_name", name).put("p_location", location).put("p_avatar_url", avatarUrl ?: JSONObject.NULL)
        return profile(rpc(token, "contest_update_profile", fields))
    }
    /** Lists popular players without exposing private authentication information. */
    fun popularPlayers(token: String): List<ProfileDto> = JSONArray(api.request("GET", "/rest/v1/contest_profiles?select=*&order=points.desc&limit=12", token)).objects().map(::profile)
    /** Creates a match; seed, ownership and lifecycle are assigned by the server. */
    fun createGame(token: String, title: String, category: String, startsAt: String, endsAt: String?, winningPoints: Int, description: String): GameDto = game(rpc(token, "contest_create_game", JSONObject()
        .put("p_title", title).put("p_category", category).put("p_starts_at", startsAt).put("p_ends_at", endsAt ?: JSONObject.NULL)
        .put("p_winning_points", winningPoints).put("p_description", description)))
    /** Lists all match statuses in date order; offset supports additional pages. */
    fun listGames(token: String, offset: Int = 0): List<GameDto> = JSONArray(api.request("GET", "/rest/v1/contest_games?select=*&order=starts_at.desc&limit=100&offset=$offset", token)).objects().map(::game)
    /** Gets match info and updates the caller's participation heartbeat. */
    fun getGame(token: String, id: String): GameDto = game(rpc(token, "contest_get_game", idBody(id)))
    /** Claims the available opponent slot under a server row lock. */
    fun joinGame(token: String, id: String): GameDto = game(rpc(token, "contest_join_game", idBody(id)))
    /** Starts a due match with a common server timestamp. */
    fun startGame(token: String, id: String): GameDto = game(rpc(token, "contest_start_game", idBody(id)))
    /** Validates the supplied replay and records the first valid completion. */
    fun saveResult(token: String, id: String, moves: List<Int>): GameDto = game(rpc(token, "contest_save_result", idBody(id).put("p_moves", JSONArray(moves))))
    /** Records leaving a game as defeat; completed matches remain unchanged. */
    fun forfeitGame(token: String, id: String): GameDto = game(rpc(token, "contest_forfeit_game", idBody(id)))
    /** Computes statistics for the caller, never for a user-selected private identity. */
    fun statistics(token: String): StatisticsDto {
        val data = rpc(token, "contest_statistics", JSONObject())
        val daily = data.optJSONArray("daily_points") ?: JSONArray()
        return StatisticsDto(data.optInt("weekly_points"), data.optInt("image_wins"), data.optInt("circle_wins"), data.optInt("scheduled_this_week"), (0..6).map { daily.optInt(it) })
    }
    /** Uploads profile image bytes into the caller's RLS-protected folder. */
    fun uploadAvatar(token: String, userId: String, bytes: ByteArray, isPng: Boolean): String {
        val path = "$userId/avatar.${if (isPng) "png" else "jpg"}"
        api.request("POST", "/storage/v1/object/contest-avatars/$path", token, bytes,
            mapOf("Content-Type" to if (isPng) "image/png" else "image/jpeg", "x-upsert" to "true"))
        return path
    }
    private fun idBody(id: String) = JSONObject().put("p_game_id", id)
    private fun rpc(token: String, name: String, body: JSONObject) = json("POST", "/rest/v1/rpc/$name", token, body)
    private fun json(method: String, path: String, token: String? = null, body: JSONObject? = null): JSONObject =
        JSONObject(api.request(method, path, token, body?.toString()?.toByteArray()) .ifBlank { "{}" })
    private fun session(data: JSONObject): SessionDto? {
        if (data.optString("access_token").isBlank()) return null
        val user = data.getJSONObject("user")
        return SessionDto(user.getString("id"), user.optString("email"), data.getString("access_token"), data.getString("refresh_token"),
            data.optLong("expires_at", System.currentTimeMillis() / 1000 + data.optLong("expires_in", 3600)))
    }
    private fun profile(data: JSONObject) = ProfileDto(data.getString("id"), data.getString("name"), data.nullable("avatar_url"), data.optString("location"), data.optInt("points"), data.optString("username"))
    private fun game(data: JSONObject) = GameDto(data.getString("id"), data.getString("title"), data.getString("category"), data.getString("starts_at"),
        data.getString("host_id"), data.nullable("guest_id"), data.getString("status"), data.getInt("image_seed"), data.nullable("winner_id"),
        data.nullable("started_at"), data.nullable("ends_at"), data.optInt("winning_points"), data.optString("description"))
    private fun JSONObject.nullable(key: String): String? = if (!has(key) || isNull(key)) null else getString(key)
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
}
