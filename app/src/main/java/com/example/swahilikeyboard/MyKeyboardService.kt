package com.example.swahilikeyboard

import android.inputmethodservice.InputMethodService
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.EditText
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


    private var currentView: View? = null
    private var targetSearchInput: EditText? = null

    // Swahili root verbs for fuzzy matching
    private val swahiliRoots = listOf("lia", "cheka", "kula", "lala", "sikia", "imba", "andika", "kimbia")

    // Function to perform Swahili-aware fuzzy matching
    private fun matchesSwahili(query: String, label: String): Boolean {
        val lowerQuery = query.lowercase()
        val lowerLabel = label.lowercase()
        if (lowerLabel.contains(lowerQuery)) return true
        for (root in swahiliRoots) {
            if (lowerQuery.contains(root) && lowerLabel.contains(root)) {
                return true
            }
        }
        return false
    }

    override fun onCreateInputView(): View {
        val inflater = LayoutInflater.from(this)
        letterKeyboardView = inflater.inflate(R.layout.letter_keyboard_layout, null)
        emojiKeyboardView = inflater.inflate(R.layout.emoji_keyboard_layout, null)
        emojiSearchView = inflater.inflate(R.layout.emoji_search_layout, null)
        symbolKeyboardView = inflater.inflate(R.layout.symbol_first_keyboard_layout, null)
        symbolSecondKeyboardView = layoutInflater.inflate(R.layout.symbol_second_keyboard_layout, null)


        setupLetterKeyboard(letterKeyboardView)
        setupEmojiKeyboard(emojiKeyboardView)
        setupEmojiSearchKeyboard(emojiSearchView)
        setupSymbolKeyboard(symbolKeyboardView)
        setupSecondSymbolKeyboard(symbolSecondKeyboardView)


        currentView = letterKeyboardView
        return currentView!!
    }

    private fun switchTo(view: View) {
        currentView?.visibility = View.GONE
        view.visibility = View.VISIBLE
        currentView = view
        setInputView(view)
    }

    private fun setupLetterKeyboard(view: View) {
        val keys = listOf(
            R.id.key_q to "q", R.id.key_w to "w", R.id.key_e to "e", R.id.key_r to "r",
            R.id.key_t to "t", R.id.key_y to "y", R.id.key_u to "u", R.id.key_i to "i",
            R.id.key_o to "o", R.id.key_p to "p", R.id.key_a to "a", R.id.key_s to "s",
            R.id.key_d to "d", R.id.key_f to "f", R.id.key_g to "g", R.id.key_h to "h",
            R.id.key_j to "j", R.id.key_k to "k", R.id.key_l to "l", R.id.key_z to "z",
            R.id.key_x to "x", R.id.key_c to "c", R.id.key_v to "v", R.id.key_b to "b",
            R.id.key_n to "n", R.id.key_m to "m", R.id.key_1 to "1", R.id.key_2 to "2",
            R.id.key_3 to "3", R.id.key_4 to "4", R.id.key_5 to "5", R.id.key_6 to "6",
            R.id.key_7 to "7", R.id.key_8 to "8", R.id.key_9 to "9", R.id.key_0 to "0",
            R.id.key_comma to ",", R.id.key_dot to "."
        )

        keys.forEach { (id, text) ->
            view.findViewById<Button>(id)?.setOnClickListener {
                targetSearchInput?.let {
                    val pos = it.selectionStart
                    it.text?.insert(pos, text)
                } ?: currentInputConnection?.commitText(text, 1)
            }
        }

        view.findViewById<Button>(R.id.key_backspace)?.setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }

        view.findViewById<Button>(R.id.key_space)?.setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
        }

        view.findViewById<Button>(R.id.key_enter)?.setOnClickListener {
            currentInputConnection?.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            )
        }

        view.findViewById<Button>(R.id.key_emoji)?.setOnClickListener {
            switchTo(emojiKeyboardView)
        }

        view.findViewById<Button>(R.id.key_symbols)?.setOnClickListener {
            switchTo(symbolKeyboardView)
        }
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
            currentInputConnection?.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            )
        }

        view.findViewById<Button>(R.id.key_emoji)?.setOnClickListener {
            switchTo(emojiKeyboardView)
        }

        view.findViewById<Button>(R.id.key_symbols)?.setOnClickListener {
            switchTo(letterKeyboardView)
        }

        view.findViewById<Button>(R.id.key_shiftforward)?.setOnClickListener {
            switchTo(symbolSecondKeyboardView)
        }

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

        view.findViewById<Button>(R.id.key_symbols)?.setOnClickListener {
            switchTo(letterKeyboardView)
        }

        view.findViewById<Button>(R.id.key_emoji)?.setOnClickListener {
            switchTo(emojiKeyboardView)
        }

        view.findViewById<Button>(R.id.key_symbol_shiftback)?.setOnClickListener {
            switchTo(symbolKeyboardView)
        }
    }


    private fun setupEmojiKeyboard(view: View) {
        val emojiGrid = view.findViewById<RecyclerView>(R.id.emoji_grid)
        val searchInput = view.findViewById<EditText>(R.id.search_input)

        val allEmojis = loadEmojiData()
        val adapter = EmojiGridAdapter(allEmojis) { emoji ->
            currentInputConnection?.commitText(emoji, 1)
        }

        emojiGrid.layoutManager = GridLayoutManager(this, 8)
        emojiGrid.adapter = adapter

        view.findViewById<Button>(R.id.key_alphanumeric)?.setOnClickListener {
            switchTo(letterKeyboardView)
        }

        // Clicking search bar switches layout
        searchInput.setOnClickListener {
            switchTo(emojiSearchView)
        }

        // Optional: setup category scroll jumps here
        // findViewById<Button>(R.id.category_smileys)?.setOnClickListener { ... }
    }

    private fun setupEmojiSearchKeyboard(view: View) {
        val emojiList = view.findViewById<RecyclerView>(R.id.emoji_list)
        val searchInput = view.findViewById<EditText>(R.id.search_input)

        val allEmojis = loadEmojiData()
        val adapter = EmojiAdapter(allEmojis) { emoji ->
            currentInputConnection?.commitText(emoji, 1)
        }

        emojiList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        emojiList.adapter = adapter

        targetSearchInput = searchInput

        searchInput.setOnClickListener {
            targetSearchInput = searchInput
        }

        searchInput.setOnFocusChangeListener { _, hasFocus ->
            targetSearchInput = if (hasFocus) searchInput else null
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtered = allEmojis.filter { emojiItem ->
                    emojiItem.swahiliDescription.lowercase().contains(query) ||
                            emojiItem.swahiliVerbs.contains(query) ||
                            emojiItem.english.lowercase().contains(query)
                }
                adapter.updateEmojiList(filtered)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })


        view.findViewById<Button>(R.id.key_alphanumeric)?.setOnClickListener {
            targetSearchInput = null
            switchTo(letterKeyboardView)
        }

        setupLetterKeyboard(view) // Add working keyboard inside search layout

        view.findViewById<Button>(R.id.key_backspace)?.setOnClickListener {
            targetSearchInput?.let {
                val editable = it.text
                val start = it.selectionStart
                if (start > 0) {
                    editable?.delete(start - 1, start)
                }
            } ?: currentInputConnection?.deleteSurroundingText(1, 0)
        }

    }

    private fun loadEmojiData(): List<EmojiItem> {
        val jsonString = assets.open("emojis.json").bufferedReader().use { it.readText() }
        val listType = object : TypeToken<List<EmojiRawItem>>() {}.type
        val rawList: List<EmojiRawItem> = Gson().fromJson(jsonString, listType)
        return rawList.map { it.toEmojiItem() }
    }

}
