package com.midroid.prayertimes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.midroid.prayertimes.R
import com.midroid.prayertimes.domain.ManualCoords
import com.midroid.prayertimes.domain.Place
import java.time.ZoneId

/** Enter latitude/longitude by hand. [onSave] null place = clear. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCoordsScreen(current: Place?, deviceZone: Boolean, onSave: (Place?, Boolean) -> Unit, onBack: () -> Unit) {
    var lat by remember { mutableStateOf(current?.lat?.toString() ?: "") }
    var lon by remember { mutableStateOf(current?.lon?.toString() ?: "") }
    var name by remember { mutableStateOf(current?.nameEn?.takeIf { it != "Manual coordinates" } ?: "") }
    var useDevice by remember { mutableStateOf(deviceZone) }
    var zone by remember { mutableStateOf(if (current != null && !deviceZone) current.zone else ZoneId.systemDefault().id) }
    var errors by remember { mutableStateOf(emptySet<ManualCoords.Error>()) }
    val zones = remember { ZoneId.getAvailableZoneIds().filter { it.contains('/') && !it.startsWith("Etc") && !it.startsWith("SystemV") }.sorted() }
    val suggestions = remember(zone) { if (zone.length < 2 || zone in zones) emptyList() else zones.filter { it.contains(zone, ignoreCase = true) }.take(6) }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.manual_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.manual_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val num = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            OutlinedTextField(lat, { lat = it }, Modifier.fillMaxWidth().testTag("manual_lat"), label = { Text(stringResource(R.string.manual_lat)) },
                singleLine = true, keyboardOptions = num, isError = ManualCoords.Error.LAT in errors,
                supportingText = { Text(stringResource(if (ManualCoords.Error.LAT in errors) R.string.manual_lat_err else R.string.manual_lat_hint)) })
            OutlinedTextField(lon, { lon = it }, Modifier.fillMaxWidth().testTag("manual_lon"), label = { Text(stringResource(R.string.manual_lon)) },
                singleLine = true, keyboardOptions = num, isError = ManualCoords.Error.LON in errors,
                supportingText = { Text(stringResource(if (ManualCoords.Error.LON in errors) R.string.manual_lon_err else R.string.manual_lon_hint)) })
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth().testTag("manual_name"), label = { Text(stringResource(R.string.manual_name)) }, singleLine = true)
            Row(Modifier.fillMaxWidth().clickable { useDevice = !useDevice }, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.manual_device_zone, ZoneId.systemDefault().id), Modifier.weight(1f))
                Switch(useDevice, { useDevice = it }, Modifier.testTag("manual_device_zone"))
            }
            if (!useDevice) {
                OutlinedTextField(zone, { zone = it }, Modifier.fillMaxWidth().testTag("manual_zone"), label = { Text(stringResource(R.string.manual_zone)) },
                    singleLine = true, isError = ManualCoords.Error.ZONE in errors,
                    supportingText = { Text(stringResource(if (ManualCoords.Error.ZONE in errors) R.string.manual_zone_err else R.string.manual_zone_hint)) })
                suggestions.forEach { z -> Text(z, Modifier.fillMaxWidth().clickable { zone = z }.padding(8.dp), color = MaterialTheme.colorScheme.primary) }
            }
            Button(onClick = {
                when (val r = ManualCoords.build(lat, lon, name, if (useDevice) null else zone)) {
                    is ManualCoords.Result.Ok -> { errors = emptySet(); onSave(r.place, useDevice); onBack() }
                    is ManualCoords.Result.Bad -> errors = r.errors
                }
            }, Modifier.fillMaxWidth().testTag("manual_save")) { Text(stringResource(R.string.manual_save)) }
            if (current != null) OutlinedButton(onClick = { onSave(null, true); onBack() }, Modifier.fillMaxWidth().testTag("manual_clear")) { Text(stringResource(R.string.manual_clear)) }
        }
    }
}
