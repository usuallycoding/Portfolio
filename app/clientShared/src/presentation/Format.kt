package com.usuallycoding.portfolio.client.presentation

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** Formats an ISO date (`2026-01-31`) as `31 Jan 2026`. Anything unparseable is returned unchanged. */
fun formatDate(iso: String): String {
    val parts = iso.split('-')
    if (parts.size != 3) return iso
    val year = parts[0].toIntOrNull() ?: return iso
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return iso
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return iso
    return "$day ${MONTHS[month - 1]} $year"
}
