/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.materialkolor.score.Score

data class HuniPalette(
    val id: String,
    val label: String,
    val accentDark: Color,
    val accentLight: Color,
    val darkBg: Color,
    val darkSurface: Color,
    val darkRaised: Color,
    val lightBg: Color,
    val lightRaised: Color,
)

val HuniPalettes = listOf(
    HuniPalette("amber", "Amber", Color(0xFFFFB84D), Color(0xFFB36B00), Color(0xFF16121C), Color(0xFF211B2A), Color(0xFF2C2437), Color(0xFFFBF8FC), Color(0xFFF1EAF6)),
    HuniPalette("violet", "Violet", Color(0xFFB79CFF), Color(0xFF6B3FE0), Color(0xFF14111F), Color(0xFF1E1A2E), Color(0xFF2A2440), Color(0xFFF9F7FE), Color(0xFFEEE9FB)),
    HuniPalette("green", "Green", Color(0xFF6FD98B), Color(0xFF1E7A3C), Color(0xFF0F1A14), Color(0xFF17251C), Color(0xFF213227), Color(0xFFF6FBF7), Color(0xFFE6F3EA)),
    HuniPalette("maroon", "Maroon", Color(0xFFE5707E), Color(0xFF9B1C31), Color(0xFF1C1114), Color(0xFF29181C), Color(0xFF382127), Color(0xFFFDF7F8), Color(0xFFF6E6E9)),
    HuniPalette("ocean", "Ocean", Color(0xFF6CB8FF), Color(0xFF1565C0), Color(0xFF0E1620), Color(0xFF162130), Color(0xFF202E42), Color(0xFFF5F9FE), Color(0xFFE4EEF9)),
    HuniPalette("teal", "Teal", Color(0xFF4FD6C8), Color(0xFF0F766E), Color(0xFF0D1918), Color(0xFF142524), Color(0xFF1E3432), Color(0xFFF4FBFA), Color(0xFFE0F2F0)),
    HuniPalette("rose", "Rose", Color(0xFFFF8FB8), Color(0xFFC2185B), Color(0xFF1D1119), Color(0xFF2A1823), Color(0xFF3A2231), Color(0xFFFEF6F9), Color(0xFFF8E4EC)),
    HuniPalette("orange", "Orange", Color(0xFFFF9A5C), Color(0xFFC2510A), Color(0xFF1C1410), Color(0xFF2A1D16), Color(0xFF3A2A20), Color(0xFFFEF8F4), Color(0xFFF8EAE0)),
    HuniPalette("indigo", "Indigo", Color(0xFF8C9EFF), Color(0xFF3949AB), Color(0xFF11131F), Color(0xFF1A1D31), Color(0xFF252A45), Color(0xFFF6F7FE), Color(0xFFE7EAFA)),
    HuniPalette("slate", "Slate", Color(0xFFCBD5E1), Color(0xFF334155), Color(0xFF111317), Color(0xFF1B1E24), Color(0xFF262A32), Color(0xFFF7F8FA), Color(0xFFE8EBF0)),
)

val DefaultPalette = HuniPalettes.first()
val DefaultThemeColor = DefaultPalette.accentDark

private val HuniTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontFamily = bbhBartle, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        headlineMedium = base.headlineMedium.copy(fontFamily = bbhBartle, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        headlineSmall = base.headlineSmall.copy(fontFamily = bbhBartle, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleLarge = base.titleLarge.copy(fontFamily = bbhBartle, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleMedium = base.titleMedium.copy(fontSize = 16.sp), titleSmall = base.titleSmall.copy(fontSize = 16.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 13.sp), bodySmall = base.bodySmall.copy(fontSize = 13.sp),
        labelLarge = base.labelLarge.copy(fontSize = 16.sp), labelMedium = base.labelMedium.copy(fontSize = 16.sp), labelSmall = base.labelSmall.copy(fontSize = 13.sp),
    )
}

private val HuniShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp), small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp),
)

private fun Color.mix(other: Color, amount: Float): Color = Color(
    red + (other.red - red) * amount, green + (other.green - green) * amount,
    blue + (other.blue - blue) * amount, alpha + (other.alpha - alpha) * amount,
)

fun HuniPalette.colorScheme(dark: Boolean): ColorScheme {
    val accent = if (dark) accentDark else accentLight
    val bg = if (dark) darkBg else lightBg
    val surface = if (dark) darkSurface else Color.White
    val raised = if (dark) darkRaised else lightRaised
    val text = if (dark) Color(0xFFF4F1F7) else Color(0xFF1B1722)
    val muted = if (dark) Color(0xFFA9A3B5) else Color(0xFF6B6475)
    val outline = if (dark) raised.mix(Color.White, 0.08f) else raised.mix(Color.Black, 0.10f)
    val onAccent = if (dark) Color(0xFF14110F) else Color.White
    return if (dark) darkColorScheme(primary = accent, onPrimary = onAccent, primaryContainer = raised, onPrimaryContainer = text,
        secondary = accent, onSecondary = onAccent, secondaryContainer = raised, onSecondaryContainer = text,
        background = bg, onBackground = text, surface = surface, onSurface = text, surfaceVariant = raised, onSurfaceVariant = muted,
        surfaceTint = accent, outline = outline, outlineVariant = outline, surfaceContainerLowest = bg, surfaceContainerLow = surface,
        surfaceContainer = surface, surfaceContainerHigh = raised, surfaceContainerHighest = raised)
    else lightColorScheme(primary = accent, onPrimary = onAccent, primaryContainer = lightRaised, onPrimaryContainer = text,
        secondary = accent, onSecondary = onAccent, secondaryContainer = lightRaised, onSecondaryContainer = text,
        background = bg, onBackground = text, surface = surface, onSurface = text, surfaceVariant = lightRaised, onSurfaceVariant = muted,
        surfaceTint = accent, outline = outline, outlineVariant = outline, surfaceContainerLowest = bg, surfaceContainerLow = surface,
        surfaceContainer = surface, surfaceContainerHigh = lightRaised, surfaceContainerHighest = lightRaised)
}

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = true,
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    palette: HuniPalette = HuniPalettes.find { it.accentDark == themeColor || it.accentLight == themeColor } ?: DefaultPalette,
    content: @Composable () -> Unit,
) {
    val colorScheme = palette.colorScheme(darkTheme).let { if (pureBlack && darkTheme) it.pureBlack(true) else it }
    MaterialTheme(colorScheme = colorScheme, typography = HuniTypography, shapes = HuniShapes, content = content)
}

fun Bitmap.extractThemeColor(): Color = Color(Palette.from(this).maximumColorCount(8).generate().rankedColors(1, DefaultThemeColor.toArgb()).first())
internal fun Palette.rankedColors(desiredColorCount: Int, fallbackColor: Int): List<Int> = Score.score(swatches.associate { it.rgb to it.population }, desiredColorCount, fallbackColor, true)
fun ColorScheme.pureBlack(apply: Boolean) = if (apply) copy(surface = Color.Black, background = Color.Black) else this
val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
