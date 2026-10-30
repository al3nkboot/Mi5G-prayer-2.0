package com.midroid.prayertimes.ui

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.midroid.prayertimes.data.Dhikr
import java.util.Locale

/** Morning / evening Hisn items, plus a short after-prayer and waking set. Counters reset when the screen is left. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhkarScreen(tab: Int, onTab: (Int) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val all = remember { Adhkar.load(ctx) }
    val extra = remember { Adhkar.loadExtra(ctx) }
    val done = remember { mutableStateMapOf<String, Int>() }
    var short by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) { onDispose { speaker?.shutdown() } }
    fun listen(text: String) {
        val engine = speaker
        if (engine == null) {
            speaker = TextToSpeech(ctx) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    speaker?.language = Locale("ar")
                    speaker?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dhikr")
                }
            }
        } else {
            engine.language = Locale("ar")
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "dhikr")
        }
    }
    val titles = listOf(R.string.morning, R.string.evening, R.string.adhkar_after, R.string.adhkar_waking)
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.adhkar_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = tab.coerceIn(0, titles.lastIndex)) {
                titles.forEachIndexed { i, r ->
                    Tab(tab == i, onClick = { onTab(i) }, text = { Text(stringResource(r)) }, modifier = Modifier.testTag("tab_$i"))
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { short = !short }, modifier = Modifier.testTag("adhkar_short")) {
                    Text(stringResource(if (short) R.string.adhkar_full else R.string.adhkar_short))
                }
            }
            val list = when (tab) {
                2 -> extra.after
                3 -> extra.waking
                else -> all.filter { if (tab == 0) it.morning else it.evening }
            }.let { rows -> if (short) rows.filter { it.count < 10 } else rows }
            LazyColumn(Modifier.fillMaxSize().testTag("adhkar_list"), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (tab >= 2) item { Text(stringResource(R.string.adhkar_extra_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(list, key = { "$tab-${it.id}" }) { d -> DhikrCard(d, tab, done, ::listen) }
                item { Text(stringResource(if (tab < 2) R.string.adhkar_credit else R.string.adhkar_extra_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun DhikrCard(d: Dhikr, tab: Int, done: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Int>, listen: (String) -> Unit) {
    val key = "$tab-${d.id}"
    val n = done[key] ?: 0
    val finished = n >= d.count
    Card(Modifier.fillMaxWidth().alpha(if (finished) 0.55f else 1f)
        .clickable(enabled = !finished, role = Role.Button, onClickLabel = stringResource(R.string.count_tap)) { done[key] = n + 1 }) {
        Column(Modifier.padding(14.dp)) {
            Text(d.text, style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.dhikr_source, d.id, d.ref), Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.repeat_fmt, d.count), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { listen(d.text) }, modifier = Modifier.testTag("listen_${d.id}")) { Text(stringResource(R.string.adhkar_listen)) }
                if (n > 0) TextButton(onClick = { done.remove(key) }) { Text(stringResource(R.string.reset)) }
                FilledTonalButton(onClick = { done[key] = n + 1 }, enabled = !finished, modifier = Modifier.testTag("count_${d.id}")) {
                    Text(if (finished) "✓" else stringResource(R.string.count_fmt, n, d.count), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
