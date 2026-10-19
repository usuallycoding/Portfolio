package com.usuallycoding.portfolio.server.api

import com.usuallycoding.portfolio.ApiPaths
import com.usuallycoding.portfolio.server.domain.DomainError
import com.usuallycoding.portfolio.server.domain.invalid
import com.usuallycoding.portfolio.server.service.AuthService
import com.usuallycoding.portfolio.server.service.ImageService
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray

/** Must be mounted inside the admin `authenticate` block. */
fun Route.adminImageRoutes(images: ImageService) {
    post(ApiPaths.ADMIN_IMAGES) {
        val limit = images.maxUploadBytes
        var bytes: ByteArray? = null
        call.receiveMultipart().forEachPart { part ->
            try {
                if (part is PartData.FileItem && part.name == "file" && bytes == null) {
                    val read = part.provider().readBuffer(limit + 1).readByteArray()
                    if (read.size > limit) {
                        throw DomainError.PayloadTooLarge("Image is larger than $limit bytes")
                    }
                    bytes = read
                }
            } finally {
                part.release()
            }
        }
        val file = bytes ?: invalid("Expected a multipart file part named \"file\"")
        call.respond(HttpStatusCode.Created, images.store(file))
    }
}

/**
 * Serves stored images by file name. Anonymous visitors only get images reachable from public content;
 * the owner (with a valid token) can fetch any image, including staged and private ones.
 */
fun Route.publicImageRoutes(images: ImageService, auth: AuthService) {
    get("${ApiPaths.IMAGES}/{fileName}") {
        val fileName = call.parameters["fileName"].orEmpty()
        val thumbnail = call.request.queryParameters["size"] == "thumb"
        val isOwner = call.hasValidOwnerToken(auth)

        val image = images.open(fileName, thumbnail, isOwner)
        // File names are unique and content never changes, so public responses can be cached for a long time.
        // Owner-only views of non-public images must not be cached by shared caches.
        call.response.header(
            HttpHeaders.CacheControl,
            if (isOwner) "private, no-cache" else "public, max-age=31536000, immutable",
        )
        call.respondBytes(image.bytes, ContentType.parse(image.contentType))
    }
}
