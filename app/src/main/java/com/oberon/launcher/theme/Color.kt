package com.oberon.launcher.theme

import androidx.compose.ui.graphics.Color

data class AccentOption(val name: String, val color: Color)

val AccentOptions = listOf(
    AccentOption("Mor", Color(0xFF6750A4)),
    AccentOption("Lacivert", Color(0xFF1565C0)),
    AccentOption("Mavi", Color(0xFF0288D1)),
    AccentOption("Yeşil", Color(0xFF00695C)),
    AccentOption("Zümrüt", Color(0xFF00897B)),
    AccentOption("Kırmızı", Color(0xFFB3261E)),
    AccentOption("Turuncu", Color(0xFFAD5B00)),
    AccentOption("Pembe", Color(0xFF9C3A7E))
)