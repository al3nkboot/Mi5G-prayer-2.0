package com.midroid.prayertimes

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.midroid.prayertimes.notify.AlarmReceiver
import com.midroid.prayertimes.notify.Silencer
import com.midroid.prayertimes.notify.SystemReceiver
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SilencerRobolectricTest {
    private val ctx = ApplicationProvider.getApplicationContext<PrayerApp>()
    private val audio = ctx.getSystemService(AudioManager::class.java)
    private val am = ctx.getSystemService(AlarmManager::class.java)
    private fun restoreAlarms() = shadowOf(am).scheduledAlarms.filter { shadowOf(it.operation).savedIntent.action == Silencer.ACTION_RESTORE }

    @Before fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(ctx.getSystemService(NotificationManager::class.java)).setNotificationPolicyAccessGranted(true)
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
    }

    @Test fun silenceThenRestoreSurvivesReboot() {
        Silencer.start(ctx, 20, vibrate = true, now = 1_000L)
        assertEquals(AudioManager.RINGER_MODE_VIBRATE, audio.ringerMode)
        assertEquals(1_000L + 20 * 60_000L, restoreAlarms().single().triggerAtTime)
        SystemReceiver().onReceive(ctx, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertEquals(1, restoreAlarms().size)
        AlarmReceiver().onReceive(ctx, Intent(Silencer.ACTION_RESTORE))
        assertEquals(AudioManager.RINGER_MODE_NORMAL, audio.ringerMode)
    }

    @Test fun manualChangeIsKept() {
        Silencer.start(ctx, 10, vibrate = false)
        audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE // user changed it during the window
        Silencer.restore(ctx)
        assertEquals(AudioManager.RINGER_MODE_VIBRATE, audio.ringerMode)
    }

    @Test fun deniedPermissionDoesNothing() {
        shadowOf(ctx.getSystemService(NotificationManager::class.java)).setNotificationPolicyAccessGranted(false)
        Silencer.start(ctx, 20, vibrate = true)
        assertEquals(AudioManager.RINGER_MODE_NORMAL, audio.ringerMode)
        assertTrue(restoreAlarms().isEmpty())
    }
}
