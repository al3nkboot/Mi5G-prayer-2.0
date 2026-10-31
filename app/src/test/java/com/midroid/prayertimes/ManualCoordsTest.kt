package com.midroid.prayertimes

import android.app.AlarmManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.midroid.prayertimes.data.Cities
import com.midroid.prayertimes.data.SettingsRepo
import com.midroid.prayertimes.domain.ManualCoords
import com.midroid.prayertimes.domain.ManualCoords.Error
import com.midroid.prayertimes.domain.ManualCoords.Result
import com.midroid.prayertimes.domain.Prayer
import com.midroid.prayertimes.notify.Alarms
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

class ManualCoordsParseTest {
    @Test fun decimalsAndArabicDigits() {
        assertEquals(30.0444, ManualCoords.parseNumber("30.0444")!!, 1e-9)
        assertEquals(30.0444, ManualCoords.parseNumber("30,0444")!!, 1e-9)
        assertEquals(31.2357, ManualCoords.parseNumber("٣١٫٢٣٥٧")!!, 1e-9)
        assertEquals(-33.5, ManualCoords.parseNumber(" -33.5 ")!!, 1e-9)
        assertNull(ManualCoords.parseNumber("abc")); assertNull(ManualCoords.parseNumber("")); assertNull(ManualCoords.parseNumber("1e5"))
    }
    @Test fun rangesAndZone() {
        val bad = ManualCoords.build("91", "-181", "", "Mars/Base") as Result.Bad
        assertEquals(setOf(Error.LAT, Error.LON, Error.ZONE), bad.errors)
        assertTrue(ManualCoords.build("90", "180", "", null) is Result.Ok)
        assertTrue(ManualCoords.build("-90", "-180", "", null) is Result.Ok)
        val ok = ManualCoords.build("30.0444", "31.2357", " بيتي ", "Africa/Cairo") as Result.Ok
        assertEquals("بيتي", ok.place.nameAr); assertEquals("Africa/Cairo", ok.place.zone); assertEquals(ManualCoords.ID, ok.place.id)
        val dev = ManualCoords.build("1", "2", "", null, deviceZone = "Asia/Muscat") as Result.Ok
        assertEquals("Asia/Muscat", dev.place.zone); assertEquals("إحداثيات يدوية", dev.place.nameAr)
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ManualCoordsRepoTest {
    private val ctx = ApplicationProvider.getApplicationContext<PrayerApp>()
    private val now = ZonedDateTime.of(2026, 10, 16, 14, 0, 0, 0, ZoneId.of("Asia/Muscat"))
    init { PrayerApp.clock = Clock.fixed(now.toInstant(), now.zone) }
    private val cairo = (ManualCoords.build("30.0444", "31.2357", "Home", "Africa/Cairo") as Result.Ok).place

    @Test fun overridesCityAndGpsPersistsAndClears() {
        val repo = ctx.settings
        val muscat = Cities.byId("muscat")!!
        repo.update { it.copy(place = muscat) }
        repo.setManual(cairo, deviceZone = false)
        assertEquals(cairo, repo.state.value.place)
        repo.update { it.copy(place = Cities.default) }            // city pick while manual is on
        assertEquals(ManualCoords.ID, repo.state.value.place.id)
        val reloaded = SettingsRepo(ctx).state.value               // survives restart
        assertEquals(cairo.lat, reloaded.place.lat, 0.0); assertEquals("Africa/Cairo", reloaded.place.zone); assertEquals("Home", reloaded.place.nameEn)
        repo.setManual(null, true)
        assertNull(repo.state.value.manual)
        assertEquals(Cities.default.id, repo.state.value.place.id)  // last city picked
        assertNull(SettingsRepo(ctx).state.value.manual)
    }

    @Test fun changeReschedulesAdhanAlarms() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val am = ctx.getSystemService(AlarmManager::class.java)
        ctx.settings.setManual(null, true)
        ctx.settings.update { it.copy(place = Cities.byId("muscat")!!) }
        val before = shadowOf(am).nextScheduledAlarm!!.triggerAtTime
        ctx.settings.setManual(cairo, false)
        val after = shadowOf(am).nextScheduledAlarm!!.triggerAtTime
        assertNotEquals(before, after)
        assertEquals(Alarms.reschedule(ctx).first().at.toInstant().toEpochMilli(), after)
        ctx.settings.setManual(null, true)
    }
}
