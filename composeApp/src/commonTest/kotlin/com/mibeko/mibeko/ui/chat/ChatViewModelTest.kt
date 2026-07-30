package com.mibeko.mibeko.ui.chat

import com.mibeko.mibeko.data.remote.AgentConversation
import com.mibeko.mibeko.data.remote.AiChatApi
import com.mibeko.mibeko.data.remote.AiChatReference
import com.mibeko.mibeko.data.remote.AiMode
import com.mibeko.mibeko.data.remote.AiStreamEvent
import com.mibeko.mibeko.data.remote.AssistantReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Faux AiChatApi dont le flux de streaming est scripté par le test. */
private class FakeAiChatApi(
    var streamFactory: () -> Flow<AiStreamEvent> = { flow {} }
) : AiChatApi {
    var streamCallCount = 0

    override suspend fun getConversationDetails(id: String): AgentConversation =
        AgentConversation(id = id)

    override suspend fun searchReferences(query: String?): List<AssistantReference> = emptyList()

    override suspend fun sendMessageStream(
        message: String,
        conversationId: String?,
        mode: AiMode,
        references: List<AiChatReference>,
        onConversationIdReceived: (String) -> Unit
    ): Flow<AiStreamEvent> {
        streamCallCount++
        return streamFactory()
    }
}

class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- classifyChatFailure : classification pure du transport ---

    @Test
    fun `429 est classe quota avec Retry-After`() {
        val error = classifyChatFailure(statusCode = 429, retryAfterHeader = "120")
        assertEquals(ChatErrorKind.QUOTA, error.kind)
        assertEquals(120, error.retryAfterSeconds)
    }

    @Test
    fun `absence de reponse est classee reseau`() {
        val error = classifyChatFailure(statusCode = null, retryAfterHeader = null)
        assertEquals(ChatErrorKind.NETWORK, error.kind)
        assertNull(error.retryAfterSeconds)
    }

    @Test
    fun `statut 500 est classe generique`() {
        assertEquals(ChatErrorKind.GENERIC, classifyChatFailure(500, null).kind)
    }

    @Test
    fun `Retry-After illisible est ignore`() {
        assertNull(classifyChatFailure(429, "mercredi").retryAfterSeconds)
    }

    // --- Échec d'envoi : bannière + retry ---

    @Test
    fun `echec total retire la bulle IA vide et propose un retry`() = runTest(dispatcher) {
        val api = FakeAiChatApi(streamFactory = { flow { throw RuntimeException("boom") } })
        val viewModel = ChatViewModel(api)

        viewModel.sendMessage("Quelle est la duree du preavis ?")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.chatState.value as ChatState.Content
        // La bulle utilisateur reste, la bulle IA vide a été retirée.
        assertEquals(1, state.messages.size)
        assertEquals("user", state.messages[0].role)
        assertEquals(ChatErrorKind.NETWORK, state.inlineError?.kind)
        assertTrue(state.inlineError?.canRetry == true)
        assertFalse(state.isTyping)
    }

    @Test
    fun `retry renvoie le message sans doubler la bulle utilisateur`() = runTest(dispatcher) {
        val api = FakeAiChatApi(streamFactory = { flow { throw RuntimeException("boom") } })
        val viewModel = ChatViewModel(api)

        viewModel.sendMessage("Ma question")
        dispatcher.scheduler.advanceUntilIdle()

        // Le second envoi réussit.
        api.streamFactory = { flow { emit(AiStreamEvent.Delta("Réponse")) } }
        viewModel.retryLastFailedMessage()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.chatState.value as ChatState.Content
        assertEquals(2, api.streamCallCount)
        assertNull(state.inlineError)
        assertEquals(listOf("user", "assistant"), state.messages.map { it.role })
        assertEquals("Ma question", state.messages[0].content)
        assertEquals("Réponse", state.messages[1].content)
    }

    @Test
    fun `retry sans echec prealable ne fait rien`() = runTest(dispatcher) {
        val api = FakeAiChatApi()
        val viewModel = ChatViewModel(api)

        viewModel.retryLastFailedMessage()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, api.streamCallCount)
    }

    // --- Événement SSE error : libellé local, jamais le texte serveur ---

    @Test
    fun `event error quota affiche le libelle local sans mention d abonnement`() = runTest(dispatcher) {
        val api = FakeAiChatApi(streamFactory = {
            flow { emit(AiStreamEvent.Error(code = "AI_RATE_LIMITED")) }
        })
        val viewModel = ChatViewModel(api)

        viewModel.sendMessage("Question")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.chatState.value as ChatState.Content
        val aiContent = state.messages.last().content
        assertTrue(aiContent.contains("Limite temporaire de requêtes atteinte"), aiContent)
        assertFalse(aiContent.contains("abonnement", ignoreCase = true), aiContent)
    }

    @Test
    fun `event error inconnu affiche le libelle generique`() = runTest(dispatcher) {
        val api = FakeAiChatApi(streamFactory = {
            flow { emit(AiStreamEvent.Error(code = "AUTRE_CODE")) }
        })
        val viewModel = ChatViewModel(api)

        viewModel.sendMessage("Question")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.chatState.value as ChatState.Content
        assertTrue(state.messages.last().content.contains("L'assistant a rencontré une erreur"))
    }

    @Test
    fun `flux nominal accumule les deltas et les sources`() = runTest(dispatcher) {
        val api = FakeAiChatApi(streamFactory = {
            flow {
                emit(AiStreamEvent.Status("Recherche"))
                emit(AiStreamEvent.Delta("Le préavis "))
                emit(AiStreamEvent.Delta("est de trois mois."))
            }
        })
        val viewModel = ChatViewModel(api)

        viewModel.sendMessage("Question")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.chatState.value as ChatState.Content
        assertEquals("Le préavis est de trois mois.", state.messages.last().content)
        assertFalse(state.isTyping)
        assertNull(state.inlineError)
    }
}
