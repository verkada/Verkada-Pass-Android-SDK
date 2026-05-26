package com.verkada.pass.client.wear.presentation.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun StepNumberBadge(number: String) {
    Box(
        modifier = Modifier
            .size(ChipDefaults.LargeIconSize)
            .background(
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.15f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        )
    }
}
