package com.midroid.prayertimes.domain

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class HijriDay(val day: Int, val month: Int, val year: Int)

/** Umm al-Qura (java.time HijrahDate) with a user +/- day adjustment for local moon-sighting. */
object Hijri {
    fun of(date: LocalDate, adjustDays: Int): HijriDay {
        val h = HijrahDate.from(date.plusDays(adjustDays.toLong()))
        return HijriDay(h.get(ChronoField.DAY_OF_MONTH), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.YEAR))
    }
    val monthsAr = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
    val monthsEn = listOf("Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Shaban", "Ramadan", "Shawwal", "Dhu al-Qadah", "Dhu al-Hijjah")
    fun format(h: HijriDay, arabic: Boolean) = if (arabic) "${h.day} ${monthsAr[h.month - 1]} ${h.year} هـ" else "${h.day} ${monthsEn[h.month - 1]} ${h.year} AH"
}
