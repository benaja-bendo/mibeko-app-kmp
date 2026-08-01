package com.mibeko.mibeko.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Helper object for Context injection via Koin.
 */
private object NotificationContextProvider : KoinComponent {
    val context: Context by inject()
}

/**
 * Implémentation Android de la gestion des notifications.
 */
class AndroidNotificationManager(private val context: Context) : NotificationManager {

    /**
     * Demande les permissions de notification.
     */
    override fun requestPermission(onPermissionResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val currentPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            
            if (currentPermission == PackageManager.PERMISSION_GRANTED) {
                onPermissionResult(true)
            } else {
                ActivityProvider.getActivity()?.let { activity ->
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        101
                    )
                    // Note: On ne peut pas facilement attendre le résultat ici sans changer l'architecture.
                    // On retourne true pour permettre au switch de s'activer, car l'utilisateur
                    // a manifesté son intention. La vérification réelle se fera lors de getPushToken.
                    onPermissionResult(true) 
                } ?: onPermissionResult(false)
            }
        } else {
            onPermissionResult(true)
        }
    }

    /**
     * Vérifie si les notifications sont autorisées.
     */
    override fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Récupère le token FCM.
     */
    override fun getPushToken(onTokenResult: (String?) -> Unit) {
        if (isPermissionGranted()) {
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        onTokenResult(null)
                        return@OnCompleteListener
                    }
                    // Get new FCM registration token
                    val token = task.result
                    onTokenResult(token)
                })
            } catch (e: Exception) {
                // Fallback en cas d'erreur (ex: google services manquant)
                e.printStackTrace()
                onTokenResult(null)
            }
        } else {
            onTokenResult(null)
        }
    }
}

actual fun getNotificationManager(): NotificationManager {
    return AndroidNotificationManager(NotificationContextProvider.context)
}
