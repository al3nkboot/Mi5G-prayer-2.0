package com.midroid.prayertimes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.midroid.prayertimes.R
import com.midroid.prayertimes.data.AppSettings
import com.midroid.prayertimes.data.ThemeMode
import com.midroid.prayertimes.data.SoundChoice
import com.midroid.prayertimes.notify.Sounds
import com.midroid.prayertimes.domain.AlertMode
import com.midroid.prayertimes.domain.Asr
import com.midroid.prayertimes.domain.HighLat
import com.midroid.prayertimes.domain.Method
import com.midroid.prayertimes.domain.Prayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(s: AppSettings, perms: Perms, update: ((AppSettings) -> AppSettings) -> Unit, onExact: () -> Unit, onGrantNotif: () -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.settings)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Section(stringResource(R.string.notifications))
            if (!perms.notifications) NoticeCard(stringResource(R.string.notif_perm_off), stringResource(R.string.grant), onGrantNotif)
            else if (!perms.exact) NoticeCard(stringResource(R.string.exact_off), stringResource(R.string.fix), onExact)
            Prayer.entries.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(prayerName(p), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    SingleChoiceSegmentedButtonRow(Modifier.testTag("alert_${p.name}")) {
                        listOf(AlertMode.OFF to R.string.mode_off, AlertMode.SILENT to R.string.mode_silent, AlertMode.ADHAN to R.string.mode_adhan).forEachIndexed { i, (m, r) ->
                            SegmentedButton(selected = s.alerts[p.ordinal] == m, onClick = { update { st -> st.copy(alerts = st.alerts.toMutableList().also { it[p.ordinal] = m }) } },
                                shape = SegmentedButtonDefaults.itemShape(i, 3), icon = {}) { Text(stringResource(r), style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                }
            }
            val opts = listOf(0, 5, 10, 15, 20, 30)
            Stepper(stringResource(R.string.reminder_before), if (s.reminderMinutes == 0) stringResource(R.string.off) else stringResource(R.string.minutes_fmt, s.reminderMinutes),
                { update { it.copy(reminderMinutes = opts[(opts.indexOf(it.reminderMinutes) - 1).coerceAtLeast(0)]) } },
                { update { it.copy(reminderMinutes = opts[(opts.indexOf(it.reminderMinutes) + 1).coerceAtMost(opts.size - 1)]) } })
            Text(stringResource(R.string.audio_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(top = 12.dp))
            SoundSection(s, update)
            HorizontalDivider(Modifier.padding(top = 12.dp))
            Section(stringResource(R.string.calc_method))
            Method.entries.forEach { m -> Choice(methodName(m), s.calc.method == m) { update { it.copy(calc = it.calc.copy(method = m)) } } }
            Section(stringResource(R.string.asr_method))
            Choice(stringResource(R.string.asr_shafi), s.calc.asr == Asr.SHAFI) { update { it.copy(calc = it.calc.copy(asr = Asr.SHAFI)) } }
            Choice(stringResource(R.string.asr_hanafi), s.calc.asr == Asr.HANAFI) { update { it.copy(calc = it.calc.copy(asr = Asr.HANAFI)) } }
            Section(stringResource(R.string.high_lat))
            listOf(HighLat.NONE to R.string.hl_none, HighLat.MIDDLE_OF_NIGHT to R.string.hl_middle, HighLat.SEVENTH_OF_NIGHT to R.string.hl_seventh, HighLat.TWILIGHT_ANGLE to R.string.hl_twilight)
                .forEach { (h, r) -> Choice(stringResource(r), s.calc.highLat == h) { update { it.copy(calc = it.calc.copy(highLat = h)) } } }
            Section(stringResource(R.string.adjustments))
            Prayer.entries.forEach { p ->
                Stepper(prayerName(p), s.calc.adjustments[p.ordinal].let { "%+d".format(java.util.Locale.ROOT, it) },
                    { update { st -> st.copy(calc = st.calc.copy(adjustments = st.calc.adjustments.toMutableList().also { it[p.ordinal] = (it[p.ordinal] - 1).coerceAtLeast(-60) })) } },
                    { update { st -> st.copy(calc = st.calc.copy(adjustments = st.calc.adjustments.toMutableList().also { it[p.ordinal] = (it[p.ordinal] + 1).coerceAtMost(60) })) } })
            }
            if (s.calc.adjustments.any { it != 0 }) TextButton(onClick = { update { it.copy(calc = it.calc.copy(adjustments = List(6) { 0 })) } }) { Text(stringResource(R.string.reset_adj)) }
            Section(stringResource(R.string.hijri_adjust))
            Stepper("", stringResource(R.string.days_fmt, s.hijriAdjust),
                { update { it.copy(hijriAdjust = (it.hijriAdjust - 1).coerceAtLeast(-2)) } },
                { update { it.copy(hijriAdjust = (it.hijriAdjust + 1).coerceAtMost(2)) } })
            Section(stringResource(R.string.theme))
            listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark)
                .forEach { (t, r) -> Choice(stringResource(r), s.theme == t) { update { it.copy(theme = t) } } }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.about), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.widget_hint), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(top = 12.dp))
            Section(stringResource(R.string.credits))
            Text(stringResource(R.string.credits_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("v0.4.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable private fun Section(t: String) {
    Text(t, Modifier.padding(top = 18.dp, bottom = 4.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

@Composable private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().selectable(selected, onClick = onClick, role = Role.RadioButton).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, null); Spacer(Modifier.width(10.dp)); Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable private fun Stepper(label: String, value: String, minus: () -> Unit, plus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        OutlinedIconButton(onClick = minus) { Text("−") }
        Text(value, Modifier.widthIn(min = 72.dp).padding(horizontal = 8.dp), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        OutlinedIconButton(onClick = plus) { Text("+") }
    }
}

@Composable
private fun SoundSection(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var playing by remember { mutableStateOf<SoundChoice?>(null) }
    DisposableEffect(Unit) { onDispose { Sounds.stopPreview() } }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val cr = ctx.contentResolver
            runCatching { cr.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            s.customUri?.let { old -> if (old != uri.toString()) runCatching { cr.releasePersistableUriPermission(android.net.Uri.parse(old), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
            val name = runCatching { cr.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } }.getOrNull()
            update { it.copy(customUri = uri.toString(), customName = name ?: uri.lastPathSegment, sounds = it.sounds.map { c -> c }) }
        }
    }
    Section(stringResource(R.string.sound))
    val labels = listOf(SoundChoice.CHIME to R.string.snd_chime, SoundChoice.ADHAN to R.string.snd_adhan, SoundChoice.CUSTOM to R.string.snd_custom)
    Prayer.entries.filter { s.alerts[it.ordinal] == AlertMode.ADHAN }.forEach { p ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(prayerName(p), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            SingleChoiceSegmentedButtonRow(Modifier.testTag("sound_${p.name}")) {
                labels.forEachIndexed { i, (c, r) ->
                    SegmentedButton(selected = s.sounds[p.ordinal] == c, enabled = c != SoundChoice.CUSTOM || s.customUri != null,
                        onClick = { update { st -> st.copy(sounds = st.sounds.toMutableList().also { it[p.ordinal] = c }) } },
                        shape = SegmentedButtonDefaults.itemShape(i, 3), icon = {}) { Text(stringResource(r), style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
    Text(if (s.customName != null) stringResource(R.string.my_file, s.customName) else stringResource(R.string.no_file),
        Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
    val lost = s.customUri != null && remember(s.customUri) { !Sounds.customReadable(ctx, s.customUri) }
    if (lost) Text(stringResource(R.string.file_lost), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }, modifier = Modifier.testTag("choose_file")) { Text(stringResource(R.string.choose_file)) }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEach { (c, r) ->
            val isPlaying = playing == c
            FilledTonalButton(enabled = c != SoundChoice.CUSTOM || s.customUri != null, onClick = {
                if (isPlaying) { Sounds.stopPreview(); playing = null }
                else { Sounds.playPreview(ctx, c, s.customUri) { playing = null }; playing = c }
            }) { Text((if (isPlaying) "■ " else "▶ ") + stringResource(r), style = MaterialTheme.typography.labelMedium) }
        }
    }
    Text(stringResource(R.string.adhan_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
