package com.usuallycoding.portfolio.server.domain

/** Checks a candidate against the one owner password, without ever exposing or storing it in clear. */
fun interface PasswordVerifier {
    fun matches(candidate: String): Boolean
}
