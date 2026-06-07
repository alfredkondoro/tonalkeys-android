package com.example.swahilikeyboard

/**
 * Shared emoji retrieval core (Android).
 *
 * Behavioural twin of run_eval.py (the canonical reference) and emojiSearch.js.
 * stem() and score() MUST match those exactly so the phone, web, and the
 * evaluated reference return the same results for the same query.
 *
 * Hybrid Swahili stemmer:
 *   1. lexicon lookup: inflected form -> root (swahili_stem_lexicon.json)
 *   2. already-a-root passthrough
 *   3. rule-based affix peeling, validated against the emoji root inventory
 *   4. final-vowel normalization (negative present -i -> -a, etc.)
 * Ranking is word-boundary (no substrings), verb-weighted, flags de-prioritized.
 *
 * @param emojis   parsed emoji items (from emojis.json)
 * @param lexicon  inflected form -> root map (from swahili_stem_lexicon.json)
 */
class EmojiSearchEngine(
    private val emojis: List<EmojiItem>,
    private val lexicon: Map<String, String>
) {
    companion object {
        private const val MAX_RESULTS = 10
        private const val MIN_ROOT = 2
        private const val MAX_PEELS = 4

        // Subject + negative + tense/aspect + object prefixes, longest first.
        private val PREFIXES: List<String> = listOf(
            "ni", "u", "a", "tu", "m", "mu", "wa", "ki", "vi", "li", "ya", "zi",
            "ku", "pa", "i", "ji",
            "si", "hu", "ha", "hatu", "ham", "hawa",
            "na", "me", "ta", "ka", "nge", "ngeli", "ngali", "ja", "sha", "mesha", "mw"
        ).distinct().sortedByDescending { it.length }

        private val WORD = Regex("[\\w']+")
        private val SPACE = Regex("\\s+")
        private fun words(s: String): Set<String> =
            WORD.findAll(s.lowercase()).map { it.value }.toSet()
    }

    // Single-word verb roots that actually have emoji; used to validate peels.
    private val roots: Set<String> =
        emojis.flatMap { it.swahiliVerbs }
            .map { it.lowercase() }
            .filter { !it.contains(' ') }
            .toSet()

    private fun peel(word: String, depth: Int = 0): String? {
        if (word in roots) return word
        if (depth >= MAX_PEELS) return null
        for (p in PREFIXES) {
            if (word.startsWith(p) && word.length - p.length >= MIN_ROOT) {
                val r = peel(word.substring(p.length), depth + 1)
                if (r != null) return r
            }
        }
        return null
    }

    /** Reduce one token to its Swahili root (or return it unchanged). */
    fun stem(token: String): String {
        val t = token.trim().lowercase()
        if (t.isEmpty()) return t
        lexicon[t]?.let { return it }                 // 1. lexicon
        if (t in roots) return t                       // 2. already a root
        peel(t)?.let { return it }                     // 3. rule peel
        if (t.last() in "ieu") {                       // 4. final-vowel normalization
            val c = t.dropLast(1) + "a"
            if (c in roots || c in lexicon) return lexicon[c] ?: c
        }
        return t
    }

    private fun score(item: EmojiItem, tokens: List<String>): Int {
        var s = 0
        val dw = words(item.swahiliDescription)
        val ew = words(item.english)
        val verbsLower = item.swahiliVerbs.map { it.lowercase() }
        val vw = (verbsLower + verbsLower.flatMap { it.split(" ") }).toSet()
        for (t in tokens) {
            if (t in vw) s += 3
            if (t in dw) s += 1
            if (t in ew) s += 1
        }
        if (s > 0 && item.group == "Flags") s -= 2
        return s
    }

    /**
     * Up to MAX_RESULTS items, most relevant first. Empty query returns the
     * default ordering (browse mode); the evaluation never calls it empty.
     * useStemmer=false reproduces the stemmer-off ablation.
     */
    fun search(rawQuery: String, useStemmer: Boolean = true): List<EmojiItem> {
        val q = rawQuery.trim().lowercase()
        if (q.isEmpty()) return emojis.take(MAX_RESULTS)
        val tokens = q.split(SPACE)
            .filter { it.isNotEmpty() }
            .map { if (useStemmer) stem(it) else it }
        return emojis
            .map { it to score(it, tokens) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(MAX_RESULTS)
            .map { it.first }
    }
}
