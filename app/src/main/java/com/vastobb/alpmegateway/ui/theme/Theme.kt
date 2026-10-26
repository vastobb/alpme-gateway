package com.vastobb.alpmegateway.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.vastobb.alpmegateway.data.AppThemeMode

val LocalAlpmePalette = staticCompositionLocalOf { MidnightSlate }

private val AlpmeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun AlpmeTheme(
    themeMode: AppThemeMode = AppThemeMode.MIDNIGHT,
    content: @Composable () -> Unit,
) {
    val palette = when (themeMode) {
        AppThemeMode.MIDNIGHT -> MidnightSlate
        AppThemeMode.GRAPHITE -> Graphite
        AppThemeMode.WARM -> WarmNeutral
    }
    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.btnBg,
            onPrimary = palette.btnText,
            secondary = palette.accent,
            onSecondary = palette.appBg,
            tertiary = palette.accent,
            background = palette.appBg,
            onBackground = palette.textPrimary,
            surface = palette.panelBg,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.sidebarBg,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            outlineVariant = palette.border,
            error = palette.error,
            onError = palette.appBg,
        )
    } else {
        lightColorScheme(
            primary = palette.btnBg,
            onPrimary = palette.btnText,
            secondary = palette.accent,
            onSecondary = Color.White,
            tertiary = palette.accent,
            background = palette.appBg,
            onBackground = palette.textPrimary,
            surface = palette.panelBg,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.sidebarBg,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            outlineVariant = palette.border,
            error = palette.error,
            onError = Color.White,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !palette.isDark
            controller.isAppearanceLightNavigationBars = !palette.isDark
        }
    }

    CompositionLocalProvider(LocalAlpmePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AlpmeTypography,
            shapes = AlpmeShapes,
            content = content,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
