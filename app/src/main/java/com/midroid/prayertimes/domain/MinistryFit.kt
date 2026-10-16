package com.midroid.prayertimes.domain

/**
 * Per-wilayat corrections, applied ONLY with [Method.OMAN], fitted to the Ministry of Endowments 2026 calendar
 * (docs/mara_2026_reference.csv, June + December) so every time is within ±1 min. See docs/accuracy.md.
 * offsets index = Prayer.ordinal. calcLat/calcLon: the Ministry's table behaves like a different reference point.
 */
data class MinistryFit(val offsets: List<Int> = List(6) { 0 }, val calcLat: Double? = null, val calcLon: Double? = null)

object MinistryFits {
    val byPlace: Map<String, MinistryFit> = mapOf(
        "rakhyut" to MinistryFit(List(6) { -2 }),
        "jazer" to MinistryFit(List(6) { -1 }),
        "mahout" to MinistryFit(listOf(1, 0, 0, 1, 0, 0)),
        // Al Hamra: the error flips sign between June and December (latitude effect), so no constant offset fits.
        // The Ministry's Al Hamra table matches a point at ~23.765N 57.291E (≈72 km north of the town).
        "hamra" to MinistryFit(calcLat = 23.765, calcLon = 57.291),
    )
}
