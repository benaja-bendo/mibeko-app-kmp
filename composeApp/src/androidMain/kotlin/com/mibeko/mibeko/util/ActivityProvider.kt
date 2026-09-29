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

    /**
     * Oublie [activity] seulement si c'est encore l'activité courante.
     *
     * Un lien qui démarre l'application fait relancer l'activité par la
     * bibliothèque de navigation : la nouvelle s'enregistre, puis l'ancienne
     * est détruite. Un effacement inconditionnel laissait alors l'application
     * sans activité, et plus aucune fenêtre du système (autorisation des
     * notifications, demande d'avis) ne pouvait s'ouvrir — même famille que
     * kmp#51.
     */
    fun clear(activity: Activity) {
        if (currentActivity?.get() === activity) {
            currentActivity = null
        }
    }
}
