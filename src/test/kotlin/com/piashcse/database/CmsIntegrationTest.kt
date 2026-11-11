package com.piashcse.database

import com.piashcse.constants.CmsPageStatus
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.SeoEntityType
import com.piashcse.constants.UserType
import com.piashcse.database.entities.ProductCategoryDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.feature.cms.CmsPageRequest
import com.piashcse.feature.cms.CmsService
import com.piashcse.feature.cms.SeoMetadataRequest
import com.piashcse.utils.validator.NotFoundException
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Assume
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** CMS pages, SEO metadata, sitemap and robots against PostgreSQL. */
class CmsIntegrationTest {
    private fun envDb(): Triple<String, String, String>? {
        val host = System.getenv("DB_HOST")?.takeIf { it.isNotBlank() } ?: return null
        val port = System.getenv("DB_PORT") ?: "5432"
        val name = System.getenv("DB_NAME") ?: "ktor-ecommerce"
        val user = System.getenv("DB_USER") ?: "postgres"
        val password = System.getenv("DB_PASSWORD") ?: "postgres"
        return Triple("jdbc:postgresql://$host:$port/$name", user, password)
    }

    private fun dockerAvailable(): Boolean =
        runCatching {
            ProcessBuilder("docker", "info").redirectErrorStream(true).start().waitFor() == 0
        }.getOrDefault(false)

    @Test
    fun `cms pages and sitemap persist`() {
        val external = envDb()
        if (external != null) {
            runCmsFlow(external.first, external.second, external.third)
            return
        }
        if (System.getenv("CI") != null) {
            assertTrue(dockerAvailable(), "DB_* environment or Docker is required in CI")
        } else {
            Assume.assumeTrue("Docker daemon or DB_* environment required", dockerAvailable())
        }
        val pg = PostgreSQLContainer("postgres:16-alpine")
        pg.start()
        try {
            runCmsFlow(pg.jdbcUrl, pg.username, pg.password)
        } finally {
            pg.stop()
        }
    }

    private fun runCmsFlow(
        jdbcUrl: String,
        user: String,
        dbPassword: String,
    ) {
        Flyway.configure()
            .dataSource(jdbcUrl, user, dbPassword)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        Database.connect(jdbcUrl, user = user, password = dbPassword)

        val suffix = UUID.randomUUID().toString().take(8)
        lateinit var productId: String

        transaction {
            val seller =
                UserDAO.new {
                    email = "cms-seller-$suffix@example.com"
                    userType = UserType.SELLER
                    password = "hashed"
                    isVerified = true
                }
            val category = ProductCategoryDAO.new { name = "CMS Cat $suffix" }
            productId =
                ProductDAO.new {
                    userId = seller.id
                    categoryId = category.id
                    name = "CMS Product $suffix"
                    description = "Sitemap product"
                    sku = "CMS-$suffix"
                    price = BigDecimal("10.00")
                    status = ProductStatus.ACTIVE
                }.id.value
        }

        val cms = CmsService()
        val published =
            kotlinx.coroutines.runBlocking {
                cms.create(
                    CmsPageRequest(
                        slug = "about-$suffix",
                        title = "About Us",
                        excerpt = "About",
                        body = "<p>Hello</p>",
                        status = CmsPageStatus.PUBLISHED,
                        seoTitle = "About Us",
                    ),
                )
            }
        assertEquals(CmsPageStatus.PUBLISHED, published.status)
        assertTrue(published.publishedAt != null)

        val draft =
            kotlinx.coroutines.runBlocking {
                cms.create(
                    CmsPageRequest(
                        slug = "draft-$suffix",
                        title = "Draft",
                        body = "Draft body",
                        status = CmsPageStatus.DRAFT,
                    ),
                )
            }
        assertEquals(CmsPageStatus.DRAFT, draft.status)
        assertEquals(1, kotlinx.coroutines.runBlocking { cms.publicPages(10, 0) }.data.size)
        assertEquals("About Us", kotlinx.coroutines.runBlocking { cms.publicPage("about-$suffix") }.title)
        assertFailsWith<NotFoundException> {
            kotlinx.coroutines.runBlocking { cms.publicPage("draft-$suffix") }
        }

        val seo =
            kotlinx.coroutines.runBlocking {
                cms.upsertSeo(
                    SeoMetadataRequest(
                        entityType = SeoEntityType.PAGE,
                        entityId = "about-$suffix",
                        title = "About Us",
                        description = "About page",
                        keywords = "about,company",
                    ),
                )
            }
        assertEquals("About Us", seo.title)
        val fetched =
            kotlinx.coroutines.runBlocking {
                cms.seo(SeoEntityType.PAGE, "about-$suffix", "en")
            }
        assertEquals("About page", fetched.description)

        val sitemap = kotlinx.coroutines.runBlocking { cms.sitemap("https://shop.example") }
        assertTrue(sitemap.contains("https://shop.example/products/$productId"))
        assertTrue(sitemap.contains("https://shop.example/pages/about-$suffix"))
        assertTrue(!sitemap.contains("draft-$suffix"))
        val robots = kotlinx.coroutines.runBlocking { cms.robots("https://shop.example") }
        assertTrue(robots.contains("Sitemap: https://shop.example/sitemap.xml"))

        val archived = kotlinx.coroutines.runBlocking { cms.archive(published.id) }
        assertTrue(archived)
        assertEquals(0, kotlinx.coroutines.runBlocking { cms.publicPages(10, 0) }.data.size)
    }
}
