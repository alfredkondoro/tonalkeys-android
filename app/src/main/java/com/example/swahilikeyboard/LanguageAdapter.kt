package com.example.swahilikeyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** Region-grouped list of all 18 languages for the picker. */
class LanguageAdapter(
    private val rows: List<PickerRow>,
    private val activeId: String,
    private val onPick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object { const val HEADER = 0; const val LANG = 1 }

    override fun getItemViewType(position: Int) =
        if (rows[position] is PickerRow.Header) HEADER else LANG

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == HEADER)
            HeaderVH(inf.inflate(R.layout.item_region_header, parent, false))
        else
            LangVH(inf.inflate(R.layout.item_language, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is PickerRow.Header -> (holder as HeaderVH).title.text = row.name
            is PickerRow.Lang -> {
                val h = holder as LangVH
                val kb = row.layout
                h.flag.text = kb.flag
                h.name.text = kb.name
                h.nativeName.text = kb.nativeName
                h.check.visibility = if (kb.id == activeId) View.VISIBLE else View.INVISIBLE
                h.itemView.setOnClickListener { onPick(kb.id) }
            }
        }
    }

    override fun getItemCount() = rows.size

    class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.region_title)
    }
    class LangVH(v: View) : RecyclerView.ViewHolder(v) {
        val flag: TextView = v.findViewById(R.id.lang_flag)
        val name: TextView = v.findViewById(R.id.lang_name)
        val nativeName: TextView = v.findViewById(R.id.lang_native)
        val check: TextView = v.findViewById(R.id.lang_check)
    }
}
