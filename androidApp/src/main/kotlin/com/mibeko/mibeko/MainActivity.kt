package com.mibeko.mibeko

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mibeko.mibeko.util.ActivityProvider
import com.mibeko.mibeko.util.ExternalUriHandler

class MainActivity : ComponentActivity() {
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

    override fun onDestroy() {
        super.onDestroy()
        ActivityProvider.clear()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}