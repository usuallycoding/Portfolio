package com.usuallycoding.portfolio.server

import com.usuallycoding.portfolio.ApiPaths
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import java.nio.file.Files

/**
 * True when the request carries a valid owner token. Unlike the auth plugin, a missing or invalid token
 * is not an error here: public routes treat it as an anonymous visitor.
 */
private suspend fun ApplicationCall.hasValidOwnerToken(context: ServerContext): Boolean {
    val header = request.headers[HttpHeaders.Authorization] ?: return false
    if (!header.startsWith("Bearer ", ignoreCase = true)) return false
    return context.auth.isValid(header.substring(7).trim())
}

private val FILE_NAME = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)$")

fun Route.adminImageRoutes(context: ServerContext) {
    post(ApiPaths.ADMIN_IMAGES) {
        val limit = context.config.maxUploadBytes
        var bytes: ByteArray? = null
        call.receiveMultipart().forEachPart { part ->
            try {
                if (part is PartData.FileItem && part.name == "file" && bytes == null) {
                    val read = part.provider().readBuffer(limit + 1).readByteArray()
                    if (read.size > limit) {
                        throw ApiError(HttpStatusCode.PayloadTooLarge, "Image is larger than $limit bytes")
                    }
                    bytes = read
                }
            } finally {
                part.release()
            }
        }
        val file = bytes ?: badRequest("Expected a multipart file part named \"file\"")
        call.respond(HttpStatusCode.Created, context.images.store(file))
    }
}

/**
 * Serves stored images by file name. Anonymous visitors only get images reachable from public content;
 * the owner (with a valid token) can fetch any image, including staged and private ones.
 */
fun Route.publicImageRoutes(context: ServerContext) {
    get("${ApiPaths.IMAGES}/{fileName}") {
        val fileName = call.parameters["fileName"].orEmpty()
        if (!FILE_NAME.matches(fileName)) notFound()
        val thumbnail = call.request.queryParameters["size"] == "thumb"
        val isOwner = call.hasValidOwnerToken(context)

        val allowed = context.database.transaction { conn ->
            val record = Images.findByFileName(conn, fileName) ?: return@transaction false
            isOwner || Images.isPubliclyReferenced(conn, record.id)
        }
        if (!allowed) notFound()

        val path = context.images.fileFor(fileName, thumbnail)
        val bytes = withContext(Dispatchers.IO) { if (Files.exists(path)) Files.readAllBytes(path) else null } ?: notFound()
        // File names are unique and content never changes, so public responses can be cached for a long time.
        // Owner-only views of non-public images must not be cached by shared caches.
        call.response.header(
            HttpHeaders.CacheControl,
            if (isOwner) "private, no-cache" else "public, max-age=31536000, immutable",
        )
        val format = ImageFormat.fromExtension(fileName.substringAfterLast('.'))!!
        call.respondBytes(bytes, ContentType.parse(format.contentType))
    }
}
