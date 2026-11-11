package com.piashcse.feature.platform

import com.piashcse.RouteTestHelper.authHeader
import com.piashcse.RouteTestHelper.installTestInfra
import com.piashcse.constants.CmsPageStatus
import com.piashcse.constants.SeoEntityType
import com.piashcse.feature.cms.CmsPageResponse
import com.piashcse.feature.cms.CmsService
import com.piashcse.feature.cms.SeoMetadataResponse
import com.piashcse.feature.cms.cmsAdminRoutes
import com.piashcse.feature.cms.cmsRoutes
import com.piashcse.feature.cms.seoAdminRoutes
import com.piashcse.feature.cms.seoRoutes
import com.piashcse.feature.cms.seoSiteRoutes
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

class CmsRoutesTest {
    private val service: CmsService = mockk()

    private fun page() =
        CmsPageResponse(
            "page-1",
            "about-us",
            "About us",
            null,
            "<p>Hello</p>",
            CmsPageStatus.PUBLISHED,
            "About",
            "About page",
            null,
            null,
            null,
            null,
            null,
        )

    private fun seo() = SeoMetadataResponse(SeoEntityType.PAGE, "about-us", "en", "About", "About page", null, null, null, false)

    private fun ApplicationTestBuilder.setup() {
        application {
            installTestInfra(module { single { service } })
            routing {
                cmsRoutesAt()
            }
        }
    }

    private fun Routing.cmsRoutesAt() {
        route("/api/v1/cms") { cmsRoutes() }
        route("/api/v1/seo") { seoRoutes() }
        route("/api/v1/admin/cms/pages") { adminAuth { cmsAdminRoutes() } }
        route("/api/v1/admin/seo") { adminAuth { seoAdminRoutes() } }
        seoSiteRoutes()
    }

    @Test
    fun `published page is public`() =
        testApplication {
            coEvery { service.publicPage("about-us") } returns page()
            setup()
            val res = client.get("/api/v1/cms/pages/about-us")
            assertEquals(HttpStatusCode.OK, res.status)
            assertContains(res.bodyAsText(), "About us")
        }

    @Test
    fun `admin creates a page`() =
        testApplication {
            coEvery { service.create(any()) } returns page()
            setup()
            val res =
                client.post("/api/v1/admin/cms/pages") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"slug":"about-us","title":"About us","body":"<p>Hello</p>","status":"PUBLISHED"}""")
                }
            assertEquals(HttpStatusCode.Created, res.status)
        }

    @Test
    fun `seo metadata is public and admin writable`() =
        testApplication {
            coEvery { service.seo(SeoEntityType.PAGE, "about-us", "en") } returns seo()
            coEvery { service.upsertSeo(any()) } returns seo()
            setup()
            val public =
                client.get("/api/v1/seo/metadata?entityType=PAGE&entityId=about-us")
            assertEquals(HttpStatusCode.OK, public.status)
            val admin =
                client.put("/api/v1/admin/seo/metadata") {
                    header(HttpHeaders.Authorization, authHeader(userType = "ADMIN"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"entityType":"PAGE","entityId":"about-us","title":"About"}""")
                }
            assertEquals(HttpStatusCode.OK, admin.status)
        }

    @Test
    fun `sitemap and robots are served at the root`() =
        testApplication {
            coEvery { service.sitemap(any()) } returns "<?xml version=\"1.0\"?><urlset/>"
            coEvery { service.robots(any()) } returns "User-agent: *\nAllow: /\n"
            setup()
            val sitemap = client.get("/sitemap.xml")
            assertEquals(HttpStatusCode.OK, sitemap.status)
            assertContains(sitemap.bodyAsText(), "urlset")
            val robots = client.get("/robots.txt")
            assertEquals(HttpStatusCode.OK, robots.status)
            assertContains(robots.bodyAsText(), "User-agent")
        }

    @Test
    fun `customers cannot manage cms`() =
        testApplication {
            setup()
            val res =
                client.post("/api/v1/admin/cms/pages") {
                    header(HttpHeaders.Authorization, authHeader(userType = "CUSTOMER"))
                    contentType(ContentType.Application.Json)
                    setBody("""{"slug":"about","title":"About","body":"x"}""")
                }
            assertEquals(HttpStatusCode.Forbidden, res.status)
        }
}
