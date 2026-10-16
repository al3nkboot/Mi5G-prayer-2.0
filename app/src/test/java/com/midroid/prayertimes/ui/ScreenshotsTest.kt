package com.midroid.prayertimes.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.midroid.prayertimes.MainActivity
import com.midroid.prayertimes.PrayerApp
import com.midroid.prayertimes.data.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime

abstract class ShotsBase {
    init {
        MainActivity.ticking = false
        MainActivity.askOnStart = false
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(true)
        org.robolectric.Shadows.shadowOf(androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>())
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        // Friday 16 Oct 2026, 14:25 Muscat (between Dhuhr/Jumuah and Asr)
        PrayerApp.clock = Clock.fixed(ZonedDateTime.of(2026, 10, 16, 14, 25, 37, 0, ZoneId.of("Asia/Muscat")).toInstant(), ZoneId.of("Asia/Muscat"))
    }
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val dir = File("build/screenshots").apply { mkdirs() }
    fun shot(name: String) {
        rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { v.draw(Canvas(bmp)) }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    fun theme(t: ThemeMode) = rule.runOnUiThread { (rule.activity.application as PrayerApp).settings.update { it.copy(theme = t) } }
}

@RunWith(AndroidJUnit4::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ar-w393dp-h852dp-xhdpi")
class ScreenshotsArTest : ShotsBase() {
    @Test fun render() {
        theme(ThemeMode.LIGHT); shot("01_home_ar_light")
        theme(ThemeMode.DARK); shot("02_home_ar_dark")
        theme(ThemeMode.LIGHT)
        rule.onNodeWithTag("city").performClick(); shot("03_city_picker_ar")
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag("settings").performClick(); shot("04_settings_notifications_ar")
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
    }
}

@RunWith(AndroidJUnit4::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ar-w393dp-h852dp-xhdpi")
class ScreenshotsExactTest : ShotsBase() {
    @Test fun render() {
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(false)
        theme(ThemeMode.LIGHT)
        rule.activity.runOnUiThread { rule.activity.recreate() }
        rule.waitForIdle()
        shot("06_home_inexact_note_ar")
        rule.onNodeWithText("تفعيل").performClick(); shot("07_exact_alarm_explainer_ar")
    }
}

@RunWith(AndroidJUnit4::class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w393dp-h852dp-xhdpi")
class ScreenshotsEnTest : ShotsBase() {
    @Test fun render() { theme(ThemeMode.LIGHT); shot("05_home_en_light") }
}
