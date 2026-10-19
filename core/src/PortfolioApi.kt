package com.usuallycoding.portfolio

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiException(val status: Int, override val message: String) : Exception(message)

fun createHttpClient(configure: HttpClientConfig<*>.() -> Unit = {}): HttpClient = HttpClient {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    configure()
}

/**
 * Client for the portfolio server. Public calls need no token; admin calls need [token],
 * which is set after [login]. Non-2xx responses throw [ApiException].
 */
class PortfolioApi(
    private val baseUrl: String,
    private val client: HttpClient = createHttpClient(),
) {
    var token: String? = null

    private val base = baseUrl.trimEnd('/')

    fun imageUrl(fileName: String, thumbnail: Boolean = false): String =
        "$base${ApiPaths.IMAGES}/$fileName" + if (thumbnail) "?size=thumb" else ""

    // --- public ---

    suspend fun profile(): ProfileDto = get(ApiPaths.PROFILE)
    suspend fun links(): List<SocialLinkDto> = get(ApiPaths.LINKS)
    suspend fun projects(): List<ProjectSummaryDto> = get(ApiPaths.PROJECTS)
    suspend fun project(id: Long): ProjectDto = get("${ApiPaths.PROJECTS}/$id")

    // --- admin ---

    suspend fun login(password: String): LoginResponse {
        val response = client.post("$base${ApiPaths.ADMIN_LOGIN}") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(password))
        }
        val result: LoginResponse = response.checked().body()
        token = result.token
        return result
    }

    suspend fun adminProfile(): ProfileDto = get(ApiPaths.ADMIN_PROFILE, auth = true)
    suspend fun updateProfile(input: ProfileInput): ProfileDto = send(ApiPaths.ADMIN_PROFILE, input, put = true)

    suspend fun adminLinks(): List<SocialLinkDto> = get(ApiPaths.ADMIN_LINKS, auth = true)
    suspend fun createLink(input: SocialLinkInput): SocialLinkDto = send(ApiPaths.ADMIN_LINKS, input)
    suspend fun updateLink(id: Long, input: SocialLinkInput): SocialLinkDto =
        send("${ApiPaths.ADMIN_LINKS}/$id", input, put = true)
    suspend fun deleteLink(id: Long) = delete("${ApiPaths.ADMIN_LINKS}/$id")
    suspend fun reorderLinks(ids: List<Long>): List<SocialLinkDto> =
        send("${ApiPaths.ADMIN_LINKS}/order", OrderRequest(ids), put = true)

    suspend fun adminProjects(): List<ProjectSummaryDto> = get(ApiPaths.ADMIN_PROJECTS, auth = true)
    suspend fun adminProject(id: Long): ProjectDto = get("${ApiPaths.ADMIN_PROJECTS}/$id", auth = true)
    suspend fun createProject(input: ProjectInput): ProjectDto = send(ApiPaths.ADMIN_PROJECTS, input)
    suspend fun updateProject(id: Long, input: ProjectInput): ProjectDto =
        send("${ApiPaths.ADMIN_PROJECTS}/$id", input, put = true)
    suspend fun deleteProject(id: Long) = delete("${ApiPaths.ADMIN_PROJECTS}/$id")
    suspend fun reorderProjects(ids: List<Long>): List<ProjectSummaryDto> =
        send("${ApiPaths.ADMIN_PROJECTS}/order", OrderRequest(ids), put = true)

    /** Uploads one image file. [contentType] must be `image/jpeg` or `image/png`. */
    suspend fun uploadImage(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
        onProgress: (sent: Long, total: Long?) -> Unit = { _, _ -> },
    ): ImageDto {
        val response = client.submitFormWithBinaryData(
            url = "$base${ApiPaths.ADMIN_IMAGES}",
            formData = formData {
                append("file", bytes, Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            },
        ) {
            authorize()
            onUpload { sent, total -> onProgress(sent, total) }
        }
        return response.checked().body()
    }

    private fun HttpRequestBuilder.authorize() {
        token?.let { bearerAuth(it) }
    }

    private suspend inline fun <reified T> get(path: String, auth: Boolean = false): T =
        client.get("$base$path") { if (auth) authorize() }.checked().body()

    private suspend inline fun <reified T> send(path: String, payload: Any, put: Boolean = false): T {
        val block: HttpRequestBuilder.() -> Unit = {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        val response = if (put) client.put("$base$path", block) else client.post("$base$path", block)
        return response.checked().body()
    }

    private suspend fun delete(path: String) {
        client.delete("$base$path") { authorize() }.checked()
    }

    private suspend fun HttpResponse.checked(): HttpResponse {
        if (!status.isSuccess()) throw ApiException(status.value, runCatching { bodyAsText() }.getOrDefault(""))
        return this
    }
}
