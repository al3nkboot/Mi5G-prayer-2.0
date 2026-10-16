package com.midroid.prayertimes.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.midroid.prayertimes.R
import com.midroid.prayertimes.domain.Method
import com.midroid.prayertimes.domain.Place
import com.midroid.prayertimes.domain.Prayer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

@Composable fun isArabic() = LocalConfiguration.current.locales[0].language == "ar"

@Composable
fun prayerName(p: Prayer, date: LocalDate? = null): String = stringResource(
    when (p) {
        Prayer.FAJR -> R.string.fajr; Prayer.SUNRISE -> R.string.sunrise
        Prayer.DHUHR -> if (date?.dayOfWeek == DayOfWeek.FRIDAY) R.string.jumuah else R.string.dhuhr
        Prayer.ASR -> R.string.asr; Prayer.MAGHRIB -> R.string.maghrib; Prayer.ISHA -> R.string.isha
    }
)

@Composable
fun methodName(m: Method): String = stringResource(
    when (m) {
        Method.OMAN -> R.string.m_oman; Method.UMM_AL_QURA -> R.string.m_uaq; Method.MWL -> R.string.m_mwl
        Method.DUBAI -> R.string.m_dubai; Method.QATAR -> R.string.m_qatar; Method.KUWAIT -> R.string.m_kuwait
        Method.EGYPTIAN -> R.string.m_egypt; Method.KARACHI -> R.string.m_karachi; Method.ISNA -> R.string.m_isna
    }
)

@Composable fun placeName(p: Place) = if (isArabic()) p.nameAr else p.nameEn
@Composable fun countryName(p: Place) = if (isArabic()) p.countryAr else p.countryEn

/** 12-hour clock with Western digits (common in Oman), Arabic ص/م suffix. */
fun formatTime(t: ZonedDateTime, arabic: Boolean): String {
    val h = t.hour % 12
    val s = "%d:%02d".format(Locale.ROOT, if (h == 0) 12 else h, t.minute)
    return s + if (arabic) (if (t.hour < 12) " ص" else " م") else (if (t.hour < 12) " AM" else " PM")
}

fun formatGregorian(d: LocalDate, arabic: Boolean): String {
    val loc = if (arabic) Locale("ar") else Locale.ENGLISH
    val dow = d.dayOfWeek.getDisplayName(TextStyle.FULL, loc)
    val mon = d.month.getDisplayName(TextStyle.FULL, loc)
    return "$dow، ${d.dayOfMonth} $mon ${d.year}".let { if (arabic) it else it.replace("،", ",") }
}
