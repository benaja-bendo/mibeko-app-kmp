package com.mibeko.mibeko

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mibeko.mibeko.util.ActivityProvider
import com.mibeko.mibeko.util.ExternalUriHandler
import com.mibeko.mibeko.util.NOTIFICATION_PERMISSION_REQUEST_CODE
import com.mibeko.mibeko.util.NotificationPermissionPrompt
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val notificationPermissionPrompt: NotificationPermissionPrompt by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityProvider.setActivity(this)
        enableEdgeToEdge()

        setContent {
            App()
        }
    }

    // Démarrage à chaud : l'activité étant en singleTask, un App Link ouvert
    // alors que l'app tourne déjà arrive ici (et non par onCreate). On le relaie
    // au pont de deep links partagé, qui le route vers le NavController — sans ça
    // le lien serait silencieusement ignoré.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { ExternalUriHandler.onNewUri(it.toString()) }
    }

    // Android 13 et plus : la vraie réponse à la fenêtre d'autorisation des
    // notifications n'arrive qu'ici. Sans ce relais, l'appareil ne
    // s'enregistrait qu'au démarrage suivant (kmp#34).
    @Deprecated("Demande lancée par ActivityCompat.requestPermissions")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            notificationPermissionPrompt.onSystemPermissionResult(granted)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityProvider.clear(this)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}