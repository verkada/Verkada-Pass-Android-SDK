package com.verkada.pass.client.mobile.ui.views

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
internal fun AnimatedDots() {
    val transition = rememberInfiniteTransition(label = "dots")

    val alphas = (0..2).map { index ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1200
                    0.2f at 0 using LinearEasing
                    1f at 400 using LinearEasing
                    0.2f at 800 using LinearEasing
                    0.2f at 1200 using LinearEasing
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(index * 200),
            ),
            label = "dot_alpha_$index",
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        alphas.forEachIndexed { index, alpha ->
            if (index > 0) Spacer(Modifier.width(3.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .alpha(alpha.value)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}
