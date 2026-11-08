package com.piashcse.plugin

import com.piashcse.config.DotEnvConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*
import java.io.File

/**
 * Serves public catalog/profile imagery. Refund evidence is deliberately NOT
 * mounted here: it is private and served through an authorization-checked route
 * in the refund module.
 */
fun Application.configureStaticContent() {
    val uploadBaseDir = DotEnvConfig.uploadDir
    val uploadDir = File(uploadBaseDir).canonicalFile
    if (!uploadDir.exists()) uploadDir.mkdirs()

    val publicDirectories = listOf("profile-images", "product-images", "shop-images", "category-images", "brand-images")

    routing {
        publicDirectories.forEach { directory ->
            val dir = File(uploadDir, directory).canonicalFile
            if (!dir.exists()) dir.mkdirs()
            @OptIn(ExperimentalKtorApi::class)
            staticFiles("/uploads/$directory", dir) {
                cacheControl { _ ->
                    listOf(
                        CacheControl.MaxAge(
                            maxAgeSeconds = 3600,
                            visibility = CacheControl.Visibility.Public,
                            mustRevalidate = true,
                        ),
                    )
                }
            }.hide()
        }
    }
}
