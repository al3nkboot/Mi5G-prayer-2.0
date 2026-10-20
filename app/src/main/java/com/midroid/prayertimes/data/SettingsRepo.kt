package com.midroid.prayertimes.data

import android.content.Context
import com.midroid.prayertimes.domain.AlertMode
import com.midroid.prayertimes.domain.Asr
import com.midroid.prayertimes.domain.CalcSettings
import com.midroid.prayertimes.domain.HighLat
import com.midroid.prayertimes.domain.Method
import com.midroid.prayertimes.domain.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Sound for an audible alert: bundled chime, bundled adhan recording, or the user's own file (SAF uri). */
enum class SoundChoice { CHIME, ADHAN, CUSTOM }

data class AppSettings(
    val calc: CalcSettings = CalcSettings(),
    val place: Place = Cities.default,
    val hijriAdjust: Int = 0,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** index = Prayer.ordinal; sunrise off by default */
    val alerts: List<AlertMode> = DEFAULT_ALERTS,
    val reminderMinutes: Int = 0,
    val sounds: List<SoundChoice> = List(6) { SoundChoice.ADHAN },
    val customUri: String? = null,
    val customName: String? = null,
    val autoSilent: Boolean = false,
    val silentMinutes: Int = 20,
    /** true = vibrate, false = silent */
    val silentVibrate: Boolean = true,
    val adhkarAm: Boolean = false,
    /** minutes after Fajr */
    val adhkarAmOffset: Int = 30,
    val adhkarPm: Boolean = false,
    /** minutes after Asr */
    val adhkarPmOffset: Int = 30,
) { companion object { val DEFAULT_ALERTS = listOf(AlertMode.ADHAN, AlertMode.OFF, AlertMode.ADHAN, AlertMode.ADHAN, AlertMode.ADHAN, AlertMode.ADHAN) } }

class SettingsRepo(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    init { migrate() }
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state
    /** called after every persisted change (used to re-arm alarms) */
    var onChange: ((AppSettings) -> Unit)? = null

    private inline fun <reified E : Enum<E>> enumOr(key: String, def: E): E =
        sp.getString(key, null)?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: def

    /**
     * Schema 2 (1.0.1): alert/sound prefs written by earlier installs (incl. ones restored by Android
     * auto-backup from a previous install) are reset once to the defaults: Fajr..Isha = adhan sound, Sunrise off.
     */
    private fun migrate() {
        if (sp.getInt("schema", 0) >= SCHEMA) return
        sp.edit().apply {
            for (i in 0 until 6) { remove("alert_$i"); remove("sound_$i") }
            putInt("schema", SCHEMA)
        }.commit()
    }

    private fun load(): AppSettings {
        val place = sp.getString("place_id", null)?.let { id ->
            if (id == "gps") Place("gps", sp.getString("gps_ar", "")!!, sp.getString("gps_en", "")!!,
                sp.getFloat("gps_lat", 0f).toDouble(), sp.getFloat("gps_lon", 0f).toDouble(), java.time.ZoneId.systemDefault().id)
            else Cities.byId(id)
        } ?: Cities.default
        return AppSettings(
            calc = CalcSettings(
                method = enumOr("method", Method.OMAN), asr = enumOr("asr", Asr.SHAFI), highLat = enumOr("highlat", HighLat.NONE),
                adjustments = List(6) { sp.getInt("adj_$it", 0) },
            ),
            place = place, hijriAdjust = sp.getInt("hijri_adj", 0), theme = enumOr("theme", ThemeMode.SYSTEM),
            alerts = List(6) { i -> sp.getString("alert_$i", null)?.let { v -> AlertMode.entries.firstOrNull { it.name == v } } ?: AppSettings.DEFAULT_ALERTS[i] },
            reminderMinutes = sp.getInt("reminder_min", 0),
            sounds = List(6) { i -> enumOr("sound_$i", SoundChoice.ADHAN) },
            customUri = sp.getString("custom_uri", null), customName = sp.getString("custom_name", null),
            autoSilent = sp.getBoolean("auto_silent", false), silentMinutes = sp.getInt("silent_min", 20), silentVibrate = sp.getBoolean("silent_vibrate", true),
            adhkarAm = sp.getBoolean("adhkar_am", false), adhkarAmOffset = sp.getInt("adhkar_am_off", 30),
            adhkarPm = sp.getBoolean("adhkar_pm", false), adhkarPmOffset = sp.getInt("adhkar_pm_off", 30),
        )
    }

    companion object { const val SCHEMA = 2 }

    fun update(f: (AppSettings) -> AppSettings) {
        val n = f(_state.value)
        sp.edit().apply {
            putString("method", n.calc.method.name); putString("asr", n.calc.asr.name); putString("highlat", n.calc.highLat.name)
            n.calc.adjustments.forEachIndexed { i, v -> putInt("adj_$i", v) }
            putInt("hijri_adj", n.hijriAdjust); putString("theme", n.theme.name)
            putString("place_id", n.place.id)
            n.alerts.forEachIndexed { i, v -> putString("alert_$i", v.name) }
            putInt("reminder_min", n.reminderMinutes)
            n.sounds.forEachIndexed { i, v -> putString("sound_$i", v.name) }
            putString("custom_uri", n.customUri); putString("custom_name", n.customName)
            putBoolean("auto_silent", n.autoSilent); putInt("silent_min", n.silentMinutes); putBoolean("silent_vibrate", n.silentVibrate)
            putBoolean("adhkar_am", n.adhkarAm); putInt("adhkar_am_off", n.adhkarAmOffset); putBoolean("adhkar_pm", n.adhkarPm); putInt("adhkar_pm_off", n.adhkarPmOffset)
            if (n.place.id == "gps") {
                putString("gps_ar", n.place.nameAr); putString("gps_en", n.place.nameEn)
                putFloat("gps_lat", n.place.lat.toFloat()); putFloat("gps_lon", n.place.lon.toFloat()); putString("gps_zone", n.place.zone)
            }
        }.apply()
        _state.value = n
        onChange?.invoke(n)
    }

    /** GPS place follows the device time zone (called on TIMEZONE_CHANGED); manual cities keep their own zone. */
    fun syncGpsZone() {
        val z = java.time.ZoneId.systemDefault().id
        if (_state.value.place.id == "gps" && _state.value.place.zone != z) update { it.copy(place = it.place.copy(zone = z)) }
    }
}
