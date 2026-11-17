import Foundation

enum TileCategory {
  case latinLetter
  case number
  case arabicLetter
}

enum TilePack: String, CaseIterable, Identifiable {
  case all = "ALL"
  case latinOnly = "LATIN_ONLY"
  case numbersOnly = "NUMBERS_ONLY"
  case arabicOnly = "ARABIC_ONLY"

  var id: String { rawValue }

  var titleFr: String {
    switch self {
    case .all: return "Tout"
    case .latinOnly: return "Lettres A-Z"
    case .numbersOnly: return "Chiffres 0-10"
    case .arabicOnly: return "الحروف أ-ي"
    }
  }
}

enum Difficulty: String, CaseIterable, Identifiable {
  case easy = "EASY"
  case medium = "MEDIUM"
  case hard = "HARD"

  var id: String { rawValue }

  var labelFr: String {
    switch self {
    case .easy: return "Facile"
    case .medium: return "Moyen"
    case .hard: return "Difficile"
    }
  }

  var labelAr: String {
    switch self {
    case .easy: return "سهل"
    case .medium: return "متوسط"
    case .hard: return "صعب"
    }
  }

  var minPairs: Int {
    switch self {
    case .easy: return 6
    case .medium: return 12
    case .hard: return 20
    }
  }

  var maxPairs: Int {
    switch self {
    case .easy: return 10
    case .medium: return 18
    case .hard: return 28
    }
  }
}

enum ArabicVoiceMode: String, CaseIterable, Identifiable {
  case bilingual = "BILINGUAL"
  case arabicOnly = "ARABIC_ONLY"
  case frenchOnly = "FRENCH_ONLY"

  var id: String { rawValue }

  var label: String {
    switch self {
    case .bilingual: return "العربية + Français"
    case .arabicOnly: return "العربية uniquement"
    case .frenchOnly: return "Français uniquement"
    }
  }
}

struct TileSymbol: Identifiable, Equatable {
  let id: String
  let glyph: String
  let category: TileCategory
  let frenchSpeech: String
  let arabicSpeech: String
  let frenchDisplayName: String
  let arabicDisplayName: String?
}

struct BoardTile: Identifiable, Equatable {
  let id: Int
  let symbol: TileSymbol
  let x: Int
  let y: Int
  let z: Int
  var isFree: Bool = false
}

enum TileCatalog {
  private static let latinSpeech: [Character: String] = [
    "A": "a", "B": "bé", "C": "cé", "D": "dé", "E": "e", "F": "effe",
    "G": "gé", "H": "hache", "I": "i", "J": "ji", "K": "ka", "L": "elle",
    "M": "emme", "N": "enne", "O": "o", "P": "pé", "Q": "ku", "R": "erre",
    "S": "esse", "T": "té", "U": "u", "V": "vé", "W": "double vé",
    "X": "ixe", "Y": "i grec", "Z": "zède"
  ]

  static let latinLetters: [TileSymbol] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".map { ch in
    TileSymbol(
      id: "lat_\(ch)",
      glyph: String(ch),
      category: .latinLetter,
      frenchSpeech: latinSpeech[ch] ?? String(ch).lowercased(),
      arabicSpeech: String(ch),
      frenchDisplayName: "Lettre \(ch)",
      arabicDisplayName: nil
    )
  }

  private static let numberWords = [
    "zéro", "un", "deux", "trois", "quatre", "cinq",
    "six", "sept", "huit", "neuf", "dix"
  ]

  static let numbers: [TileSymbol] = (0...10).map { n in
    let w = numberWords[n]
    return TileSymbol(
      id: "num_\(n)",
      glyph: "\(n)",
      category: .number,
      frenchSpeech: w,
      arabicSpeech: w,
      frenchDisplayName: w.prefix(1).uppercased() + w.dropFirst(),
      arabicDisplayName: nil
    )
  }

  private static let arabicSpecs: [(String, String, String, String, String, String)] = [
    ("alif", "أ", "حَرْفُ الأَلِف", "ألف", "Alif", "Alif"),
    ("ba", "ب", "حَرْفُ البَاء", "باء", "Bâ", "Ba"),
    ("ta", "ت", "حَرْفُ التَّاء", "تاء", "Tâ", "Ta"),
    ("tha", "ث", "حَرْفُ الثَّاء", "ثاء", "Thâ", "Tha"),
    ("jim", "ج", "حَرْفُ الجِيم", "جيم", "Djime", "Jim"),
    ("ha", "ح", "حَرْفُ الحَاء", "حاء", "Hâ", "Ha"),
    ("kha", "خ", "حَرْفُ الخَاء", "خاء", "Khâ", "Kha"),
    ("dal", "د", "حَرْفُ الدَّال", "دال", "Dâl", "Dal"),
    ("dhal", "ذ", "حَرْفُ الذَّال", "ذال", "Dhâl", "Dhal"),
    ("ra", "ر", "حَرْفُ الرَّاء", "راء", "Râ", "Ra"),
    ("zay", "ز", "حَرْفُ الزَّاي", "زاي", "Zây", "Zay"),
    ("sin", "س", "حَرْفُ السِّين", "سين", "Sine", "Sin"),
    ("shin", "ش", "حَرْفُ الشِّين", "شين", "Chine", "Chin"),
    ("sad", "ص", "حَرْفُ الصَّاد", "صاد", "Sâd", "Sad"),
    ("dad", "ض", "حَرْفُ الضَّاد", "ضاد", "Dâd", "Dad"),
    ("tah", "ط", "حَرْفُ الطَّاء", "طاء", "Tâ", "Tâ"),
    ("zah", "ظ", "حَرْفُ الظَّاء", "ظاء", "Zâ", "Zâ"),
    ("ayn", "ع", "حَرْفُ العَيْن", "عين", "Aïne", "Aïn"),
    ("ghayn", "غ", "حَرْفُ الغَيْن", "غين", "Ghaïne", "Ghaïn"),
    ("fa", "ف", "حَرْفُ الفَاء", "فاء", "Fâ", "Fa"),
    ("qaf", "ق", "حَرْفُ القَاف", "قاف", "Qâf", "Qaf"),
    ("kaf", "ك", "حَرْفُ الكَاف", "كاف", "Kâf", "Kaf"),
    ("lam", "ل", "حَرْفُ اللَّام", "لام", "Lâm", "Lam"),
    ("mim", "م", "حَرْفُ المِيم", "ميم", "Mime", "Mim"),
    ("noun", "ن", "حَرْفُ النُّون", "نون", "Noune", "Noun"),
    ("hah", "ه", "حَرْفُ الهَاء", "هاء", "Hâ", "Ha"),
    ("waw", "و", "حَرْفُ الوَاو", "واو", "Waou", "Waw"),
    ("ya", "ي", "حَرْفُ اليَاء", "ياء", "Yâ", "Ya")
  ]

  static let arabicLetters: [TileSymbol] = arabicSpecs.map { spec in
    TileSymbol(
      id: "ar_\(spec.0)",
      glyph: spec.1,
      category: .arabicLetter,
      frenchSpeech: spec.4,
      arabicSpeech: spec.2,
      frenchDisplayName: spec.5,
      arabicDisplayName: spec.3
    )
  }

  static let allSymbols: [TileSymbol] = numbers + latinLetters + arabicLetters

  static func symbols(for pack: TilePack) -> [TileSymbol] {
    switch pack {
    case .all: return allSymbols
    case .latinOnly: return latinLetters
    case .numbersOnly: return numbers
    case .arabicOnly: return arabicLetters
    }
  }
}
