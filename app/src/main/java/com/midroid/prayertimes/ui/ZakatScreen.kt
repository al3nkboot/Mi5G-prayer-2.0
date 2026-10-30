package com.midroid.prayertimes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.midroid.prayertimes.R
import com.midroid.prayertimes.domain.Zakat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatScreen(onBack: () -> Unit) {
    var wealth by remember { mutableStateOf("") }
    var nisab by remember { mutableStateOf("") }
    val due = Zakat.due(parseAmount(wealth) ?: -1.0, parseAmount(nisab) ?: 0.0)
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.zakat)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.zakat_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(wealth, { wealth = it }, label = { Text(stringResource(R.string.zakat_wealth)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().testTag("zakat_wealth"), singleLine = true)
            OutlinedTextField(nisab, { nisab = it }, label = { Text(stringResource(R.string.zakat_nisab)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().testTag("zakat_nisab"), singleLine = true)
            if (due != null && parseAmount(wealth) != null) {
                val label = if (due == 0.0) stringResource(R.string.zakat_below) else stringResource(R.string.zakat_due, String.format(Locale.ROOT, "%.2f", due))
                Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("zakat_result"))
            }
        }
    }
}

internal fun parseAmount(s: String): Double? =
    s.trim().replace('٫', '.').replace('٬', '.').replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
