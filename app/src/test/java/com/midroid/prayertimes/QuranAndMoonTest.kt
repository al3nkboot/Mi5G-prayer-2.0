package com.midroid.prayertimes

import com.midroid.prayertimes.domain.MoonPosition
import com.midroid.prayertimes.domain.QuranText
import com.midroid.prayertimes.ui.mask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime

class QuranAndMoonTest {
    @Test fun tanzilTextIsVerbatimAndComplete() {
        val raw = File("src/main/assets/quran-uthmani.txt").readText()
        assertTrue(raw.contains("PLEASE DO NOT REMOVE OR CHANGE THIS COPYRIGHT BLOCK"))
        assertTrue(raw.contains("Tanzil Project"))
        val surahs = QuranText.parse(raw)
        assertEquals(114, surahs.size)
        assertEquals(6236, surahs.sumOf { it.ayahs.size })
        assertEquals(7, surahs[0].ayahs.size)
        assertEquals(286, surahs[1].ayahs.size)
        assertEquals(6, surahs[113].ayahs.size)
        assertTrue(surahs[0].ayahs[0].text.startsWith("بِسْمِ"))
        assertTrue(raw.lineSequence().last { it.isNotBlank() }.startsWith("#"))
    }

    @Test fun maskDoesNotRewriteStoredWords() {
        val verse = "بِسْمِ اللَّهِ الرَّحْمَٰنِ"
        assertEquals("بِسْمِ ···· ····", mask(verse, 1))
        assertEquals(verse, mask(verse, 9))
    }

    @Test fun moonStaysOnTheSky() {
        val a = MoonPosition.at(24.25, 55.79, ZonedDateTime.of(2026, 10, 16, 21, 0, 0, 0, ZoneId.of("Asia/Muscat")))
        val b = MoonPosition.at(24.25, 55.79, ZonedDateTime.of(2026, 10, 16, 3, 0, 0, 0, ZoneId.of("Asia/Muscat")))
        assertTrue(a.altitudeDeg in -90.0..90.0)
        assertTrue(a.azimuthDeg in 0.0..360.0)
        assertTrue(kotlin.math.abs(a.azimuthDeg - b.azimuthDeg) > 1.0)
    }
}
