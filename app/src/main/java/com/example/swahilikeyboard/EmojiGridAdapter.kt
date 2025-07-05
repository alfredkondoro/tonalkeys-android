package com.example.swahilikeyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EmojiGridAdapter(
    private var emojiList: List<EmojiItem>,
    private val onEmojiSelected: (String) -> Unit
) : RecyclerView.Adapter<EmojiGridAdapter.EmojiViewHolder>() {

    class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emojiText: TextView = itemView.findViewById(R.id.emoji_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emoji_grid, parent, false)
        return EmojiViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val emoji = emojiList[position].emoji
        holder.emojiText.text = emoji
        holder.itemView.setOnClickListener {
            onEmojiSelected(emoji)
        }
    }

    override fun getItemCount(): Int = emojiList.size

    fun updateEmojiList(newList: List<EmojiItem>) {
        emojiList = newList
        notifyDataSetChanged()
    }
}
