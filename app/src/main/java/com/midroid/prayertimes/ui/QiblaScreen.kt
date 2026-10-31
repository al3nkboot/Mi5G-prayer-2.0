package com.midroid.prayertimes.ui

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.midroid.prayertimes.R
import com.midroid.prayertimes.domain.Place
import com.midroid.prayertimes.domain.Qibla
import com.midroid.prayertimes.domain.MoonPosition
import com.midroid.prayertimes.domain.SunPosition
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class Heading(val trueDeg: Double, val lowAccuracy: Boolean)

private enum class QiblaMode { COMPASS, AR, SHADOW, SUN }

/** Test hook for screenshots: when set, sensors are bypassed. */
object QiblaSensorOverride { @Volatile var heading: Heading? = null; @Volatile var forceNoSensor = false }

/** Device heading (true north) from rotation vector, falling back to accelerometer + magnetometer. null = no sensor. */
@Composable
fun rememberHeading(place: Place): State<Heading?>? {
    QiblaSensorOverride.heading?.let { return remember { mutableStateOf(it) } }
    val ctx = LocalContext.current
    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rot = if (QiblaSensorOverride.forceNoSensor) null else sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    val acc = if (QiblaSensorOverride.forceNoSensor) null else sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val mag = if (QiblaSensorOverride.forceNoSensor) null else sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    if (rot == null && (acc == null || mag == null)) return null
    val state = remember { mutableStateOf<Heading?>(null) }
    val declination = remember(place) { GeomagneticField(place.lat.toFloat(), place.lon.toFloat(), 0f, System.currentTimeMillis()).declination.toDouble() }
    DisposableEffect(place) {
        val r = FloatArray(9); val o = FloatArray(3); val g = FloatArray(3); val m = FloatArray(3)
        var hasG = false; var hasM = false; var low = false
        var sx = 0.0; var sy = 0.0
        fun emit(azimuthRad: Float) {
            val a = 0.15
            sx = (1 - a) * sx + a * cos(azimuthRad.toDouble()); sy = (1 - a) * sy + a * sin(azimuthRad.toDouble())
            val mag0 = Math.toDegrees(atan2(sy, sx))
            state.value = Heading(((mag0 + declination) % 360 + 360) % 360, low)
        }
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> { SensorManager.getRotationMatrixFromVector(r, e.values); SensorManager.getOrientation(r, o); emit(o[0]) }
                    Sensor.TYPE_ACCELEROMETER -> { System.arraycopy(e.values, 0, g, 0, 3); hasG = true }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(e.values, 0, m, 0, 3); hasM = true
                        if (rot == null && hasG && SensorManager.getRotationMatrix(r, null, g, m)) { SensorManager.getOrientation(r, o); emit(o[0]) }
                    }
                }
            }
            override fun onAccuracyChanged(s: Sensor, accuracy: Int) {
                if (s.type == Sensor.TYPE_MAGNETIC_FIELD || s.type == Sensor.TYPE_ROTATION_VECTOR)
                    low = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
            }
        }
        if (rot != null) sm.registerListener(l, rot, SensorManager.SENSOR_DELAY_UI)
        else { sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_UI) }
        if (mag != null) sm.registerListener(l, mag, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(l) }
    }
    return state
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(place: Place, onBack: () -> Unit) {
    val target = remember(place) { Qibla.bearing(place.lat, place.lon) }
    val heading = rememberHeading(place)
    val view = LocalView.current
    var mode by remember { mutableStateOf(QiblaMode.COMPASS) }
    val sun = remember(place) { SunPosition.at(place.lat, place.lon, ZonedDateTime.now(place.zoneId)) }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.qibla)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(placeName(place), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.qibla_bearing, target.roundToInt(), Qibla.distanceKm(place.lat, place.lon).roundToInt()),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            ScrollableTabRow(selectedTabIndex = mode.ordinal, edgePadding = 8.dp) {
                Tab(mode == QiblaMode.COMPASS, { mode = QiblaMode.COMPASS }, text = { Text(stringResource(R.string.qibla_compass)) }, modifier = Modifier.testTag("qibla_compass"))
                Tab(mode == QiblaMode.AR, { mode = QiblaMode.AR }, text = { Text(stringResource(R.string.qibla_ar)) }, modifier = Modifier.testTag("qibla_ar_tab"))
                Tab(mode == QiblaMode.SHADOW, { mode = QiblaMode.SHADOW }, text = { Text(stringResource(R.string.qibla_shadow)) }, modifier = Modifier.testTag("qibla_shadow"))
                Tab(mode == QiblaMode.SUN, { mode = QiblaMode.SUN }, text = { Text(stringResource(R.string.qibla_sun)) }, modifier = Modifier.testTag("qibla_sun"))
            }
            Spacer(Modifier.height(16.dp))
            if (mode == QiblaMode.AR) {
                QiblaAr(place, target, heading?.value, Modifier.fillMaxWidth())
                return@Column
            }
            if (mode == QiblaMode.SUN || mode == QiblaMode.SHADOW) {
                SolarMethod(mode, sun.up, sun.azimuthDeg, sun.shadowAzimuthDeg, target)
                if (mode == QiblaMode.SUN) MoonLine(place, target)
                return@Column
            }
            if (heading == null) {
                Card(Modifier.fillMaxWidth().testTag("no_sensor")) {
                    Text(stringResource(R.string.qibla_no_sensor, target.roundToInt()), Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                }
                return@Column
            }
            val h = heading.value
            val facing = h != null && Qibla.isFacing(target, h.trueDeg)
            var wasFacing by remember { mutableStateOf(false) }
            LaunchedEffect(facing) {
                if (facing && !wasFacing) view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
                wasFacing = facing
            }
            Compass(h?.trueDeg ?: 0.0, target, facing, Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                when { h == null -> stringResource(R.string.qibla_waiting); facing -> stringResource(R.string.qibla_facing)
                    else -> stringResource(R.string.qibla_turn, abs(Qibla.delta(target, h.trueDeg)).roundToInt()) },
                fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("qibla_status"),
                color = if (facing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
            if (h?.lowAccuracy == true) {
                Spacer(Modifier.height(10.dp))
                NoticeCard(stringResource(R.string.qibla_calibrate), "", {})
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.qibla_tip), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SolarMethod(mode: QiblaMode, up: Boolean, sunAz: Double, shadowAz: Double, qibla: Double) {
    if (!up) {
        Text(stringResource(R.string.qibla_sun_down), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.testTag("qibla_sun_down"))
        return
    }
    val mark = if (mode == QiblaMode.SUN) sunAz else shadowAz
    val delta = Qibla.delta(mark, qibla)
    val sunAligned = mode == QiblaMode.SUN && abs(delta) < 2.0
    val shadowBehind = mode == QiblaMode.SHADOW && abs(abs(delta) - 180.0) < 2.0
    val line = when {
        sunAligned -> stringResource(R.string.qibla_sun_aligned)
        shadowBehind -> stringResource(R.string.qibla_shadow_aligned)
        mode == QiblaMode.SUN -> stringResource(R.string.qibla_sun_line, sunAz.roundToInt(), qibla.roundToInt(), stringResource(sideRes(delta)), abs(delta).roundToInt())
        else -> stringResource(R.string.qibla_shadow_line, shadowAz.roundToInt(), stringResource(sideRes(delta)), abs(delta).roundToInt())
    }
    Text(line, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("qibla_solar"))
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.qibla_sun_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}


@Composable
private fun MoonLine(place: Place, qibla: Double) {
    val moon = remember(place) { MoonPosition.at(place.lat, place.lon, java.time.ZonedDateTime.now(place.zoneId)) }
    val line = if (!moon.up) stringResource(R.string.qibla_moon_down)
        else stringResource(R.string.qibla_moon_line, moon.azimuthDeg.roundToInt(), stringResource(sideRes(Qibla.delta(moon.azimuthDeg, qibla))), kotlin.math.abs(Qibla.delta(moon.azimuthDeg, qibla)).roundToInt())
    Text(line, Modifier.padding(top = 12.dp).testTag("qibla_moon"), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
}

private fun sideRes(delta: Double) = if (delta >= 0) R.string.qibla_side_right else R.string.qibla_side_left

@Composable
private fun Compass(heading: Double, target: Double, facing: Boolean, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    Canvas(modifier) {
        val r = size.minDimension / 2; val ctr = Offset(size.width / 2, size.height / 2)
        drawCircle(c.surface, r, ctr)
        drawCircle(if (facing) Gold else c.outline, r, ctr, style = Stroke(if (facing) 10f else 4f))
        drawLine(c.primary, Offset(ctr.x, ctr.y - r - 4f), Offset(ctr.x, ctr.y - r + 34f), 8f)
        rotate(-heading.toFloat(), ctr) {
            for (i in 0 until 72) {
                val a = Math.toRadians(i * 5.0); val len = if (i % 18 == 0) 30f else if (i % 2 == 0) 16f else 8f
                val s = Offset(ctr.x + (r - 12f) * sin(a).toFloat(), ctr.y - (r - 12f) * cos(a).toFloat())
                val e = Offset(ctr.x + (r - 12f - len) * sin(a).toFloat(), ctr.y - (r - 12f - len) * cos(a).toFloat())
                drawLine(if (i == 0) Color(0xFFC0392B) else c.onSurfaceVariant, s, e, if (i % 18 == 0) 5f else 2f)
            }
            val n = Path().apply { moveTo(ctr.x, ctr.y - r + 50f); lineTo(ctr.x - 14f, ctr.y - r + 78f); lineTo(ctr.x + 14f, ctr.y - r + 78f); close() }
            drawPath(n, Color(0xFFC0392B))
            rotate(target.toFloat(), ctr) {
                drawLine(if (facing) Gold else c.primary, ctr, Offset(ctr.x, ctr.y - r + 96f), 10f)
                val k = 26f; val top = ctr.y - r + 96f - k * 2
                drawRect(Color(0xFF1B1C1A), Offset(ctr.x - k, top), androidx.compose.ui.geometry.Size(k * 2, k * 2))
                drawRect(Gold, Offset(ctr.x - k, top + k * 0.45f), androidx.compose.ui.geometry.Size(k * 2, k * 0.25f))
            }
        }
        drawCircle(c.primary, 14f, ctr)
    }
}
