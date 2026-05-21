package com.verkada.pass.client.mobile

import android.Manifest
import android.app.Notification
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verkada.pass.client.core.Notifications
import com.verkada.pass.client.mobile.ui.theme.VerkadaPassAndroidClientTheme
import com.verkada.pass.client.mobile.ui.views.AppUiState
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

            LaunchedEffect(appState) {
                if (appState is AppUiState.Ready) {
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
            }

            VerkadaPassAndroidClientTheme {
                when (val state = appState) {
                    is AppUiState.Initializing -> SdkInitScreen(
                        state = state.sdkInit,
                        onCopyChallenge = {},
                        onTokenSubmit = viewModel::exchangeToken,
                        modifier = Modifier.fillMaxSize(),
                    )

                    is AppUiState.Ready -> SdkReadyScreen(
                        state = state.state,
                        onRefreshDoors = viewModel::refreshDoors,
                        onResetSdk = viewModel::resetSdk,
                        modifier = Modifier.fillMaxSize(),
                    )

                    AppUiState.Unknown -> SdkLoadingScreen(
                        modifier = Modifier.fillMaxSize(),
                    )
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

        return Notifications.createNotification(
            this,
            channelId = "BLE_UNLOCK_CHANNEL_ID",
            title = getString(R.string.service_notification_title),
            content = getString(R.string.service_notification_text),
            smallLogo = R.drawable.ic_stat_name,
            ongoing = true,
            autoCancel = false,
            priorityLevel = NotificationCompat.PRIORITY_MIN
        )
    }
}
