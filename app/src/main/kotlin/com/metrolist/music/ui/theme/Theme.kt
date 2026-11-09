/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.materialkolor.score.Score

val DefaultThemeColor = Color(0xFFFFB84D)

private val HuniTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontFamily = bbhBartle, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        headlineMedium = base.headlineMedium.copy(fontFamily = bbhBartle, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        headlineSmall = base.headlineSmall.copy(fontFamily = bbhBartle, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleLarge = base.titleLarge.copy(fontFamily = bbhBartle, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleMedium = base.titleMedium.copy(fontSize = 16.sp),
        titleSmall = base.titleSmall.copy(fontSize = 16.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 13.sp),
        bodySmall = base.bodySmall.copy(fontSize = 13.sp),
        labelLarge = base.labelLarge.copy(fontSize = 16.sp),
        labelMedium = base.labelMedium.copy(fontSize = 16.sp),
        labelSmall = base.labelSmall.copy(fontSize = 13.sp),
    )
}

private val HuniShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = true,
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    // Huni uses a fixed dark palette; retain parameters for existing callers/preferences.
    val colorScheme = darkColorScheme(
        primary = Color(0xFFFFB84D), onPrimary = Color(0xFF2A1B00),
        primaryContainer = Color(0xFF5A3B13), onPrimaryContainer = Color(0xFFFFDDAA),
        secondary = Color(0xFFD9C7E8), onSecondary = Color(0xFF30243B),
        secondaryContainer = Color(0xFF44364F), onSecondaryContainer = Color(0xFFEADCF5),
        background = Color(0xFF16121C), onBackground = Color(0xFFF4EEF8),
        surface = Color(0xFF211B2A), onSurface = Color(0xFFF4EEF8),
        surfaceVariant = Color(0xFF2C2437), onSurfaceVariant = Color(0xFFA99FB8),
        surfaceTint = Color(0xFFFFB84D), outline = Color(0xFF3A3147),
        outlineVariant = Color(0xFF3A3147),
        surfaceContainerLowest = Color(0xFF16121C),
        surfaceContainerLow = Color(0xFF211B2A),
        surfaceContainer = Color(0xFF211B2A),
        surfaceContainerHigh = Color(0xFF2C2437),
        surfaceContainerHighest = Color(0xFF332B3F),
    )
    // Use standard MaterialTheme instead of MaterialExpressiveTheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = HuniTypography,
        shapes = HuniShapes,
        content = content,
    )
}

fun Bitmap.extractThemeColor(): Color = Color(
    Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .rankedColors(1, DefaultThemeColor.toArgb())
        .first()
)

internal fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> = Score.score(
    swatches.associate { it.rgb to it.population },
    desiredColorCount,
    fallbackColor,
    true,
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
