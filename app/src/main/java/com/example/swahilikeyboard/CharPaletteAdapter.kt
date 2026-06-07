package com.example.swahilikeyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** Horizontal strip of the active language's special characters. Tap inserts. */
class CharPaletteAdapter(
    private var chars: List<String>,
    private val onCharClick: (String) -> Unit
) : RecyclerView.Adapter<CharPaletteAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val charText: TextView = view.findViewById(R.id.palette_char)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_palette_char, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = chars[position]
        holder.charText.text = c
        holder.itemView.setOnClickListener { onCharClick(c) }
    }

    override fun getItemCount() = chars.size

    fun update(newChars: List<String>) {
        chars = newChars
        notifyDataSetChanged()
    }
}
