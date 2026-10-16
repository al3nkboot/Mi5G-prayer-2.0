package com.midroid.prayertimes

import com.midroid.prayertimes.data.AppSettings
import com.midroid.prayertimes.data.Cities
import com.midroid.prayertimes.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleTest {
    private val p = Cities.default; private val z = ZoneId.of("Asia/Muscat"); private val s = CalcSettings()
    private val defaults = AppSettings.DEFAULT_ALERTS

    @Test fun rollingWindowSkipsOffAndPast() {
        val now = ZonedDateTime.of(2026, 10, 16, 14, 0, 0, 0, z)
        val list = Schedule.upcoming(p, now, s, defaults, 0)
        assertTrue(list.all { it.at.isAfter(now) && !it.at.isAfter(now.plusDays(2)) })
        assertTrue(list.none { it.prayer == Prayer.SUNRISE })
        // today: Asr, Maghrib, Isha; +2 full days of 5 prayers until 14:00 day+2 (Fajr, Dhuhr of day+2 included)
        assertEquals(listOf(Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA), list.take(3).map { it.prayer })
        assertEquals(3 + 5 + 2, list.size)
        assertEquals(list.size, list.map { it.requestCode }.toSet().size)
        assertTrue(list.all { it.requestCode in 0 until Schedule.MAX_CODES })
        assertEquals(list.sortedBy { it.at }, list)
    }

    @Test fun afterIshaStartsWithTomorrowFajr() {
        val list = Schedule.upcoming(p, ZonedDateTime.of(2026, 10, 16, 23, 0, 0, 0, z), s, defaults, 0)
        assertEquals(Prayer.FAJR, list.first().prayer); assertEquals(17, list.first().at.dayOfMonth)
    }

    @Test fun perPrayerMuteAndReminders() {
        val modes = listOf(AlertMode.SILENT, AlertMode.OFF, AlertMode.OFF, AlertMode.OFF, AlertMode.ADHAN, AlertMode.OFF)
        val now = ZonedDateTime.of(2026, 10, 16, 3, 0, 0, 0, z)
        val list = Schedule.upcoming(p, now, s, modes, 10)
        assertEquals(setOf(Prayer.FAJR, Prayer.MAGHRIB), list.map { it.prayer }.toSet())
        val fajr = PrayerEngine.day(p, now.toLocalDate(), s)[Prayer.FAJR]
        val r = list.first { it.reminder && it.prayer == Prayer.FAJR && it.at.dayOfMonth == 16 }
        assertEquals(fajr.minusMinutes(10), r.at)
        assertEquals(list.size, list.map { it.requestCode }.toSet().size)
        assertTrue(Schedule.upcoming(p, now, s, List(6) { AlertMode.OFF }, 10).isEmpty())
    }

    @Test fun reminderInPastIsSkippedButPrayerKept() {
        val fajr = PrayerEngine.day(p, java.time.LocalDate.of(2026, 10, 16), s)[Prayer.FAJR]
        val list = Schedule.upcoming(p, fajr.minusMinutes(3), s, defaults, 10)
        assertEquals(Prayer.FAJR, list.first().prayer); assertFalse(list.first().reminder)
    }
}
