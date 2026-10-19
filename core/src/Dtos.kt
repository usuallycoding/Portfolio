package com.usuallycoding.portfolio

import kotlinx.serialization.Serializable

/** A stored image. `fileName` is the server-assigned unique name used to build image URLs. */
@Serializable
data class ImageDto(
    val id: String,
    val fileName: String,
    val width: Int,
    val height: Int,
)

@Serializable
data class ProfileDto(
    val name: String,
    val description: String,
    val profileImage: ImageDto? = null,
    val coverImage: ImageDto? = null,
)

@Serializable
data class ProfileInput(
    val name: String,
    val description: String,
    val profileImageId: String? = null,
    val coverImageId: String? = null,
)

@Serializable
data class SocialLinkDto(
    val id: Long,
    val name: String,
    val address: String,
    val isPublic: Boolean,
    val icon: ImageDto? = null,
)

@Serializable
data class SocialLinkInput(
    val name: String,
    val address: String,
    val isPublic: Boolean,
    val iconId: String? = null,
)

/** Project without its gallery, used in lists. `createdAt` is an ISO-8601 date (yyyy-MM-dd). */
@Serializable
data class ProjectSummaryDto(
    val id: Long,
    val title: String,
    val createdAt: String,
    val isPublic: Boolean,
    val summary: String,
    val thumbnail: ImageDto? = null,
)

@Serializable
data class ProjectDto(
    val id: Long,
    val title: String,
    val createdAt: String,
    val isPublic: Boolean,
    val summary: String,
    val thumbnail: ImageDto? = null,
    val images: List<ImageDto> = emptyList(),
)

/** Saving a project sets its thumbnail and its ordered images atomically. */
@Serializable
data class ProjectInput(
    val title: String,
    val createdAt: String,
    val isPublic: Boolean,
    val summary: String,
    val thumbnailId: String? = null,
    val imageIds: List<String> = emptyList(),
)

/** The complete, desired order of ids. Partial or duplicated lists are rejected. */
@Serializable
data class OrderRequest(val ids: List<Long>)

@Serializable
data class LoginRequest(val password: String)

@Serializable
data class LoginResponse(val token: String, val expiresAtEpochSeconds: Long)

@Serializable
data class ErrorDto(val message: String)
