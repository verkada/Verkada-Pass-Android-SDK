package com.verkada.pass.client.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat

object Notifications {

    fun createChannel(
        context: Context,
        channelId: String,
        @StringRes channelName: Int,
        @StringRes channelDescription: Int,
        importanceLevel: Int = NotificationManager.IMPORTANCE_HIGH
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(channelName),
                importanceLevel
            ).apply {
                description = context.getString(channelDescription)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        } else {
            NotificationCompat.Builder(context, channelId)
                .setPriority(importanceLevel)
                .build()
        }
    }

    fun createNotification(
        context: Context,
        channelId: String,
        content: String,
        title: String? = null,
        @DrawableRes
        smallLogo: Int,
        pendingIntent: PendingIntent? = null,
        deletePendingIntent: PendingIntent? = null,
        ongoing: Boolean = false,
        autoCancel: Boolean = true,
        priorityLevel: Int = NotificationCompat.PRIORITY_MAX
    ) : Notification {
        val builder = NotificationCompat.Builder(context, channelId)
        return builder
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(smallLogo)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setPriority(priorityLevel)
            .setAutoCancel(autoCancel)
            .build()
    }
}