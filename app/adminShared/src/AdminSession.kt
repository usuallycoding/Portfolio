package com.usuallycoding.portfolio.admin

import androidx.compose.runtime.staticCompositionLocalOf
import com.usuallycoding.portfolio.ApiException
import com.usuallycoding.portfolio.PortfolioApi
import com.usuallycoding.portfolio.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The owner's login state and the API client that uses it. Any call that comes back 401 ends the session,
 * which sends the app back to the login screen.
 */
class AdminSession(serverUrl: String, httpClient: HttpClient? = null) {
    private val _token = MutableStateFlow<String?>(null)

    /** Also used to load images, so staged and private images can be previewed with the owner's token. */
    val httpClient: HttpClient = httpClient ?: createHttpClient {
        install(DefaultRequest) { _token.value?.let { header(HttpHeaders.Authorization, "Bearer $it") } }
    }

    val api = PortfolioApi(serverUrl, this.httpClient)

    private val _loggedIn = MutableStateFlow(false)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    /** Throws [ApiException] (401 wrong password, 429 throttled) or a network error. */
    suspend fun login(password: String) {
        val response = api.login(password)
        _token.value = response.token
        _loggedIn.value = true
    }

    fun logout() {
        api.token = null
        _token.value = null
        _loggedIn.value = false
    }

    /** Runs an API call; a 401 logs out before the error is rethrown. */
    suspend fun <T> call(block: suspend PortfolioApi.() -> T): T = try {
        api.block()
    } catch (e: ApiException) {
        if (e.status == 401) logout()
        throw e
    }

    fun imageUrl(fileName: String, thumbnail: Boolean) = api.imageUrl(fileName, thumbnail)
}

val LocalAdminSession = staticCompositionLocalOf<AdminSession> { error("No AdminSession provided") }

fun Throwable.adminMessage(): String = when {
    this is NotAnImageException -> message.orEmpty()
    this is ApiException && status == 401 -> "Your session expired. Please log in again."
    this is ApiException && status == 429 -> "Too many attempts. Try again in a few minutes."
    this is ApiException && message.contains("\"message\"") ->
        Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(message)?.groupValues?.get(1) ?: "The server rejected the request ($status)"
    this is ApiException -> "The server responded with an error ($status)"
    else -> "Could not reach the server. Check your connection and try again."
}
