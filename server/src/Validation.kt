package com.usuallycoding.portfolio.server

import java.time.LocalDate
import java.time.format.DateTimeParseException

object Validation {
    private val ALLOWED_LINK_SCHEMES = setOf("http", "https", "mailto", "tel")

    fun text(value: String, field: String, max: Int, required: Boolean = true): String {
        val trimmed = value.trim()
        if (required && trimmed.isEmpty()) badRequest("$field must not be empty")
        if (trimmed.length > max) badRequest("$field must be at most $max characters")
        return trimmed
    }

    /** Link addresses are opened by client apps, so only a small set of safe schemes is accepted. */
    fun linkAddress(value: String): String {
        val address = text(value, "address", 2000)
        val scheme = address.substringBefore(':', missingDelimiterValue = "").lowercase()
        if (scheme !in ALLOWED_LINK_SCHEMES) {
            badRequest("address must start with one of: ${ALLOWED_LINK_SCHEMES.joinToString { "$it:" }}")
        }
        return address
    }

    fun isoDate(value: String, field: String): String = try {
        LocalDate.parse(value.trim()).toString()
    } catch (e: DateTimeParseException) {
        badRequest("$field must be an ISO date (yyyy-MM-dd)")
    }
}
