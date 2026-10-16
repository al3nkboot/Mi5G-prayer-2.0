package com.midroid.prayertimes.domain

import java.time.ZonedDateTime

enum class AlertMode { OFF, SILENT, ADHAN }

data class Alert(val prayer: Prayer, val at: ZonedDateTime, val reminder: Boolean, val requestCode: Int)

/** Pure planning of the rolling alarm window (no Android types) so it can be unit-tested. */
object Schedule {
    const val WINDOW_DAYS = 2
    /** request codes are 0 until MAX_CODES; the scheduler cancels all of them before re-arming. */
    const val MAX_CODES = 3 * 20

    fun code(dayOffset: Int, p: Prayer, reminder: Boolean) = dayOffset * 20 + p.ordinal + if (reminder) 10 else 0

    fun upcoming(place: Place, now: ZonedDateTime, calc: CalcSettings, modes: List<AlertMode>, reminderMinutes: Int, days: Int = WINDOW_DAYS): List<Alert> {
        val local = now.withZoneSameInstant(place.zoneId)
        val out = mutableListOf<Alert>()
        for (off in 0..days) {
            val d = PrayerEngine.day(place, local.toLocalDate().plusDays(off.toLong()), calc)
            for (p in Prayer.entries) {
                if (modes[p.ordinal] == AlertMode.OFF) continue
                val t = d[p]
                if (t.isAfter(local)) out += Alert(p, t, false, code(off, p, false))
                if (reminderMinutes > 0 && p != Prayer.SUNRISE) {
                    val r = t.minusMinutes(reminderMinutes.toLong())
                    if (r.isAfter(local)) out += Alert(p, r, true, code(off, p, true))
                }
            }
        }
        // keep a window of `days` x 24h from now
        val end = local.plusDays(days.toLong())
        return out.filter { !it.at.isAfter(end) }.sortedBy { it.at }
    }
}
