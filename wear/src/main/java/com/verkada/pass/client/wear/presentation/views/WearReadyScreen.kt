package com.verkada.pass.client.wear.presentation.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.verkada.pass.client.wear.R
import com.verkada.pass.client.wear.presentation.ButtonState
import com.verkada.pass.client.wear.presentation.SdkReadyUiState

@Composable
fun WearReadyScreen(
    state: SdkReadyUiState,
    onRefreshDoors: () -> Unit,
    onResetSdk: () -> Unit,
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Icon(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colors.primary,
            )
        }
        item {
            Text(
                text = stringResource(R.string.verkada_pass_title),
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center,
            )
        }
        item {
            Text(
                text = stringResource(R.string.active),
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRefreshDoors,
                enabled = state.refreshButtonState == ButtonState.Idle,
                label = { Text(stringResource(R.string.refresh_doors)) },
                icon = {
                    if (state.refreshButtonState == ButtonState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ChipDefaults.LargeIconSize),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(ChipDefaults.LargeIconSize),
                        )
                    }
                },
            )
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = onResetSdk,
                enabled = state.resetButtonState == ButtonState.Idle,
                colors = ChipDefaults.chipColors(
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.75f),
                    contentColor = MaterialTheme.colors.onError,
                ),
                label = { Text(stringResource(R.string.reset_sdk)) },
                icon = {
                    if (state.resetButtonState == ButtonState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ChipDefaults.LargeIconSize),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(ChipDefaults.LargeIconSize),
                        )
                    }
                },
            )
        }
    }
}
