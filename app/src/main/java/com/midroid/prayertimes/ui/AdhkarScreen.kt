package com.midroid.prayertimes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.midroid.prayertimes.R
import com.midroid.prayertimes.data.Adhkar

/** Morning (tab 0) / evening (tab 1) adhkar with a tap counter per dhikr. Counters reset when the screen is left. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhkarScreen(tab: Int, onTab: (Int) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val all = remember { Adhkar.load(ctx) }
    val done = remember { mutableStateMapOf<String, Int>() }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.adhkar_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(tab) {
                listOf(R.string.morning, R.string.evening).forEachIndexed { i, r ->
                    Tab(tab == i, onClick = { onTab(i) }, text = { Text(stringResource(r)) }, modifier = Modifier.testTag("tab_$i"))
                }
            }
            val list = all.filter { if (tab == 0) it.morning else it.evening }
            LazyColumn(Modifier.fillMaxSize().testTag("adhkar_list"), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { "$tab-${it.id}" }) { d ->
                    val key = "$tab-${d.id}"; val n = done[key] ?: 0; val finished = n >= d.count
                    Card(Modifier.fillMaxWidth().alpha(if (finished) 0.55f else 1f)
                        .clickable(enabled = !finished, role = Role.Button, onClickLabel = stringResource(R.string.count_tap)) { done[key] = n + 1 }) {
                        Column(Modifier.padding(14.dp)) {
                            Text(d.text, style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.dhikr_source, d.id, d.ref), Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.repeat_fmt, d.count), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                                if (n > 0) TextButton(onClick = { done.remove(key) }) { Text(stringResource(R.string.reset)) }
                                FilledTonalButton(onClick = { done[key] = n + 1 }, enabled = !finished, modifier = Modifier.testTag("count_${d.id}")) {
                                    Text(if (finished) "✓" else stringResource(R.string.count_fmt, n, d.count), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                item { Text(stringResource(R.string.adhkar_credit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
