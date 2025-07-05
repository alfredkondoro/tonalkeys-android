package com.example.swahilikeyboard

data class EmojiRawItem(
    val emoji: String,
    val english: String,
    val swahili: String,
    val group: String,
    val sub_group: String,
    val codepoints: String
) {
    fun toEmojiItem(): EmojiItem {
        val parts = swahili.split(",").map { it.trim() }
        val description = parts.firstOrNull() ?: ""
        val verbs = if (parts.size > 1) parts.subList(1, parts.size) else emptyList()
        return EmojiItem(emoji, english, description, verbs, group, sub_group)
    }
}

data class EmojiItem(
    val emoji: String,
    val english: String,
    val swahiliDescription: String,
    val swahiliVerbs: List<String>,
    val group: String,
    val sub_group: String
)
