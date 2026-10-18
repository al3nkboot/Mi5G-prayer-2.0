package com.midroid.prayertimes.domain

import java.time.ZonedDateTime

enum class AlertMode { OFF, SILENT, ADHAN }

/** PRAYER = adhan/silent alert, REMINDER = n minutes before, SILENCE = auto-silent start, ADHKAR = morning (Fajr) / evening (Asr) adhkar reminder */
enum class Kind { PRAYER, REMINDER, SILENCE, ADHKAR }

data class Alert(val prayer: Prayer, val at: ZonedDateTime, val kind: Kind, val requestCode: Int) {
    val reminder get() = kind == Kind.REMINDER
}

/** Pure planning of the rolling alarm window (no Android types) so it can be unit-tested. */
object Schedule {
    const val WINDOW_DAYS = 2
    /** request codes are 0 until MAX_CODES; the scheduler cancels all of them before re-arming. */
    const val MAX_CODES = 3 * 40
    /** auto-silent starts this long after the prayer time, so the adhan (which skips when the ringer is not normal) starts first */
    const val SILENCE_DELAY_MIN = 1L

    fun code(dayOffset: Int, p: Prayer, kind: Kind) = dayOffset * 40 + kind.ordinal * 8 + p.ordinal

    /**
     * @param silence true = arm auto-silent for the five prayers
     * @param adhkarAm minutes after Fajr for the morning adhkar reminder, null = off
     * @param adhkarPm minutes after Asr for the evening adhkar reminder, null = off
     */
    fun upcoming(place: Place, now: ZonedDateTime, calc: CalcSettings, modes: List<AlertMode>, reminderMinutes: Int, days: Int = WINDOW_DAYS,
                 silence: Boolean = false, adhkarAm: Int? = null, adhkarPm: Int? = null): List<Alert> {
        val local = now.withZoneSameInstant(place.zoneId)
        val out = mutableListOf<Alert>()
        fun add(p: Prayer, t: ZonedDateTime, k: Kind, off: Int) { if (t.isAfter(local)) out += Alert(p, t, k, code(off, p, k)) }
        for (off in 0..days) {
            val d = PrayerEngine.day(place, local.toLocalDate().plusDays(off.toLong()), calc)
            for (p in Prayer.entries) {
                val t = d[p]
                if (silence && p != Prayer.SUNRISE) add(p, t.plusMinutes(SILENCE_DELAY_MIN), Kind.SILENCE, off)
                if (p == Prayer.FAJR && adhkarAm != null) add(p, t.plusMinutes(adhkarAm.toLong()), Kind.ADHKAR, off)
                if (p == Prayer.ASR && adhkarPm != null) add(p, t.plusMinutes(adhkarPm.toLong()), Kind.ADHKAR, off)
                if (modes[p.ordinal] == AlertMode.OFF) continue
                add(p, t, Kind.PRAYER, off)
                if (reminderMinutes > 0 && p != Prayer.SUNRISE) add(p, t.minusMinutes(reminderMinutes.toLong()), Kind.REMINDER, off)
            }
        }
        // keep a window of `days` x 24h from now
        val end = local.plusDays(days.toLong())
        return out.filter { !it.at.isAfter(end) }.sortedBy { it.at }
    }
}
