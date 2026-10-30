package com.midroid.prayertimes.domain

import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Moon altitude and azimuth from true north, clockwise. Low-precision, no network. */
data class MoonNow(val altitudeDeg: Double, val azimuthDeg: Double) {
    val up: Boolean get() = altitudeDeg > 0.0
}

object MoonPosition {
    fun at(lat: Double, lon: Double, whenAt: ZonedDateTime): MoonNow {
        val utc = whenAt.withZoneSameInstant(ZoneOffset.UTC)
        val hour = utc.hour + utc.minute / 60.0 + utc.second / 3600.0
        var y = utc.year
        var m = utc.monthValue
        val day = utc.dayOfMonth
        if (m <= 2) { y -= 1; m += 12 }
        val a = y / 100
        val b = 2 - a + a / 4
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + hour / 24.0 + b - 1524.5
        val d = jd - 2451545.0
        fun rad(x: Double) = Math.toRadians(x)
        val lp = (218.316 + 13.176396 * d).mod(360.0)
        val mp = (134.963 + 13.064993 * d).mod(360.0)
        val f = (93.272 + 13.229350 * d).mod(360.0)
        val lam = (lp + 6.289 * sin(rad(mp))).mod(360.0)
        val beta = 5.128 * sin(rad(f))
        val eps = 23.4397
        val lamR = rad(lam)
        val betaR = rad(beta)
        val epsR = rad(eps)
        val ra = Math.toDegrees(atan2(sin(lamR) * cos(epsR) - kotlin.math.tan(betaR) * sin(epsR), cos(lamR))).mod(360.0)
        val dec = Math.toDegrees(asin((sin(betaR) * cos(epsR) + cos(betaR) * sin(epsR) * sin(lamR)).coerceIn(-1.0, 1.0)))
        val gmst = (280.46061837 + 360.98564736629 * d).mod(360.0)
        var ha = (gmst + lon - ra).mod(360.0)
        if (ha > 180.0) ha -= 360.0
        val haR = rad(ha)
        val latR = rad(lat)
        val decR = rad(dec)
        val alt = Math.toDegrees(asin((sin(latR) * sin(decR) + cos(latR) * cos(decR) * cos(haR)).coerceIn(-1.0, 1.0)))
        val az = Math.toDegrees(atan2(-sin(haR), kotlin.math.tan(decR) * cos(latR) - sin(latR) * cos(haR))).mod(360.0)
        return MoonNow(alt, az)
    }
}
