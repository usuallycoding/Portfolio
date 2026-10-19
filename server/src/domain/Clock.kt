package com.usuallycoding.portfolio.server.domain

import java.time.Instant

fun interface Clock {
    fun now(): Instant

    companion object {
        val System = Clock { Instant.now() }
    }
}
