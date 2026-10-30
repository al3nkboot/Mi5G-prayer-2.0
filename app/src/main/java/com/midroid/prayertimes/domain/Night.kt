package com.midroid.prayertimes.domain

import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Maghrib-to-Fajr marks. Midnight is the halfway point; the last third starts two-thirds of the way. */
data class NightMarks(
    val maghrib: ZonedDateTime,
    val fajr: ZonedDateTime,
    val midnight: ZonedDateTime,
    val lastThird: ZonedDateTime,
)

object Night {
    fun between(maghrib: ZonedDateTime, fajr: ZonedDateTime): NightMarks {
        val span = Duration.between(maghrib.toInstant(), fajr.toInstant())
        require(!span.isNegative && !span.isZero) { "Fajr must be after Maghrib" }
        return NightMarks(maghrib, fajr, at(maghrib, span, 1, 2), at(maghrib, span, 2, 3))
    }

    /** The night in progress, or tonight once today's Fajr has passed. */
    fun around(place: Place, now: ZonedDateTime, s: CalcSettings): NightMarks {
        val local = now.withZoneSameInstant(place.zoneId)
        val today = PrayerEngine.day(place, local.toLocalDate(), s)
        return if (local.isBefore(today[Prayer.FAJR])) {
            val yesterday = PrayerEngine.day(place, local.toLocalDate().minusDays(1), s)
            between(yesterday[Prayer.MAGHRIB], today[Prayer.FAJR])
        } else {
            val tomorrow = PrayerEngine.day(place, local.toLocalDate().plusDays(1), s)
            between(today[Prayer.MAGHRIB], tomorrow[Prayer.FAJR])
        }
    }

    private fun at(start: ZonedDateTime, span: Duration, part: Long, of: Long): ZonedDateTime {
        val point = start.toInstant().plusNanos(span.toNanos() * part / of)
        val z = point.atZone(start.zone)
        val up = if (z.second >= 30) 1L else 0L
        return z.truncatedTo(ChronoUnit.MINUTES).plusMinutes(up)
    }
}
