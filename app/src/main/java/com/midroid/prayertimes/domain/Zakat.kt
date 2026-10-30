package com.midroid.prayertimes.domain

object Zakat {
    const val RATE = 0.025

    /** 2.5% of cash at or above a caller-supplied nisab. Null if the inputs are not usable. Not a ruling. */
    fun due(wealth: Double, nisab: Double): Double? {
        if (wealth < 0 || nisab <= 0 || wealth.isNaN() || nisab.isNaN() || wealth.isInfinite() || nisab.isInfinite()) return null
        if (wealth < nisab) return 0.0
        return wealth * RATE
    }
}
