package com.metrolist.music.ui.screens

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.metrolist.music.R

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
private fun MinimalSetupIndicator(animate: Boolean) {
    if (animate) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp).semantics { contentDescription = "Loading" },
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
        )
    } else {
        Box(
            Modifier.size(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .semantics { contentDescription = "Loading" },
        )
    }
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
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.huni_wordmark_bird),
            contentDescription = "Huni wordmark with bird",
            modifier = Modifier.width(224.dp).height(91.dp),
            contentScale = ContentScale.Fit,
        )
        MinimalSetupIndicator(animate = animationsEnabled)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
