package com.midroid.prayertimes.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.midroid.prayertimes.PrayerApp
import com.midroid.prayertimes.data.AppSettings
import com.midroid.prayertimes.domain.Alert
import com.midroid.prayertimes.domain.Schedule
import java.time.ZonedDateTime

/** Arms a rolling window of alarms (next [Schedule.WINDOW_DAYS] days). Re-armed whenever an alarm fires, settings
 *  change, the device boots, or time/zone changes. */
object Alarms {
    const val EXTRA_PRAYER = "prayer"; const val EXTRA_REMINDER = "reminder"; const val EXTRA_AT = "at"

    fun canExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    private fun pending(ctx: Context, code: Int, a: Alert?, flags: Int): PendingIntent? {
        val i = Intent(ctx, AlarmReceiver::class.java).setAction("com.midroid.prayertimes.ALARM_$code")
        if (a != null) i.putExtra(EXTRA_PRAYER, a.prayer.name).putExtra(EXTRA_REMINDER, a.reminder).putExtra(EXTRA_AT, a.at.toInstant().toEpochMilli())
        return PendingIntent.getBroadcast(ctx, code, i, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    /** @return the alerts that were armed */
    fun reschedule(ctx: Context, s: AppSettings = (ctx.applicationContext as PrayerApp).settings.state.value,
                   now: ZonedDateTime = ZonedDateTime.now(PrayerApp.clock)): List<Alert> {
        val am = ctx.getSystemService(AlarmManager::class.java)
        for (c in 0 until Schedule.MAX_CODES) pending(ctx, c, null, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it); it.cancel() }
        val list = Schedule.upcoming(s.place, now, s.calc, s.alerts, s.reminderMinutes)
        val exact = canExact(ctx)
        for (a in list) {
            val pi = pending(ctx, a.requestCode, a, PendingIntent.FLAG_UPDATE_CURRENT)!!
            val t = a.at.toInstant().toEpochMilli()
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi) // inexact fallback (may be late)
        }
        return list
    }
}
