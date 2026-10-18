package com.midroid.prayertimes.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.SystemClock
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
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
import com.midroid.prayertimes.data.ThemeMode
import com.midroid.prayertimes.domain.NowState
import com.midroid.prayertimes.domain.Hijri
import com.midroid.prayertimes.domain.Prayer
import com.midroid.prayertimes.domain.PrayerEngine
import com.midroid.prayertimes.notify.Alarms
import com.midroid.prayertimes.notify.Notifier
import com.midroid.prayertimes.ui.formatTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.ZonedDateTime

/** Everything the widget shows, computed once per update (pure). */
data class WidgetData(
    val city: String, val hijri: String, val next: String, val nextTime: String, val remainingMs: Long,
    val rows: List<Triple<String, String, Boolean>>, // the five prayers: name, time, highlighted
    /** when the widget must be refreshed next (next prayer boundary or local midnight) */
    val refreshAt: ZonedDateTime,
    val rtl: Boolean,
)

object WidgetModel {
    private val FIVE = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

    /** Next of the five prayers today; after Isha the current one (Isha) stays highlighted. */
    fun highlighted(st: NowState): Prayer =
        FIVE.firstOrNull { st.next == it && st.nextAt.toLocalDate() == st.day.date }
            ?: if (st.next == Prayer.SUNRISE) Prayer.DHUHR else Prayer.ISHA

    /** The app's own language (AppCompat per-app locale), not just the system one. */
    fun localized(ctx: Context): Context {
        // API 33+: the platform already applies the per-app locale to our resources.
        // shortcut: below 33 AppCompat only knows the choice once an activity ran in this process; upgrade by mirroring the language into AppSettings.
        val l = AppCompatDelegate.getApplicationLocales()
        if (Build.VERSION.SDK_INT >= 33 || l.isEmpty) return ctx
        return ctx.createConfigurationContext(Configuration(ctx.resources.configuration).apply { setLocale(l[0]) })
    }

    fun build(ctx: Context, s: AppSettings, now: ZonedDateTime): WidgetData {
        val loc = ctx.resources.configuration.locales[0]
        val ar = loc.language == "ar"
        val st = PrayerEngine.now(s.place, now, s.calc)
        val friday = { d: java.time.LocalDate -> d.dayOfWeek == DayOfWeek.FRIDAY }
        val local = now.withZoneSameInstant(s.place.zoneId)
        val midnight = local.toLocalDate().plusDays(1).atStartOfDay(s.place.zoneId)
        val hl = highlighted(st)
        return WidgetData(
            city = if (ar) s.place.nameAr else s.place.nameEn,
            hijri = Hijri.format(Hijri.at(s.place, now, s.calc, s.hijriAdjust), ar),
            next = Notifier.prayerLabel(ctx, st.next, friday(st.nextAt.toLocalDate())),
            nextTime = formatTime(st.nextAt, ar),
            remainingMs = st.remaining.toMillis(),
            rows = FIVE.map { p -> Triple(Notifier.prayerLabel(ctx, p, friday(st.day.date)), formatTime(st.day[p], ar), p == hl) },
            refreshAt = if (st.nextAt.isBefore(midnight)) st.nextAt else midnight,
            rtl = TextUtils.getLayoutDirectionFromLocale(loc) == View.LAYOUT_DIRECTION_RTL,
        )
    }
}

