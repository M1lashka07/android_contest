package ru.professionals.network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Contract tests cover all eleven competition API operations plus lifecycle/error cases. */
class SupabaseClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: SupabaseClient
    private val token = "test-access-token"
    private val profile = """{"id":"u1","name":"Anna","username":"anna","location":"Moscow","points":120,"avatar_url":null}"""
    private val game = """{"id":"g1","title":"Morning match","category":"IMAGE","starts_at":"2027-03-01T09:00:00Z","ends_at":null,"host_id":"u1","guest_id":null,"status":"SCHEDULED","image_seed":42,"winner_id":null,"started_at":null,"winning_points":100,"description":"Puzzle"}"""
    private val session = """{"access_token":"a","refresh_token":"r","expires_at":1999999999,"user":{"id":"u1","email":"a@b.ru"}}"""

    @Before fun prepare() {
        server = MockWebServer().apply { start() }
        client = SupabaseClient(ApiExecutor(UrlConnectionClient(server.url("/").toString()), "publishable-test"))
    }
    @After fun finish() { server.shutdown() }
    private fun respond(body: String, code: Int = 200) { server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type","application/json").setBody(body)) }
    private fun request(method: String, path: String, authenticated: Boolean = true): JSONObject {
        val request = server.takeRequest()
        assertEquals(method, request.method)
        assertEquals(path, request.path)
        assertEquals("publishable-test", request.getHeader("apikey"))
        assertEquals(if(authenticated) "Bearer $token" else null, request.getHeader("Authorization"))
        return request.body.readUtf8().let { if(it.isBlank()) JSONObject() else JSONObject(it) }
    }
    @Test fun signInUsesPasswordGrantAndMapsSession() {
        respond(session)
        val result = client.signIn("a@b.ru","password1")
        assertEquals("u1",result.userId); assertEquals("r",result.refreshToken)
        val payload = request("POST","/auth/v1/token?grant_type=password",false)
        assertEquals("a@b.ru",payload.getString("email")); assertEquals("password1",payload.getString("password"))
    }
    @Test fun signUpPreservesProfileMetadata() {
        respond(session)
        assertNotNull(client.signUp("Anna","a@b.ru","password1","+7000","anna"))
        val data = request("POST","/auth/v1/signup",false).getJSONObject("data")
        assertEquals("Anna",data.getString("name")); assertEquals("anna",data.getString("username"))
    }
    @Test fun confirmedEmailSignupDoesNotInventSession() {
        respond("""{"user":{"id":"u1"}}""")
        assertNull(client.signUp("Anna","a@b.ru","password1"))
    }
    @Test fun profileUpdateOnlySendsEditableColumns() {
        respond(profile)
        assertEquals("Anna",client.updateProfile(token,"u1","Anna","Moscow").name)
        val payload = request("POST","/rest/v1/rpc/contest_update_profile")
        assertEquals("Moscow",payload.getString("p_location")); assertFalse(payload.has("points")); assertFalse(payload.has("id"))
    }
    @Test fun getProfileDeserializesAndUsesCorrectFilter() {
        respond("[$profile]")
        val result = client.getProfile(token,"u1")
        assertEquals("u1",result.id); assertEquals(120,result.points); assertNull(result.avatarUrl)
        request("GET","/rest/v1/contest_profiles?id=eq.u1&select=*")
    }
    @Test fun logoutSupportsNoContent() {
        server.enqueue(MockResponse().setResponseCode(204))
        client.signOut(token)
        request("POST","/auth/v1/logout")
    }
    @Test fun createGameCannotChooseHostSeedOrWinner() {
        respond(game)
        assertEquals(42,client.createGame(token,"Morning match","IMAGE","2027-03-01T09:00:00Z",null,100,"Puzzle").imageSeed)
        val payload = request("POST","/rest/v1/rpc/contest_create_game")
        assertEquals("IMAGE",payload.getString("p_category")); assertFalse(payload.has("host_id")); assertFalse(payload.has("image_seed")); assertFalse(payload.has("winner_id"))
    }
    @Test fun listGamesDeserializesAllRowsInPage() {
        respond("[$game,$game]")
        assertEquals(2,client.listGames(token).size)
        request("GET","/rest/v1/contest_games?select=*&order=starts_at.desc&limit=100&offset=0")
    }
    @Test fun getGameUsesHeartbeatRpc() {
        respond(game)
        assertEquals("g1",client.getGame(token,"g1").id)
        assertEquals("g1",request("POST","/rest/v1/rpc/contest_get_game").getString("p_game_id"))
    }
    @Test fun joinGameNeverUpdatesGuestDirectly() {
        respond(game.replace("\"guest_id\":null","\"guest_id\":\"u2\""))
        assertEquals("u2",client.joinGame(token,"g1").guestId)
        assertEquals(1,request("POST","/rest/v1/rpc/contest_join_game").length())
    }
    @Test fun statisticsParsesWeeklyAndCategoryCounts() {
        respond("""{"weekly_points":100,"image_wins":2,"circle_wins":3,"scheduled_this_week":4,"daily_points":[0,0,100,0,0,0,0]}""")
        val result = client.statistics(token)
        assertEquals(100,result.weeklyPoints); assertEquals(2,result.imageWins); assertEquals(3,result.circleWins); assertEquals(4,result.scheduledThisWeek)
        assertEquals(100,result.dailyPoints[2]); request("POST","/rest/v1/rpc/contest_statistics")
    }
    @Test fun resultSendsReplayRatherThanClientElapsedTimeOrWinner() {
        respond(game)
        client.saveResult(token,"g1",listOf(5,4,3,2,1))
        val payload = request("POST","/rest/v1/rpc/contest_save_result")
        assertEquals(5,payload.getJSONArray("p_moves").length()); assertFalse(payload.has("elapsed_ms")); assertFalse(payload.has("winner_id"))
    }
    @Test fun startAndForfeitUseDedicatedAtomicOperations() {
        respond(game); respond(game)
        client.startGame(token,"g1"); client.forfeitGame(token,"g1")
        request("POST","/rest/v1/rpc/contest_start_game"); request("POST","/rest/v1/rpc/contest_forfeit_game")
    }
    @Test fun refreshDoesNotExposeBearerToken() {
        respond(session)
        assertEquals("a",client.refresh("r").accessToken)
        assertEquals("r",request("POST","/auth/v1/token?grant_type=refresh_token",false).getString("refresh_token"))
    }
    @Test fun errorBodyIsReturnedAsReadableTypedFailure() {
        respond("""{"message":"В этой игре нет свободного места"}""",409)
        val error = runCatching { client.joinGame(token,"g1") }.exceptionOrNull()
        assertTrue(error is ApiException); assertEquals(409,(error as ApiException).status)
        assertEquals("В этой игре нет свободного места",error.message)
    }
    @Test fun missingProfileIsTyped404() {
        respond("[]")
        val error = runCatching { client.getProfile(token,"missing") }.exceptionOrNull()
        assertEquals(404,(error as ApiException).status)
    }
    @Test fun disconnectedServerReturnsNetworkFailure() {
        server.shutdown()
        val error = runCatching { client.getProfile(token,"u1") }.exceptionOrNull()
        assertTrue(error is ApiException); assertEquals(0,(error as ApiException).status)
    }
}
