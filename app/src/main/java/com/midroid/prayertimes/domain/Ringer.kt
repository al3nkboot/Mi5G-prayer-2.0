package com.midroid.prayertimes.domain

/** Auto-silent decisions on AudioManager ringer modes (0 silent, 1 vibrate, 2 normal); pure so it can be unit-tested. */
object Ringer {
    const val SILENT = 0; const val VIBRATE = 1; const val NORMAL = 2

    /** @return the mode to switch to, or null to leave the phone alone (already silent/vibrate by the user's choice). */
    fun start(current: Int, target: Int): Int? = if (current == NORMAL && target != NORMAL) target else null

    /** @return the mode to restore, or null when the user changed the mode during the window (don't override them). */
    fun restore(current: Int, weSet: Int, previous: Int): Int? = if (current == weSet) previous else null
}
