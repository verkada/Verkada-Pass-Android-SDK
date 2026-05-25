package com.verkada.pass.client.mobile

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verkada.pass.client.core.Notifications
import com.verkada.pass.client.mobile.ui.theme.VerkadaPassAndroidClientTheme
import com.verkada.pass.client.mobile.ui.views.AppUiState
import com.verkada.pass.client.mobile.ui.views.UiEvent
import com.verkada.pass.client.mobile.ui.views.SdkInitScreen
import com.verkada.pass.client.mobile.ui.views.SdkLoadingScreen
import com.verkada.pass.client.mobile.ui.views.SdkReadyScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class ClientActivity : ComponentActivity() {

    private val viewModel: ClientViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val context = LocalContext.current
            val appState by viewModel.uiState.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        UiEvent.BuildServiceNotification -> {
                            if (ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                if (ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) ==
                                    PackageManager.PERMISSION_GRANTED
                                ) {
                                    val notification = buildServiceNotification()
                                    viewModel.startBleService(notification)
                                }
                            }
                        }

                        is UiEvent.ShowSnackbar -> {
                            snackbarHostState.showSnackbar(
                                message = event.message,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            }

            VerkadaPassAndroidClientTheme {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                            Snackbar(
                                snackbarData = snackbarData,
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                ) { paddingValues ->
                    when (val state = appState) {
                        is AppUiState.Initializing -> SdkInitScreen(
                            state = state.sdkInit,
                            onCopyChallenge = {},
                            onTokenSubmit = viewModel::exchangeToken,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        )

                        is AppUiState.Ready -> SdkReadyScreen(
                            state = state.state,
                            onRefreshDoors = viewModel::refreshDoors,
                            onResetSdk = viewModel::resetSdk,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        )

                        AppUiState.Unknown -> SdkLoadingScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                        )
                    }
                }
            }
        }
    }

    private fun buildServiceNotification(): Notification {
        Notifications.createChannel(
            this,
            channelId = "BLE_UNLOCK_CHANNEL_ID",
            channelName = R.string.notification_channel_name,
            channelDescription = R.string.notification_channel_description,
            importanceLevel = NotificationManagerCompat.IMPORTANCE_HIGH
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, ClientActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notifications.createNotification(
            this,
            channelId = "BLE_UNLOCK_CHANNEL_ID",
            title = getString(R.string.service_notification_title),
            content = getString(R.string.service_notification_text),
            smallLogo = R.drawable.ic_stat_name,
            pendingIntent = contentIntent,
            ongoing = true,
            autoCancel = false,
            priorityLevel = NotificationCompat.PRIORITY_MIN
        )
    }
}
