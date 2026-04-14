package com.piashcse.plugin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.hide
import io.ktor.utils.io.ExperimentalKtorApi
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

    // Ensure the upload directory exists
    if (!uploadDir.exists()) {
        uploadDir.mkdirs()
    }

    routing {
        // Serve uploaded files from upload directory
        // Static file serving is not auto-documented by OpenAPI
        @OptIn(ExperimentalKtorApi::class)
        staticFiles("/uploads", uploadDir) {
            // Enable caching for better performance
            cacheControl { _ ->
                listOf(CacheControl.MaxAge(
                    maxAgeSeconds = 3600, // 1 hour
                    visibility = CacheControl.Visibility.Public,
                    mustRevalidate = true
                ))
            }
        }.hide()
    }
}