class PrayerWidget : GlanceAppWidget() {
    /** 2x1: next prayer only; 4x2: five prayers in a row; 4x3+: vertical list. */
    companion object { val SMALL = DpSize(110.dp, 40.dp); val WIDE = DpSize(250.dp, 110.dp); val TALL = DpSize(250.dp, 200.dp) }
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as PrayerApp
        val s = app.settings.state.value
        val c = WidgetModel.localized(context)
        val d = WidgetModel.build(c, s, ZonedDateTime.now(PrayerApp.clock))
        provideContent { Content(c, d, s.theme) }
    }

    @Composable
    fun Content(ctx: Context, d: WidgetData, theme: ThemeMode = ThemeMode.SYSTEM) {
        // app theme setting wins; SYSTEM follows the launcher's dark/light
        fun col(day: Long, night: Long) = when (theme) {
            ThemeMode.SYSTEM -> ColorProvider(day = Color(day), night = Color(night))
            ThemeMode.LIGHT -> ColorProvider(day = Color(day), night = Color(day))
            ThemeMode.DARK -> ColorProvider(day = Color(night), night = Color(night))
        }
        val bg = col(0xFFFFFCF5, 0xFF152424)
        val fg = col(0xFF1B1C1A, 0xFFE3E6E3)
        val dim = col(0xFF5B5A52, 0xFFB4C2BF)
        val gold = col(0xFF9A6B00, 0xFFE9B949)
        val hl = col(0xFFCFE8E2, 0xFF14504B)
        val hlFg = col(0xFF0F4C4A, 0xFFCFF3EC)
        val size = LocalSize.current
        val wide = size.width >= WIDE.width && size.height >= WIDE.height
        val tall = wide && size.height >= TALL.height
        val cdColor = when (theme) { ThemeMode.LIGHT -> 0xFF0F4C4A.toInt(); ThemeMode.DARK -> 0xFF7FD3C6.toInt(); else -> null }
        // RemoteViews are inflated in the launcher: force the app's direction via a container view
        AndroidRemoteViews(RemoteViews(ctx.packageName, if (d.rtl) R.layout.widget_root_rtl else R.layout.widget_root_ltr), R.id.widget_root,
            GlanceModifier.fillMaxSize()) {
        Column(GlanceModifier.fillMaxSize().background(bg).cornerRadius(20.dp).padding(horizontal = 12.dp, vertical = if (wide) 10.dp else 6.dp)
            .clickable(actionStartActivity<MainActivity>()), verticalAlignment = Alignment.CenterVertically) {
            if (wide || size.height >= 80.dp) Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(d.city, GlanceModifier.defaultWeight(), maxLines = 1, style = TextStyle(color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp))
                Text(d.hijri, maxLines = 1, style = TextStyle(color = gold, fontSize = 12.sp))
            }
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val name = TextStyle(color = gold, fontWeight = FontWeight.Bold, fontSize = if (tall) 24.sp else 19.sp)
                val time = TextStyle(color = dim, fontSize = if (tall) 15.sp else 13.sp)
                if (wide && !tall) Row(GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) { // one line saves height in 4x2
                    Text(d.next, maxLines = 1, style = name); Spacer(GlanceModifier.width(8.dp)); Text(d.nextTime, maxLines = 1, style = time)
                } else Column(GlanceModifier.defaultWeight()) {
                    Text(d.next, maxLines = 1, style = name)
                    Text(d.nextTime, maxLines = 1, style = time)
                }
                AndroidRemoteViews(countdown(ctx, d.remainingMs, if (tall) 30f else if (wide) 22f else 20f, cdColor), GlanceModifier.wrapContentWidth().wrapContentHeight())
            }
            if (tall) {
                Spacer(GlanceModifier.height(2.dp))
                d.rows.forEach { (name, time, on) ->
                    val st = TextStyle(color = if (on) hlFg else fg, fontSize = 15.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                    Row(GlanceModifier.fillMaxWidth().cornerRadius(10.dp).background(if (on) hl else bg).padding(horizontal = 10.dp, vertical = 1.dp)) {
                        Text(name, GlanceModifier.defaultWeight(), style = st)
                        Text(time, style = st)
                    }
                }
            } else if (wide) {
                Spacer(GlanceModifier.height(4.dp))
                Row(GlanceModifier.fillMaxWidth()) {
                    d.rows.forEach { (name, time, on) ->
                        Column(GlanceModifier.defaultWeight().cornerRadius(10.dp).background(if (on) hl else bg).padding(vertical = 1.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(name, maxLines = 1, style = TextStyle(color = if (on) hlFg else dim, fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal))
                            Text(time.substringBefore(' '), maxLines = 1, style = TextStyle(color = if (on) hlFg else fg, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
        }
    }

    /** Platform Chronometer counting down: ticks on its own, so the widget needs no polling. */
    private fun countdown(ctx: Context, remainingMs: Long, sp: Float, color: Int?) = RemoteViews(ctx.packageName, R.layout.widget_countdown).apply {
        setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + remainingMs, null, true)
        setChronometerCountDown(R.id.countdown, true)
        setTextViewTextSize(R.id.countdown, TypedValue.COMPLEX_UNIT_SP, sp)
        color?.let { setTextColor(R.id.countdown, it) }
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
    override fun onReceive(context: Context, intent: Intent) { Widgets.refreshAsync(this, context) }
}

object Widgets {
    const val REQ = 99
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun pi(ctx: Context, flags: Int) = PendingIntent.getBroadcast(ctx, REQ, Intent(ctx, WidgetTickReceiver::class.java), flags or PendingIntent.FLAG_IMMUTABLE)

    fun hasWidgets(ctx: Context): Boolean = runCatching {
        android.appwidget.AppWidgetManager.getInstance(ctx).getAppWidgetIds(android.content.ComponentName(ctx, PrayerWidgetReceiver::class.java)).isNotEmpty()
    }.getOrDefault(false)

    fun refresh(ctx: Context): Job? {
        if (!hasWidgets(ctx)) return null
        val app = ctx.applicationContext
        val job = scope.launch { runCatching { PrayerWidget().updateAll(app) } }
        scheduleBoundary(ctx)
        return job
    }

    /** From a receiver: keep the process alive until Glance has pushed the new RemoteViews (boot, alarm). */
    fun refreshAsync(r: BroadcastReceiver, ctx: Context) {
        val p: BroadcastReceiver.PendingResult? = r.goAsync() // null when onReceive is called directly (tests)
        refresh(ctx)?.invokeOnCompletion { p?.finish() } ?: p?.finish()
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
