package com.verkada.pass.client.wear.presentation

import android.Manifest
import android.app.Notification
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verkada.pass.client.core.Notifications
import com.verkada.pass.client.wear.R
import com.verkada.pass.client.wear.presentation.views.WearPassClientApp
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

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        UiEvent.BuildServiceNotification -> {
                            if (ActivityCompat.checkSelfPermission(
                                    this@ClientActivity,
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                if (ActivityCompat.checkSelfPermission(
                                        this@ClientActivity,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    val notification = buildServiceNotification()
                                    viewModel.startBleService(notification)
                                }
                            }
                        }

                        is UiEvent.ShowToast -> {
                            Toast.makeText(this@ClientActivity, event.message, Toast.LENGTH_LONG).show()
                        }
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
