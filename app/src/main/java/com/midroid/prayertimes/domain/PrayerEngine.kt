package com.midroid.prayertimes.domain

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerAdjustments
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

enum class Prayer { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

/**
 * Calculation methods offered. OMAN is the default:
 * Fajr 18°, Isha 18° + Awqaf-style precaution minutes (sunrise +1, dhuhr/asr/maghrib +5),
 * fitted against the Ministry of Endowments & Religious Affairs (mara.gov.om) published Muscat times.
 */
enum class Method(val fajr: Double, val isha: Double, val ishaInterval: Int = 0, val base: CalculationMethod = CalculationMethod.OTHER,
                  val methodAdj: IntArray = IntArray(6)) {
    OMAN(18.0, 18.0, methodAdj = intArrayOf(0, 1, 5, 5, 5, 0)),
    UMM_AL_QURA(18.5, 0.0, 90, CalculationMethod.UMM_AL_QURA),
    MWL(18.0, 17.0, base = CalculationMethod.MUSLIM_WORLD_LEAGUE),
    DUBAI(18.2, 18.2, base = CalculationMethod.DUBAI),
    QATAR(18.0, 0.0, 90, CalculationMethod.QATAR),
    KUWAIT(18.0, 17.5, base = CalculationMethod.KUWAIT),
    EGYPTIAN(19.5, 17.5, base = CalculationMethod.EGYPTIAN),
    KARACHI(18.0, 18.0, base = CalculationMethod.KARACHI),
    ISNA(15.0, 15.0, base = CalculationMethod.NORTH_AMERICA);

    fun params(): CalculationParameters {
        val p = if (base == CalculationMethod.OTHER) {
            if (ishaInterval > 0) CalculationParameters(fajr, ishaInterval) else CalculationParameters(fajr, isha)
        } else base.parameters
        if (this == OMAN) p.withMethodAdjustments(PrayerAdjustments(methodAdj[0], methodAdj[1], methodAdj[2], methodAdj[3], methodAdj[4], methodAdj[5]))
        return p
    }
}

enum class Asr { SHAFI, HANAFI }
enum class HighLat { NONE, MIDDLE_OF_NIGHT, SEVENTH_OF_NIGHT, TWILIGHT_ANGLE }

data class CalcSettings(
    val method: Method = Method.OMAN,
    val asr: Asr = Asr.SHAFI,
    val highLat: HighLat = HighLat.NONE,
    /** user minute offsets, index = Prayer.ordinal */
    val adjustments: List<Int> = List(6) { 0 },
)

data class Place(val id: String, val nameAr: String, val nameEn: String, val lat: Double, val lon: Double, val zone: String, val countryAr: String = "", val countryEn: String = "") {
    val zoneId: ZoneId get() = ZoneId.of(zone)
}

data class DayTimes(val date: LocalDate, val times: Map<Prayer, ZonedDateTime>) {
    operator fun get(p: Prayer) = times.getValue(p)
}

data class NowState(val day: DayTimes, val current: Prayer?, val next: Prayer, val nextAt: ZonedDateTime, val remaining: Duration)

object PrayerEngine {
    fun day(place: Place, date: LocalDate, s: CalcSettings): DayTimes {
        val p = s.method.params()
        p.madhab = if (s.asr == Asr.HANAFI) Madhab.HANAFI else Madhab.SHAFI
        when (s.highLat) {
            HighLat.NONE -> Unit // adhan default (MIDDLE_OF_THE_NIGHT) only matters at high latitudes; keep library default
            HighLat.MIDDLE_OF_NIGHT -> p.highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT
            HighLat.SEVENTH_OF_NIGHT -> p.highLatitudeRule = HighLatitudeRule.SEVENTH_OF_THE_NIGHT
            HighLat.TWILIGHT_ANGLE -> p.highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE
        }
        val a = s.adjustments
        p.adjustments = PrayerAdjustments(a[0], a[1], a[2], a[3], a[4], a[5])
        val t = PrayerTimes(Coordinates(place.lat, place.lon), DateComponents(date.year, date.monthValue, date.dayOfMonth), p)
        val z = place.zoneId
        fun c(d: java.util.Date) = d.toInstant().atZone(z).truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
        return DayTimes(date, linkedMapOf(
            Prayer.FAJR to c(t.fajr), Prayer.SUNRISE to c(t.sunrise), Prayer.DHUHR to c(t.dhuhr),
            Prayer.ASR to c(t.asr), Prayer.MAGHRIB to c(t.maghrib), Prayer.ISHA to c(t.isha),
        ))
    }

    /** Next event strictly after [now] (rolls over to tomorrow's Fajr after Isha). Current = last event at or before now
     *  (yesterday's Isha before today's Fajr). */
    fun now(place: Place, now: ZonedDateTime, s: CalcSettings): NowState {
        val local = now.withZoneSameInstant(place.zoneId)
        val today = day(place, local.toLocalDate(), s)
        val nextEntry = today.times.entries.firstOrNull { it.value.isAfter(local) }
        val (next, at) = nextEntry?.let { it.key to it.value }
            ?: (Prayer.FAJR to day(place, local.toLocalDate().plusDays(1), s)[Prayer.FAJR])
        val current = today.times.entries.lastOrNull { !it.value.isAfter(local) }?.key ?: Prayer.ISHA
        return NowState(today, current, next, at, Duration.between(local, at))
    }

    fun formatCountdown(d: Duration): String {
        val s = d.seconds.coerceAtLeast(0)
        return "%02d:%02d:%02d".format(java.util.Locale.ROOT, s / 3600, (s % 3600) / 60, s % 60)
    }
}
