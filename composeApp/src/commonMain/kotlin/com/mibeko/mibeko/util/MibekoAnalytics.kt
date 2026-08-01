package com.mibeko.mibeko.util

import com.mibeko.mibeko.data.preferences.UserPreferencesRepository

/**
 * Façade analytics de l'app : SEUL point d'émission d'événements produit.
 *
 * Trois garde-fous non négociables :
 * - **Consentement** : rien ne part si le toggle « Partage de statistiques
 *   anonymes » est coupé (en plus de setAnalyticsCollectionEnabled côté SDK,
 *   ceinture et bretelles).
 * - **Aucune donnée juridiquement sensible** : jamais le texte d'une recherche,
 *   d'une question à l'assistant ou d'un contenu de dossier — uniquement des
 *   compteurs, longueurs, types et drapeaux.
 * - **Aucun identifiant de compte** : on n'appelle JAMAIS setUserId. Le sujet
 *   juridique consulté (divorce, licenciement, pénal…) ne doit pas pouvoir
 *   être rattaché à une personne identifiée — c'est ce qui rend le mot
 *   « anonymes » du réglage et de la politique de confidentialité exact.
 */
class MibekoAnalytics(
    private val delegate: AnalyticsManager,
    private val preferences: UserPreferencesRepository
) {

    /** À appeler au démarrage : applique le consentement persisté au SDK. */
    fun applyStoredConsent() {
        delegate.setCollectionEnabled(preferences.isTelemetryEnabled())
    }

    /** Change le consentement (toggle des Réglages) et l'applique au SDK. */
    fun setTelemetryEnabled(enabled: Boolean) {
        preferences.setTelemetryEnabled(enabled)
        delegate.setCollectionEnabled(enabled)
    }

    fun isTelemetryEnabled(): Boolean = preferences.isTelemetryEnabled()

    fun logEvent(name: String, params: Map<String, Any>? = null) {
        if (!preferences.isTelemetryEnabled()) return
        delegate.logEvent(name, params)
    }
}

/** Noms des événements produit (funnel v1.1). */
object AnalyticsEvents {
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    const val SEARCH_PERFORMED = "search_performed"
    const val DOCUMENT_OPENED = "document_opened"
    const val ARTICLE_READ = "article_read"
    const val READER_SHARE = "reader_share"
    const val OFFLINE_DOWNLOAD = "offline_download"
    const val FAVORITE_ADDED = "favorite_added"
    const val DOSSIER_CREATED = "dossier_created"
    const val CHAT_MESSAGE_SENT = "chat_message_sent"
    const val CHAT_ERROR = "chat_error"
    const val NOTIFICATION_OPT_IN = "notification_opt_in"
    const val DEEP_LINK_OPENED = "deep_link_opened"
    const val DEEP_LINK_RESOLUTION_FAILED = "deep_link_resolution_failed"
    const val REPORT_SUBMITTED = "report_submitted"
    const val APP_UPDATE_PROMPTED = "app_update_prompted"
    const val APP_UPDATE_FORCED = "app_update_forced"
    const val REVIEW_REQUESTED = "review_requested"
    const val CONTACT_SUBMITTED = "contact_submitted"
}
