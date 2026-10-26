package com.vastobb.alpmegateway.ui.theme

import androidx.compose.ui.graphics.Color

data class AlpmePalette(
    val isDark: Boolean,
    val appBg: Color,
    val sidebarBg: Color,
    val panelBg: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val btnBg: Color,
    val btnText: Color,
    val hoverSurface: Color,
    val codeHighlight: Color,
    val accent: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
)

val MidnightSlate = AlpmePalette(
    isDark = true,
    appBg = Color(0xFF101114),
    sidebarBg = Color(0xFF141519),
    panelBg = Color(0xFF191B20),
    border = Color(0xFF2A2C32),
    textPrimary = Color(0xFFEDEEF0),
    textSecondary = Color(0xFF9699A2),
    btnBg = Color(0xFFEDEEF0),
    btnText = Color(0xFF101114),
    hoverSurface = Color(0x0DEDEEF0),
    codeHighlight = Color(0x0AEDEEF0),
    accent = Color(0xFF8BE9C0),
    success = Color(0xFF6EE7B7),
    warning = Color(0xFFFBBF24),
    error = Color(0xFFF87171),
)

val Graphite = AlpmePalette(
    isDark = true,
    appBg = Color(0xFF0D0D0D),
    sidebarBg = Color(0xFF111111),
    panelBg = Color(0xFF171717),
    border = Color(0xFF292929),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFF929292),
    btnBg = Color(0xFFF5F5F5),
    btnText = Color(0xFF111111),
    hoverSurface = Color(0x0DF5F5F5),
    codeHighlight = Color(0x0AF5F5F5),
    accent = Color(0xFFA3E635),
    success = Color(0xFF6EE7B7),
    warning = Color(0xFFFBBF24),
    error = Color(0xFFF87171),
)

val WarmNeutral = AlpmePalette(
    isDark = false,
    appBg = Color(0xFFF7F7F5),
    sidebarBg = Color(0xFFF0F0ED),
    panelBg = Color(0xFFFFFFFF),
    border = Color(0xFFE5E5E0),
    textPrimary = Color(0xFF171717),
    textSecondary = Color(0xFF737373),
    btnBg = Color(0xFF202020),
    btnText = Color(0xFFFFFFFF),
    hoverSurface = Color(0x0A171717),
    codeHighlight = Color(0x08171717),
    accent = Color(0xFF047857),
    success = Color(0xFF047857),
    warning = Color(0xFFB45309),
    error = Color(0xFFB91C1C),
)
