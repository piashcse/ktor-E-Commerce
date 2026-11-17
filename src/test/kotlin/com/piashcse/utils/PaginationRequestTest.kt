package com.piashcse.utils

import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.utils.extension.cursorRequest
import com.piashcse.utils.extension.pageRequest
import com.piashcse.utils.extension.queryLimit
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

class PaginationRequestTest {
    private fun io.ktor.server.testing.ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { })
            routing {
                get("/page") {
                    val page = call.pageRequest()
                    call.respondText("${page.limit},${page.offset}")
                }
                get("/cursor") {
                    val cursor = call.cursorRequest()
                    call.respondText("${cursor.limit},${cursor.cursor.orEmpty()}")
                }
                get("/limit") {
                    call.respondText(call.queryLimit(default = 10, max = 50).toString())
                }
            }
        }
    }

    @Test
    fun `defaults to the platform page size`() =
        testApplication {
            setup()
            assertEquals("20,0", client.get("/page").bodyAsText())
        }

    @Test
    fun `page and perPage map to limit and offset`() =
        testApplication {
            setup()
            assertEquals("10,20", client.get("/page?page=3&perPage=10").bodyAsText())
        }

    @Test
    fun `legacy limit and offset still work`() =
        testApplication {
            setup()
            assertEquals("5,7", client.get("/page?limit=5&offset=7").bodyAsText())
        }

    @Test
    fun `page params win over legacy params`() =
        testApplication {
            setup()
            assertEquals("10,10", client.get("/page?page=2&perPage=10&limit=99&offset=99").bodyAsText())
        }

    @Test
    fun `values are clamped to safe bounds`() =
        testApplication {
            setup()
            assertEquals("100,10000", client.get("/page?perPage=999&page=9999").bodyAsText())
            assertEquals("1,0", client.get("/page?page=0&perPage=0").bodyAsText())
            assertEquals("100,10000", client.get("/page?perPage=100&page=2147483647").bodyAsText())
            assertEquals("10,10000", client.get("/page?limit=10&offset=999999").bodyAsText())
        }

    @Test
    fun `malformed pagination values are client errors`() =
        testApplication {
            setup()
            assertEquals(HttpStatusCode.BadRequest, client.get("/page?perPage=abc").status)
            assertEquals(HttpStatusCode.BadRequest, client.get("/page?limit=abc").status)
        }

    @Test
    fun `cursor request carries limit and opaque cursor`() =
        testApplication {
            setup()
            assertEquals("25,abc", client.get("/cursor?limit=25&cursor=abc").bodyAsText())
            assertEquals("20,", client.get("/cursor?cursor=  ").bodyAsText())
        }

    @Test
    fun `query limit is clamped and validated`() =
        testApplication {
            setup()
            assertEquals("10", client.get("/limit").bodyAsText())
            assertEquals("50", client.get("/limit?limit=999").bodyAsText())
            assertEquals(HttpStatusCode.BadRequest, client.get("/limit?limit=abc").status)
        }
}
