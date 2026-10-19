package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.SocialLinkDto
import com.usuallycoding.portfolio.SocialLinkInput
import com.usuallycoding.portfolio.server.domain.AspectRatio
import com.usuallycoding.portfolio.server.domain.LinkFields
import com.usuallycoding.portfolio.server.domain.Repositories
import com.usuallycoding.portfolio.server.domain.Transactor
import com.usuallycoding.portfolio.server.domain.Validation
import com.usuallycoding.portfolio.server.domain.notFound

class LinkService(private val transactor: Transactor, private val ratioTolerance: Double) {
    suspend fun list(publicOnly: Boolean): List<SocialLinkDto> = transactor.transaction { links.list(publicOnly) }

    suspend fun create(input: SocialLinkInput): SocialLinkDto = transactor.transaction { links.create(fields(input)) }

    suspend fun update(id: Long, input: SocialLinkInput): SocialLinkDto = transactor.transaction {
        links.get(id) ?: notFound("Link not found")
        links.update(id, fields(input))
    }

    suspend fun delete(id: Long) = transactor.transaction {
        if (!links.delete(id)) notFound("Link not found")
    }

    suspend fun reorder(ids: List<Long>): List<SocialLinkDto> = transactor.transaction {
        requireExactOrder(ids, links.ids())
        links.reorder(ids)
        links.list(publicOnly = false)
    }

    private fun Repositories.fields(input: SocialLinkInput): LinkFields {
        val name = Validation.text(input.name, "name", 100)
        val address = Validation.linkAddress(input.address)
        val icon = images.requireForAttach(input.iconId, AspectRatio.SQUARE, ratioTolerance, "icon")
        return LinkFields(name, address, input.isPublic, icon?.id)
    }
}
