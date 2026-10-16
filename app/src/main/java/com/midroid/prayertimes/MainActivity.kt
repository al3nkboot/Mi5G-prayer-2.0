package com.midroid.prayertimes

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.midroid.prayertimes.notify.Alarms
import com.midroid.prayertimes.notify.Notifier
import com.midroid.prayertimes.ui.ExactAlarmScreen
import com.midroid.prayertimes.ui.Perms
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.midroid.prayertimes.data.Locator
import com.midroid.prayertimes.ui.CityScreen
import com.midroid.prayertimes.ui.HomeScreen
import com.midroid.prayertimes.ui.PrayerTheme
import com.midroid.prayertimes.ui.SettingsScreen
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

enum class Screen { HOME, CITY, SETTINGS, EXACT, QIBLA }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as PrayerApp).settings
        setContent {
            val s by repo.state.collectAsStateWithLifecycle()
            var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
            var now by remember { mutableStateOf(ZonedDateTime.now(PrayerApp.clock)) }
            LaunchedEffect(Unit) {
                while (ticking) { now = ZonedDateTime.now(PrayerApp.clock); delay(1000L - System.currentTimeMillis() % 1000L) }
            }
            val ctx = LocalContext.current
            fun readPerms() = Perms(Notifier.canPost(ctx), Alarms.canExact(ctx))
            var perms by remember { mutableStateOf(readPerms()) }
            val owner = LocalLifecycleOwner.current
            DisposableEffect(owner) {
                val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) { perms = readPerms(); runCatching { Alarms.reschedule(ctx) } } }
                owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
            }
            val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { perms = readPerms(); runCatching { Alarms.reschedule(ctx) } }
            val askNotif = { if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            val prefs = remember { ctx.getSharedPreferences("ui", MODE_PRIVATE) }
            LaunchedEffect(Unit) {
                if (askOnStart && !prefs.getBoolean("asked_notif", false) && !perms.notifications && Build.VERSION.SDK_INT >= 33) {
                    prefs.edit().putBoolean("asked_notif", true).apply(); askNotif()
                }
            }
            val openExact = {
                if (Build.VERSION.SDK_INT >= 31) runCatching {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                }
                Unit
            }
            PrayerTheme(s.theme) {
                Surface {
                    BackHandler(screen != Screen.HOME) { screen = Screen.HOME }
                    when (screen) {
                        Screen.HOME -> HomeScreen(s, now, perms, onCity = { screen = Screen.CITY }, onSettings = { screen = Screen.SETTINGS },
                            onExact = { screen = Screen.EXACT }, onGrantNotif = askNotif, onQibla = { screen = Screen.QIBLA })
                        Screen.SETTINGS -> SettingsScreen(s, perms, repo::update, onExact = { screen = Screen.EXACT }, onGrantNotif = askNotif) { screen = Screen.HOME }
                        Screen.QIBLA -> com.midroid.prayertimes.ui.QiblaScreen(s.place) { screen = Screen.HOME }
                        Screen.EXACT -> ExactAlarmScreen(perms.exact, onOpenSettings = openExact) { screen = Screen.HOME }
                        Screen.CITY -> CityRoute(s.place, onDone = { screen = Screen.HOME }, pick = { p -> repo.update { it.copy(place = p) } })
                    }
                }
            }
        }
    }

    companion object {
        /** disabled in screenshot tests to keep the composition idle */ @Volatile var ticking = true
        @Volatile var askOnStart = true
    }
}

@Composable
private fun CityRoute(current: com.midroid.prayertimes.domain.Place, onDone: () -> Unit, pick: (com.midroid.prayertimes.domain.Place) -> Unit) {
    val ctx = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    val locating = stringResource(R.string.locating); val failed = stringResource(R.string.location_failed); val denied = stringResource(R.string.location_denied)
    fun locate() {
        status = locating
        Locator.request(ctx) { loc ->
            if (loc == null) status = failed else { pick(Locator.toPlace(loc.latitude, loc.longitude)); onDone() }
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) locate() else status = denied }
    CityScreen(current, status, onPick = { pick(it); onDone() }, onBack = onDone, onLocate = {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) locate()
        else launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    })
}
