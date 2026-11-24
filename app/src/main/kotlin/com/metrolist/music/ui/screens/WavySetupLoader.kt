package com.metrolist.music.ui.screens

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import com.metrolist.music.R

@Composable
fun WavySetupLoader(modifier: Modifier = Modifier, animate: Boolean = true) {
    val phase = if (animate) {
        val transition = rememberInfiniteTransition(label = "huni_setup_wave")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2).toFloat(),
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
            label = "wave_phase",
        )
        animatedPhase
    } else 0f
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

@Composable
internal fun FallingSakura(modifier: Modifier = Modifier, animate: Boolean = true) {
    val progress = if (animate) {
        val transition = rememberInfiniteTransition(label = "huni_sakura")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(11_000), RepeatMode.Restart),
            label = "petal_fall",
        )
        value
    } else 0f
    val petal = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.semantics { contentDescription = "Soft falling sakura" }) {
        repeat(6) { index ->
            val phase = (progress + index * .19f) % 1f
            val x = size.width * (.08f + index * .17f) + sin((phase * 6.28f + index).toDouble()).toFloat() * 13.dp.toPx()
            val y = size.height * phase
            drawCircle(petal.copy(alpha = .16f), radius = (2.6f + index % 2) * density, center = Offset(x, y))
        }
    }
}

@Composable
fun rememberSystemAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) { systemAnimationsEnabled(context) }
}

private fun systemAnimationsEnabled(context: Context): Boolean {
    val animatorScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val transitionScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    val systemAllowsAnimations = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
    return systemAllowsAnimations && animatorScale > 0f && transitionScale > 0f
}

@Composable
fun FirstRunSetupContent(step: Int, animationsEnabled: Boolean, modifier: Modifier = Modifier) {
    val message = when (step.coerceIn(0, 2)) {
        0 -> "Setting things up…"
        1 -> "Almost done…"
        else -> "Welcome to Huni"
    }
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.huni_wordmark_bird),
            contentDescription = "Huni wordmark with bird",
            modifier = Modifier.width(260.dp).height(92.dp),
            contentScale = ContentScale.Fit,
        )
        FallingSakura(Modifier.width(220.dp).height(100.dp), animationsEnabled)
        WavySetupLoader(animate = animationsEnabled)
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
