package com.example.domain.model

object TileCatalog {

  private val latinSpeechMap = mapOf(
    'A' to "a",
    'B' to "bé",
    'C' to "cé",
    'D' to "dé",
    'E' to "e",
    'F' to "effe",
    'G' to "gé",
    'H' to "hache",
    'I' to "i",
    'J' to "ji",
    'K' to "ka",
    'L' to "elle",
    'M' to "emme",
    'N' to "enne",
    'O' to "o",
    'P' to "pé",
    'Q' to "ku",
    'R' to "erre",
    'S' to "esse",
    'T' to "té",
    'U' to "u",
    'V' to "vé",
    'W' to "double vé",
    'X' to "ixe",
    'Y' to "i grec",
    'Z' to "zède",
  )

  val latinLetters: List<TileSymbol> = ('A'..'Z').map { ch ->
    TileSymbol(
      id = "lat_$ch",
      glyph = ch.toString(),
      category = TileCategory.LATIN_LETTER,
      frenchSpeech = latinSpeechMap[ch] ?: ch.lowercase(),
      arabicSpeech = ch.toString(),
      frenchDisplayName = "Lettre $ch",
    )
  }

  private val numberFrenchWords = listOf(
    "zéro",
    "un",
    "deux",
    "trois",
    "quatre",
    "cinq",
    "six",
    "sept",
    "huit",
    "neuf",
    "dix",
  )

  private val numberArabicWords = listOf(
    "صفر",
    "واحد",
    "اثنان",
    "ثلاثة",
    "أربعة",
    "خمسة",
    "ستة",
    "سبعة",
    "ثمانية",
    "تسعة",
    "عشرة",
  )

  val numbers: List<TileSymbol> = (0..10).map { n ->
    val fr = numberFrenchWords[n]
    TileSymbol(
      id = "num_$n",
      glyph = n.toString(),
      category = TileCategory.NUMBER,
      frenchSpeech = fr,
      arabicSpeech = numberArabicWords[n],
      frenchDisplayName = fr.replaceFirstChar { it.uppercase() },
    )
  }

  private data class ArabicLetterSpec(
    val id: String,
    val glyph: String,
    val arabicSpeech: String,
    val arabicName: String,
    val frenchSpeech: String,
    val frenchName: String,
  )

  // Use "حَرْفُ ..." with full tashkeel for Arabic TTS so every letter name
  // is articulated clearly as an alphabet letter without clipping or word ambiguity,
  // and phonetic French spellings for French TTS.
  private val arabicSpecs = listOf(
    ArabicLetterSpec("alif", "أ", "حَرْفُ الأَلِف", "ألف", "Alif", "Alif"),
    ArabicLetterSpec("ba", "ب", "حَرْفُ البَاء", "باء", "Bâ", "Ba"),
    ArabicLetterSpec("ta", "ت", "حَرْفُ التَّاء", "تاء", "Tâ", "Ta"),
    ArabicLetterSpec("tha", "ث", "حَرْفُ الثَّاء", "ثاء", "Thâ", "Tha"),
    ArabicLetterSpec("jim", "ج", "حَرْفُ الجِيم", "جيم", "Djime", "Jim"),
    ArabicLetterSpec("ha", "ح", "حَرْفُ الحَاء", "حاء", "Hâ", "Ha"),
    ArabicLetterSpec("kha", "خ", "حَرْفُ الخَاء", "خاء", "Khâ", "Kha"),
    ArabicLetterSpec("dal", "د", "حَرْفُ الدَّال", "دال", "Dâl", "Dal"),
    ArabicLetterSpec("dhal", "ذ", "حَرْفُ الذَّال", "ذال", "Dhâl", "Dhal"),
    ArabicLetterSpec("ra", "ر", "حَرْفُ الرَّاء", "راء", "Râ", "Ra"),
    ArabicLetterSpec("zay", "ز", "حَرْفُ الزَّاي", "زاي", "Zây", "Zay"),
    ArabicLetterSpec("sin", "س", "حَرْفُ السِّين", "سين", "Sine", "Sin"),
    ArabicLetterSpec("shin", "ش", "حَرْفُ الشِّين", "شين", "Chine", "Chin"),
    ArabicLetterSpec("sad", "ص", "حَرْفُ الصَّاد", "صاد", "Sâd", "Sad"),
    ArabicLetterSpec("dad", "ض", "حَرْفُ الضَّاد", "ضاد", "Dâd", "Dad"),
    ArabicLetterSpec("tah", "ط", "حَرْفُ الطَّاء", "طاء", "Tâ", "Tâ"),
    ArabicLetterSpec("zah", "ظ", "حَرْفُ الظَّاء", "ظاء", "Zâ", "Zâ"),
    ArabicLetterSpec("ayn", "ع", "حَرْفُ العَيْن", "عين", "Aïne", "Aïn"),
    ArabicLetterSpec("ghayn", "غ", "حَرْفُ الغَيْن", "غين", "Ghaïne", "Ghaïn"),
    ArabicLetterSpec("fa", "ف", "حَرْفُ الفَاء", "فاء", "Fâ", "Fa"),
    ArabicLetterSpec("qaf", "ق", "حَرْفُ القَاف", "قاف", "Qâf", "Qaf"),
    ArabicLetterSpec("kaf", "ك", "حَرْفُ الكَاف", "كاف", "Kâf", "Kaf"),
    ArabicLetterSpec("lam", "ل", "حَرْفُ اللَّام", "لام", "Lâm", "Lam"),
    ArabicLetterSpec("mim", "م", "حَرْفُ المِيم", "ميم", "Mime", "Mim"),
    ArabicLetterSpec("noun", "ن", "حَرْفُ النُّون", "نون", "Noune", "Noun"),
    ArabicLetterSpec("hah", "ه", "حَرْفُ الهَاء", "هاء", "Hâ", "Ha"),
    ArabicLetterSpec("waw", "و", "حَرْفُ الوَاو", "واو", "Waou", "Waw"),
    ArabicLetterSpec("ya", "ي", "حَرْفُ اليَاء", "ياء", "Yâ", "Ya"),
  )

  val arabicLetters: List<TileSymbol> = arabicSpecs.map { spec ->
    TileSymbol(
      id = "ar_${spec.id}",
      glyph = spec.glyph,
      category = TileCategory.ARABIC_LETTER,
      frenchSpeech = spec.frenchSpeech,
      arabicSpeech = spec.arabicSpeech,
      frenchDisplayName = spec.frenchName,
      arabicDisplayName = spec.arabicName,
    )
  }

  val allSymbols: List<TileSymbol> = numbers + latinLetters + arabicLetters

  fun symbolsForPack(pack: TilePack): List<TileSymbol> = when (pack) {
    TilePack.ALL -> allSymbols
    TilePack.LATIN_ONLY -> latinLetters
    TilePack.NUMBERS_ONLY -> numbers
    TilePack.ARABIC_ONLY -> arabicLetters
  }
}
