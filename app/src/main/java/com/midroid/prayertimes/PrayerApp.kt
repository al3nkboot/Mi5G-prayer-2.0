package com.midroid.prayertimes

import android.app.Application
import com.midroid.prayertimes.data.SettingsRepo
import java.time.Clock

class PrayerApp : Application() {
    lateinit var settings: SettingsRepo
    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepo(this)
        com.midroid.prayertimes.notify.Notifier.channels(this)
        settings.onChange = { runCatching { com.midroid.prayertimes.notify.Alarms.reschedule(this, it) } }
        runCatching { com.midroid.prayertimes.notify.Alarms.reschedule(this) }
    }
    companion object { /** swappable for deterministic tests/screenshots */ @Volatile var clock: Clock = Clock.systemDefaultZone() }
}
