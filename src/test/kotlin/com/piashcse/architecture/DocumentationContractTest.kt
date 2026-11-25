package com.piashcse.architecture

import com.piashcse.plugin.configureAuth
import com.piashcse.plugin.configureRateLimiting
import com.piashcse.plugin.configureRoute
import com.piashcse.plugin.configureSwagger
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Fails when a path documented under `docs/` no longer exists in the runtime
 * OpenAPI document. Parameter names are normalized, so `{id}` and `{productId}`
 * are treated as the same shape.
 */
class DocumentationContractTest {
    private val specPath = Regex("(?m)^ {2}(/\\S+):\\s*$")
    private val docPath = Regex("(?<![\\w`])/(?:api/v1|health|metrics|sitemap\\.xml|robots\\.txt)[A-Za-z0-9_.~{}:/?-]*")
    private val templateParam = Regex("\\{[^}/]+}")

    // Metrics is deliberately hidden from OpenAPI; the notification example uses a
    // concrete event type where the spec has a {eventType} template.
    private val allowlisted = setOf("/metrics")

    @Test
    fun `documented paths exist in the generated OpenAPI document`() {
        val spec = runBlockingSpec()
        val documented = documentedPaths()
        val specPaths = specPath.findAll(spec).map { it.groupValues[1] }.toSet()
        val offenders = documented.filterNot { matches(it, specPaths) }.sorted()

        assertTrue(
            offenders.isEmpty(),
            "Documented paths missing from OpenAPI: $offenders",
        )
    }

    private fun runBlockingSpec(): String {
        lateinit var spec: String
        kotlinx.coroutines.runBlocking {
            testApplication {
                application {
                    configureAuth()
                    configureRateLimiting()
                    configureSwagger()
                    configureRoute()
                }
                spec = client.get("/swagger/documentation.yaml").bodyAsText()
            }
        }
        return spec
    }

    private fun documentedPaths(): Set<String> =
        File("docs").walkTopDown()
            .filter { it.isFile && it.extension == "md" }
            .flatMap { file ->
                docPath.findAll(file.readText())
                    .map { it.value.split('?').first().split('#').first().trimEnd('.', ',', ';', ':', ')', ']', '`') }
            }.filter { it.length > "/api/v1".length }
            .toSet()

    private fun matches(
        documented: String,
        specPaths: Set<String>,
    ): Boolean {
        if (documented in allowlisted) return true
        if (normalize(documented) in specPaths.map(::normalize)) return true
        val candidate = normalize(documented)
        return specPaths.any { template ->
            val shape = normalize(template)
            if (!shape.contains("{}")) return@any false
            val regex = Regex("^" + shape.split("{}").joinToString("[^/]+") { Regex.escape(it) } + "$")
            regex.matches(candidate)
        }
    }

    private fun normalize(path: String): String = templateParam.replace(path, "{}").trimEnd('/')
}
