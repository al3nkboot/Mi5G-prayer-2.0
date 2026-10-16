package com.midroid.prayertimes.domain

import kotlin.math.*

object Qibla {
    const val KAABA_LAT = 21.4225; const val KAABA_LON = 39.8262

    /** Initial great-circle bearing from (lat, lon) to the Kaaba, degrees clockwise from TRUE north, 0..360. */
    fun bearing(lat: Double, lon: Double): Double {
        val p1 = Math.toRadians(lat); val p2 = Math.toRadians(KAABA_LAT); val dl = Math.toRadians(KAABA_LON - lon)
        val y = sin(dl) * cos(p2); val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    fun distanceKm(lat: Double, lon: Double) = com.midroid.prayertimes.data.Cities.distanceKm(lat, lon, KAABA_LAT, KAABA_LON)

    /** Signed smallest difference target - heading in (-180, 180]. */
    fun delta(target: Double, heading: Double): Double { var d = (target - heading) % 360; if (d > 180) d -= 360; if (d <= -180) d += 360; return d }

    const val FACING_TOLERANCE = 5.0
    fun isFacing(target: Double, heading: Double) = abs(delta(target, heading)) <= FACING_TOLERANCE
}
