package com.midroid.prayertimes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.midroid.prayertimes.data.AppSettings
import com.midroid.prayertimes.data.SettingsRepo
import com.midroid.prayertimes.data.SoundChoice
import com.midroid.prayertimes.domain.AlertMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class) @Config(sdk = [35])
class DefaultsTest {
    private val ctx = ApplicationProvider.getApplicationContext<Context>()
    private val expected = listOf(AlertMode.ADHAN, AlertMode.OFF, AlertMode.ADHAN, AlertMode.ADHAN, AlertMode.ADHAN, AlertMode.ADHAN)

    @Test fun freshInstallDefaults() {
        ctx.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().clear().commit()
        val s = SettingsRepo(ctx).state.value
        assertEquals(expected, s.alerts)
        assertEquals(List(6) { SoundChoice.ADHAN }, s.sounds)
        assertEquals(AppSettings().alerts, s.alerts)
    }

    @Test fun staleOffPrefsFromOldInstallAreResetOnce() {
        val sp = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
        sp.edit().clear().apply { for (i in 0 until 6) { putString("alert_$i", "OFF"); putString("sound_$i", "CHIME") } }.commit()
        assertEquals(expected, SettingsRepo(ctx).state.value.alerts)
        // user's later choice is kept after the one-time migration
        SettingsRepo(ctx).update { it.copy(alerts = List(6) { AlertMode.OFF }) }
        assertEquals(List(6) { AlertMode.OFF }, SettingsRepo(ctx).state.value.alerts)
    }
}
