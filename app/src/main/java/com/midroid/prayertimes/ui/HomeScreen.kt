package com.midroid.prayertimes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.midroid.prayertimes.R
import com.midroid.prayertimes.data.AppSettings
import com.midroid.prayertimes.domain.DayPhase
import com.midroid.prayertimes.domain.Hijri
import com.midroid.prayertimes.domain.Night
import com.midroid.prayertimes.domain.NowState
import com.midroid.prayertimes.domain.Prayer
import com.midroid.prayertimes.domain.PrayerEngine
import com.midroid.prayertimes.domain.Routine
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class Perms(val notifications: Boolean, val exact: Boolean, val dnd: Boolean = true)

@Composable
fun HomeScreen(
    s: AppSettings,
    now: ZonedDateTime,
    perms: Perms,
    onCity: () -> Unit,
    onSettings: () -> Unit,
    onExact: () -> Unit,
    onGrantNotif: () -> Unit,
    onQibla: () -> Unit = {},
    onAdhkar: () -> Unit = {},
    onTasbih: () -> Unit = {},
    onZakat: () -> Unit = {},
) {
    val st = remember(s, now.withSecond(0).withNano(0)) { PrayerEngine.now(s.place, now, s.calc) }
    val night = remember(s, now.withSecond(0).withNano(0)) { Night.around(s.place, now, s.calc) }
    val remaining = Duration.between(now, st.nextAt)
    val ar = isArabic()
    val local = now.withZoneSameInstant(s.place.zoneId)
    val phase = Routine.phase(local, st.day[Prayer.FAJR], st.day[Prayer.SUNRISE], st.day[Prayer.DHUHR], st.day[Prayer.ASR], st.day[Prayer.MAGHRIB], night.lastThird, night.fajr)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onCity).padding(8.dp).testTag("city"), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(placeName(s.place), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (countryName(s.place).isNotEmpty()) Text(countryName(s.place), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onQibla, modifier = Modifier.testTag("qibla")) { Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_qibla), stringResource(R.string.qibla)) }
            IconButton(onClick = onSettings, modifier = Modifier.testTag("settings")) { Icon(Icons.Filled.Settings, stringResource(R.string.settings)) }
        }
        val anyAlert = s.alerts.any { it != com.midroid.prayertimes.domain.AlertMode.OFF }
        if (anyAlert && !perms.notifications) { NoticeCard(stringResource(R.string.notif_perm_off), stringResource(R.string.grant), onGrantNotif); Spacer(Modifier.height(8.dp)) }
        else if (anyAlert && !perms.exact) { NoticeCard(stringResource(R.string.exact_off), stringResource(R.string.fix), onExact, Modifier.testTag("exact_note")); Spacer(Modifier.height(8.dp)) }
        HeroCard(st, remaining, local)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatGregorian(local.toLocalDate(), ar), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Hijri.format(Hijri.at(s.place, now, s.calc, s.hijriAdjust), ar), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        RoutineCard(phase)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.tools), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolCard(stringResource(R.string.qibla), onQibla, Modifier.weight(1f).testTag("tool_qibla"))
            ToolCard(stringResource(R.string.adhkar_title), onAdhkar, Modifier.weight(1f).testTag("adhkar"))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolCard(stringResource(R.string.tasbih), onTasbih, Modifier.weight(1f).testTag("tasbih"))
            ToolCard(stringResource(R.string.zakat), onZakat, Modifier.weight(1f).testTag("zakat"))
        }
        Spacer(Modifier.height(12.dp))
        Prayer.entries.forEach { p -> PrayerRow(p, st, ar) }
        Spacer(Modifier.height(6.dp))
        NightRow(stringResource(R.string.midnight), formatTime(night.midnight, ar), stringResource(R.string.last_third), formatTime(night.lastThird, ar))
        Spacer(Modifier.height(12.dp))
        Text(methodName(s.calc.method), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HeroCard(st: NowState, remaining: Duration, local: ZonedDateTime) {
    val nextName = prayerName(st.next, st.nextAt.toLocalDate())
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Teal, TealLight))).padding(20.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.after_named, nextName), color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(PrayerEngine.formatCountdown(remaining), color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace, style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr), modifier = Modifier.testTag("countdown"))
            Text(formatTime(st.nextAt, isArabic()), color = Gold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            SunArc(st, local, Modifier.fillMaxWidth().height(70.dp))
        }
    }
}

