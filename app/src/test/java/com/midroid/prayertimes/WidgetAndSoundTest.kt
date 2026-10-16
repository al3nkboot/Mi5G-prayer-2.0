package com.midroid.prayertimes

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.compose
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.midroid.prayertimes.data.Cities
import com.midroid.prayertimes.data.SoundChoice
import com.midroid.prayertimes.domain.Prayer
import com.midroid.prayertimes.domain.PrayerEngine
import com.midroid.prayertimes.notify.Sounds
import com.midroid.prayertimes.widget.PrayerWidget
import com.midroid.prayertimes.widget.PrayerWidgetReceiver
import com.midroid.prayertimes.widget.WidgetModel
import com.midroid.prayertimes.widget.Widgets
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ar-w393dp-h852dp-xhdpi")
class WidgetAndSoundTest {
    private val ctx = ApplicationProvider.getApplicationContext<PrayerApp>()
    private val z = ZoneId.of("Asia/Muscat")
    private val now = ZonedDateTime.of(2026, 10, 16, 14, 25, 37, 0, z)
    init { PrayerApp.clock = Clock.fixed(now.toInstant(), z) }

    @Test fun widgetModelRefreshesAtNextBoundaryOrMidnight() {
        val s = ctx.settings.state.value
        val d = WidgetModel.build(ctx, s, now)
        val asr = PrayerEngine.day(s.place, now.toLocalDate(), s.calc)[Prayer.ASR]
        assertEquals(asr, d.refreshAt)
        assertEquals(java.time.Duration.between(now, asr).toMillis(), d.remainingMs)
        assertEquals("العصر", d.next)
        assertEquals(6, d.rows.size); assertTrue(d.rows[Prayer.ASR.ordinal].third)
        assertEquals("الجمعة", d.rows[Prayer.DHUHR.ordinal].first) // Friday
        // after Isha: refresh at local midnight, next = tomorrow's Fajr
        val late = WidgetModel.build(ctx, s, ZonedDateTime.of(2026, 10, 16, 22, 0, 0, 0, z))
        assertEquals(ZonedDateTime.of(2026, 10, 17, 0, 0, 0, 0, z), late.refreshAt)
        assertEquals("الفجر", late.next)
        assertTrue(late.rows.none { it.third })
    }

    @Test fun boundaryAlarmOnlyWhenWidgetsExist() {
        assertNull(Widgets.scheduleBoundary(ctx, now))
        shadowOf(AppWidgetManager.getInstance(ctx)).createWidget(PrayerWidgetReceiver::class.java, R.layout.widget_countdown)
        val at = Widgets.scheduleBoundary(ctx, now)!!
        val am = ctx.getSystemService(android.app.AlarmManager::class.java)
        assertTrue(shadowOf(am).scheduledAlarms.any { it.triggerAtTime == at.toInstant().toEpochMilli() })
    }

    @Test fun lostCustomFileFallsBackToChime() {
        assertFalse(Sounds.customReadable(ctx, "content://com.example.gone/audio/1"))
        assertFalse(Sounds.customReadable(ctx, null))
        assertEquals(SoundChoice.CHIME, Sounds.effective(SoundChoice.CUSTOM, false))
        assertEquals(SoundChoice.CUSTOM, Sounds.effective(SoundChoice.CUSTOM, true))
        assertEquals(SoundChoice.ADHAN, Sounds.effective(SoundChoice.ADHAN, false))
        assertEquals(R.raw.adhan_hargeisa, Sounds.rawFor(SoundChoice.ADHAN)); assertEquals(R.raw.prayer_chime, Sounds.rawFor(SoundChoice.CHIME))
    }

    @Test fun hamraUsesRealCoordinates() {
        val h = Cities.byId("hamra")!!
        assertEquals(23.11519, h.lat, 1e-6); assertEquals(57.29143, h.lon, 1e-6)
    }

    @OptIn(ExperimentalGlanceApi::class)
    private fun renderWidget(name: String, w: Int, h: Int, night: Boolean) {
        val c = if (night) ctx.createConfigurationContext(android.content.res.Configuration(ctx.resources.configuration).apply {
            uiMode = (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or android.content.res.Configuration.UI_MODE_NIGHT_YES }) else ctx
        val rv = runBlocking { PrayerWidget().compose(c, size = DpSize(w.dp, h.dp)) }
        val parent = FrameLayout(c)
        val v = rv.apply(c, parent)
        val d = c.resources.displayMetrics.density
        val wp = (w * d).toInt(); val hp = (h * d).toInt()
        parent.addView(v)
        parent.measure(View.MeasureSpec.makeMeasureSpec(wp, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(hp, View.MeasureSpec.EXACTLY))
        parent.layout(0, 0, wp, hp)
        val pad = (16 * d).toInt()
        val bmp = Bitmap.createBitmap(wp + 2 * pad, hp + 2 * pad, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp); cv.drawColor(if (night) 0xFF2B3A55.toInt() else 0xFF8FB7D9.toInt()) // wallpaper-ish backdrop
        cv.translate(pad.toFloat(), pad.toFloat()); parent.draw(cv)
        File("build/screenshots").apply { mkdirs() }.let { File(it, "$name.png").outputStream().use { o -> bmp.compress(Bitmap.CompressFormat.PNG, 100, o) } }
    }

    @Test fun widgetScreenshots() {
        renderWidget("12_widget_large_ar_light", 300, 260, false)
        renderWidget("13_widget_large_ar_dark", 300, 260, true)
        renderWidget("14_widget_small_ar_light", 180, 90, false)
    }
}
