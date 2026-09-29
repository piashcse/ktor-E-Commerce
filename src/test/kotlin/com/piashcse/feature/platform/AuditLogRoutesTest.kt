package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.feature.audit_log.AuditLogRepository
import com.piashcse.feature.audit_log.auditLogAdminRoutes
import com.piashcse.model.response.AuditLogResponse
import com.piashcse.plugin.adminAuth
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

class AuditLogRoutesTest {
    private val repo: AuditLogRepository = mockk()

    private fun sample() =
        AuditLogResponse(
            id = "a-1",
            actorId = "admin-1",
            actorEmail = "admin@example.com",
            actorRole = "ADMIN",
            action = "CREATE",
            resourceType = "COUPON",
            resourceId = "c-1",
            details = null,
            ipAddress = null,
            userAgent = null,
            outcome = "SUCCESS",
            executedAt = "2024-01-01T00:00:00",
            createdAt = "2024-01-01T00:00:00",
        )

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single<AuditLogRepository> { repo } })
            routing { route("/api/v1/admin/audit-logs") { adminAuth { auditLogAdminRoutes() } } }
        }
    }

    @Test
    fun `list happy path returns 200`() =
        testApplication {
            coEvery { repo.getAuditLogs(any(), any(), any(), any(), any(), any(), any()) } returns
                PaginatedResponse(listOf(sample()), PaginationMetadata(1, 10, 0))
            setup()
            val res =
                client.get("/api/v1/admin/audit-logs") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "a-1")
        }

    @Test
    fun `list without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/admin/audit-logs")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }

    @Test
    fun `list with customer token returns 403`() =
        testApplication {
            setup()
            val res =
                client.get("/api/v1/admin/audit-logs") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }

    @Test
    fun `get by id happy path returns 200`() =
        testApplication {
            coEvery { repo.getAuditLogById("a-1") } returns sample()
            setup()
            val res =
                client.get("/api/v1/admin/audit-logs/a-1") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                }
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "CREATE")
        }

    @Test
    fun `get by id without token returns 401`() =
        testApplication {
            setup()
            val res = client.get("/api/v1/admin/audit-logs/a-1")
            assertEquals(HttpStatusCode.Unauthorized, res.status)
        }
}
