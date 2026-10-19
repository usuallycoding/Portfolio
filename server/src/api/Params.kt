package com.usuallycoding.portfolio.server.api

import com.usuallycoding.portfolio.server.domain.notFound
import io.ktor.server.routing.RoutingCall

/** The `{id}` path segment; anything that is not a number is simply a page that does not exist. */
fun RoutingCall.idParam(): Long = parameters["id"]?.toLongOrNull() ?: notFound()
