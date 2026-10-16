package com.midroid.prayertimes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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

enum class Screen { HOME, CITY, SETTINGS }

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
            PrayerTheme(s.theme) {
                Surface {
                    BackHandler(screen != Screen.HOME) { screen = Screen.HOME }
                    when (screen) {
                        Screen.HOME -> HomeScreen(s, now, onCity = { screen = Screen.CITY }, onSettings = { screen = Screen.SETTINGS })
                        Screen.SETTINGS -> SettingsScreen(s, repo::update) { screen = Screen.HOME }
                        Screen.CITY -> CityRoute(s.place, onDone = { screen = Screen.HOME }, pick = { p -> repo.update { it.copy(place = p) } })
                    }
                }
            }
        }
    }

    companion object { /** disabled in screenshot tests to keep the composition idle */ @Volatile var ticking = true }
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
