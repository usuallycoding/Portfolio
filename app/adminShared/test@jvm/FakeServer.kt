package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.ImageDto
import com.usuallycoding.portfolio.LoginRequest
import com.usuallycoding.portfolio.OrderRequest
import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProfileInput
import com.usuallycoding.portfolio.ProjectDto
import com.usuallycoding.portfolio.ProjectInput
import com.usuallycoding.portfolio.ProjectSummaryDto
import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.SocialLinkInput
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** An in-memory stand-in for the portfolio server's admin API, good enough to drive the admin screens. */
class FakeServer(private val password: String = "secret") {
    private val json = Json { ignoreUnknownKeys = true }

    var profile = ProfileDto("Your name", "Not set up yet")
    val links = mutableListOf<SocialLinkDto>()
    val projects = mutableListOf<ProjectDto>()
    val images = mutableMapOf<String, ImageDto>()
    val requests = mutableListOf<String>()

    /** Dimensions given to the next uploaded image(s); lets tests fake ratio-correct uploads. */
    var nextUploadSize = 800 to 800
    var uploadCount = 0
        private set
    var failUploadsWhere: (Int) -> Boolean = { false }
    var rejectToken = false
    var uploadDelay: suspend () -> Unit = {}
    var activeUploads = 0
        private set
    var maxConcurrentUploads = 0
        private set
    var nextId = 1000L

    fun addImage(width: Int = 800, height: Int = 800): ImageDto {
        val id = "img-${images.size + 1}"
        return ImageDto(id, "$id.jpg", width, height).also { images[id] = it }
    }

    private inline fun <reified T> body(request: HttpRequestData): T =
        json.decodeFromString((request.body as TextContent).text)

    private fun MockRequestHandleScope.ok(value: String, status: HttpStatusCode = HttpStatusCode.OK) = json(value, status)
    private inline fun <reified T> MockRequestHandleScope.ok(value: T, status: HttpStatusCode = HttpStatusCode.OK) =
        json(this@FakeServer.json.encodeToString(value), status)

    private fun summary(p: ProjectDto) = ProjectSummaryDto(p.id, p.title, p.createdAt, p.isPublic, p.summary, p.thumbnail)

    private fun image(id: String?) = id?.let { images[it] ?: error("unknown image $it") }

    suspend fun handle(scope: MockRequestHandleScope, request: HttpRequestData): HttpResponseData = with(scope) {
        val path = request.url.encodedPath
        val method = request.method
        requests += "$method $path"

        if (path == "/admin/login") {
            return if (body<LoginRequest>(request).password == password) ok("""{"token":"tok","expiresAtEpochSeconds":1}""")
            else status(HttpStatusCode.Unauthorized)
        }
        if (rejectToken) return status(HttpStatusCode.Unauthorized)

        val segments = path.trim('/').split('/')
        return when {
            path == "/admin/profile" && method == HttpMethod.Get -> ok(profile)
            path == "/admin/profile" && method == HttpMethod.Put -> {
                val input = body<ProfileInput>(request)
                profile = ProfileDto(input.name, input.description, image(input.profileImageId), image(input.coverImageId))
                ok(profile)
            }

            path == "/admin/links" && method == HttpMethod.Get -> ok(links.toList())
            path == "/admin/links" && method == HttpMethod.Post -> {
                val input = body<SocialLinkInput>(request)
                val link = SocialLinkDto(nextId++, input.name, input.address, input.isPublic, image(input.iconId))
                links += link
                ok(link, HttpStatusCode.Created)
            }
            path == "/admin/links/order" && method == HttpMethod.Put -> {
                val ids = body<OrderRequest>(request).ids
                if (ids.sorted() != links.map { it.id }.sorted()) return status(HttpStatusCode.BadRequest)
                val byId = links.associateBy { it.id }
                links.clear(); links += ids.map { byId.getValue(it) }
                ok(links.toList())
            }
            segments.size == 3 && segments[1] == "links" && method == HttpMethod.Put -> {
                val id = segments[2].toLong()
                val input = body<SocialLinkInput>(request)
                val index = links.indexOfFirst { it.id == id }
                if (index < 0) return status(HttpStatusCode.NotFound)
                links[index] = SocialLinkDto(id, input.name, input.address, input.isPublic, image(input.iconId))
                ok(links[index])
            }
            segments.size == 3 && segments[1] == "links" && method == HttpMethod.Delete -> {
                links.removeAll { it.id == segments[2].toLong() }
                respond("", HttpStatusCode.NoContent)
            }

            path == "/admin/projects" && method == HttpMethod.Get -> ok(projects.map(::summary))
            path == "/admin/projects" && method == HttpMethod.Post -> {
                val input = body<ProjectInput>(request)
                val project = ProjectDto(nextId++, input.title, input.createdAt, input.isPublic, input.summary,
                    image(input.thumbnailId), input.imageIds.map { image(it)!! })
                projects += project
                ok(project, HttpStatusCode.Created)
            }
            path == "/admin/projects/order" && method == HttpMethod.Put -> {
                val ids = body<OrderRequest>(request).ids
                if (ids.sorted() != projects.map { it.id }.sorted()) return status(HttpStatusCode.BadRequest)
                val byId = projects.associateBy { it.id }
                projects.clear(); projects += ids.map { byId.getValue(it) }
                ok(projects.map(::summary))
            }
            segments.size == 3 && segments[1] == "projects" && method == HttpMethod.Get ->
                projects.firstOrNull { it.id == segments[2].toLong() }?.let { ok(it) } ?: status(HttpStatusCode.NotFound)
            segments.size == 3 && segments[1] == "projects" && method == HttpMethod.Put -> {
                val id = segments[2].toLong()
                val input = body<ProjectInput>(request)
                val index = projects.indexOfFirst { it.id == id }
                if (index < 0) return status(HttpStatusCode.NotFound)
                if (input.title.isBlank()) return status(HttpStatusCode.BadRequest)
                projects[index] = ProjectDto(id, input.title, input.createdAt, input.isPublic, input.summary,
                    image(input.thumbnailId), input.imageIds.map { image(it)!! })
                ok(projects[index])
            }
            segments.size == 3 && segments[1] == "projects" && method == HttpMethod.Delete -> {
                projects.removeAll { it.id == segments[2].toLong() }
                respond("", HttpStatusCode.NoContent)
            }

            path == "/admin/images" && method == HttpMethod.Post -> {
                val number = ++uploadCount
                activeUploads++
                maxConcurrentUploads = maxOf(maxConcurrentUploads, activeUploads)
                try {
                    uploadDelay()
                } finally {
                    activeUploads--
                }
                if (failUploadsWhere(number)) return status(HttpStatusCode.InternalServerError)
                val (w, h) = nextUploadSize
                ok(addImage(w, h), HttpStatusCode.Created)
            }
            else -> status(HttpStatusCode.NotFound)
        }
    }

    fun session(): AdminSession = testSession { request -> handle(this, request) }
}
