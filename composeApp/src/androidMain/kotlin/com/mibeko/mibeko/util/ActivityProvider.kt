package com.mibeko.mibeko.util

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Fournisseur d'activité pour Android permettant d'accéder à l'activité courante
 * depuis des services ou des classes utilitaires.
 */
object ActivityProvider {
    private var currentActivity: WeakReference<Activity>? = null

    fun setActivity(activity: Activity) {
        currentActivity = WeakReference(activity)
    }

    fun getActivity(): Activity? {
        return currentActivity?.get()
    }

    fun clear() {
        currentActivity = null
    }
}
