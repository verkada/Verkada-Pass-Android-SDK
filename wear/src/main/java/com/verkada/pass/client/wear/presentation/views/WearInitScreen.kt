package com.verkada.pass.client.wear.presentation.views

import android.app.RemoteInput
import android.content.ClipData
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
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
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import com.verkada.pass.client.wear.R
import com.verkada.pass.client.wear.presentation.ButtonState
import com.verkada.pass.client.wear.presentation.SdkInitUiState
import com.verkada.pass.client.wear.presentation.StepState

@Composable
fun WearInitScreen(
    state: SdkInitUiState,
    onTokenSubmit: (String) -> Unit,
) {
    val tokenKey = "sdk_token"
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    val tokenInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val bundle = RemoteInput.getResultsFromIntent(
            result.data ?: return@rememberLauncherForActivityResult,
        )
        val token = bundle?.getString(tokenKey)?.takeIf { it.isNotBlank() }
            ?: return@rememberLauncherForActivityResult
        onTokenSubmit(token)
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.sdk_setup),
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                enabled = state.step1 != StepState.Waiting,
                onClick = {
                    if (state.step1 == StepState.Complete) {
                        state.challenge?.let {
                            scope.launch {
                                clipboard.setClipEntry(ClipData.newPlainText(null, it).toClipEntry())
                            }
                            copied = true
                        }
                    }
                },
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (state.step1 == StepState.Complete)
                        MaterialTheme.colors.primaryVariant
                    else
                        MaterialTheme.colors.surface,
                ),
                label = {
                    Text(
                        text = when (state.step1) {
                            StepState.InProgress -> stringResource(R.string.generating)
                            StepState.Complete -> stringResource(R.string.challenge_ready)
                            StepState.Waiting -> stringResource(R.string.generate_challenge)
                        },
                    )
                },

                secondaryLabel = {
                    Text(
                        text = when (state.step1) {
                            StepState.InProgress -> stringResource(R.string.step_1_of_2)
                            StepState.Complete -> stringResource(if (copied) R.string.copied else R.string.tap_to_copy)
                            StepState.Waiting -> ""
                        },
                    )
                },
                icon = {
                    when (state.step1) {
                        StepState.Complete -> Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(ChipDefaults.LargeIconSize),
                        )

                        StepState.InProgress -> CircularProgressIndicator(
                            modifier = Modifier.size(ChipDefaults.LargeIconSize),
                            strokeWidth = 2.dp,
                        )

                        StepState.Waiting -> StepNumberBadge("1")
                    }
                },
            )
        }

        if (state.challenge != null) {
            item {
                Text(
                    text = state.challenge,
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                )
            }
        }

        item {
            val label = stringResource(R.string.paste_or_dictate_sdk_token)

            Chip(
                modifier = Modifier.fillMaxWidth(),
                enabled = state.step2 == StepState.InProgress,
                onClick = {
                    val remoteInputs = listOf(
                        RemoteInput.Builder(tokenKey)
                            .setLabel(label)
                            .wearableExtender { setEmojisAllowed(false) }
                            .build(),
                    )
                    val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                    RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                    tokenInputLauncher.launch(intent)
                },
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (state.step2 == StepState.Complete)
                        MaterialTheme.colors.primaryVariant
                    else
                        MaterialTheme.colors.surface,
                ),
                label = {
                    Text(
                        text = when (state.step2) {
                            StepState.Waiting -> stringResource(R.string.enter_sdk_token)
                            StepState.InProgress -> stringResource(R.string.enter_sdk_token)
                            StepState.Complete -> stringResource(R.string.token_accepted)
                        },
                    )
                },
                secondaryLabel = {
                    Text(
                        text = when (state.step2) {
                            StepState.Waiting -> stringResource(R.string.waiting_for_step_1)
                            StepState.InProgress -> stringResource(R.string.tap_to_dictate_type)
                            StepState.Complete -> stringResource(R.string.sdk_configured)
                        },
                    )
                },
                icon = {
                    when (state.step2) {
                        StepState.Complete -> Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(ChipDefaults.LargeIconSize),
                        )

                        StepState.InProgress -> if (state.exchangeButtonState == ButtonState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(ChipDefaults.LargeIconSize),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(ChipDefaults.LargeIconSize),
                            )
                        }

                        StepState.Waiting -> StepNumberBadge("2")
                    }
                },
            )
        }
    }
}
