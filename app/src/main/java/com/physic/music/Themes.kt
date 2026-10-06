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
        ThemeColors("System24 Light", Color(0xFFF5F5F5), Color(0xFFE8E8E8), Color(0xFF1A1A1A), Color(0xFF555555), Color(0xFF8A5FBF)),
        ThemeColors("Catppuccin Latte", Color(0xFFEFF1F5), Color(0xFFE6E9EF), Color(0xFF4C4F69), Color(0xFF6C6F85), Color(0xFF8839EF)),
        ThemeColors("Everforest Light", Color(0xFFFDF6E3), Color(0xFFF4F0D9), Color(0xFF5C6A72), Color(0xFF829181), Color(0xFF8DA101)),
        ThemeColors("Tokyo Night Day", Color(0xFFE1E2E7), Color(0xFFD5D6DB), Color(0xFF3760BF), Color(0xFF6172B0), Color(0xFF2E7DE9)),
        ThemeColors("Gruvbox Light", Color(0xFFFBF1C7), Color(0xFFF2E5BC), Color(0xFF3C3836), Color(0xFF7C6F64), Color(0xFFD65D0E)),
        ThemeColors("Nord Light", Color(0xFFECEFF4), Color(0xFFE5E9F0), Color(0xFF2E3440), Color(0xFF4C566A), Color(0xFF5E81AC)),
        ThemeColors("Rose Pine Dawn", Color(0xFFFAF4ED), Color(0xFFF2E9E1), Color(0xFF575279), Color(0xFF797593), Color(0xFF907AA9)),
        ThemeColors("Solarized Light", Color(0xFFFDF6E3), Color(0xFFEEE8D5), Color(0xFF657B83), Color(0xFF93A1A1), Color(0xFF268BD2)),
        ThemeColors("One Light", Color(0xFFFAFAFA), Color(0xFFF0F0F0), Color(0xFF383A42), Color(0xFF8A8F98), Color(0xFF4078F2)),
        ThemeColors("Sakura Light", Color(0xFFFFF0F5), Color(0xFFFCE4EC), Color(0xFF6D2E52), Color(0xFFA8698D), Color(0xFFD81B60)),
        ThemeColors("Paper", Color(0xFFFFFBF2), Color(0xFFF5EFE0), Color(0xFF2B2B2B), Color(0xFF8A8A8A), Color(0xFF3366FF)),
        ThemeColors("Gentoo", Color(0xFF1A1A1E), Color(0xFF2A2A35), Color(0xFFF2F0F5), Color(0xFFB0A8C0), Color(0xFF9B7ED9)),
        ThemeColors("Bodypop", Color(0xFF102236), Color(0xFF142C42), Color(0xFFE8F1F2), Color(0xFF86A8C1), Color(0xFFF7D038)),
    )
    fun byName(name: String?) = all.firstOrNull { it.name == name } ?: all.first()
}
