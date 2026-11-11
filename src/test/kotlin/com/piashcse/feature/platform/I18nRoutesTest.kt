package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.i18n.I18nService
import com.piashcse.feature.i18n.MessagesResponse
import com.piashcse.feature.i18n.ProductTranslationResponse
import com.piashcse.feature.i18n.ProductTranslationService
import com.piashcse.feature.i18n.i18nRoutes
import com.piashcse.feature.i18n.productTranslationAdminRoutes
import com.piashcse.plugin.adminAuth
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class I18nRoutesTest {
    private val i18n: I18nService = mockk()
    private val translations: ProductTranslationService = mockk()

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(
                module {
                    single { i18n }
                    single { translations }
                },
            )
            routing {
                route("/api/v1/i18n") { i18nRoutes() }
                route("/api/v1/admin/products") { adminAuth { productTranslationAdminRoutes() } }
            }
        }
    }

    @Test
    fun `locales are public`() =
        testApplication {
            coEvery { i18n.locales() } returns
                listOf(com.piashcse.feature.i18n.LocaleResponse("en", "English", "English", "USD"))
            setup()
            val res = client.get("/api/v1/i18n/locales")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "English")
        }

    @Test
    fun `message catalog is locale aware`() =
        testApplication {
            coEvery { i18n.messages("es") } returns
                MessagesResponse("es", mapOf("product_not_found" to "Producto no encontrado"))
            setup()
            val res = client.get("/api/v1/i18n/messages?locale=es")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Producto no encontrado")
        }

    @Test
    fun `admin saves a product translation`() =
        testApplication {
            coEvery { translations.upsert("p-1", "fr", any()) } returns
                ProductTranslationResponse("p-1", "fr", "Chaise", "Belle chaise")
            setup()
            val res =
                client.put("/api/v1/admin/products/p-1/translations/fr") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Chaise","description":"Belle chaise"}""")
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "Chaise")
        }

    @Test
    fun `deleting a missing translation returns 404`() =
        testApplication {
            coEvery { translations.delete("p-1", "fr") } returns false
            setup()
            val res =
                client.delete("/api/v1/admin/products/p-1/translations/fr") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.NotFound, res.status)
        }

    @Test
    fun `customers cannot edit translations`() =
        testApplication {
            setup()
            val res =
                client.put("/api/v1/admin/products/p-1/translations/fr") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"name":"Chaise","description":"Belle chaise"}""")
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }
}
