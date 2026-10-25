package com.midroid.prayertimes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
fun SettingsScreen(s: AppSettings, perms: Perms, update: ((AppSettings) -> AppSettings) -> Unit, onExact: () -> Unit, onGrantNotif: () -> Unit, onAbout: () -> Unit = {}, onDnd: () -> Unit = {}, onBack: () -> Unit) {
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
            Section(stringResource(R.string.auto_silent))
            Toggle(stringResource(R.string.auto_silent_toggle), s.autoSilent, "auto_silent") { v -> update { it.copy(autoSilent = v) } }
            if (s.autoSilent) {
                if (!perms.dnd) NoticeCard(stringResource(R.string.dnd_needed), stringResource(R.string.open_settings), onDnd, Modifier.testTag("dnd_note"))
                val mins = (10..40 step 5).toList()
                Stepper(stringResource(R.string.silent_duration), stringResource(R.string.minutes_fmt, s.silentMinutes),
                    { update { it.copy(silentMinutes = mins[(mins.indexOf(it.silentMinutes) - 1).coerceAtLeast(0)]) } },
                    { update { it.copy(silentMinutes = mins[(mins.indexOf(it.silentMinutes) + 1).coerceIn(0, mins.size - 1)]) } })
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    listOf(true to R.string.vibrate, false to R.string.mode_silent).forEachIndexed { i, (v, r) ->
                        SegmentedButton(selected = s.silentVibrate == v, onClick = { update { it.copy(silentVibrate = v) } }, shape = SegmentedButtonDefaults.itemShape(i, 2), icon = {}) { Text(stringResource(r)) }
                    }
                }
                Text(stringResource(R.string.auto_silent_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(top = 12.dp))
            Section(stringResource(R.string.adhkar_title))
            val offs = listOf(0, 15, 30, 45, 60, 90, 120)
            Toggle(stringResource(R.string.adhkar_am_toggle), s.adhkarAm, "adhkar_am") { v -> update { it.copy(adhkarAm = v) } }
            if (s.adhkarAm) Stepper(stringResource(R.string.after_fajr), stringResource(R.string.minutes_fmt, s.adhkarAmOffset),
                { update { it.copy(adhkarAmOffset = offs[(offs.indexOf(it.adhkarAmOffset) - 1).coerceAtLeast(0)]) } },
                { update { it.copy(adhkarAmOffset = offs[(offs.indexOf(it.adhkarAmOffset) + 1).coerceIn(0, offs.size - 1)]) } })
            Toggle(stringResource(R.string.adhkar_pm_toggle), s.adhkarPm, "adhkar_pm") { v -> update { it.copy(adhkarPm = v) } }
            if (s.adhkarPm) Stepper(stringResource(R.string.after_asr), stringResource(R.string.minutes_fmt, s.adhkarPmOffset),
                { update { it.copy(adhkarPmOffset = offs[(offs.indexOf(it.adhkarPmOffset) - 1).coerceAtLeast(0)]) } },
                { update { it.copy(adhkarPmOffset = offs[(offs.indexOf(it.adhkarPmOffset) + 1).coerceIn(0, offs.size - 1)]) } })
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
            Section(stringResource(R.string.language))
            val cur = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
            listOf("" to R.string.lang_system, "ar" to R.string.lang_ar, "en" to R.string.lang_en).forEach { (tag, r) ->
                Choice(stringResource(r), cur == tag) {
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                        if (tag.isEmpty()) androidx.core.os.LocaleListCompat.getEmptyLocaleList() else androidx.core.os.LocaleListCompat.forLanguageTags(tag))
                }
            }
            Section(stringResource(R.string.theme))
            listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark)
                .forEach { (t, r) -> Choice(stringResource(r), s.theme == t) { update { it.copy(theme = t) } } }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.about), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.widget_hint), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.padding(top = 12.dp))
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            OutlinedButton(onClick = { runCatching { uriHandler.openUri(PAYPAL_URL) } }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("support_paypal")) { Text(stringResource(R.string.support_paypal_btn)) }
            OutlinedButton(onClick = onAbout, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("about")) { Text(stringResource(R.string.about_title)) }
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

@Composable private fun Toggle(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, onValueChange = onChange, role = Role.Switch).padding(vertical = 6.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge); Switch(checked, null)
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
