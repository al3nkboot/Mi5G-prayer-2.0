package com.midroid.prayertimes

import com.midroid.prayertimes.domain.DayPhase
import com.midroid.prayertimes.domain.Night
import com.midroid.prayertimes.domain.Routine
import com.midroid.prayertimes.domain.SunPosition
import com.midroid.prayertimes.domain.Zakat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

class NightAndSunTest {
    private val z = ZoneId.of("Asia/Riyadh")

    @Test fun midnightAndLastThirdRoundToTheNearestMinute() {
        val maghrib = ZonedDateTime.of(2026, 3, 1, 18, 34, 0, 0, z)
        val fajr = ZonedDateTime.of(2026, 3, 2, 5, 3, 0, 0, z)
        val n = Night.between(maghrib, fajr)
        assertEquals(23, n.midnight.hour)
        assertEquals(49, n.midnight.minute)
        assertEquals(1, n.lastThird.hour)
        assertEquals(33, n.lastThird.minute)
        assertEquals(2, n.lastThird.dayOfMonth)
    }

    @Test fun sunAzimuthMatchesNoaaPin() {
        val at = ZonedDateTime.of(2026, 10, 16, 14, 25, 0, 0, ZoneId.of("Asia/Muscat"))
        val sun = SunPosition.at(23.588, 58.383, at)
        assertEquals(40.43, sun.altitudeDeg, 0.05)
        assertEquals(233.45, sun.azimuthDeg, 0.05)
        assertTrue(sun.up)
        assertEquals(53.45, sun.shadowAzimuthDeg, 0.05)
    }

    @Test fun sunIsDownAtNight() {
        val at = ZonedDateTime.of(2026, 10, 16, 0, 0, 0, 0, ZoneId.of("Asia/Muscat"))
        assertTrue(SunPosition.at(23.588, 58.383, at).altitudeDeg < 0)
    }

    @Test fun routineFollowsTheNightThenTheDay() {
        val fajr = ZonedDateTime.of(2026, 10, 16, 5, 0, 0, 0, z)
        val sunrise = fajr.plusHours(1)
        val dhuhr = fajr.plusHours(7)
        val asr = fajr.plusHours(10)
        val maghrib = fajr.plusHours(13)
        val lastThird = fajr.plusHours(20)
        val nightFajr = fajr.plusDays(1)
        assertEquals(DayPhase.AFTERNOON, Routine.phase(fajr.plusHours(8), fajr, sunrise, dhuhr, asr, maghrib, lastThird, nightFajr))
        assertEquals(DayPhase.EVENING_ADHKAR, Routine.phase(asr.plusMinutes(10), fajr, sunrise, dhuhr, asr, maghrib, lastThird, nightFajr))
        assertEquals(DayPhase.LAST_THIRD, Routine.phase(lastThird.plusMinutes(5), fajr, sunrise, dhuhr, asr, maghrib, lastThird, nightFajr))
        assertEquals(DayPhase.MORNING_ADHKAR, Routine.phase(fajr.plusMinutes(20), fajr, sunrise, dhuhr, asr, maghrib, lastThird, nightFajr))
    }

    @Test fun zakatIsTwoAndAHalfPercentAboveNisabOnly() {
        assertEquals(25.0, Zakat.due(1000.0, 500.0)!!, 0.001)
        assertEquals(0.0, Zakat.due(100.0, 500.0)!!, 0.001)
        assertNull(Zakat.due(-1.0, 500.0))
        assertNull(Zakat.due(1000.0, 0.0))
        assertTrue(abs(Zakat.RATE - 0.025) < 1e-9)
    }
}
