package com.mibeko.mibeko.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Le client HTTP de l'app n'active pas `expectSuccess` : sans garde, un 403
 * de l'export (compte non Pro, mibeko-dashboard#86) revenait comme un tableau
 * d'octets — le corps JSON de l'erreur — que l'app partageait sous le nom
 * `Article_N.pdf`. Le message « réservé aux comptes Pro » ne s'affichait jamais.
 */
class LegalApiServiceDownloadTest {

    private fun service(status: HttpStatusCode, body: String): LegalApiService {
        val engine = MockEngine {
            respond(body, status = status, headers = headersOf("Content-Type", "application/json"))
        }
        return LegalApiService(HttpClient(engine) { install(HttpTimeout) }, "https://api.test/api")
    }

    @Test
    fun `un refus 403 leve une erreur au lieu de renvoyer le corps comme un fichier`() = runTest {
        val error = assertFailsWith<ClientRequestException> {
            service(HttpStatusCode.Forbidden, """{"success":false,"message":"Forbidden"}""")
                .downloadFile("https://api.test/api/v1/articles/a1/export")
        }
        assertEquals(403, error.response.status.value)
    }

    @Test
    fun `une reponse 200 renvoie les octets du fichier`() = runTest {
        val bytes = service(HttpStatusCode.OK, "%PDF-1.4").downloadFile("https://api.test/api/v1/articles/a1/export")
        assertEquals("%PDF-1.4", bytes.decodeToString())
    }
}
