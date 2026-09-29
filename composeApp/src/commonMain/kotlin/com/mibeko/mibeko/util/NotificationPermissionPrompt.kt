package com.mibeko.mibeko.util

import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.repository.PushTokenRegistrar

/**
 * Demande l'autorisation des notifications au bon moment (APP-004, kmp#34) :
 * une seule fois, après le premier téléchargement d'un texte pour le
 * hors-ligne, jamais à l'ouverture ni pendant le guide de découverte.
 *
 * Jusqu'à la 1.4, elle n'était demandée que depuis les Réglages : 3 appareils
 * étaient inscrits pour recevoir la veille, sur plus de 70 comptes mobiles.
 */
class NotificationPermissionPrompt(
    private val preferences: UserPreferencesRepository,
    private val notificationManager: NotificationManager,
    private val pushTokenRegistrar: PushTokenRegistrar,
    private val analytics: MibekoAnalytics,
    /**
     * Seul Android reçoit des push aujourd'hui. Sur iPhone, rien n'arrivera
     * avant kmp#18 (APNs) : on garde la seule demande qu'iOS autorise pour le
     * jour où elle servira.
     */
    private val pushSupported: Boolean
) {
    /** Vrai tant qu'une demande partie d'ici attend la réponse de la fenêtre du système. */
    private var awaitingSystemAnswer = false

    /** Appelé après un téléchargement hors-ligne réussi. Sans effet après la première fois. */
    fun afterOfflineDownload() {
        if (!pushSupported) return
        // Réglage coupé dans l'application : c'est un choix explicite, on le respecte.
        if (!preferences.isNotificationsEnabled()) return
        if (preferences.wasNotificationPermissionAsked()) return

        if (notificationManager.isPermissionGranted()) {
            // Android 12 et avant, ou autorisation déjà donnée : rien à demander.
            preferences.setNotificationPermissionAsked(true)
            registerDevice()
            return
        }
        awaitingSystemAnswer = true
        notificationManager.requestPermission { windowShown ->
            // Android : ce rappel arrive dès l'ouverture de la fenêtre, avant
            // la réponse, qui passe par [onSystemPermissionResult]. `false`
            // veut dire qu'aucune fenêtre n'a pu s'ouvrir (pas d'activité au
            // premier plan) : l'unique demande n'est pas consommée, elle
            // sera reposée au prochain téléchargement.
            if (!windowShown) {
                awaitingSystemAnswer = false
                return@requestPermission
            }
            preferences.setNotificationPermissionAsked(true)
            if (notificationManager.isPermissionGranted()) {
                onSystemPermissionResult(true)
            }
        }
    }

    /**
     * Réponse réelle de la fenêtre du système (Android : `MainActivity`),
     * quelle que soit l'origine de la demande : Réglages compris, l'appareil
     * s'enregistre dès que l'autorisation est accordée.
     */
    fun onSystemPermissionResult(granted: Boolean) {
        if (awaitingSystemAnswer) {
            awaitingSystemAnswer = false
            analytics.logEvent(
                AnalyticsEvents.NOTIFICATION_OPT_IN,
                mapOf("granted" to granted, "context" to "after_download")
            )
        }
        if (granted) registerDevice()
    }

    private fun registerDevice() {
        notificationManager.getPushToken { token ->
            if (token != null) {
                pushTokenRegistrar.onNewToken(token)
            } else {
                pushTokenRegistrar.flushPendingToken()
            }
        }
    }
}
