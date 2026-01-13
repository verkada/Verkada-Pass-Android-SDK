package com.verkada.pass.client.mobile

import android.Manifest
import android.app.Notification
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.verkada.android.pass.sdk.ble.BleService
import com.verkada.pass.client.core.Notifications
import com.verkada.pass.client.mobile.ui.theme.VerkadaPassAndroidClientTheme

class ClientActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VerkadaPassAndroidClientTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text = stringResource(R.string.launcher_activity_text)
                        )
                    }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {

                val notification = buildServiceNotification()

                BleService.start(
                    context = this,
                    notificationId = 1,
                    notification = notification
                )
            }

            return
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
