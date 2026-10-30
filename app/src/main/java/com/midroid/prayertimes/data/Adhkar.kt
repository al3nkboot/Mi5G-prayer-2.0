package com.midroid.prayertimes.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** One dhikr. Hisn al-Muslim chapter 27 items keep the book's numbering. Extra items are selected hadith wordings, not that chapter. */
data class Dhikr(val id: Int, val text: String, val count: Int, val ref: String, val morning: Boolean, val evening: Boolean)

data class ExtraAdhkar(val after: List<Dhikr>, val waking: List<Dhikr>)

object Adhkar {
    fun load(ctx: Context): List<Dhikr> = read(JSONArray(ctx.assets.open("adhkar.json").bufferedReader().use { it.readText() }), morning = true, evening = true)

    fun loadExtra(ctx: Context): ExtraAdhkar {
        val o = JSONObject(ctx.assets.open("adhkar_extra.json").bufferedReader().use { it.readText() })
        return ExtraAdhkar(read(o.getJSONArray("after"), false, false), read(o.getJSONArray("waking"), false, false))
    }

    private fun read(a: JSONArray, morning: Boolean, evening: Boolean): List<Dhikr> = List(a.length()) { i ->
        a.getJSONObject(i).run {
            Dhikr(
                getInt("id"),
                getString("text"),
                getInt("count"),
                getString("ref"),
                if (has("morning")) getBoolean("morning") else morning,
                if (has("evening")) getBoolean("evening") else evening,
            )
        }
    }
}
