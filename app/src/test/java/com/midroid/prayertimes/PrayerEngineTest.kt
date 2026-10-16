package com.midroid.prayertimes

import com.midroid.prayertimes.data.Cities
import com.midroid.prayertimes.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneId
import kotlin.math.abs

class PrayerEngineTest {
    private val buraimi = Cities.default
    private val muscatMinistry = Cities.byId("muscat")!!
    private fun hm(z: ZonedDateTime) = z.hour * 60 + z.minute
    private fun parse(s: String) = s.split(" ").map { val (h, m) = it.split(":").map(String::toInt); h * 60 + m }

    private fun assertDay(place: Place, date: LocalDate, expected: String, tol: Int, s: CalcSettings = CalcSettings()) {
        val d = PrayerEngine.day(place, date, s)
        val exp = parse(expected)
        Prayer.entries.forEachIndexed { i, p -> assertTrue("$date $p got ${d[p].toLocalTime()} exp ${expected.split(" ")[i]}", abs(hm(d[p]) - exp[i]) <= tol) }
    }

    /** Official Ministry of Endowments & Religious Affairs (mara.gov.om) Muscat times; Asr/Maghrib/Isha published 12h -> 24h. */
    @Test fun omanMethodMatchesMinistryMuscat() {
        assertDay(muscatMinistry, LocalDate.of(2026, 6, 13), "03:52 05:20 12:12 15:30 18:59 20:21", 1)
        assertDay(muscatMinistry, LocalDate.of(2026, 1, 26), "05:31 06:50 12:24 15:32 17:54 19:07", 1)
        assertDay(muscatMinistry, LocalDate.of(2026, 6, 1), "03:54 05:20 12:10 15:29 18:54 20:15", 1)
        assertDay(muscatMinistry, LocalDate.of(2026, 6, 30), "03:56 05:23 12:15 15:34 19:03 20:25", 1)
    }

    /** Al Buraimi pinned values (regression) for a few dates, Oman method. */
    @Test fun buraimiPinned() {
        assertDay(buraimi, LocalDate.of(2026, 10, 16), "04:59 06:15 12:07 15:26 17:55 19:05", 0)
    }

