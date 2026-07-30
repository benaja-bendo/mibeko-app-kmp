package com.mibeko.mibeko.util

import android.content.Context
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.provider.Settings
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper object for Context injection via Koin.
 */
private object PlatformContextProvider : KoinComponent {
    val context: Context by inject()
}

actual fun getDatabaseSize(): Long {
    val context = PlatformContextProvider.context
    val dbFile = context.getDatabasePath("mibeko.db")
    return if (dbFile.exists()) {
        dbFile.length()
    } else {
        0L
    }
}
actual fun formatTimestampToDate(timestamp: Long): String {
    if (timestamp == 0L) return "Jamais"
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

actual fun getDeviceId(): String {
    val context = PlatformContextProvider.context
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_android"
}

actual fun copyToClipboard(text: String) {
    val context = PlatformContextProvider.context
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Mibeko AI", text)
    clipboard.setPrimaryClip(clip)
}

actual fun getAppVersionName(): String {
    val context = PlatformContextProvider.context
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
    } catch (e: Exception) {
        "0.0.0"
    }
}

actual fun requestInAppReview() {
    // Le flux Play exige une Activity au premier plan ; sans elle, on ne
    // fait rien (l'occasion se représentera).
    val activity = ActivityProvider.getActivity() ?: return
    try {
        val manager = com.google.android.play.core.review.ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                manager.launchReviewFlow(activity, request.result)
            }
        }
    } catch (e: Exception) {
        recordException(e, "in_app_review")
    }
}


