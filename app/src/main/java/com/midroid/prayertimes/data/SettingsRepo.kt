package com.midroid.prayertimes.data

import android.content.Context
import com.midroid.prayertimes.domain.Asr
import com.midroid.prayertimes.domain.CalcSettings
import com.midroid.prayertimes.domain.HighLat
import com.midroid.prayertimes.domain.Method
import com.midroid.prayertimes.domain.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val calc: CalcSettings = CalcSettings(),
    val place: Place = Cities.default,
    val hijriAdjust: Int = 0,
    val theme: ThemeMode = ThemeMode.SYSTEM,
)

class SettingsRepo(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state

    private inline fun <reified E : Enum<E>> enumOr(key: String, def: E): E =
        sp.getString(key, null)?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: def

    private fun load(): AppSettings {
        val place = sp.getString("place_id", null)?.let { id ->
            if (id == "gps") Place("gps", sp.getString("gps_ar", "")!!, sp.getString("gps_en", "")!!,
                sp.getFloat("gps_lat", 0f).toDouble(), sp.getFloat("gps_lon", 0f).toDouble(), sp.getString("gps_zone", "Asia/Muscat")!!)
            else Cities.byId(id)
        } ?: Cities.default
        return AppSettings(
            calc = CalcSettings(
                method = enumOr("method", Method.OMAN), asr = enumOr("asr", Asr.SHAFI), highLat = enumOr("highlat", HighLat.NONE),
                adjustments = List(6) { sp.getInt("adj_$it", 0) },
            ),
            place = place, hijriAdjust = sp.getInt("hijri_adj", 0), theme = enumOr("theme", ThemeMode.SYSTEM),
        )
    }

    fun update(f: (AppSettings) -> AppSettings) {
        val n = f(_state.value)
        sp.edit().apply {
            putString("method", n.calc.method.name); putString("asr", n.calc.asr.name); putString("highlat", n.calc.highLat.name)
            n.calc.adjustments.forEachIndexed { i, v -> putInt("adj_$i", v) }
            putInt("hijri_adj", n.hijriAdjust); putString("theme", n.theme.name)
            putString("place_id", n.place.id)
            if (n.place.id == "gps") {
                putString("gps_ar", n.place.nameAr); putString("gps_en", n.place.nameEn)
                putFloat("gps_lat", n.place.lat.toFloat()); putFloat("gps_lon", n.place.lon.toFloat()); putString("gps_zone", n.place.zone)
            }
        }.apply()
        _state.value = n
    }
}
