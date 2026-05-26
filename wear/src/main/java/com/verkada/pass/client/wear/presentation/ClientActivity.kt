package com.verkada.pass.client.wear.presentation

import android.Manifest
import android.app.Notification
import android.app.RemoteInput
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender
import com.verkada.pass.client.core.Notifications
import com.verkada.pass.client.wear.R
import com.verkada.pass.client.wear.presentation.theme.VerkadaPassAndroidClientTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClientActivity : ComponentActivity() {

    private val viewModel: WearClientViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            val appState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(appState) {
                if (appState is AppUiState.Ready) {
                    val hasBleCon = ActivityCompat.checkSelfPermission(
                        this@ClientActivity,
                        Manifest.permission.BLUETOOTH_CONNECT,
                    ) == PackageManager.PERMISSION_GRANTED

                    val hasNotif = ActivityCompat.checkSelfPermission(
                        this@ClientActivity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasBleCon && hasNotif) {
                        viewModel.startBleService(buildServiceNotification())
                    }
                }
            }

            WearPassClientApp(
                appState = appState,
                onTokenSubmit = viewModel::exchangeToken,
                onRefreshDoors = viewModel::refreshDoors,
                onResetSdk = viewModel::resetSdk,
            )
        }
    }

    private fun buildServiceNotification(): Notification {
        Notifications.createChannel(
            this,
            channelId = "BLE_UNLOCK_CHANNEL_ID",
            channelName = R.string.notification_channel_name,
            channelDescription = R.string.notification_channel_description,
            importanceLevel = NotificationManagerCompat.IMPORTANCE_HIGH,
        )
        return Notifications.createNotification(
            this,
            channelId = "BLE_UNLOCK_CHANNEL_ID",
            title = getString(R.string.service_notification_title),
            content = getString(R.string.service_notification_text),
            smallLogo = R.drawable.ic_stat_name,
            ongoing = true,
            autoCancel = false,
            priorityLevel = NotificationCompat.PRIORITY_MIN,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Root
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WearPassClientApp(
    appState: AppUiState,
    onTokenSubmit: (String) -> Unit,
    onRefreshDoors: () -> Unit,
    onResetSdk: () -> Unit,
) {
    VerkadaPassAndroidClientTheme {
        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        ) {
            when (val state = appState) {
                AppUiState.Unknown -> WearLoadingScreen()
                is AppUiState.Initializing -> WearInitScreen(
                    state = state.sdkInit,
                    onTokenSubmit = onTokenSubmit,
                )
                is AppUiState.Ready -> WearReadyScreen(
                    state = state.state,
                    onRefreshDoors = onRefreshDoors,
                    onResetSdk = onResetSdk,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading (AppUiState.Unknown)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WearLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helper — circular badge that holds a step number
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepNumberBadge(number: String) {
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

// ─────────────────────────────────────────────────────────────────────────────
// Initializing (AppUiState.Initializing)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WearInitScreen(
    state: SdkInitUiState,
    onTokenSubmit: (String) -> Unit,
) {
    val tokenKey = "sdk_token"
    val clipboardManager = LocalClipboardManager.current

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
                text = "SDK setup",
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        // Step 1 — generate challenge
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                enabled = state.step1 != StepState.Waiting,
                onClick = {
                    if (state.step1 == StepState.Complete) {
                        state.challenge?.let { clipboardManager.setText(AnnotatedString(it)) }
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
                            StepState.InProgress -> "Generating…"
                            StepState.Complete   -> "Challenge ready"
                            StepState.Waiting    -> "Generate challenge"
                        },
                    )
                },
                // Fix ①: secondary label present for every state
                secondaryLabel = {
                    Text(
                        text = when (state.step1) {
                            StepState.InProgress -> "Step 1 of 2"
                            StepState.Complete   -> "Tap to copy"
                            StepState.Waiting    -> ""
                        },
                    )
                },
                icon = {
                    when (state.step1) {
                        // Fix ③: proper check icon instead of ic_stat_name
                        StepState.Complete   -> Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(ChipDefaults.LargeIconSize),
                        )
                        StepState.InProgress -> CircularProgressIndicator(
                            modifier = Modifier.size(ChipDefaults.LargeIconSize),
                            strokeWidth = 2.dp,
                        )
                        // Fix ②: step number wrapped in a circular badge
                        StepState.Waiting    -> StepNumberBadge("1")
                    }
                },
            )
        }

        // Step 2 — exchange SDK token via RemoteInput (voice or on-watch keyboard)
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                enabled = state.step2 == StepState.InProgress,
                onClick = {
                    val remoteInputs = listOf(
                        RemoteInput.Builder(tokenKey)
                            .setLabel("Paste or dictate SDK token")
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
                            StepState.Waiting    -> "Enter SDK token"
                            StepState.InProgress -> "Enter SDK token"
                            StepState.Complete   -> "Token accepted"
                        },
                    )
                },
                secondaryLabel = {
                    Text(
                        text = when (state.step2) {
                            StepState.Waiting    -> "Waiting for step 1"
                            StepState.InProgress -> "Tap to dictate / type"
                            StepState.Complete   -> "SDK configured"
                        },
                    )
                },
                icon = {
                    when (state.step2) {
                        StepState.Complete   -> Icon(
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
                            // Fix ④: material Mic icon instead of low-res system drawable
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(ChipDefaults.LargeIconSize),
                            )
                        }
                        // Fix ②: step number wrapped in a circular badge
                        StepState.Waiting    -> StepNumberBadge("2")
                    }
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ready (AppUiState.Ready)
// ─────────────────────────────────────────────────────────────────────────────

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
                text = "Verkada Pass",
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center,
            )
        }
        // Fix ⑤: green dot prefix matches mockup
        item {
            Text(
                text = "● Active",
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        // Refresh Doors
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRefreshDoors,
                enabled = state.refreshButtonState == ButtonState.Idle,
                label = { Text("Refresh doors") },
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

        // Reset SDK — destructive action, error-tinted background
        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = onResetSdk,
                enabled = state.resetButtonState == ButtonState.Idle,
                colors = ChipDefaults.chipColors(
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.75f),
                    contentColor = MaterialTheme.colors.onError,
                ),
                label = { Text("Reset SDK") },
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
