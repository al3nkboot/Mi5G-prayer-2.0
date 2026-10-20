package com.midroid.prayertimes

import com.midroid.prayertimes.data.Cities
import com.midroid.prayertimes.data.Locator
import com.midroid.prayertimes.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GpsZoneTest {
    private val date = LocalDate.of(2026, 10, 20); private val s = CalcSettings()

    @Test fun jakartaGpsUsesDeviceZone() {
        val p = Locator.toPlace(-6.2088, 106.8456, "Asia/Jakarta")
        assertEquals("Asia/Jakarta", p.zone); assertEquals("My location", p.nameEn)
        val fajr = PrayerEngine.day(p, date, s)[Prayer.FAJR]
        assertEquals(ZoneId.of("Asia/Jakarta"), fajr.zone)
        assertTrue(fajr.hour in 3..5) // local pre-dawn, not Muscat-shifted
        assertTrue(PrayerEngine.day(p, date, s)[Prayer.DHUHR].hour in 11..12)
    }

    @Test fun buraimiGpsUnchanged() {
        val city = Cities.default
        val p = Locator.toPlace(city.lat, city.lon, "Asia/Muscat")
        assertEquals("Al Buraimi", p.nameEn); assertEquals("Asia/Muscat", p.zone)
        assertEquals(PrayerEngine.day(city, date, s).times, PrayerEngine.day(p, date, s).times)
    }
}
