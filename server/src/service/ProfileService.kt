package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.ProfileDto
import com.usuallycoding.portfolio.ProfileInput
import com.usuallycoding.portfolio.server.domain.AspectRatio
import com.usuallycoding.portfolio.server.domain.ProfileFields
import com.usuallycoding.portfolio.server.domain.Transactor
import com.usuallycoding.portfolio.server.domain.Validation

const val PLACEHOLDER_NAME = "Your name"
const val PLACEHOLDER_DESCRIPTION = "This portfolio has not been set up yet."

class ProfileService(private val transactor: Transactor, private val ratioTolerance: Double) {
    /** The profile, or a placeholder until the owner has saved one. */
    suspend fun get(): ProfileDto =
        transactor.transaction { profile.get() } ?: ProfileDto(PLACEHOLDER_NAME, PLACEHOLDER_DESCRIPTION)

    suspend fun update(input: ProfileInput): ProfileDto = transactor.transaction {
        val name = Validation.text(input.name, "name", 200)
        val description = Validation.text(input.description, "description", 5000, required = false)
        val picture = images.requireForAttach(input.profileImageId, AspectRatio.SQUARE, ratioTolerance, "profile picture")
        val cover = images.requireForAttach(input.coverImageId, AspectRatio.COVER, ratioTolerance, "cover image")
        profile.save(ProfileFields(name, description, picture?.id, cover?.id))
        profile.get()!!
    }
}
