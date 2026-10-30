package com.midroid.prayertimes.domain

import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/** Sun altitude and azimuth (degrees from true north, clockwise). NOAA approximation, no network. */
data class SunNow(val altitudeDeg: Double, val azimuthDeg: Double) {
    val up: Boolean get() = altitudeDeg > 0.0
    /** Direction a vertical stick's shadow points, opposite the sun. */
    val shadowAzimuthDeg: Double get() = (azimuthDeg + 180.0).mod(360.0)
}

object SunPosition {
    fun at(lat: Double, lon: Double, whenAt: ZonedDateTime): SunNow {
        val utc = whenAt.withZoneSameInstant(ZoneOffset.UTC)
        val hour = utc.hour + utc.minute / 60.0 + utc.second / 3600.0
        var y = utc.year
        var m = utc.monthValue
        val d = utc.dayOfMonth
        if (m <= 2) { y -= 1; m += 12 }
        val a = y / 100
        val b = 2 - a + a / 4
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + hour / 24.0 + b - 1524.5
        val t = (jd - 2451545.0) / 36525.0
        val l0 = (280.46646 + t * (36000.76983 + t * 0.0003032)).mod(360.0)
        val mean = (357.52911 + t * (35999.05029 - 0.0001537 * t)).mod(360.0)
        val meanR = Math.toRadians(mean)
        val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)
        val c = sin(meanR) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
            sin(2 * meanR) * (0.019993 - 0.000101 * t) + sin(3 * meanR) * 0.000289
        val omega = 125.04 - 1934.136 * t
        val lam = l0 + c - 0.00569 - 0.00478 * sin(Math.toRadians(omega))
        val eps0 = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
        val eps = eps0 + 0.00256 * cos(Math.toRadians(omega))
        val decl = Math.toDegrees(asin(sin(Math.toRadians(eps)) * sin(Math.toRadians(lam))))
        val ytan = tan(Math.toRadians(eps) / 2).pow(2.0)
        val l0r = Math.toRadians(l0)
        val eqtime = 4 * Math.toDegrees(
            ytan * sin(2 * l0r) - 2 * e * sin(meanR) + 4 * e * ytan * sin(meanR) * cos(2 * l0r) -
                0.5 * ytan * ytan * sin(4 * l0r) - 1.25 * e * e * sin(2 * meanR)
        )
        val minutes = utc.hour * 60.0 + utc.minute + utc.second / 60.0
        var ha = ((minutes + eqtime + 4 * lon).mod(1440.0)) / 4.0 - 180.0
        if (ha < -180) ha += 360
        val haR = Math.toRadians(ha)
        val latR = Math.toRadians(lat)
        val decR = Math.toRadians(decl)
        val cosZen = (sin(latR) * sin(decR) + cos(latR) * cos(decR) * cos(haR)).coerceIn(-1.0, 1.0)
        val altitude = 90.0 - Math.toDegrees(acos(cosZen))
        val az = (Math.toDegrees(atan2(sin(haR), cos(haR) * sin(latR) - tan(decR) * cos(latR))) + 180.0).mod(360.0)
        return SunNow(altitude, az)
    }
}
