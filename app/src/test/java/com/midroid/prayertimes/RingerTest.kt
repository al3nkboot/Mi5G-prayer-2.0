package com.midroid.prayertimes

import com.midroid.prayertimes.domain.Ringer
import com.midroid.prayertimes.domain.Ringer.NORMAL
import com.midroid.prayertimes.domain.Ringer.SILENT
import com.midroid.prayertimes.domain.Ringer.VIBRATE
import org.junit.Assert.*
import org.junit.Test

class RingerTest {
    @Test fun startOnlyFromNormal() {
        assertEquals(VIBRATE, Ringer.start(NORMAL, VIBRATE))
        assertEquals(SILENT, Ringer.start(NORMAL, SILENT))
        assertNull(Ringer.start(VIBRATE, SILENT)) // user already quiet: leave alone
        assertNull(Ringer.start(SILENT, VIBRATE))
    }
    @Test fun restoreUnlessUserChanged() {
        assertEquals(NORMAL, Ringer.restore(current = VIBRATE, weSet = VIBRATE, previous = NORMAL))
        assertEquals(NORMAL, Ringer.restore(SILENT, SILENT, NORMAL))
        assertNull(Ringer.restore(current = NORMAL, weSet = VIBRATE, previous = NORMAL)) // user unmuted early
        assertNull(Ringer.restore(current = SILENT, weSet = VIBRATE, previous = NORMAL)) // user went fully silent
    }
}
