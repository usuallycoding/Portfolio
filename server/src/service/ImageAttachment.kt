package com.usuallycoding.portfolio.server.service

import com.usuallycoding.portfolio.server.domain.AspectRatio
import com.usuallycoding.portfolio.server.domain.ImageRecord
import com.usuallycoding.portfolio.server.domain.ImageRepository
import com.usuallycoding.portfolio.server.domain.invalid

/**
 * Resolves an image id to attach to a field. Returns null for a null id (clearing the field).
 * Throws [com.usuallycoding.portfolio.server.domain.DomainError.Invalid] for an unknown image or, when [ratio] is
 * given, an image outside the tolerance.
 */
fun ImageRepository.requireForAttach(id: String?, ratio: AspectRatio?, tolerance: Double, field: String): ImageRecord? {
    if (id == null) return null
    val record = find(id) ?: invalid("Unknown image for $field")
    if (ratio != null && !ratio.matches(record.width, record.height, tolerance)) {
        invalid("$field must have a ${ratio.label} aspect ratio (got ${record.width}x${record.height})")
    }
    return record
}
