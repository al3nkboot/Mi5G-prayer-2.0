package com.midroid.prayertimes.domain

import java.time.ZonedDateTime

enum class DayPhase { LAST_THIRD, MORNING_ADHKAR, FORENOON, AFTERNOON, EVENING_ADHKAR, NIGHT }

object Routine {
    fun phase(
        now: ZonedDateTime,
        fajr: ZonedDateTime,
        sunrise: ZonedDateTime,
        dhuhr: ZonedDateTime,
        asr: ZonedDateTime,
        maghrib: ZonedDateTime,
        lastThird: ZonedDateTime,
        nightFajr: ZonedDateTime,
    ): DayPhase {
        val t = now.withZoneSameInstant(fajr.zone)
        return when {
            !t.isBefore(lastThird) && t.isBefore(nightFajr) -> DayPhase.LAST_THIRD
            !t.isBefore(fajr) && t.isBefore(sunrise) -> DayPhase.MORNING_ADHKAR
            !t.isBefore(sunrise) && t.isBefore(dhuhr) -> DayPhase.FORENOON
            !t.isBefore(dhuhr) && t.isBefore(asr) -> DayPhase.AFTERNOON
            !t.isBefore(asr) && t.isBefore(maghrib) -> DayPhase.EVENING_ADHKAR
            else -> DayPhase.NIGHT
        }
    }
}
