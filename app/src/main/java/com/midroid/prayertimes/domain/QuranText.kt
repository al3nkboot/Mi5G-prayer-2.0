package com.midroid.prayertimes.domain

/** One verse, verbatim from the bundled Tanzil file. The parser never rewrites the text. */
data class Ayah(val surah: Int, val number: Int, val text: String)

data class Surah(val number: Int, val ayahs: List<Ayah>)

object QuranText {
    private val line = Regex("""^(\d+)\|(\d+)\|(.*)$""")

    /** Parses Tanzil `surah|ayah|text` lines. Copyright lines and blanks are skipped, not altered. */
    fun parse(raw: String): List<Surah> {
        val ayahs = raw.lineSequence().mapNotNull { row ->
            line.matchEntire(row)?.let { m ->
                Ayah(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3])
            }
        }.toList()
        return ayahs.groupBy { it.surah }.toSortedMap().map { (n, list) -> Surah(n, list) }
    }
}
