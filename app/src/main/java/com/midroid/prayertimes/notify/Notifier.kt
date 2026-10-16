package com.midroid.prayertimes.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.midroid.prayertimes.MainActivity
import com.midroid.prayertimes.R
import com.midroid.prayertimes.domain.Prayer

object Notifier {
    const val CH_ADHAN = "adhan"; const val CH_SILENT = "prayer_silent"; const val CH_REMIND = "reminder"
    const val ID_ADHAN = 1000

    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_ADHAN, ctx.getString(R.string.ch_adhan), NotificationManager.IMPORTANCE_HIGH).apply { setSound(null, null) })
        nm.createNotificationChannel(NotificationChannel(CH_SILENT, ctx.getString(R.string.ch_silent), NotificationManager.IMPORTANCE_DEFAULT).apply { setSound(null, null); enableVibration(false) })
        nm.createNotificationChannel(NotificationChannel(CH_REMIND, ctx.getString(R.string.ch_reminder), NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun canPost(ctx: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Silent/vibrate ringer or Do-Not-Disturb => don't play the adhan audio, post a silent notification instead. */
    fun audioAllowed(ctx: Context): Boolean {
        val am = ctx.getSystemService(AudioManager::class.java)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        return am.ringerMode == AudioManager.RINGER_MODE_NORMAL && nm.currentInterruptionFilter <= NotificationManager.INTERRUPTION_FILTER_ALL
    }

    fun prayerLabel(ctx: Context, p: Prayer, friday: Boolean) = ctx.getString(when (p) {
        Prayer.FAJR -> R.string.fajr; Prayer.SUNRISE -> R.string.sunrise; Prayer.DHUHR -> if (friday) R.string.jumuah else R.string.dhuhr
        Prayer.ASR -> R.string.asr; Prayer.MAGHRIB -> R.string.maghrib; Prayer.ISHA -> R.string.isha })

    fun contentIntent(ctx: Context): PendingIntent = PendingIntent.getActivity(ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)

    fun base(ctx: Context, channel: String, title: String, text: String) = NotificationCompat.Builder(ctx, channel)
        .setSmallIcon(R.drawable.ic_stat_prayer).setContentTitle(title).setContentText(text)
        .setContentIntent(contentIntent(ctx)).setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_REMINDER)

    fun post(ctx: Context, id: Int, b: NotificationCompat.Builder) {
        if (!canPost(ctx)) return
        @Suppress("MissingPermission") ctx.getSystemService(NotificationManager::class.java).notify(id, b.build())
    }
}
