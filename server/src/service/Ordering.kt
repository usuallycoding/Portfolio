package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.server.domain.invalid

/** A new order must list every existing id exactly once. */
internal fun requireExactOrder(ids: List<Long>, existing: Set<Long>) {
    if (ids.size != ids.toSet().size || ids.toSet() != existing) {
        invalid("The order must list every existing id exactly once")
    }
}
