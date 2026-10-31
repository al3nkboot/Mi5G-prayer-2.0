package com.midroid.prayertimes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.midroid.prayertimes.R
import com.midroid.prayertimes.data.Cities
import com.midroid.prayertimes.domain.Place

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityScreen(current: Place, status: String?, onPick: (Place) -> Unit, onLocate: () -> Unit, onBack: () -> Unit, manualActive: Boolean = false, onManual: () -> Unit = {}) {
    var q by remember { mutableStateOf("") }
    val list = remember(q) {
        val t = q.trim().lowercase()
        if (t.isEmpty()) Cities.all else Cities.all.filter { it.nameAr.contains(t) || it.nameEn.lowercase().contains(t) || it.countryEn.lowercase().contains(t) || it.countryAr.contains(t) }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.choose_city)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("search"), singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text(stringResource(R.string.search_city)) })
            FilledTonalButton(onClick = onLocate, modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp)) {
                Icon(Icons.Filled.LocationOn, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.use_location))
            }
            OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth().padding(16.dp, 0.dp).testTag("manual")) { Text(stringResource(R.string.manual_title)) }
            if (manualActive) Text(stringResource(R.string.manual_active), Modifier.padding(16.dp, 4.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (status != null) Text(status, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.fillMaxSize()) {
                items(list, key = { it.id }) { p ->
                    ListItem(
                        headlineContent = { Text(placeName(p)) },
                        supportingContent = { Text(countryName(p)) },
                        trailingContent = { if (p.id == current.id) Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { onPick(p) },
                    )
                }
            }
        }
    }
}
