package com.verkada.pass.client.mobile.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.verkada.pass.client.mobile.R

@Composable
fun SdkInitScreen(
    state: SdkInitUiState,
    onCopyChallenge: (String) -> Unit,
    onTokenSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.sdk_initialization),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(24.dp))

            InitStepRow(
                stepNumber = 1,
                state = state.step1,
                showConnector = true,
            ) {
                ChallengeStepContent(
                    state = state.step1,
                    challenge = state.challenge,
                    onCopy = onCopyChallenge,
                )
            }

            InitStepRow(
                stepNumber = 2,
                state = state.step2,
                showConnector = false,
            ) {
                TokenStepContent(
                    state = state.step2,
                    buttonState = state.exchangeButtonState,
                    onSubmit = onTokenSubmit,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InitStepRow(
    stepNumber: Int,
    state: StepState,
    showConnector: Boolean,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight(),
        ) {
            StepIndicator(stepNumber = stepNumber, state = state)
            if (showConnector) {
                StepConnector(modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .alpha(if (state == StepState.Waiting) 0.4f else 1f),
        ) {
            content()
        }
    }
}

@Composable
private fun StepIndicator(stepNumber: Int, state: StepState) {
    val containerColor = when (state) {
        StepState.Complete -> MaterialTheme.colorScheme.primary
        StepState.InProgress,
        StepState.Waiting -> MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            StepState.Complete -> Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.step_complete, stepNumber),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
            StepState.InProgress -> AnimatedDots()
            StepState.Waiting -> Text(
                text = stepNumber.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StepConnector(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun ChallengeStepContent(
    state: StepState,
    challenge: String?,
    onCopy: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = stringResource(R.string.generate_challenge),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when (state) {
                StepState.InProgress -> stringResource(R.string.generating_challenge)
                StepState.Complete -> stringResource(R.string.copy_and_pass_to_your_backend_to_obtain_an_sdk_token)
                StepState.Waiting -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state == StepState.Complete && challenge != null) {
            Spacer(Modifier.height(12.dp))
            ChallengeCodeBox(challenge = challenge, onCopy = { onCopy(challenge) })
        }
    }
}

@Composable
private fun ChallengeCodeBox(challenge: String, onCopy: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp),
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = challenge,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )
        Spacer(Modifier.width(8.dp))
        TextButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(challenge))
                copied = true
                onCopy()
            },
        ) {
            Text(text = if (copied) stringResource(R.string.copied) else stringResource(R.string.copy))
        }
    }
}

@Composable
private fun TokenStepContent(
    state: StepState,
    buttonState: ButtonState,
    onSubmit: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = stringResource(R.string.exchange_sdk_token),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when (state) {
                StepState.Waiting -> stringResource(R.string.waiting_for_challenge)
                StepState.InProgress -> stringResource(R.string.paste_the_sdk_token_returned_by_your_backend)
                StepState.Complete -> stringResource(R.string.sdk_configured_successfully)
            },
            style = MaterialTheme.typography.bodySmall,
            color = when (state) {
                StepState.Complete -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (state == StepState.InProgress) {
            Spacer(Modifier.height(12.dp))
            SdkTokenInput(buttonState = buttonState, onSubmit = onSubmit)
        }
    }
}

@Composable
private fun SdkTokenInput(
    buttonState: ButtonState,
    onSubmit: (String) -> Unit,
) {
    var token by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = buttonState == ButtonState.Idle,
            placeholder = {
                Text(
                    text = stringResource(R.string.paste_sdk_token_here),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            minLines = 3,
        )
        Spacer(Modifier.height(8.dp))
        LoadingButton(
            state = if (token.isBlank()) ButtonState.Idle else buttonState,
            onClick = { onSubmit(token) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.configure_sdk))
        }
    }
}
