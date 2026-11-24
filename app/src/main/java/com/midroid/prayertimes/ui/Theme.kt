package com.midroid.prayertimes.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.midroid.prayertimes.R
import com.midroid.prayertimes.domain.Prayer
import com.midroid.prayertimes.data.ThemeMode

val Teal = Color(0xFF0F4C4A)
val TealLight = Color(0xFF1F7A72)
val Gold = Color(0xFFE9B949)
val Sand = Color(0xFFF7F3EA)

private val Light = lightColorScheme(
    primary = Teal, onPrimary = Color.White, primaryContainer = Color(0xFFCFE8E2), onPrimaryContainer = Color(0xFF062B29),
    secondary = Color(0xFF9A6B00), secondaryContainer = Color(0xFFFBE7B5), onSecondaryContainer = Color(0xFF3A2800),
    background = Sand, onBackground = Color(0xFF1B1C1A), surface = Color(0xFFFFFCF5), onSurface = Color(0xFF1B1C1A),
    surfaceVariant = Color(0xFFECE6D8), onSurfaceVariant = Color(0xFF5B5A52), outline = Color(0xFFCFC8B6),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF7FD3C6), onPrimary = Color(0xFF003733), primaryContainer = Color(0xFF14504B), onPrimaryContainer = Color(0xFFCFF3EC),
    secondary = Gold, secondaryContainer = Color(0xFF4D3A0A), onSecondaryContainer = Color(0xFFFBE7B5),
    background = Color(0xFF0E1A1A), onBackground = Color(0xFFE3E6E3), surface = Color(0xFF152424), onSurface = Color(0xFFE3E6E3),
    surfaceVariant = Color(0xFF1E3131), onSurfaceVariant = Color(0xFFB4C2BF), outline = Color(0xFF3A4F4D),
)

// "Dark gold" theme: warm near-black, gold accent, hairline borders
val GoldAccent = Color(0xFFD4B26A)
val GoldBg = Color(0xFF14110D)
val GoldHairline = Color(0xFF3A3226)
private val DarkGold = darkColorScheme(
    primary = GoldAccent, onPrimary = Color(0xFF2A1F05), primaryContainer = Color(0xFF3D3220), onPrimaryContainer = Color(0xFFF3E3BC),
    secondary = GoldAccent, onSecondary = Color(0xFF2A1F05), secondaryContainer = Color(0xFF3D3220), onSecondaryContainer = Color(0xFFF3E3BC),
    background = GoldBg, onBackground = Color(0xFFEDE6D8), surface = GoldBg, onSurface = Color(0xFFEDE6D8),
    surfaceVariant = Color(0xFF2A241C), onSurfaceVariant = Color(0xFFB5AA95), outline = GoldHairline, outlineVariant = GoldHairline,
    surfaceContainerLowest = Color(0xFF100D0A), surfaceContainerLow = Color(0xFF1A1611), surfaceContainer = Color(0xFF1E1A14),
    surfaceContainerHigh = Color(0xFF221D17), surfaceContainerHighest = Color(0xFF26211A),
)
private val Amiri = FontFamily(Font(R.font.amiri_bold, FontWeight.Bold))
private val GoldType = Typography().run {
    copy(headlineSmall = headlineSmall.copy(fontFamily = Amiri), titleLarge = titleLarge.copy(fontFamily = Amiri),
        titleMedium = titleMedium.copy(fontFamily = Amiri))
}

/** True inside the dark gold theme; screens use it for the extra decoration (covers, glow, timeline). */
val LocalGoldTheme = staticCompositionLocalOf { false }

/** Time-of-day gradient "cover" for each prayer card in the dark gold theme. */
fun prayerCover(p: Prayer): List<Color> = when (p) {
    Prayer.FAJR -> listOf(Color(0xFF1B2350), Color(0xFF34488A))
    Prayer.SUNRISE -> listOf(Color(0xFF6E8FC4), Color(0xFFE2AE8C))
    Prayer.DHUHR -> listOf(Color(0xFF2A6DB5), Color(0xFF78B2E2))
    Prayer.ASR -> listOf(Color(0xFFA9844C), Color(0xFFE0CB9C))
    Prayer.MAGHRIB -> listOf(Color(0xFF4E1A26), Color(0xFFA4423C))
    Prayer.ISHA -> listOf(Color(0xFF080B1A), Color(0xFF1F2A4A))
}

@Composable
fun PrayerTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    if (mode == ThemeMode.GOLD) {
        CompositionLocalProvider(LocalGoldTheme provides true) { MaterialTheme(colorScheme = DarkGold, typography = GoldType, content = content) }
        return
    }
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; else -> true }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
