package com.usuallycoding.portfolio.admin

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Today's date (UTC) as `yyyy-MM-dd`. */
@OptIn(ExperimentalTime::class)
fun todayIso(): String = epochDayToIso(Clock.System.now().epochSeconds.floorDiv(86_400L))

/** Converts days since 1970-01-01 to `yyyy-MM-dd` (civil-from-days algorithm). */
fun epochDayToIso(epochDay: Long): String {
    val z = epochDay + 719_468
    val era = z.floorDiv(146_097L)
    val dayOfEra = z - era * 146_097
    val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
    val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
    val mp = (5 * dayOfYear + 2) / 153
    val day = dayOfYear - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    val year = yearOfEra + era * 400 + if (month <= 2) 1 else 0
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

/** True for a real calendar date written as `yyyy-MM-dd`. */
fun isValidIsoDate(value: String): Boolean {
    val match = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").matchEntire(value) ?: return false
    val (year, month, day) = match.destructured.toList().map { it.toInt() }
    if (month !in 1..12 || day < 1) return false
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val daysInMonth = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)[month - 1]
    return day <= daysInMonth
}
