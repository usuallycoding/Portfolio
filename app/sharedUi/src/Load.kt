package com.usuallycoding.portfolio.ui

/** The state of something fetched from the server. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Failed(val message: String) : Load<Nothing>
    data class Loaded<T>(val value: T) : Load<T>
}

fun Throwable.userMessage(): String = when (this) {
    is com.usuallycoding.portfolio.ApiException -> "The server responded with an error ($status)"
    else -> "Could not reach the server. Check your connection and try again."
}
