package com.midroid.prayertimes.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.midroid.prayertimes.MainActivity
import com.midroid.prayertimes.PrayerApp
import com.midroid.prayertimes.R
import com.midroid.prayertimes.data.AppSettings
import com.midroid.prayertimes.domain.Hijri
import com.midroid.prayertimes.domain.Prayer
import com.midroid.prayertimes.domain.PrayerEngine
import com.midroid.prayertimes.notify.Alarms
import com.midroid.prayertimes.notify.Notifier
import com.midroid.prayertimes.ui.formatTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.ZonedDateTime

/** Everything the widget shows, computed once per update (pure). */
data class WidgetData(
    val city: String, val hijri: String, val next: String, val nextTime: String, val remainingMs: Long,
    val rows: List<Triple<String, String, Boolean>>, // name, time, isNext
    /** when the widget must be refreshed next (next prayer boundary or local midnight) */
    val refreshAt: ZonedDateTime,
)

object WidgetModel {
    fun build(ctx: Context, s: AppSettings, now: ZonedDateTime): WidgetData {
        val ar = ctx.resources.configuration.locales[0].language == "ar"
        val st = PrayerEngine.now(s.place, now, s.calc)
        val friday = { d: java.time.LocalDate -> d.dayOfWeek == DayOfWeek.FRIDAY }
        val local = now.withZoneSameInstant(s.place.zoneId)
        val midnight = local.toLocalDate().plusDays(1).atStartOfDay(s.place.zoneId)
        return WidgetData(
            city = if (ar) s.place.nameAr else s.place.nameEn,
            hijri = Hijri.format(Hijri.at(s.place, now, s.calc, s.hijriAdjust), ar),
            next = Notifier.prayerLabel(ctx, st.next, friday(st.nextAt.toLocalDate())),
            nextTime = formatTime(st.nextAt, ar),
            remainingMs = st.remaining.toMillis(),
            rows = Prayer.entries.map { p ->
                Triple(Notifier.prayerLabel(ctx, p, friday(st.day.date)), formatTime(st.day[p], ar), st.next == p && st.nextAt.toLocalDate() == st.day.date)
            },
            refreshAt = if (st.nextAt.isBefore(midnight)) st.nextAt else midnight,
        )
    }
}

class PrayerWidget : GlanceAppWidget() {
    companion object { val SMALL = DpSize(120.dp, 60.dp); val LARGE = DpSize(220.dp, 200.dp) }
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as PrayerApp
        val d = WidgetModel.build(context, app.settings.state.value, ZonedDateTime.now(PrayerApp.clock))
        provideContent { Content(context, d) }
    }

    @Composable
    fun Content(ctx: Context, d: WidgetData) {
        val bg = ColorProvider(day = Color(0xFFFFFCF5), night = Color(0xFF152424))
        val fg = ColorProvider(day = Color(0xFF1B1C1A), night = Color(0xFFE3E6E3))
        val dim = ColorProvider(day = Color(0xFF5B5A52), night = Color(0xFFB4C2BF))
        val gold = ColorProvider(day = Color(0xFF9A6B00), night = Color(0xFFE9B949))
        val hl = ColorProvider(day = Color(0xFFCFE8E2), night = Color(0xFF14504B))
        val large = LocalSize.current.height >= LARGE.height
        Column(GlanceModifier.fillMaxSize().background(bg).cornerRadius(20.dp).padding(12.dp)
            .clickable(actionStartActivity<MainActivity>())) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(d.city, GlanceModifier.defaultWeight(), maxLines = 1, style = TextStyle(color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp))
                Text(d.hijri, maxLines = 1, style = TextStyle(color = gold, fontSize = 11.sp))
            }
            Spacer(GlanceModifier.height(4.dp))
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    Text(d.next, style = TextStyle(color = gold, fontWeight = FontWeight.Bold, fontSize = 18.sp))
                    if (large) Text(d.nextTime, style = TextStyle(color = dim, fontSize = 12.sp))
                }
                AndroidRemoteViews(countdown(ctx, d.remainingMs), GlanceModifier.wrapContentWidth().wrapContentHeight())
            }
            if (large) {
                Spacer(GlanceModifier.height(4.dp))
                d.rows.forEach { (name, time, isNext) ->
                    Row(GlanceModifier.fillMaxWidth().cornerRadius(8.dp).background(if (isNext) hl else bg).padding(horizontal = 8.dp, vertical = 1.dp)) {
                        Text(name, GlanceModifier.defaultWeight(), style = TextStyle(color = fg, fontSize = 12.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal))
                        Text(time, style = TextStyle(color = fg, fontSize = 12.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal))
                    }
                }
            }
        }
    }

    /** Platform Chronometer counting down: ticks on its own, so the widget needs no polling. */
    private fun countdown(ctx: Context, remainingMs: Long) = RemoteViews(ctx.packageName, R.layout.widget_countdown).apply {
        setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + remainingMs, null, true)
        setChronometerCountDown(R.id.countdown, true)
    }
}

class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()
    override fun onEnabled(context: Context) { super.onEnabled(context); Widgets.scheduleBoundary(context) }
    override fun onUpdate(context: Context, m: android.appwidget.AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, m, ids); Widgets.scheduleBoundary(context)
    }
    override fun onDisabled(context: Context) { super.onDisabled(context); Widgets.cancelBoundary(context) }
}

/** Fires at the next prayer boundary / midnight, refreshes widgets and re-arms itself. */
class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { Widgets.refresh(context) }
}

object Widgets {
    const val REQ = 99
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun pi(ctx: Context, flags: Int) = PendingIntent.getBroadcast(ctx, REQ, Intent(ctx, WidgetTickReceiver::class.java), flags or PendingIntent.FLAG_IMMUTABLE)

    fun hasWidgets(ctx: Context): Boolean = runCatching {
        android.appwidget.AppWidgetManager.getInstance(ctx).getAppWidgetIds(android.content.ComponentName(ctx, PrayerWidgetReceiver::class.java)).isNotEmpty()
    }.getOrDefault(false)

    fun refresh(ctx: Context) {
        if (!hasWidgets(ctx)) return
        val app = ctx.applicationContext
        scope.launch { runCatching { PrayerWidget().updateAll(app) } }
        scheduleBoundary(ctx)
    }

    /** @return the planned refresh time (null when there are no widgets) */
    fun scheduleBoundary(ctx: Context, now: ZonedDateTime = ZonedDateTime.now(PrayerApp.clock)): ZonedDateTime? {
        if (!hasWidgets(ctx)) return null
        val s = (ctx.applicationContext as PrayerApp).settings.state.value
        val at = WidgetModel.build(ctx, s, now).refreshAt.plusSeconds(1)
        val am = ctx.getSystemService(AlarmManager::class.java)
        val t = at.toInstant().toEpochMilli()
        if (Alarms.canExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC, t, pi(ctx, PendingIntent.FLAG_UPDATE_CURRENT))
        else am.setAndAllowWhileIdle(AlarmManager.RTC, t, pi(ctx, PendingIntent.FLAG_UPDATE_CURRENT))
        return at
    }

    fun cancelBoundary(ctx: Context) { ctx.getSystemService(AlarmManager::class.java).cancel(pi(ctx, PendingIntent.FLAG_UPDATE_CURRENT)) }
}
