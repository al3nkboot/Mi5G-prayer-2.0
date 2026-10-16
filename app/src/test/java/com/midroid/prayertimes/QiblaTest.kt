package com.midroid.prayertimes

import com.midroid.prayertimes.data.Cities
import com.midroid.prayertimes.domain.Qibla
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class QiblaTest {
    /** independent formula: bearing via 3-D unit vectors (north/east components of the great-circle tangent) */
    private fun vecBearing(lat: Double, lon: Double): Double {
        fun v(la: Double, lo: Double) = doubleArrayOf(cos(Math.toRadians(la)) * cos(Math.toRadians(lo)), cos(Math.toRadians(la)) * sin(Math.toRadians(lo)), sin(Math.toRadians(la)))
        val a = v(lat, lon); val b = v(Qibla.KAABA_LAT, Qibla.KAABA_LON)
        val north = doubleArrayOf(-sin(Math.toRadians(lat)) * cos(Math.toRadians(lon)), -sin(Math.toRadians(lat)) * sin(Math.toRadians(lon)), cos(Math.toRadians(lat)))
        val east = doubleArrayOf(-sin(Math.toRadians(lon)), cos(Math.toRadians(lon)), 0.0)
        val d = DoubleArray(3) { b[it] - a[it] * (a[0] * b[0] + a[1] * b[1] + a[2] * b[2]) }
        return (Math.toDegrees(atan2(d.indices.sumOf { d[it] * east[it] }, d.indices.sumOf { d[it] * north[it] })) + 360) % 360
    }

    @Test fun knownPublishedBearings() {
        // widely published qibla directions (true north)
        assertEquals(118.99, Qibla.bearing(51.5074, -0.1278), 0.3)   // London
        assertEquals(58.48, Qibla.bearing(40.7128, -74.0060), 0.3)   // New York
        assertEquals(295.15, Qibla.bearing(-6.2088, 106.8456), 0.3)  // Jakarta
    }

    @Test fun omanAndGulfMatchIndependentFormula() {
        for (id in listOf("buraimi", "muscat", "salalah", "khasab", "sur", "dubai", "riyadh", "cairo", "rabat")) {
            val p = Cities.byId(id)!!
            assertEquals(id, vecBearing(p.lat, p.lon), Qibla.bearing(p.lat, p.lon), 0.01)
        }
        // sanity: from Oman the qibla is west-south-west
        val b = Qibla.bearing(Cities.default.lat, Cities.default.lon)
        assertEquals(262.31, b, 0.05)                                  // Al Buraimi
        assertEquals(266.47, Qibla.bearing(23.58413, 58.40778), 0.05)  // Muscat
        assertEquals(258.2, Qibla.bearing(25.2048, 55.2708), 0.5)      // Dubai (published ≈258°)
        assertEquals(1665.0, Qibla.distanceKm(Cities.default.lat, Cities.default.lon), 5.0)
    }

    @Test fun deltaAndFacing() {
        assertEquals(10.0, Qibla.delta(255.0, 245.0), 1e-9)
        assertEquals(-20.0, Qibla.delta(350.0, 10.0), 1e-9)
        assertEquals(20.0, Qibla.delta(10.0, 350.0), 1e-9)
        assertTrue(Qibla.isFacing(255.0, 251.0)); assertFalse(Qibla.isFacing(255.0, 249.0))
    }
}
