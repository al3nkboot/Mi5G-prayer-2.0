package com.midroid.prayertimes

import com.midroid.prayertimes.domain.MoonPosition
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class MoonTest {
    @Test fun moonStaysOnTheSky() {
        val a = MoonPosition.at(24.25, 55.79, ZonedDateTime.of(2026, 10, 16, 21, 0, 0, 0, ZoneId.of("Asia/Muscat")))
        val b = MoonPosition.at(24.25, 55.79, ZonedDateTime.of(2026, 10, 16, 3, 0, 0, 0, ZoneId.of("Asia/Muscat")))
        assertTrue(a.altitudeDeg in -90.0..90.0)
        assertTrue(a.azimuthDeg in 0.0..360.0)
        assertTrue(kotlin.math.abs(a.azimuthDeg - b.azimuthDeg) > 1.0)
    }
}
