package com.midroid.prayertimes.data

import android.content.Context
import org.json.JSONArray

/** One dhikr from Hisn al-Muslim, chapter 27 (أذكار الصباح والمساء); id = the book's numbering. Text and source are verbatim, see CREDITS.md. */
data class Dhikr(val id: Int, val text: String, val count: Int, val ref: String, val morning: Boolean, val evening: Boolean)

object Adhkar {
    fun load(ctx: Context): List<Dhikr> {
        val a = JSONArray(ctx.assets.open("adhkar.json").bufferedReader().use { it.readText() })
        return List(a.length()) { i -> a.getJSONObject(i).run {
            Dhikr(getInt("id"), getString("text"), getInt("count"), getString("ref"), getBoolean("morning"), getBoolean("evening")) } }
    }
}
