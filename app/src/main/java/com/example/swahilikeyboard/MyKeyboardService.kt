package com.example.swahilikeyboard

import android.inputmethodservice.InputMethodService
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MyKeyboardService : InputMethodService() {

    private lateinit var letterKeyboardView: View
    private lateinit var emojiKeyboardView: View
    private lateinit var emojiSearchView: View
    private lateinit var symbolKeyboardView: View
    private lateinit var symbolSecondKeyboardView: View
    private lateinit var languagePickerView: View

    private var currentView: View? = null
    private var targetSearchInput: EditText? = null

    // ── Shift / capitalization state ──
    private enum class ShiftState { OFF, AUTO, ON }
    private var shiftState = ShiftState.AUTO          // AUTO = first letter capital, then lowercase
    private var letterCaps: List<Pair<Button, String>> = emptyList()

    // ── Diacritic / language state ──
    private val prefs by lazy { KeyboardPrefs(this) }
    private var activeLayout: KeyboardLayout? = null
    private var paletteAdapter: CharPaletteAdapter? = null

    // ── Cached data + retrieval core (loaded once per service lifetime) ──
    private val allEmojis: List<EmojiItem> by lazy { loadEmojiData() }
    private val stemLexicon: Map<String, String> by lazy { loadStemLexicon() }
    private val engine: EmojiSearchEngine by lazy { EmojiSearchEngine(allEmojis, stemLexicon) }

    override fun onCreateInputView(): View {
        val inflater = LayoutInflater.from(this)

        activeLayout = Keyboards.layout(this, prefs.activeKeyboardId)

        letterKeyboardView = inflater.inflate(R.layout.letter_keyboard_layout, null)
        emojiKeyboardView = inflater.inflate(R.layout.emoji_keyboard_layout, null)
        emojiSearchView = inflater.inflate(R.layout.emoji_search_layout, null)
        symbolKeyboardView = inflater.inflate(R.layout.symbol_first_keyboard_layout, null)
        symbolSecondKeyboardView = inflater.inflate(R.layout.symbol_second_keyboard_layout, null)
        languagePickerView = inflater.inflate(R.layout.language_picker_layout, null)

        setupLetterKeyboard(letterKeyboardView)
        setupEmojiKeyboard(emojiKeyboardView)
        setupEmojiSearchKeyboard(emojiSearchView)
        setupSymbolKeyboard(symbolKeyboardView)
        setupSecondSymbolKeyboard(symbolSecondKeyboardView)
        setupLanguagePicker(languagePickerView)

        currentView = letterKeyboardView
        return currentView!!
    }

    // Reset to auto-capital each time the keyboard opens on a field.
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        shiftState = ShiftState.AUTO
        refreshKeyCaps()
    }

    private fun switchTo(view: View) {
        currentView?.visibility = View.GONE
        view.visibility = View.VISIBLE
        currentView = view
        setInputView(view)
        if (view !== emojiSearchView) targetSearchInput = null
    }

    /** Insert into the in-keyboard search box ONLY when the search view is showing; else the host app. */
    private fun insertText(text: String) {
        val target = targetSearchInput
        if (target != null && currentView === emojiSearchView) {
            val pos = target.selectionStart
            target.text?.insert(pos, text)
        } else {
            currentInputConnection?.commitText(text, 1)
        }
    }

    /** Relabel letter keys and the shift key to match the current case. */
    private fun refreshKeyCaps() {
        if (!::letterKeyboardView.isInitialized) return
        val upper = shiftState != ShiftState.OFF
        letterCaps.forEach { (btn, base) -> btn.text = if (upper) base.uppercase() else base }
        letterKeyboardView.findViewById<Button>(R.id.key_shift)?.text =
            if (shiftState == ShiftState.ON) "⇪" else "⇧"
    }

    private fun showComingSoon() {
        Toast.makeText(this, "Voice typing is coming soon", Toast.LENGTH_SHORT).show()
    }

    /** Localized space-bar label, falling back to the language name then "Space". */
    private fun spaceLabel(): String =
        activeLayout?.spaceLabel?.takeIf { it.isNotBlank() }
            ?: activeLayout?.name
            ?: "Space"

    private fun setupLetterKeyboard(view: View) {
        // Case-aware letters
        val letterKeys = listOf(
            R.id.key_q to "q", R.id.key_w to "w", R.id.key_e to "e", R.id.key_r to "r",
            R.id.key_t to "t", R.id.key_y to "y", R.id.key_u to "u", R.id.key_i to "i",
            R.id.key_o to "o", R.id.key_p to "p", R.id.key_a to "a", R.id.key_s to "s",
            R.id.key_d to "d", R.id.key_f to "f", R.id.key_g to "g", R.id.key_h to "h",
            R.id.key_j to "j", R.id.key_k to "k", R.id.key_l to "l", R.id.key_z to "z",
            R.id.key_x to "x", R.id.key_c to "c", R.id.key_v to "v", R.id.key_b to "b",
            R.id.key_n to "n", R.id.key_m to "m"
        )
        val letterButtons = letterKeys.mapNotNull { (id, base) ->
            view.findViewById<Button>(id)?.let { btn ->
                btn.setOnClickListener {
                    val ch = if (shiftState == ShiftState.OFF) base else base.uppercase()
                    insertText(ch)
                    if (shiftState == ShiftState.AUTO) {
                        shiftState = ShiftState.OFF
                        refreshKeyCaps()
                    }
                }
                btn to base
            }
        }
        // Track only the real letter view's buttons for relabeling.
        if (view === letterKeyboardView) {
            letterCaps = letterButtons
            refreshKeyCaps()
        }

        // Literal keys (numbers + punctuation)
        val literalKeys = listOf(
            R.id.key_1 to "1", R.id.key_2 to "2", R.id.key_3 to "3", R.id.key_4 to "4",
            R.id.key_5 to "5", R.id.key_6 to "6", R.id.key_7 to "7", R.id.key_8 to "8",
            R.id.key_9 to "9", R.id.key_0 to "0", R.id.key_comma to ",", R.id.key_dot to "."
        )
        literalKeys.forEach { (id, text) ->
            view.findViewById<Button>(id)?.setOnClickListener { insertText(text) }
        }

        // Shift: tap = capitalize next letter (or caps-lock), tap again to release.
        view.findViewById<Button>(R.id.key_shift)?.setOnClickListener {
            shiftState = when (shiftState) {
                ShiftState.OFF -> ShiftState.ON
                ShiftState.ON -> ShiftState.OFF
                ShiftState.AUTO -> ShiftState.ON
            }
            refreshKeyCaps()
        }

        view.findViewById<Button>(R.id.key_backspace)?.setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        view.findViewById<Button>(R.id.key_space)?.apply {
            setOnClickListener { currentInputConnection?.commitText(" ", 1) }
            text = spaceLabel()
        }
        view.findViewById<Button>(R.id.key_enter)?.setOnClickListener {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        }
        view.findViewById<Button>(R.id.key_emoji)?.setOnClickListener { switchTo(emojiKeyboardView) }
        view.findViewById<Button>(R.id.key_symbols)?.setOnClickListener { switchTo(symbolKeyboardView) }
        view.findViewById<Button>(R.id.key_mic)?.setOnClickListener { showComingSoon() }

        // ── Diacritic palette strip for the active language ──
        view.findViewById<RecyclerView>(R.id.char_palette)?.apply {
            layoutManager = LinearLayoutManager(this@MyKeyboardService, LinearLayoutManager.HORIZONTAL, false)
            val pa = CharPaletteAdapter(activeLayout?.palette ?: emptyList()) { ch -> insertText(ch) }
            paletteAdapter = pa
            adapter = pa
        }
        // ── Language picker entry point ──
        view.findViewById<Button>(R.id.key_language)?.setOnClickListener { switchTo(languagePickerView) }
    }

    private fun setupSymbolKeyboard(view: View) {
        val symbolKeys = listOf(
            R.id.key_1 to "1", R.id.key_2 to "2", R.id.key_3 to "3", R.id.key_4 to "4",
            R.id.key_5 to "5", R.id.key_6 to "6", R.id.key_7 to "7", R.id.key_8 to "8",
            R.id.key_9 to "9", R.id.key_0 to "0",
            R.id.key_add to "+", R.id.key_multiply to "×", R.id.key_division to "÷",
            R.id.key_equals to "=", R.id.key_backslash to "/", R.id.key_lowdash to "_",
            R.id.key_leftpointbracket to "<", R.id.key_rightpointbracket to ">",
            R.id.key_leftsquarebracket to "[", R.id.key_rightsquarebracket to "]",
            R.id.key_exclamation to "!", R.id.key_mail to "@", R.id.key_hashtag to "#",
            R.id.key_dollarsign to "$", R.id.key_percent to "%", R.id.key_exponent to "^",
            R.id.key_ampersand to "&", R.id.key_asterik to "*", R.id.key_leftbracket to "(",
            R.id.key_rightbracket to ")", R.id.key_dash to "-", R.id.key_singleapo to "'",
            R.id.key_doubleapo to "\"", R.id.key_colon to ":", R.id.key_semicolon to ";",
            R.id.key_comma to ",", R.id.key_dot to ".", R.id.key_question to "?"
        )
        symbolKeys.forEach { (id, symbol) ->
            view.findViewById<Button>(id)?.setOnClickListener {
                currentInputConnection?.commitText(symbol, 1)
            }
        }
        view.findViewById<Button>(R.id.key_backspace)?.setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        view.findViewById<Button>(R.id.key_space)?.setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
        }
        view.findViewById<Button>(R.id.key_enter)?.setOnClickListener {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        }
        view.findViewById<Button>(R.id.key_emoji)?.setOnClickListener { switchTo(emojiKeyboardView) }
        view.findViewById<Button>(R.id.key_symbols)?.setOnClickListener { switchTo(letterKeyboardView) }
        view.findViewById<Button>(R.id.key_shiftforward)?.setOnClickListener { switchTo(symbolSecondKeyboardView) }
    }

    private fun setupSecondSymbolKeyboard(view: View) {
        val symbolKeys = listOf(
            R.id.key_backtick to "`", R.id.key_tilde to "~", R.id.key_backslash to "\\", R.id.key_pipe to "|",
            R.id.key_left_curly to "{", R.id.key_right_curly to "}", R.id.key_euro to "€", R.id.key_pound to "£",
            R.id.key_yen to "¥", R.id.key_won to "₩", R.id.key_dot_center to "◦", R.id.key_dot_bold to "•",
            R.id.key_circle to "○", R.id.key_circle_filled to "●", R.id.key_square to "■", R.id.key_spade to "♠",
            R.id.key_heart to "♥", R.id.key_diamond to "♦", R.id.key_club to "♣", R.id.key_star to "★",
            R.id.key_square_hollow to "☐", R.id.key_gear to "⚙", R.id.key_angle_left to "«", R.id.key_angle_right to "»",
            R.id.key_info to "ℹ", R.id.key_question_inverted to "¿", R.id.key_comma to ",", R.id.key_dot to "."
        )
        symbolKeys.forEach { (id, symbol) ->
            view.findViewById<Button>(id)?.setOnClickListener {
                currentInputConnection?.commitText(symbol, 1)
            }
        }
        view.findViewById<Button>(R.id.key_backspace)?.setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        view.findViewById<Button>(R.id.key_space)?.setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
        }
        view.findViewById<Button>(R.id.key_enter)?.setOnClickListener {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        }
        view.findViewById<Button>(R.id.key_symbols)?.setOnClickListener { switchTo(letterKeyboardView) }
        view.findViewById<Button>(R.id.key_emoji)?.setOnClickListener { switchTo(emojiKeyboardView) }
        view.findViewById<Button>(R.id.key_symbol_shiftback)?.setOnClickListener { switchTo(symbolKeyboardView) }
    }

    private fun setupEmojiKeyboard(view: View) {
        val emojiGrid = view.findViewById<RecyclerView>(R.id.emoji_grid)
        val searchInput = view.findViewById<EditText>(R.id.search_input)

        val adapter = EmojiGridAdapter(allEmojis) { emoji ->
            currentInputConnection?.commitText(emoji, 1)
        }
        emojiGrid.layoutManager = GridLayoutManager(this, 8)
        emojiGrid.adapter = adapter

        fun showGroups(vararg groups: String) {
            val set = groups.toSet()
            adapter.updateEmojiList(allEmojis.filter { it.group in set })
        }
        view.findViewById<Button>(R.id.category_smileys)?.setOnClickListener { showGroups("Smileys & Emotion", "People & Body") }
        view.findViewById<Button>(R.id.category_animals)?.setOnClickListener { showGroups("Animals & Nature") }
        view.findViewById<Button>(R.id.category_food)?.setOnClickListener { showGroups("Food & Drink") }
        view.findViewById<Button>(R.id.category_activities)?.setOnClickListener { showGroups("Activities") }
        view.findViewById<Button>(R.id.category_travel)?.setOnClickListener { showGroups("Travel & Places") }
        view.findViewById<Button>(R.id.category_objects)?.setOnClickListener { showGroups("Objects") }
        view.findViewById<Button>(R.id.category_symbols)?.setOnClickListener { showGroups("Symbols") }
        view.findViewById<Button>(R.id.category_flags)?.setOnClickListener { showGroups("Flags") }

        view.findViewById<Button>(R.id.key_alphanumeric)?.setOnClickListener { switchTo(letterKeyboardView) }
        searchInput.setOnClickListener { switchTo(emojiSearchView) }
    }

    private fun setupEmojiSearchKeyboard(view: View) {
        val emojiList = view.findViewById<RecyclerView>(R.id.emoji_list)
        val searchInput = view.findViewById<EditText>(R.id.search_input)

        val adapter = EmojiAdapter(allEmojis) { emoji ->
            currentInputConnection?.commitText(emoji, 1)
        }
        emojiList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        emojiList.adapter = adapter

        targetSearchInput = searchInput
        searchInput.setOnClickListener { targetSearchInput = searchInput }
        searchInput.setOnFocusChangeListener { _, hasFocus ->
            targetSearchInput = if (hasFocus) searchInput else null
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.updateEmojiList(engine.search(s.toString()))
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        view.findViewById<Button>(R.id.key_alphanumeric)?.setOnClickListener {
            targetSearchInput = null
            switchTo(letterKeyboardView)
        }

        setupLetterKeyboard(view) // working keyboard inside the search layout

        view.findViewById<Button>(R.id.key_backspace)?.setOnClickListener {
            targetSearchInput?.let {
                val start = it.selectionStart
                if (start > 0) it.text?.delete(start - 1, start)
            } ?: currentInputConnection?.deleteSurroundingText(1, 0)
        }
    }

    private fun setupLanguagePicker(view: View) {
        view.findViewById<RecyclerView>(R.id.language_list).apply {
            layoutManager = LinearLayoutManager(this@MyKeyboardService)
            adapter = LanguageAdapter(
                Keyboards.pickerRows(this@MyKeyboardService),
                prefs.activeKeyboardId
            ) { id ->
                prefs.activeKeyboardId = id
                activeLayout = Keyboards.layout(this@MyKeyboardService, id)
                letterKeyboardView.findViewById<RecyclerView>(R.id.char_palette)?.let { rv ->
                    (rv.adapter as? CharPaletteAdapter)?.update(activeLayout?.palette ?: emptyList())
                }
                letterKeyboardView.findViewById<Button>(R.id.key_space)?.text = spaceLabel()
                switchTo(letterKeyboardView)
            }
        }
        view.findViewById<Button>(R.id.key_back_to_keyboard)?.setOnClickListener {
            switchTo(letterKeyboardView)
        }
    }

    private fun loadEmojiData(): List<EmojiItem> {
        val jsonString = assets.open("emojis.json").bufferedReader().use { it.readText() }
        val listType = object : TypeToken<List<EmojiRawItem>>() {}.type
        val rawList: List<EmojiRawItem> = Gson().fromJson(jsonString, listType)
        return rawList.map { it.toEmojiItem() }
    }

    private fun loadStemLexicon(): Map<String, String> {
        val jsonString = assets.open("swahili_stem_lexicon.json").bufferedReader().use { it.readText() }
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson(jsonString, mapType)
    }
}