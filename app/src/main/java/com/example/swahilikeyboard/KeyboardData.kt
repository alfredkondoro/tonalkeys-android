package com.example.swahilikeyboard

import android.content.Context
import com.google.gson.Gson

/** One language's layout, mirrors a KEYBOARDS entry in keyboards.json. */
data class KeyboardLayout(
    val id: String,
    val name: String,
    val nativeName: String,
    val region: String,
    val flag: String,
    val description: String,
    val spaceLabel: String = "",                          // localized space-bar label
    val charMap: Map<String, List<String>> = emptyMap(),  // base letter -> variants (long-press)
    val palette: List<String> = emptyList()               // quick-insert strip
)

data class Region(val name: String, val keyboards: List<String>)

data class KeyboardData(
    val keyboards: Map<String, KeyboardLayout> = emptyMap(),
    val regions: List<Region> = emptyList()
)

/** Loads keyboards.json once and caches it for the service lifetime. */
object Keyboards {
    @Volatile private var cached: KeyboardData? = null

    fun get(context: Context): KeyboardData {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val json = context.assets.open("keyboards.json")
                .bufferedReader().use { it.readText() }
            return Gson().fromJson(json, KeyboardData::class.java).also { cached = it }
        }
    }

    fun layout(context: Context, id: String): KeyboardLayout? = get(context).keyboards[id]

    /** Region-grouped rows for the language picker (headers + languages). */
    fun pickerRows(context: Context): List<PickerRow> {
        val data = get(context)
        val rows = ArrayList<PickerRow>()
        for (region in data.regions) {
            rows.add(PickerRow.Header(region.name))
            for (id in region.keyboards) {
                data.keyboards[id]?.let { rows.add(PickerRow.Lang(it)) }
            }
        }
        return rows
    }
}

sealed class PickerRow {
    data class Header(val name: String) : PickerRow()
    data class Lang(val layout: KeyboardLayout) : PickerRow()
}

/** Persists the active language across sessions (mirrors web chrome.storage). */
class KeyboardPrefs(context: Context) {
    private val sp = context.getSharedPreferences("tonalkeys", Context.MODE_PRIVATE)
    var activeKeyboardId: String
        get() = sp.getString("activeKeyboard", "swahili") ?: "swahili"
        set(value) { sp.edit().putString("activeKeyboard", value).apply() }
}