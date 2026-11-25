package com.piashcse.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Cheap architecture guard: route files are the HTTP edge and must not reach
 * into the persistence layer. Repository/use-case refactors can move code, but
 * this invariant should stay true.
 */
class LayeringTest {
    private val sourceRoot = File("src/main/kotlin")

    @Test
    fun `route files do not import Exposed`() {
        val offenders =
            sourceRoot.walkTopDown()
                .filter { it.isFile && it.name.endsWith("Routes.kt") }
                .filter { file -> file.readLines().any { it.trimStart().startsWith("import org.jetbrains.exposed") } }
                .map { it.relativeTo(sourceRoot).path }
                .toList()

        assertTrue(offenders.isEmpty(), "Route files must not use Exposed directly: $offenders")
    }

    @Test
    fun `route files stay inside feature packages`() {
        val offenders =
            sourceRoot.walkTopDown()
                .filter { it.isFile && it.name.endsWith("Routes.kt") }
                .filterNot { it.path.contains("/feature/") }
                .map { it.relativeTo(sourceRoot).path }
                .toList()

        assertTrue(offenders.isEmpty(), "Route files must live under feature/: $offenders")
    }

    @Test
    fun `shared service and database layers do not depend on features`() {
        val offenders =
            sourceRoot.walkTopDown()
                .filter { it.isFile && (it.path.contains("/service/") || it.path.contains("/database/")) }
                .filter { file -> file.readLines().any { it.startsWith("import com.piashcse.feature.") } }
                .map { it.relativeTo(sourceRoot).path }
                .toList()

        assertTrue(offenders.isEmpty(), "service/database must not import feature packages: $offenders")
    }

    @Test
    fun `payment does not depend on subscription or refund features`() {
        val offenders =
            sourceRoot.walkTopDown()
                .filter { it.isFile && it.path.contains("/feature/payment/") }
                .filter { file ->
                    file.readLines().any {
                        it.startsWith("import com.piashcse.feature.subscription.") ||
                            it.startsWith("import com.piashcse.feature.refund_request.")
                    }
                }.map { it.relativeTo(sourceRoot).path }
                .toList()

        assertTrue(offenders.isEmpty(), "Payment must use service.payment ports instead: $offenders")
    }
}
