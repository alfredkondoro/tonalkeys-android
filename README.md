# TonalKeys Android

TonalKeys is an African-language input system. This repository contains the
**Android keyboard prototype** and the **Swahili emoji search dataset**. The
companion web extension is published on the
[Chrome Web Store](https://chromewebstore.google.com/detail/tonalkeys-african-languag/hpancnlkbahagcopnopahadijkammphn).

📄 Demo paper: *TonalKeys: Diacritic Input and Morphology-Aware Emoji Search
for African Languages on the Web and Mobile* (under review, CIKM 2026
Demonstration Track) · 🎬 [Demo video](https://www.youtube.com/watch?v=DM2ygDHG8bU)

## What it does

**Diacritic input.** Curated special-character palettes for 18 African
languages across five regions: Yorùbá, Igbo, Hausa, Fula, Bambara, Twi, Ewe,
Fon, and Wolof (West Africa); Swahili, Amharic, Tigrinya, and Luganda (East
Africa); Zulu, Xhosa, and Shona (Southern Africa); Tamazight (North Africa);
and Lingala (Central Africa). A one-tap palette strip sits above the keys, a
picker switches languages, the choice persists across sessions, and the space
bar is labelled in the active language.

**Morphology-aware emoji search.** Type a Swahili or code-mixed query and get
ranked emoji. A hybrid stemmer reduces inflected forms to their roots, so
*nimecheka* ("I have laughed") finds the same laughing faces as *cheka*, and
code-mixed queries such as *nimepost picha* work without language
identification. Retrieval runs fully on-device.

## Dataset

| File | Contents |
|---|---|
| `app/src/main/assets/emojis.json` | 3,642 emoji entries with English glosses, culturally grounded Swahili descriptions, and verb-stem associations (2,754 entries carry verbs). Inventory and codepoints follow the Unicode emoji data files. |
| `app/src/main/assets/swahili_stem_lexicon.json` | 30,828 inflected form → root mappings, derived by reversing the 319,156-form Swahili verb-conjugation dataset of Mathayo & Kondoro (NLPIR '24) and pruning to emoji-bearing roots. |
| `app/src/main/assets/keyboards.json` | Character palettes, base-letter variant maps, and localized space-bar labels for all 18 languages. Shared with the web extension. |

## How the stemmer works

A query token is reduced in four stages: (1) lexicon lookup against the
form-to-root mappings, (2) pass-through if the token is already an annotated
root, (3) rule-based affix peeling validated against the emoji root inventory
so peeling stops at a real root, (4) final-vowel normalization for
negative-present and related endings. Ranking uses word-boundary matches
weighted toward verb stems. See `EmojiSearchEngine.kt`.

## Build and install

1. Open the project in Android Studio and let Gradle sync.
2. **Build → Build App Bundle(s) / APK(s) → Build APK(s)**, or run
   `./gradlew assembleDebug`. The APK lands in
   `app/build/outputs/apk/debug/app-debug.apk`.
3. Copy the APK to a device and open it. Allow installs from the source app
   if prompted, and choose **Install anyway** if Play Protect objects (normal
   for sideloaded debug builds).
4. Enable the keyboard: **Settings → System → Keyboard → On-screen keyboard →
   TonalKeys**, then switch to it via the keyboard icon in the navigation bar.

## Project structure

```
app/src/main/java/com/example/swahilikeyboard/
  MyKeyboardService.kt    # IME service: layouts, toolbar, shift, language switching
  EmojiSearchEngine.kt    # hybrid stemmer + ranking (mirrors the web/reference core)
  KeyboardData.kt         # keyboards.json loader and preferences
  SettingsActivity.kt     # keyboard settings
  CharPaletteAdapter.kt, LanguageAdapter.kt, EmojiAdapter…
app/src/main/assets/      # emojis.json, swahili_stem_lexicon.json, keyboards.json
app/src/main/res/layout/  # keyboard, picker, and palette layouts
```

## Status and roadmap

This is a working prototype. Voice typing and in-keyboard translation appear
in the toolbar as coming-soon. Planned: emoji search beyond Swahili, relative
verb forms, richer noun coverage, a native-speaker-judged retrieval benchmark,
and speech input.

## Citing

If you use the dataset or stemmer, please cite the TonalKeys demo paper
(reference will be updated upon publication) and the underlying conjugation
resource:

```bibtex
@inproceedings{mathayo2025unveiling,
  author    = {Mathayo, Irene Masiringi and Kondoro, Alfred Malengo},
  title     = {Unveiling Swahili Verb Conjugations: A Comprehensive Dataset for Low-Resource NLP},
  booktitle = {Proceedings of the 2024 8th International Conference on Natural Language Processing and Information Retrieval (NLPIR '24)},
  pages     = {149--156},
  year      = {2025},
  doi       = {10.1145/3711542.3711596}
}
```

## Team

Built by [Tonative](https://tonative.org): Alfred Malengo Kondoro (Hanyang
University), Sharon Ibejih, and Cynthia Amol, with annotations from the
Tonative community.

## License

Code and dataset are released openly. <!-- TODO: add a LICENSE file (e.g. MIT or Apache-2.0 for code, CC BY 4.0 for the dataset) and name it here. -->
