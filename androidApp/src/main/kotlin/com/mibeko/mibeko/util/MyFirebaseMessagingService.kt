package com.mibeko.mibeko.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mibeko.mibeko.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mibeko.mibeko.MainActivity
import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.repository.PushTokenRegistrar
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MyFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    private val userPreferencesRepository: UserPreferencesRepository by inject()
    private val pushTokenRegistrar: PushTokenRegistrar by inject()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Second garde-fou : l'utilisateur a pu couper les notifications dans
        // l'app sans que le serveur l'ait encore appris.
        if (!userPreferencesRepository.isNotificationsEnabled()) return

        // Le serveur joint un `data` décrivant la cible (un texte publié) :
        // sans lui, taper la notification n'ouvrait que l'accueil.
        val deepLink = deepLinkFrom(remoteMessage.data)

        val notification = remoteMessage.notification
        val title = notification?.title ?: remoteMessage.data["title"] ?: "Mibeko"
        val body = notification?.body ?: remoteMessage.data["message"] ?: ""
        if (title.isNotBlank() || body.isNotBlank()) {
            sendNotification(title, body, deepLink)
        }
    }

    /**
     * Construit le lien interne à ouvrir au tap.
     *
     * Le serveur envoie `type` + `slug` (et éventuellement `article`) ; on les
     * traduit vers le schéma `mibeko://` que la navigation sait résoudre — le
     * même chemin que les liens publics `mibeko.fr/textes/…`.
     */
    private fun deepLinkFrom(data: Map<String, String>): String? {
        val slug = data["slug"]?.takeIf { it.isNotBlank() } ?: return null
        val article = data["article"]?.takeIf { it.isNotBlank() }

        return if (article != null) {
            "mibeko://textes/$slug/article-$article"
        } else {
            "mibeko://textes/$slug"
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // FCM peut renouveler le token à tout moment : sans ré-enregistrement,
        // le backend continue de pousser vers un token mort. Si l'utilisateur
        // n'est pas connecté, le token est mis en attente et envoyé au
        // prochain login / démarrage connecté.
        pushTokenRegistrar.onNewToken(token)
    }

    private fun sendNotification(title: String, messageBody: String, deepLink: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Porté en ACTION_VIEW : MainActivity le transmet à la navigation
            // Compose, qui route vers le texte concerné.
            if (deepLink != null) {
                action = Intent.ACTION_VIEW
                data = android.net.Uri.parse(deepLink)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            // Request code distinct par cible : sans lui, Android réutiliserait
            // le PendingIntent de la notification précédente et ouvrirait le
            // mauvais texte.
            deepLink?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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
                "Alertes Mibeko",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Id dérivé du contenu : un id fixe (0) faisait s'écraser les
        // notifications entre elles.
        val notificationId = (title + messageBody).hashCode()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
