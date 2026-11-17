package com.metrolist.music.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import kotlin.math.sin

@Composable
fun WavySetupLoader(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "huni_setup_wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
        label = "wave_phase",
    )
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary
    Canvas(modifier.size(width = 72.dp, height = 44.dp).then(modifier).semantics { contentDescription = "Loading" }) {
        val center = size.height / 2f
        val amplitude = size.height * .22f
        val cycles = 1.7f
        for ((index, color) in listOf(primary, secondary).withIndex()) {
            val path = Path()
            val offset = index * 1.2f
            for (step in 0..48) {
                val x = size.width * step / 48f
                val angle = (x / size.width * cycles * Math.PI * 2).toFloat() + phase + offset
                val y = center + sin(angle) * amplitude
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color.copy(alpha = if (index == 0) .95f else .65f), style = Stroke(width = 3.dp.toPx()))
        }
    }
}
