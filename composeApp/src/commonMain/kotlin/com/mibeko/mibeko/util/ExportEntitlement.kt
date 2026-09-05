package com.mibeko.mibeko.util

/**
 * Un 403 sur le partage du PDF Mibeko (`legal-documents/{id}/export`,
 * `articles/{id}/export`) signale l'entitlement export absent
 * (mibeko-dashboard#86), pas une panne réseau ou serveur — pur et testable
 * sans Ktor, comme `classifyChatFailure` côté Assistant.
 */
fun isExportEntitlementDenied(statusCode: Int?): Boolean = statusCode == 403
