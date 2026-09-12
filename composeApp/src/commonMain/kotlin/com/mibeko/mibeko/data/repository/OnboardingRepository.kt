package com.mibeko.mibeko.data.repository

import com.mibeko.mibeko.data.preferences.UserPreferencesRepository
import com.mibeko.mibeko.data.remote.OnboardingApiService
import com.mibeko.mibeko.data.remote.OnboardingJourneyData
import com.mibeko.mibeko.data.remote.OnboardingProgress
import com.mibeko.mibeko.data.remote.OnboardingStepMutation
import com.mibeko.mibeko.getCurrentTimeMillis
import com.mibeko.mibeko.getPlatform
import com.mibeko.mibeko.util.recordException
import com.mibeko.mibeko.util.NetworkConnectivityChecker
import io.ktor.client.plugins.ClientRequestException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

sealed interface OnboardingLoadResult {
    data class Ready(
        val data: OnboardingJourneyData,
        val offline: Boolean,
        val pendingMutations: Int
    ) : OnboardingLoadResult

    data object Unavailable : OnboardingLoadResult
}

@Serializable
private data class PendingOnboardingMutation(
    val kind: String,
    val stepKey: String? = null,
    val action: String? = null,
    val value: JsonElement? = null,
    val mutationId: String,
    val clientUpdatedAt: Long
)

/**
 * Cache et outbox du parcours, tous deux isolés par le compte courant.
 * Une mutation est persistée AVANT tout appel réseau. Au retour en ligne, son
 * horodatage d'origine est rejoué : le LWW serveur protège ainsi une réponse
 * web plus récente au lieu de la remplacer par l'état mobile retardé.
 */
class OnboardingRepository(
    private val api: OnboardingApiService,
    private val preferences: UserPreferencesRepository,
    private val json: Json,
    private val connectivity: NetworkConnectivityChecker
) {
    companion object {
        val KNOWN_STEP_TYPES = listOf("welcome", "single_choice", "multi_choice", "optional_field", "guided_action", "checklist")
    }

    private val platform: String
        get() = if (getPlatform().name.lowercase().contains("ios")) "ios" else "android"

    suspend fun load(): OnboardingLoadResult {
        if (preferences.getUserId() == null && preferences.getUserEmail() == null) {
            return OnboardingLoadResult.Unavailable
        }

        if (!connectivity.isNetworkAvailable()) {
            return cached()?.let { OnboardingLoadResult.Ready(it, offline = true, pendingMutations = queue().size) }
                ?: OnboardingLoadResult.Unavailable
        }

        flushPending()
        return try {
            val remote = api.journey(platform, KNOWN_STEP_TYPES)
            preferences.setAccountOnboardingCache(json.encodeToString(remote))
            OnboardingLoadResult.Ready(remote, offline = false, pendingMutations = queue().size)
        } catch (e: Exception) {
            recordException(e, "OnboardingRepository.load")
            cached()?.let { OnboardingLoadResult.Ready(it, offline = true, pendingMutations = queue().size) }
                ?: OnboardingLoadResult.Unavailable
        }
    }

    suspend fun updateStep(stepKey: String, action: String, value: JsonElement? = null): OnboardingLoadResult {
        val pending = newMutation(kind = "step", stepKey = stepKey, action = action, value = value)
        enqueue(pending)
        updateCachedStep(stepKey, action, value)
        return load()
    }

    suspend fun postpone(): OnboardingLoadResult {
        enqueue(newMutation(kind = "postpone"))
        updateCachedStatus("postponed")
        return load()
    }

    suspend fun replay(): OnboardingLoadResult {
        enqueue(newMutation(kind = "replay"))
        updateCachedStatus("in_progress")
        return load()
    }

    private fun newMutation(
        kind: String,
        stepKey: String? = null,
        action: String? = null,
        value: JsonElement? = null
    ): PendingOnboardingMutation {
        val now = getCurrentTimeMillis()
        return PendingOnboardingMutation(
            kind = kind,
            stepKey = stepKey,
            action = action,
            value = value,
            mutationId = "$platform-$now-$kind-${stepKey.orEmpty()}-${action.orEmpty()}-${queue().size}".take(100),
            clientUpdatedAt = now
        )
    }

    private suspend fun flushPending() {
        val pending = queue().toMutableList()
        while (pending.isNotEmpty()) {
            val mutation = pending.first()
            try {
                when (mutation.kind) {
                    "step" -> api.updateStep(
                        requireNotNull(mutation.stepKey),
                        OnboardingStepMutation(
                            action = requireNotNull(mutation.action),
                            value = mutation.value,
                            client_mutation_id = mutation.mutationId,
                            client_updated_at = mutation.clientUpdatedAt,
                            platform = platform
                        )
                    )
                    "postpone" -> api.postpone(mutation.mutationId)
                    "replay" -> api.replay(mutation.mutationId)
                }
                pending.removeAt(0)
                saveQueue(pending)
            } catch (e: ClientRequestException) {
                // Une mutation rejetée par le contrat ne deviendra pas valide à
                // force de retries. On la retire, journalise, puis laisse le
                // prochain GET restaurer la vérité serveur.
                if (e.response.status.value in 400..499) {
                    pending.removeAt(0)
                    saveQueue(pending)
                    recordException(e, "OnboardingRepository.flushRejected")
                } else {
                    return
                }
            } catch (e: Exception) {
                // Réseau indisponible : la file reste intacte et sera rejouée à
                // la prochaine ouverture/interaction du parcours.
                recordException(e, "OnboardingRepository.flushPending")
                return
            }
        }
    }

    private fun cached(): OnboardingJourneyData? = preferences.getAccountOnboardingCache()
        ?.let { runCatching { json.decodeFromString<OnboardingJourneyData>(it) }.getOrNull() }

    private fun queue(): List<PendingOnboardingMutation> = preferences.getAccountOnboardingQueue()
        ?.let { runCatching { json.decodeFromString<List<PendingOnboardingMutation>>(it) }.getOrNull() }
        ?: emptyList()

    private fun enqueue(mutation: PendingOnboardingMutation) = saveQueue(queue() + mutation)

    private fun saveQueue(queue: List<PendingOnboardingMutation>) {
        preferences.setAccountOnboardingQueue(if (queue.isEmpty()) null else json.encodeToString(queue))
    }

    private fun updateCachedStatus(status: String) {
        val current = cached() ?: return
        preferences.setAccountOnboardingCache(
            json.encodeToString(current.copy(enrollment = current.enrollment?.copy(status = status)))
        )
    }

    private fun updateCachedStep(stepKey: String, action: String, value: JsonElement?) {
        val current = cached() ?: return
        val journey = current.journey ?: return
        val now = "local:${getCurrentTimeMillis()}"
        val steps = journey.steps.map { step ->
            if (step.key != stepKey) return@map step
            val progress = when (action) {
                "view" -> step.progress.copy(viewed_at = step.progress.viewed_at ?: now)
                "skip" -> step.progress.copy(skipped_at = step.progress.skipped_at ?: now)
                "answer" -> OnboardingProgress(
                    viewed_at = step.progress.viewed_at,
                    skipped_at = step.progress.skipped_at,
                    completed_at = step.progress.completed_at ?: now,
                    value = value
                )
                else -> step.progress
            }
            step.copy(progress = progress)
        }
        preferences.setAccountOnboardingCache(json.encodeToString(current.copy(journey = journey.copy(steps = steps))))
    }
}
