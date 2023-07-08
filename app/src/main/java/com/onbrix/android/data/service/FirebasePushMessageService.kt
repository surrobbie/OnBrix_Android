package com.onbrix.android.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.RemoteMessage
import com.onbrix.android.data.Constants
import com.onbrix.android.data.event.BusEvent
import com.onbrix.android.data.event.BusProvider
import com.onbrix.android.data.helper.PreferenceHelper
import com.onbrix.android.ext.log
import com.onbrix.android.ui.main.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.onbrix.android.R
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class FirebasePushMessageService: FirebaseMessagingService() {

    @Override
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        log("sendNotification : " + remoteMessage.data)

        sendNotification(remoteMessage.notification, remoteMessage.data)
    }
    override fun onNewToken(token: String) {
        PreferenceHelper.setMessageToken(this, token)
        BusProvider.get()?.let { it.post(BusEvent()) }
    }

    private fun sendNotification(notification: RemoteMessage.Notification?, data: Map<String, String>) {
        try {
            val title = notification?.title
            val message = notification?.body
            var image = notification?.imageUrl
            val url = data.get("url")

            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            if (!url.isNullOrEmpty()) {
                intent.putExtra(Constants.URL, url)
            }

            val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent, PendingIntent.FLAG_MUTABLE)
            val channelId = getString(R.string.default_notification_channel_id)
            var channelName = getString(R.string.default_notification_channel_name)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

            // 이미지 정보가 있을 경우에만 적용

            image.let {
                val bitmap = getBitmapFromURL(it)
                notificationBuilder.setLargeIcon(bitmap)
                    .setStyle(NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(null))
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
                channel.setShowBadge(false)
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
            log("error : " + e.message)
        }
    }

    private fun getBitmapFromURL(strURL: Uri?): Bitmap? {
        return try {
            val url = URL(strURL.toString())
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input: InputStream = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

}