    /** Cross-check adhan (pure angles, no precaution minutes) against an independent implementation. */
    @Test fun buraimiCrossCheckIndependentAlgo() {
        val plain = CalcSettings(method = Method.KARACHI) // 18/18, no method adjustments
        for (date in listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 20), LocalDate.of(2026, 6, 21), LocalDate.of(2026, 10, 16), LocalDate.of(2026, 12, 21), LocalDate.of(2027, 4, 10))) {
            val d = PrayerEngine.day(buraimi, date, plain)
            val r = RefAlgo.times(date.year, date.monthValue, date.dayOfMonth, buraimi.lat, buraimi.lon, 4.0, 18.0, 18.0)
            Prayer.entries.forEachIndexed { i, p ->
                val refMin = r[i] * 60
                assertTrue("$date $p adhan=${d[p].toLocalTime()} ref=${"%.1f".format(refMin)}", abs(hm(d[p]) - refMin) <= 2.0)
            }
        }
    }

    @Test fun hanafiAsrLaterAndAdjustmentsApply() {
        val date = LocalDate.of(2026, 10, 16)
        val shafi = PrayerEngine.day(buraimi, date, CalcSettings())
        val hanafi = PrayerEngine.day(buraimi, date, CalcSettings(asr = Asr.HANAFI))
        assertTrue(hanafi[Prayer.ASR].isAfter(shafi[Prayer.ASR].plusMinutes(30)))
        val adj = PrayerEngine.day(buraimi, date, CalcSettings(adjustments = listOf(3, 0, 0, 0, 0, -2)))
        assertEquals(shafi[Prayer.FAJR].plusMinutes(3), adj[Prayer.FAJR])
        assertEquals(shafi[Prayer.ISHA].minusMinutes(2), adj[Prayer.ISHA])
        val uaq = PrayerEngine.day(buraimi, date, CalcSettings(method = Method.UMM_AL_QURA))
        assertEquals(90L, java.time.Duration.between(uaq[Prayer.MAGHRIB], uaq[Prayer.ISHA]).toMinutes())
    }

    private val z = ZoneId.of("Asia/Muscat")
    @Test fun nextPrayerAcrossMidnight() {
        val s = CalcSettings()
        val late = PrayerEngine.now(buraimi, ZonedDateTime.of(2026, 10, 16, 23, 30, 0, 0, z), s)
        assertEquals(Prayer.FAJR, late.next); assertEquals(Prayer.ISHA, late.current)
        assertEquals(LocalDate.of(2026, 10, 17), late.nextAt.toLocalDate())
        val fajr17 = PrayerEngine.day(buraimi, LocalDate.of(2026, 10, 17), s)[Prayer.FAJR]
        assertEquals(java.time.Duration.between(ZonedDateTime.of(2026, 10, 16, 23, 30, 0, 0, z), fajr17), late.remaining)

        val early = PrayerEngine.now(buraimi, ZonedDateTime.of(2026, 10, 17, 0, 30, 0, 0, z), s)
        assertEquals(Prayer.FAJR, early.next); assertEquals(Prayer.ISHA, early.current)
        assertEquals(fajr17, early.nextAt)

        // instant-in-other-zone input is normalised to place zone
        val utc = PrayerEngine.now(buraimi, ZonedDateTime.of(2026, 10, 16, 20, 30, 0, 0, ZoneId.of("UTC")), s)
        assertEquals(late.nextAt, utc.nextAt)
    }

    @Test fun nextPrayerDuringDayAndExactBoundary() {
        val s = CalcSettings(); val d = PrayerEngine.day(buraimi, LocalDate.of(2026, 10, 16), s)
        val atDhuhr = PrayerEngine.now(buraimi, d[Prayer.DHUHR], s)
        assertEquals(Prayer.DHUHR, atDhuhr.current); assertEquals(Prayer.ASR, atDhuhr.next)
        val before = PrayerEngine.now(buraimi, d[Prayer.DHUHR].minusSeconds(1), s)
        assertEquals(Prayer.SUNRISE, before.current); assertEquals(Prayer.DHUHR, before.next)
        assertEquals("00:00:01", PrayerEngine.formatCountdown(before.remaining))
        assertEquals("01:02:03", PrayerEngine.formatCountdown(java.time.Duration.ofSeconds(3723)))
    }

    @Test fun hijriAndAdjustment() {
        val d = LocalDate.of(2026, 10, 16)
        assertEquals(HijriDay(5, 5, 1448), Hijri.of(d, 0))
        assertEquals(HijriDay(6, 5, 1448), Hijri.of(d, 1))
        assertEquals(HijriDay(4, 5, 1448), Hijri.of(d, -1))
        // 1 Ramadan 1447 (Umm al-Qura) = 18 Feb 2026; -1 day lands in Shaban
        assertEquals(HijriDay(1, 9, 1447), Hijri.of(LocalDate.of(2026, 2, 18), 0))
        assertEquals(8, Hijri.of(LocalDate.of(2026, 2, 18), -1).month)
        assertEquals("5 جمادى الأولى 1448 هـ", Hijri.format(Hijri.of(d, 0), true))
    }

    private fun ministry() = javaClass.classLoader!!.getResourceAsStream("mara_2026.csv")!!.bufferedReader().readLines()
        .filter { !it.startsWith("#") && !it.startsWith("city_id") }.map { it.split(",") }

    /** Every day of 2026 for Al Buraimi: Oman method within 1 minute of the Ministry calendar, and mostly exact. */
    @Test fun buraimiFullYearVsMinistry() {
        val rows = ministry().filter { it[0] == "buraimi" }
        assertEquals(365, rows.size)
        var exact = 0; var total = 0
        for (r in rows) {
            val d = PrayerEngine.day(buraimi, LocalDate.parse(r[1]), CalcSettings())
            Prayer.entries.forEachIndexed { i, p ->
                val (h, m) = r[2 + i].split(":").map(String::toInt)
                val diff = hm(d[p]) - (h * 60 + m)
                assertTrue("${r[1]} $p diff $diff", abs(diff) <= 1)
                total++; if (diff == 0) exact++
            }
        }
        assertTrue("exact ratio $exact/$total", exact.toDouble() / total > 0.75)
    }

    /** Jun + Dec 2026 for each Omani wilayat the Ministry publishes, using GeoNames coordinates. */
    @Test fun wilayatsVsMinistry() {
        for ((id, rows) in ministry().filter { it[0] != "buraimi" }.groupBy { it[0] }) {
            val place = Cities.byId(id)!!
            for (r in rows) {
                val d = PrayerEngine.day(place, LocalDate.parse(r[1]), CalcSettings())
                Prayer.entries.forEachIndexed { i, p ->
                    val (h, m) = r[2 + i].split(":").map(String::toInt)
                    val diff = abs(hm(d[p]) - (h * 60 + m))
                    assertTrue("$id ${r[1]} $p diff $diff", diff <= 1)
                }
            }
        }
    }

    @Test fun ministryFitsOnlyWithOmanMethod() {
        val rak = Cities.byId("rakhyut")!!; val date = LocalDate.of(2026, 6, 1)
        val oman = PrayerEngine.day(rak, date, CalcSettings())
        val omanNoFit = PrayerEngine.day(rak.copy(id = "x"), date, CalcSettings())
        assertEquals(omanNoFit[Prayer.FAJR].minusMinutes(2), oman[Prayer.FAJR])
        val mwl = PrayerEngine.day(rak, date, CalcSettings(method = Method.MWL))
        val mwlNoFit = PrayerEngine.day(rak.copy(id = "x"), date, CalcSettings(method = Method.MWL))
        assertEquals(mwlNoFit.times, mwl.times)
        // user adjustments still stack on top
        val adj = PrayerEngine.day(rak, date, CalcSettings(adjustments = listOf(3, 0, 0, 0, 0, 0)))
        assertEquals(oman[Prayer.FAJR].plusMinutes(3), adj[Prayer.FAJR])
    }

    @Test fun hijriChangesAtMaghrib() {
        val s = CalcSettings()
        val maghrib = PrayerEngine.day(buraimi, LocalDate.of(2026, 10, 16), s)[Prayer.MAGHRIB]
        assertEquals(HijriDay(5, 5, 1448), Hijri.at(buraimi, maghrib.minusMinutes(1), s, 0))
        assertEquals(HijriDay(6, 5, 1448), Hijri.at(buraimi, maghrib, s, 0))
        assertEquals(HijriDay(6, 5, 1448), Hijri.at(buraimi, ZonedDateTime.of(2026, 10, 16, 23, 59, 0, 0, z), s, 0))
        assertEquals(HijriDay(6, 5, 1448), Hijri.at(buraimi, ZonedDateTime.of(2026, 10, 17, 0, 5, 0, 0, z), s, 0))
        assertEquals(HijriDay(7, 5, 1448), Hijri.at(buraimi, maghrib.plusMinutes(5), s, 1))
        assertEquals(HijriDay(4, 5, 1448), Hijri.at(buraimi, ZonedDateTime.of(2026, 10, 16, 9, 0, 0, 0, z), s, -1))
        // month rollover at Maghrib: 17 Feb 2026 evening -> 1 Ramadan 1447
        val m17 = PrayerEngine.day(buraimi, LocalDate.of(2026, 2, 17), s)[Prayer.MAGHRIB]
        assertEquals(HijriDay(1, 9, 1447), Hijri.at(buraimi, m17.plusMinutes(1), s, 0))
        assertEquals(8, Hijri.at(buraimi, m17.minusMinutes(1), s, 0).month)
    }

    @Test fun nearestCity() {
        val (p, km) = Cities.nearest(24.26, 55.80)
        assertEquals("buraimi", p.id); assertTrue(km < 3)
        assertEquals("Asia/Dubai", Cities.nearest(25.2, 55.3).first.zone)
    }
}
