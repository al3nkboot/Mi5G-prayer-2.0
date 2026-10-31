package com.midroid.prayertimes

import com.midroid.prayertimes.data.Cities
import com.midroid.prayertimes.domain.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

/** Dar al-Ifta (with the Egyptian General Authority of Survey), Cairo: https://www.dar-alifta.org/ar/prayer */
class EgyptTest {
    private val cairo = Cities.byId("cairo")!!
    private val egypt = CalcSettings(method = Method.EGYPTIAN)
    private fun check(date: LocalDate, expected: String) {
        val d = PrayerEngine.day(cairo, date, egypt)
        val exp = expected.split(" ")
        Prayer.entries.forEachIndexed { i, p ->
            val (h, m) = exp[i].split(":").map(String::toInt)
            val got = d[p].hour * 60 + d[p].minute
            println("$date $p app=${d[p].toLocalTime()} dar-alifta=${exp[i]}")
            assertTrue("$date $p got ${d[p].toLocalTime()} exp ${exp[i]}", abs(got - (h * 60 + m)) <= 2)
        }
    }
    @Test fun anglesAreEgyptianSurvey() { assertEquals(19.5, Method.EGYPTIAN.fajr, 0.0); assertEquals(17.5, Method.EGYPTIAN.isha, 0.0) }
    @Test fun cairo31Oct2026() = check(LocalDate.of(2026, 10, 31), "04:40 06:08 11:39 14:45 17:09 18:27")
    /** still on summer time (EEST) */
    @Test fun cairo15Oct2026() = check(LocalDate.of(2026, 10, 15), "05:30 06:57 12:41 15:57 18:24 19:42")
}