@Composable
private fun NightRow(l1: String, t1: String, l2: String, t2: String) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(horizontal = 16.dp, vertical = 8.dp).testTag("night_row"),
        verticalAlignment = Alignment.CenterVertically) {
        Text(l1, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant, modifier = Modifier.testTag("midnight"))
        Spacer(Modifier.width(6.dp))
        Text(t1, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(l2, style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant, modifier = Modifier.testTag("last_third"))
        Spacer(Modifier.width(6.dp))
        Text(t2, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RoutineCard(phase: DayPhase) {
    val text = stringResource(when (phase) {
        DayPhase.LAST_THIRD -> R.string.routine_last_third
        DayPhase.MORNING_ADHKAR -> R.string.routine_morning
        DayPhase.FORENOON -> R.string.routine_forenoon
        DayPhase.AFTERNOON -> R.string.routine_afternoon
        DayPhase.EVENING_ADHKAR -> R.string.routine_evening
        DayPhase.NIGHT -> R.string.routine_night
    })
    Card(Modifier.fillMaxWidth().testTag("routine")) {
        Column(Modifier.padding(14.dp)) {
            Text(stringResource(R.string.routine_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun ToolCard(label: String, onClick: () -> Unit, modifier: Modifier) {
    Card(modifier.clickable(onClick = onClick)) {
        Text(label, Modifier.padding(horizontal = 12.dp, vertical = 16.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
    }
}

/** Original day-arc: sun travels from sunrise to maghrib; dashed night arc otherwise. */
@Composable
private fun SunArc(st: NowState, local: ZonedDateTime, modifier: Modifier) {
    val rise = st.day[Prayer.SUNRISE].toEpochSecond(); val set = st.day[Prayer.MAGHRIB].toEpochSecond()
    val t = local.toEpochSecond()
    val frac = ((t - rise).toFloat() / (set - rise)).coerceIn(0f, 1f)
    val isDay = t in rise..set
    Canvas(modifier) {
        val w = size.width; val h = size.height * 2
        val r = Size(w * .9f, h * .9f); val tl = Offset(w * .05f, size.height * .1f)
        drawLine(Color.White.copy(alpha = .25f), Offset(0f, size.height * .1f + r.height / 2), Offset(w, size.height * .1f + r.height / 2), 2f)
        drawArc(Color.White.copy(alpha = .35f), 180f, 180f, false, tl, r, style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
        if (isDay) drawArc(Gold, 180f, 180f * frac, false, tl, r, style = Stroke(5f, cap = StrokeCap.Round))
        val ang = PI + PI * frac
        val cx = tl.x + r.width / 2 + (r.width / 2) * cos(ang).toFloat(); val cy = tl.y + r.height / 2 + (r.height / 2) * sin(ang).toFloat()
        drawCircle(if (isDay) Gold else Color.White.copy(alpha = .6f), 11f, Offset(cx, cy))
        if (isDay) drawCircle(Gold.copy(alpha = .3f), 22f, Offset(cx, cy))
    }
}

@Composable
private fun PrayerRow(p: Prayer, st: NowState, ar: Boolean) {
    val isCurrent = st.current == p && p != Prayer.SUNRISE
    val isNext = st.next == p && st.nextAt.toLocalDate() == st.day.date
    val c = MaterialTheme.colorScheme
    val bg = when { isCurrent -> c.primaryContainer; else -> c.surface }
    var m = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(bg)
    if (isNext) m = m.border(1.5.dp, c.secondary, RoundedCornerShape(16.dp))
    Row(m.padding(horizontal = 16.dp, vertical = 14.dp).testTag("row_${p.name}"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (isCurrent) c.primary else if (isNext) c.secondary else c.outline))
        Spacer(Modifier.width(12.dp))
        Text(prayerName(p, st.day.date), style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isCurrent || isNext) FontWeight.Bold else FontWeight.Normal,
            color = if (p == Prayer.SUNRISE) c.onSurfaceVariant else c.onSurface)
        if (isCurrent || isNext) {
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (isCurrent) R.string.now_label else R.string.next_label),
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrent) c.onPrimary else c.onSecondaryContainer,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isCurrent) c.primary else c.secondaryContainer).padding(horizontal = 8.dp, vertical = 2.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(formatTime(st.day[p], ar), style = MaterialTheme.typography.titleMedium, fontWeight = if (isCurrent || isNext) FontWeight.Bold else FontWeight.Medium)
    }
}
