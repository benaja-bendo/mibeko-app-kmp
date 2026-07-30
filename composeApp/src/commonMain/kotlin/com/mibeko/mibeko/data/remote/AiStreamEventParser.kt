package com.mibeko.mibeko.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parsing pur des événements SSE de l'assistant, extrait de [AiApiService]
 * pour être testable sans client HTTP.
 *
 * Retourne null pour tout événement à ignorer ([DONE], bruit, JSON malformé
 * hors événement d'erreur) : le flux de streaming ne doit jamais casser sur un
 * événement inattendu du serveur.
 */
object AiStreamEventParser {

    private val lenientJson = Json { ignoreUnknownKeys = true }

    fun parse(eventType: String?, data: String?): AiStreamEvent? {
        if (data == null || data == "[DONE]") return null
        return when (eventType) {
            "status" -> runCatching {
                lenientJson.decodeFromString<JsonObject>(data)["message"]?.jsonPrimitive?.content
            }.getOrNull()?.let { AiStreamEvent.Status(it) }

            "sources" -> runCatching {
                lenientJson.decodeFromString<List<ArticleSource>>(data)
            }.getOrNull()?.let { AiStreamEvent.Sources(it) }

            // Le texte serveur n'est volontairement PAS transporté : il peut
            // contenir des détails techniques ou commerciaux qui n'ont rien à
            // faire dans l'UI (les libellés sont locaux, choisis par code).
            // Un événement d'erreur illisible reste une erreur : on n'avale
            // jamais un event `error`.
            "error" -> runCatching {
                lenientJson.decodeFromString<JsonObject>(data)["code"]?.jsonPrimitive?.content
            }.fold(
                onSuccess = { AiStreamEvent.Error(code = it) },
                onFailure = { AiStreamEvent.Error(code = null) }
            )

            else -> runCatching {
                val json = lenientJson.decodeFromString<JsonObject>(data)
                if (json["type"]?.jsonPrimitive?.content == "text_delta") {
                    json["delta"]?.jsonPrimitive?.content?.let { AiStreamEvent.Delta(it) }
                } else {
                    null
                }
            }.getOrNull()
        }
    }
}
