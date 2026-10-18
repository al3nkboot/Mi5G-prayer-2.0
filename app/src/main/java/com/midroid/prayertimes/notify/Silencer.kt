package com.midroid.prayertimes.notify

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.midroid.prayertimes.domain.Ringer

/** Auto-silent during prayer. State lives in its own prefs so a reboot mid-window still restores the ringer. */
object Silencer {
    const val ACTION_RESTORE = "com.midroid.prayertimes.RESTORE_RINGER"
    private const val CODE = 900
    private fun sp(ctx: Context) = ctx.getSharedPreferences("silencer", Context.MODE_PRIVATE)

    /** Do Not Disturb access is needed to change the ringer mode to/from silent. */
    fun allowed(ctx: Context) = ctx.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun start(ctx: Context, minutes: Int, vibrate: Boolean, now: Long = System.currentTimeMillis()) {
        if (!allowed(ctx) || sp(ctx).contains("set")) return // no permission, or a window is already active
        val am = ctx.getSystemService(AudioManager::class.java)
        val prev = am.ringerMode
        val to = Ringer.start(prev, if (vibrate) Ringer.VIBRATE else Ringer.SILENT) ?: return
        if (runCatching { am.ringerMode = to }.onFailure { Log.w("Mi5GPrayer", "ringer change refused", it) }.isFailure) return
        // read back: some devices map silent to vibrate etc.; compare against what the system actually set
        sp(ctx).edit().putInt("prev", prev).putInt("set", am.ringerMode).putLong("until", now + minutes * 60_000L).commit()
        arm(ctx)
    }

    fun restore(ctx: Context) {
        val p = sp(ctx); if (!p.contains("set")) return
        val set = p.getInt("set", 0); val prev = p.getInt("prev", Ringer.NORMAL)
        p.edit().clear().commit()
        val am = ctx.getSystemService(AudioManager::class.java)
        Ringer.restore(am.ringerMode, set, prev)?.let { m -> runCatching { am.ringerMode = m }.onFailure { Log.w("Mi5GPrayer", "ringer restore refused", it) } }
    }

    /** (Re-)arms the restore alarm of an active window; called from [Alarms.reschedule] (boot, time change, app start). */
    fun arm(ctx: Context) {
        val until = sp(ctx).getLong("until", 0L); if (until == 0L) return
        val pi = PendingIntent.getBroadcast(ctx, CODE, Intent(ctx, AlarmReceiver::class.java).setAction(ACTION_RESTORE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (Alarms.canExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, until, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, until, pi)
    }
}
