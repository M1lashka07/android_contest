package ru.professionals.network

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/** HTTP request description independent of the underlying transport. Created: 30-09-2026. Author: participant number pending. */
data class HttpRequest(val method: String, val path: String, val headers: Map<String, String>, val body: ByteArray? = null)
/** Unparsed HTTP response. Created: 30-09-2026. Author: participant number pending. */
data class HttpResponse(val status: Int, val body: String)
/** Replaceable synchronous transport; consumers call it on an IO dispatcher. Created: 30-09-2026. Author: participant number pending. */
fun interface HttpClient { fun execute(request: HttpRequest): HttpResponse }
/** Logging boundary avoids an Android dependency and never receives secrets. Created: 30-09-2026. Author: participant number pending. */
fun interface NetworkLogger { fun log(level: String, tag: String, message: String) }
/** Structured transport/server error for presentation mapping. Created: 30-09-2026. Author: participant number pending. */
class ApiException(val status: Int, message: String, cause: Throwable? = null) : IOException(message, cause)

/** JDK/Android HTTP implementation with bounded timeouts and deterministic cleanup. Created: 30-09-2026. Author: participant number pending. */
class UrlConnectionClient(private val baseUrl: String, private val logger: NetworkLogger = NetworkLogger { _, _, _ -> }) : HttpClient {
    /** Executes one request; redirects are disabled to avoid forwarding bearer tokens to another host. */
    override fun execute(request: HttpRequest): HttpResponse {
        if (baseUrl.isBlank()) throw ApiException(0, "Supabase не настроен: укажите SUPABASE_URL и SUPABASE_PUBLISHABLE_KEY")
        logger.log("INFO", "Network", "${request.method} ${request.path.substringBefore('?')}")
        val connection = URL(baseUrl.trimEnd('/') + request.path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = request.method
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = false
            request.headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            request.body?.let { payload ->
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(payload.size)
                connection.outputStream.use { it.write(payload) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            logger.log(if (status in 200..299) "DEBUG" else "ERROR", "Network", "HTTP $status ${request.path.substringBefore('?')}")
            return HttpResponse(status, body)
        } catch (error: IOException) {
            logger.log("ERROR", "Network", "${error.javaClass.simpleName}: request failed")
            throw ApiException(0, "Нет соединения с сервером. Проверьте интернет и повторите.", error)
        } finally {
            connection.disconnect()
        }
    }
}

/** Centralized response and error handling. Created: 30-09-2026. Author: participant number pending. */
class ApiExecutor(private val http: HttpClient, private val publishableKey: String) {
    /** Sends JSON or binary content and rejects every non-success status. */
    fun request(method: String, path: String, token: String? = null, body: ByteArray? = null,
                extraHeaders: Map<String, String> = emptyMap()): String {
        if (publishableKey.isBlank()) throw ApiException(0, "Supabase не настроен: укажите SUPABASE_URL и SUPABASE_PUBLISHABLE_KEY")
        val headers = mutableMapOf("apikey" to publishableKey, "Content-Type" to "application/json", "Accept" to "application/json")
        token?.let { headers["Authorization"] = "Bearer $it" }
        headers.putAll(extraHeaders)
        val response = http.execute(HttpRequest(method, path, headers, body))
        if (response.status !in 200..299) {
            val error = runCatching { JSONObject(response.body) }.getOrNull()
            val message = listOf("message", "error_description", "msg", "error").firstNotNullOfOrNull { key ->
                error?.optString(key)?.takeIf { it.isNotBlank() }
            } ?: "Сервер вернул ошибку ${response.status}"
            throw ApiException(response.status, message)
        }
        return response.body
    }
}
