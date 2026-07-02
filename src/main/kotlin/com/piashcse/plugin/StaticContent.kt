package com.piashcse.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*
import java.io.File

/**
 * Configures static file serving for uploaded content.
 *
 * This plugin serves uploaded files (profile images, product images, etc.)
 * from the upload directory at the `/uploads` path.
 *
 * Uploaded files are accessible at:
 * - /uploads/profile-images/{filename}
 * - /uploads/product-images/{filename}
 * - /uploads/shop-images/{filename}
 * - /uploads/refund-images/{filename}
 * - /uploads/category-images/{filename}
 */
fun Application.configureStaticContent() {
    // Get the upload base directory
    val uploadBaseDir = System.getenv("UPLOAD_DIR") ?: "uploads"
    val uploadDir = File(uploadBaseDir).canonicalFile

    if (!uploadDir.exists()) {
        uploadDir.mkdirs()
    }

    routing {
        @OptIn(ExperimentalKtorApi::class)
        staticFiles("/uploads", uploadDir) {
            cacheControl { _ ->
                listOf(
                    CacheControl.MaxAge(
                        maxAgeSeconds = CacheMaxAge.ONE_HOUR,
                        visibility = CacheControl.Visibility.Public,
                        mustRevalidate = true,
                    ),
                )
            }
        }.hide()
    }
}

private object CacheMaxAge {
    const val ONE_HOUR = 3600
}
