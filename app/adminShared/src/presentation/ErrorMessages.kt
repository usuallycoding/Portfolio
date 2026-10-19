package com.usuallycoding.portfolio.admin.presentation

import com.usuallycoding.portfolio.ApiException

/** What the owner is told when a call fails. */
fun Throwable.adminMessage(): String = when {
    this is NotAnImageException -> message.orEmpty()
    this is ApiException && status == 401 -> "Your session expired. Please log in again."
    this is ApiException && status == 429 -> "Too many attempts. Try again in a few minutes."
    this is ApiException && message.contains("\"message\"") ->
        Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(message)?.groupValues?.get(1) ?: "The server rejected the request ($status)"
    this is ApiException -> "The server responded with an error ($status)"
    else -> "Could not reach the server. Check your connection and try again."
}
