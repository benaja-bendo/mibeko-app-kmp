package com.mibeko.mibeko.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mibeko.mibeko.MainActivity
import com.mibeko.mibeko.R
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MyFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    private val userPreferencesRepository: UserPreferencesRepository by inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Vérifier si les notifications sont activées dans les préférences de l'app
        // Note: On pourrait aussi le gérer côté serveur, mais c'est une sécurité supplémentaire
        // Pour l'instant on affiche toujours si reçu

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            sendNotification(it.title ?: "Mibeko", it.body ?: "")
        }
        
        // Check if message contains data payload.
        if (remoteMessage.data.isNotEmpty()) {
            // Handle data payload if needed
        }
    }

    override fun onNewToken(token: String) {
        // Si on avait une logique pour envoyer le token au serveur ici, on le ferait.
        // Mais notre architecture le fait via le ViewModel/Repository quand l'app est lancée ou quand le switch change.
        super.onNewToken(token)
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // TODO: Utiliser une icône monochrome dédiée (ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
}
