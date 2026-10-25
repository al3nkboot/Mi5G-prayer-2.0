package com.midroid.prayertimes

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.midroid.prayertimes.notify.Alarms
import com.midroid.prayertimes.notify.SystemReceiver
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AlarmsRobolectricTest {
    private val ctx = ApplicationProvider.getApplicationContext<PrayerApp>()
    private val am = ctx.getSystemService(AlarmManager::class.java)
    private val now = ZonedDateTime.of(2026, 10, 16, 14, 0, 0, 0, ZoneId.of("Asia/Muscat"))
    init { PrayerApp.clock = Clock.fixed(now.toInstant(), now.zone) }

    @Test fun exactWhenAllowed() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val list = Alarms.reschedule(ctx)
        val sched = shadowOf(am).scheduledAlarms
        assertEquals(list.size, sched.size)
        assertTrue(sched.all { it.isAllowWhileIdle })
        assertEquals(list.first().at.toInstant().toEpochMilli(), shadowOf(am).nextScheduledAlarm!!.triggerAtTime)
    }

    @Test fun inexactFallbackAndNoDuplicatesOnReschedule() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        assertFalse(Alarms.canExact(ctx))
        val list = Alarms.reschedule(ctx); Alarms.reschedule(ctx)
        assertEquals(list.size, shadowOf(am).scheduledAlarms.size)
        assertTrue(list.isNotEmpty() && shadowOf(am).scheduledAlarms.all { it.isAllowWhileIdle }) // setAndAllowWhileIdle fallback
    }

    @Test fun settingsChangeAndBootReschedule() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        ctx.settings.update { it.copy(alerts = List(6) { com.midroid.prayertimes.domain.AlertMode.OFF }) }
        assertEquals(0, shadowOf(am).scheduledAlarms.size)
        ctx.settings.update { it.copy(alerts = com.midroid.prayertimes.data.AppSettings.DEFAULT_ALERTS) }
        val n = shadowOf(am).scheduledAlarms.size
        assertTrue(n > 0)
        SystemReceiver().onReceive(ctx, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertEquals(n, shadowOf(am).scheduledAlarms.size)
    }
}
