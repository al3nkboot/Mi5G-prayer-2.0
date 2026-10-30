package com.midroid.prayertimes.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.midroid.prayertimes.R
import com.midroid.prayertimes.domain.QuranText
import com.midroid.prayertimes.domain.Surah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val surahs = remember {
        QuranText.parse(ctx.assets.open("quran-uthmani.txt").bufferedReader(Charsets.UTF_8).use { it.readText() })
    }
    var open by remember { mutableStateOf<Int?>(null) }
    val current = open?.let { n -> surahs.firstOrNull { it.number == n } }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (current == null) stringResource(R.string.quran) else QuranNames.label(current.number, isArabic())) },
            navigationIcon = {
                IconButton(onClick = { if (open != null) open = null else onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            },
        )
    }) { pad ->
        if (current == null) SurahList(surahs, Modifier.padding(pad), onOpen = { open = it }) {
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tanzil.net/updates/"))) }
        } else Reader(current, Modifier.padding(pad))
    }
}

@Composable
private fun SurahList(surahs: List<Surah>, modifier: Modifier, onOpen: (Int) -> Unit, onTanzil: () -> Unit) {
    val ar = isArabic()
    LazyColumn(modifier.fillMaxSize().testTag("quran_list"), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(stringResource(R.string.quran_note), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.quran_audio_note), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(surahs, key = { it.number }) { s ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(s.number) }.testTag("surah_${s.number}")) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(QuranNames.label(s.number, ar), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("${s.ayahs.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            TextButton(onClick = onTanzil, modifier = Modifier.testTag("tanzil_link")) { Text(stringResource(R.string.quran_credit)) }
        }
    }
}

@Composable
private fun Reader(surah: Surah, modifier: Modifier) {
    var hide by remember(surah.number) { mutableStateOf(false) }
    var revealed by remember(surah.number) { mutableIntStateOf(1) }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { hide = !hide; revealed = 1 }, modifier = Modifier.testTag("tahfeez")) {
                Text(stringResource(if (hide) R.string.quran_show else R.string.quran_tahfeez))
            }
        }
        LazyColumn(Modifier.fillMaxSize().testTag("quran_reader"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(surah.ayahs, key = { it.number }) { a ->
                val shown = if (!hide) a.text else mask(a.text, revealed)
                Text(
                    shown,
                    Modifier.fillMaxWidth().clickable(enabled = hide) { revealed += 1 }.testTag("ayah_${a.number}"),
                    textAlign = TextAlign.Right,
                    fontSize = 22.sp,
                    lineHeight = 36.sp,
                )
                Text("${a.number}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            item { Text(stringResource(R.string.quran_credit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** Hides words after [keep] for memorization. The stored verse is not changed. */
internal fun mask(text: String, keep: Int): String {
    val words = text.split(' ')
    return words.mapIndexed { i, w -> if (i < keep) w else "····" }.joinToString(" ")
}
