package com.midroid.prayertimes.domain

/**
 * Per-wilayat corrections, applied ONLY with [Method.OMAN], fitted to the Ministry of Endowments 2026 calendar
 * (docs/mara_2026_reference.csv, June + December) so every time is within ±1 min. See docs/accuracy.md.
 * offsets index = Prayer.ordinal. monthly: optional 12 x 6 table (Jan..Dec) used instead of [offsets].
 */
data class MinistryFit(val offsets: List<Int> = List(6) { 0 }, val monthly: List<List<Int>>? = null) {
    fun offsetsFor(month: Int): List<Int> = monthly?.get(month - 1) ?: offsets
}

object MinistryFits {
    val byPlace: Map<String, MinistryFit> = mapOf(
        "rakhyut" to MinistryFit(List(6) { -2 }),
        "jazer" to MinistryFit(List(6) { -1 }),
        "mahout" to MinistryFit(listOf(1, 0, 0, 1, 0, 0)),
        // Al Hamra: error flips sign through the year (latitude-like effect), so a per-month table fitted to the full
        // 2026 Ministry calendar is used (max error ±1 min). Real GeoNames coordinates are kept for location/qibla.
        "hamra" to MinistryFit(monthly = listOf(
            listOf(1, 1, 0, -2, -2, -1), listOf(0, 1, 0, -1, -1, -1), listOf(0, 0, 0, 0, 0, 0),
            listOf(-1, -1, 0, 1, 0, 1), listOf(-2, -1, 0, 1, 1, 1), listOf(-2, -1, 0, 1, 1, 2),
            listOf(-2, -1, 0, 2, 1, 2), listOf(-1, -1, 0, 2, 1, 2), listOf(-1, 0, 0, 1, 1, 1),
            listOf(0, 0, 0, 0, 0, 1), listOf(1, 1, 0, -1, -1, 0), listOf(1, 1, 0, -1, -1, -1),
        )),
    )
}
