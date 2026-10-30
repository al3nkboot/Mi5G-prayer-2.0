package com.midroid.prayertimes.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.midroid.prayertimes.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(onBack: () -> Unit) {
    var count by rememberSaveable { mutableStateOf(0) }
    var target by rememberSaveable { mutableStateOf(33) }
    val view = LocalView.current
    val done = count >= target && target > 0
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.tasbih)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.tasbih_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.size(220.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = stringResource(R.string.count_tap)) {
                    val next = count + 1
                    count = next
                    if (next == target) view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
                }.testTag("tasbih_tap"),
                contentAlignment = Alignment.Center
            ) {
                Surface(Modifier.fillMaxSize(), shape = CircleShape, color = if (done) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$count", fontSize = 64.sp, fontWeight = FontWeight.Light, modifier = Modifier.testTag("tasbih_count"))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(if (done) stringResource(R.string.tasbih_done) else stringResource(R.string.count_fmt, count, target), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.tasbih_target), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                listOf(33, 34, 100).forEach { n ->
                    FilterChip(selected = target == n, onClick = { target = n; if (count >= n) count = 0 }, label = { Text("$n") }, modifier = Modifier.testTag("tasbih_$n"))
                }
            }
            TextButton(onClick = { count = 0 }, modifier = Modifier.padding(top = 8.dp).testTag("tasbih_reset")) { Text(stringResource(R.string.reset)) }
        }
    }
}
