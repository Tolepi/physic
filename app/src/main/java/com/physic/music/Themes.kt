package com.physic.music

import androidx.compose.ui.graphics.Color

data class ThemeColors(
    val name: String,
    val bg: Color,
    val surface: Color,
    val text: Color,
    val subtext: Color,
    val accent: Color,
    val border: Color = subtext.copy(alpha = 0.35f),
)

object Themes {
    // oklch defaults from system24: lightness-matched neutrals, purple accent
    val all = listOf(
        ThemeColors(
            "System24",
            bg = Color(0xFF242424),
            surface = Color(0xFF2E2E2E),
            text = Color(0xFFF2F2F2),
            subtext = Color(0xFFA6A6A6),
            accent = Color(0xFFC6A8E8),        // purple-2 approx
        ),
        ThemeColors(
            "AMOLED",
            bg = Color(0xFF000000),
            surface = Color(0xFF0A0A0A),
            text = Color(0xFFFFFFFF),
            subtext = Color(0xFF9E9E9E),
            accent = Color(0xFFFFFFFF),
        ),
        ThemeColors("Catppuccin Mocha", Color(0xFF1E1E2E), Color(0xFF313244), Color(0xFFCDD6F4), Color(0xFFA6ADC8), Color(0xFFCBA6F7)),
        ThemeColors("Everforest", Color(0xFF2D353B), Color(0xFF343F44), Color(0xFFD3C6AA), Color(0xFFA6B0A0), Color(0xFFA7C080)),
        ThemeColors("Tokyo Night", Color(0xFF1A1B26), Color(0xFF24283B), Color(0xFFC0CAF5), Color(0xFFA9B1D6), Color(0xFF7AA2F7)),
        ThemeColors("Gruvbox", Color(0xFF282828), Color(0xFF3C3836), Color(0xFFEBDBB2), Color(0xFFA89984), Color(0xFFFE8019)),
        ThemeColors("Nord", Color(0xFF2E3440), Color(0xFF3B4252), Color(0xFFECEFF4), Color(0xFFD8DEE9), Color(0xFF88C0D0)),
        ThemeColors("Rose Pine", Color(0xFF191724), Color(0xFF1F1D2E), Color(0xFFE0DEF4), Color(0xFF908CAA), Color(0xFFC4A7E7)),
        ThemeColors("Dracula", Color(0xFF282A36), Color(0xFF44475A), Color(0xFFF8F8F2), Color(0xFFBFBFBF), Color(0xFFBD93F9)),
        ThemeColors("Solarized Dark", Color(0xFF002B36), Color(0xFF073642), Color(0xFFEEE8D5), Color(0xFF839496), Color(0xFF268BD2)),
        ThemeColors("One Dark", Color(0xFF1E2127), Color(0xFF282C34), Color(0xFFABB2BF), Color(0xFF7F848E), Color(0xFF61AFEF)),
        ThemeColors("Sakura", Color(0xFF1A0F16), Color(0xFF2A1724), Color(0xFFF8E1EE), Color(0xFFD88CB8), Color(0xFFF48FB1)),
    )
    fun byName(name: String?) = all.firstOrNull { it.name == name } ?: all.first()
}
