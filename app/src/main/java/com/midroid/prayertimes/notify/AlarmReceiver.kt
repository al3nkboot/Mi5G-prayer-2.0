package com.midroid.prayertimes.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.midroid.prayertimes.PrayerApp
import com.midroid.prayertimes.R
import com.midroid.prayertimes.domain.AlertMode
import com.midroid.prayertimes.domain.Kind
import com.midroid.prayertimes.domain.Prayer
import java.time.DayOfWeek
import java.time.Instant

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Silencer.ACTION_RESTORE) { Silencer.restore(ctx); return }
        val app = ctx.applicationContext as PrayerApp
        val s = app.settings.state.value
        val p = intent.getStringExtra(Alarms.EXTRA_PRAYER)?.let { n -> Prayer.entries.firstOrNull { it.name == n } }
        val at = intent.getLongExtra(Alarms.EXTRA_AT, 0L)
        val stale = at > 0 && System.currentTimeMillis() - at > 30 * 60_000L // fired very late (e.g. after long doze): skip
        if (p != null && !stale) {
            Notifier.channels(ctx)
            val friday = Instant.ofEpochMilli(at).atZone(s.place.zoneId).dayOfWeek == DayOfWeek.FRIDAY
            val name = Notifier.prayerLabel(ctx, p, friday)
            val mode = s.alerts[p.ordinal]
            val kind = intent.getStringExtra(Alarms.EXTRA_KIND)?.let { k -> Kind.entries.firstOrNull { it.name == k } } ?: Kind.PRAYER
            if (kind == Kind.REMINDER) {
                Notifier.post(ctx, 2000 + p.ordinal, Notifier.base(ctx, Notifier.CH_REMIND, ctx.getString(R.string.reminder_title, name, s.reminderMinutes), ctx.getString(R.string.app_name)))
            } else if (kind == Kind.SILENCE) {
                if (s.autoSilent) Silencer.start(ctx, s.silentMinutes, s.silentVibrate)
            } else if (kind == Kind.ADHKAR) {
                val tab = if (p == Prayer.FAJR) 0 else 1
                Notifier.post(ctx, 2100 + tab, Notifier.base(ctx, Notifier.CH_REMIND, ctx.getString(if (tab == 0) R.string.adhkar_am_notif else R.string.adhkar_pm_notif), ctx.getString(R.string.app_name))
                    .setContentIntent(Notifier.contentIntent(ctx, tab)))
            } else when (mode) {
                AlertMode.OFF -> Unit
                AlertMode.ADHAN -> if (Notifier.audioAllowed(ctx) && Notifier.canPost(ctx)) {
                    val ok = runCatching {
                        ContextCompat.startForegroundService(ctx, Intent(ctx, AdhanService::class.java).putExtra(AdhanService.EXTRA_TITLE, ctx.getString(R.string.time_for, name)).putExtra(AdhanService.EXTRA_SOUND, s.sounds[p.ordinal].name))
                    }.onFailure { Log.w("Mi5GPrayer", "adhan service not allowed, falling back to silent", it) }.isSuccess
                    if (!ok) silent(ctx, p, name)
                } else silent(ctx, p, name)
                AlertMode.SILENT -> silent(ctx, p, name)
            }
        }
        Alarms.reschedule(ctx)
        com.midroid.prayertimes.widget.Widgets.refresh(ctx)
    }

    private fun silent(ctx: Context, p: Prayer, name: String) =
        Notifier.post(ctx, 1100 + p.ordinal, Notifier.base(ctx, Notifier.CH_SILENT, ctx.getString(R.string.time_for, name), ctx.getString(R.string.app_name)))
}

/** BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, LOCALE_CHANGED, MY_PACKAGE_REPLACED, exact-alarm permission changes. */
class SystemReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) { Alarms.reschedule(ctx); com.midroid.prayertimes.widget.Widgets.refreshAsync(this, ctx) }
}
