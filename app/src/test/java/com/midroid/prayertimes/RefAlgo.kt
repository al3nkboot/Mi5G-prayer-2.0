package com.midroid.prayertimes

import kotlin.math.*

/** Independent second implementation (PrayTimes.org-style, Meeus low-precision sun) used only to cross-check adhan. */
object RefAlgo {
    private fun d2r(d: Double) = d * PI / 180; private fun r2d(r: Double) = r * 180 / PI
    private fun fix(a: Double, b: Double) = (a - b * floor(a / b))
    private fun sun(jd: Double): Pair<Double, Double> { // declination deg, equation of time hours
        val d = jd - 2451545.0
        val g = fix(357.529 + 0.98560028 * d, 360.0); val q = fix(280.459 + 0.98564736 * d, 360.0)
        val l = fix(q + 1.915 * sin(d2r(g)) + 0.020 * sin(d2r(2 * g)), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = r2d(atan2(cos(d2r(e)) * sin(d2r(l)), cos(d2r(l)))) / 15
        val eqt = q / 15 - fix(ra, 24.0)
        return r2d(asin(sin(d2r(e)) * sin(d2r(l)))) to (if (eqt > 12) eqt - 24 else if (eqt < -12) eqt + 24 else eqt)
    }
    /** returns local clock hours [fajr, sunrise, dhuhr, asr, maghrib, isha] */
    fun times(y: Int, m: Int, d: Int, lat: Double, lon: Double, tz: Double, fajr: Double, isha: Double, asrFactor: Int = 1): DoubleArray {
        var yy = y; var mm = m; if (mm <= 2) { yy -= 1; mm += 12 }
        val a = floor(yy / 100.0); val b = 2 - a + floor(a / 4)
        val jd0 = floor(365.25 * (yy + 4716)) + floor(30.6001 * (mm + 1)) + d + b - 1524.5 - lon / 360
        fun noon(t: Double) = fix(12 - sun(jd0 + t / 24).second, 24.0)
        fun angleTime(angle: Double, t: Double, ccw: Boolean): Double {
            val decl = sun(jd0 + t / 24).first
            val v = r2d(acos((-sin(d2r(angle)) - sin(d2r(decl)) * sin(d2r(lat))) / (cos(d2r(decl)) * cos(d2r(lat))))) / 15
            return noon(t) + if (ccw) -v else v
        }
        fun asrTime(t: Double): Double {
            val decl = sun(jd0 + t / 24).first
            val ang = -r2d(atan(1 / (asrFactor + tan(d2r(abs(lat - decl))))))
            return angleTime(ang, t, false)
        }
        // two iterations
        var tt = doubleArrayOf(5.0, 6.0, 12.0, 13.0, 18.0, 18.0)
        repeat(2) {
            tt = doubleArrayOf(angleTime(fajr, tt[0], true), angleTime(0.833, tt[1], true), noon(tt[2]), asrTime(tt[3]), angleTime(0.833, tt[4], false), angleTime(isha, tt[5], false))
        }
        return DoubleArray(6) { tt[it] + tz - lon / 15 }
    }
}
