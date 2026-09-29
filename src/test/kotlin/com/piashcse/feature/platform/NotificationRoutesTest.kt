package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.notification.NotificationRepository
import com.piashcse.feature.notification.NotificationResponse
import com.piashcse.feature.notification.UnreadCountResponse
import com.piashcse.feature.notification.notificationRoutes
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
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

class NotificationRoutesTest {
    private val repo: NotificationRepository = mockk()

    private fun sample() =
        NotificationResponse(
            id = "n-1",
            channel = "IN_APP",
            type = "ORDER",
            title = "Order update",
            body = "Your order shipped",
            resourceType = "ORDER",
            resourceId = "o-1",
            isRead = false,
            createdAt = "2024-01-01T00:00:00",
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single<NotificationRepository> { repo } })
            routing { route("/api/v1/notifications") { notificationRoutes() } }
        }
    }

    @Test
    fun `list happy path returns 200`() =
        testApplication {
            coEvery { repo.list(any(), any(), any(), any()) } returns
                PaginatedResponse(listOf(sample()), PaginationMetadata(1, 10, 0))
            setup()
            val res =
                client.get("/api/v1/notifications") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "n-1")
        }

    @Test
    fun `list without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/notifications")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `unread-count happy path returns 200`() =
        testApplication {
            coEvery { repo.unreadCount(any()) } returns 3L
            setup()
            val res =
                client.get("/api/v1/notifications/unread-count") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "3")
        }

    @Test
    fun `unread-count without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/notifications/unread-count")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `mark-read happy path returns 200`() =
        testApplication {
            coEvery { repo.markRead(any(), "n-1") } returns sample().copy(isRead = true)
            setup()
            val res =
                client.post("/api/v1/notifications/n-1/read") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "n-1")
        }

    @Test
    fun `mark-read without token returns 401`() =
        testApplication {
            setup()
            val res = client.post("/api/v1/notifications/n-1/read")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `read-all happy path returns 200`() =
        testApplication {
            coEvery { repo.markAllRead(any()) } returns 5L
            setup()
            val res =
                client.post("/api/v1/notifications/read-all") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "5")
        }

    @Test
    fun `read-all without token returns 401`() =
        testApplication {
            setup()
            val res = client.post("/api/v1/notifications/read-all")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    // Keeps UnreadCountResponse constructor covered (route wraps the raw Long).
    @Test
    fun `unread count response shape`() {
        assertEquals(3L, UnreadCountResponse(3L).unreadCount)
    }
}
