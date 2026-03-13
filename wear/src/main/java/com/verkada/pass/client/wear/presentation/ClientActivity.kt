package com.verkada.pass.client.wear.presentation

import android.Manifest
import android.app.Notification
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.verkada.android.pass.sdk.ble.VerkadaPassBle
import com.verkada.pass.client.core.Notifications
import com.verkada.pass.client.wear.R
import com.verkada.pass.client.wear.presentation.theme.VerkadaPassAndroidClientTheme

class ClientActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearPassClientApp()
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {

                val notification = buildServiceNotification()

                VerkadaPassBle.start(
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

@Composable
fun WearPassClientApp() {
    VerkadaPassAndroidClientTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = stringResource(R.string.launcher_activity_text)
            )
        }
    }
}
