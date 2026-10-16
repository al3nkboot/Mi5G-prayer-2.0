package com.midroid.prayertimes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.midroid.prayertimes.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExactAlarmScreen(granted: Boolean, onOpenSettings: () -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.exact_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Notifications, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.exact_body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Start)
            Spacer(Modifier.height(24.dp))
            if (granted) Text(stringResource(R.string.exact_ok), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            else {
                Button(onClick = onOpenSettings, Modifier.fillMaxWidth().testTag("open_exact")) { Text(stringResource(R.string.exact_open)) }
                TextButton(onClick = onBack, Modifier.fillMaxWidth()) { Text(stringResource(R.string.exact_later)) }
            }
        }
    }
}

@Composable
fun NoticeCard(text: String, action: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            TextButton(onClick = onClick) { Text(action) }
        }
    }
}
