package com.example.swahilikeyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EmojiAdapter(
    private var emojiList: List<EmojiItem>,
    private val onEmojiClick: (String) -> Unit
) : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emoji, parent, false)
        return EmojiViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val emojiItem = emojiList[position]
        holder.emojiText.text = emojiItem.emoji
        holder.itemView.setOnClickListener {
            onEmojiClick(emojiItem.emoji)
        }
    }

    override fun getItemCount(): Int = emojiList.size

    fun updateEmojiList(newList: List<EmojiItem>) {
        emojiList = newList
        notifyDataSetChanged()
    }

    class EmojiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emojiText: TextView = view.findViewById(R.id.emoji_text)
    }
}
