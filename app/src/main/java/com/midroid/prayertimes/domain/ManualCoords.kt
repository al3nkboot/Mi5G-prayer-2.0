package com.midroid.prayertimes.domain

import java.time.ZoneId

/** Manually entered coordinates. Overrides GPS and the city list until cleared. */
object ManualCoords {
    const val ID = "manual"
    enum class Error { LAT, LON, ZONE }
    sealed interface Result { data class Ok(val place: Place) : Result; data class Bad(val errors: Set<Error>) : Result }

    /** Accepts '.', ',' or Arabic '٫' decimal separators and Arabic-Indic / Persian digits. */
    fun parseNumber(s: String): Double? {
        val t = buildString {
            for (c in s.trim()) append(when (c) {
                in '٠'..'٩' -> '0' + (c - '٠'); in '۰'..'۹' -> '0' + (c - '۰')
                ',', '٫' -> '.'; '−', '–' -> '-'; else -> c
            })
        }
        if (!Regex("""[+-]?(\d+(\.\d*)?|\.\d+)""").matches(t)) return null
        return t.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    /** [zone] null = follow the device time zone. */
    fun build(lat: String, lon: String, name: String, zone: String?, deviceZone: String = ZoneId.systemDefault().id): Result {
        val la = parseNumber(lat)?.takeIf { it in -90.0..90.0 }
        val lo = parseNumber(lon)?.takeIf { it in -180.0..180.0 }
        val z = zone?.trim()?.takeIf { it.isNotEmpty() }
        val zOk = z == null || runCatching { ZoneId.of(z) }.isSuccess
        val errs = buildSet { if (la == null) add(Error.LAT); if (lo == null) add(Error.LON); if (!zOk) add(Error.ZONE) }
        if (errs.isNotEmpty()) return Result.Bad(errs)
        val n = name.trim().take(40)
        return Result.Ok(Place(ID, n.ifEmpty { "إحداثيات يدوية" }, n.ifEmpty { "Manual coordinates" }, la!!, lo!!, z ?: deviceZone))
    }
}
