package com.midroid.prayertimes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.midroid.prayertimes.R
import com.midroid.prayertimes.data.Cities
import com.midroid.prayertimes.domain.Place
import com.midroid.prayertimes.domain.Qibla
import kotlin.math.roundToInt

/** Offline schematic from bundled city coordinates. No tiles and no network. */
@Composable
fun QiblaMap(place: Place, qibla: Double, modifier: Modifier = Modifier) {
    val points = remember(place) {
        val extras = Cities.all + place + Place("kaaba", "", "", Qibla.KAABA_LAT, Qibla.KAABA_LON, "Asia/Riyadh")
        Quad(
            extras.minOf { it.lat } - 2,
            extras.maxOf { it.lat } + 2,
            extras.minOf { it.lon } - 2,
            extras.maxOf { it.lon } + 2,
            Cities.all,
        )
    }
    Column(modifier.testTag("qibla_map")) {
        Canvas(Modifier.fillMaxWidth().height(260.dp).padding(4.dp)) {
            drawRect(Color(0xFF12343A), size = Size(size.width, size.height))
            fun xy(lat: Double, lon: Double): Offset {
                val x = ((lon - points.minLon) / (points.maxLon - points.minLon)).toFloat() * size.width
                val y = (1f - ((lat - points.minLat) / (points.maxLat - points.minLat)).toFloat()) * size.height
                return Offset(x, y)
            }
            points.cities.forEach { c -> drawCircle(Color.White.copy(alpha = 0.35f), 3.2f, xy(c.lat, c.lon)) }
            val you = xy(place.lat, place.lon)
            val kaaba = xy(Qibla.KAABA_LAT, Qibla.KAABA_LON)
            drawLine(Gold, you, kaaba, 4f)
            drawCircle(Color.White, 8f, you)
            val k = 12f
            drawPath(Path().apply {
                moveTo(kaaba.x, kaaba.y - k)
                lineTo(kaaba.x + k, kaaba.y)
                lineTo(kaaba.x, kaaba.y + k)
                lineTo(kaaba.x - k, kaaba.y)
                close()
            }, Color(0xFF1B1C1A))
            drawCircle(Gold, 4f, kaaba)
        }
        Text(
            stringResource(R.string.qibla_map_line, qibla.roundToInt(), Qibla.distanceKm(place.lat, place.lon).roundToInt()),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Text(
            stringResource(R.string.qibla_map_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private data class Quad(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double, val cities: List<Place>)
