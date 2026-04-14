package com.piashcse.service

import com.piashcse.utils.ValidationException
import io.ktor.http.content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Industry-standard file upload service with security and validation.
 *
 * Security features:
 * - Configurable upload directories via UPLOAD_DIR env var
 * - File size limits to prevent DoS attacks
 * - MIME type + extension double validation
 * - Filename sanitization and path traversal prevention
 * - Secure UUID-based filename generation
 * - Canonical path verification
 * - Automatic directory initialization
 */
object UploadService {

    // Upload configuration
    private val uploadBaseDir = System.getenv("UPLOAD_DIR") ?: "uploads"
    
    // Upload directories
    private const val PROFILE_DIR = "profile-images"
    private const val PRODUCT_DIR = "product-images"
    private const val SHOP_DIR = "shop-images"
    private const val REFUND_DIR = "refund-images"
    private const val CATEGORY_DIR = "category-images"
    
    // File size limits
    private const val MAX_PROFILE_SIZE = 5L * 1024 * 1024      // 5 MB
    private const val MAX_PRODUCT_SIZE = 10L * 1024 * 1024     // 10 MB
    private const val MAX_SHOP_SIZE = 10L * 1024 * 1024        // 10 MB
    private const val MAX_REFUND_SIZE = 10L * 1024 * 1024      // 10 MB
    private const val MAX_CATEGORY_SIZE = 5L * 1024 * 1024     // 5 MB
    
    // Allowed types
    private val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
    private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")

    init {
        // Create all upload directories on startup
        listOf(PROFILE_DIR, PRODUCT_DIR, SHOP_DIR, REFUND_DIR, CATEGORY_DIR)
            .forEach { ensureDirectoryExists(it) }
    }

    /**
     * Uploads a profile image (5MB limit).
     */
    suspend fun uploadProfileImage(file: PartData.FileItem): String =
        upload(file, PROFILE_DIR, MAX_PROFILE_SIZE, "profile image")

    /**
     * Uploads a product image (10MB limit).
     */
    suspend fun uploadProductImage(file: PartData.FileItem): String =
        upload(file, PRODUCT_DIR, MAX_PRODUCT_SIZE, "product image")

    /**
     * Uploads a shop image (10MB limit).
     */
    suspend fun uploadShopImage(file: PartData.FileItem): String =
        upload(file, SHOP_DIR, MAX_SHOP_SIZE, "shop image")

    /**
     * Uploads a refund evidence image (10MB limit).
     */
    suspend fun uploadRefundImage(file: PartData.FileItem): String =
        upload(file, REFUND_DIR, MAX_REFUND_SIZE, "refund image")

    /**
     * Uploads a category image (5MB limit).
     */
    suspend fun uploadCategoryImage(file: PartData.FileItem): String =
        upload(file, CATEGORY_DIR, MAX_CATEGORY_SIZE, "category image")

    /**
     * Core upload logic with validation and security.
     */
    private suspend fun upload(
        file: PartData.FileItem,
        directory: String,
        maxSize: Long,
        purpose: String
    ): String = withContext(Dispatchers.IO) {
        // Validate filename
        val originalName = file.originalFileName
            ?: throw ValidationException("File name is required for $purpose upload")

        // Extract and validate extension
        val extension = originalName.substringAfterLast('.', "").lowercase()
        if (extension !in ALLOWED_EXTENSIONS) {
            throw ValidationException("Invalid file type for $purpose. Allowed: ${ALLOWED_EXTENSIONS.joinToString(", ")}")
        }

        // Validate MIME type (if present)
        file.contentType?.toString()?.lowercase()?.let { mimeType ->
            if (mimeType !in ALLOWED_MIME_TYPES) {
                throw ValidationException("Invalid MIME type for $purpose: $mimeType")
            }
        }

        // Read file bytes
        val bytes = file.streamProvider().readBytes()
        
        // Validate file size
        require(bytes.isNotEmpty()) { "Uploaded file is empty" }
        if (bytes.size > maxSize) {
            val maxSizeMB = maxSize / (1024 * 1024)
            throw ValidationException("File size exceeds ${maxSizeMB}MB limit for $purpose upload")
        }

        // Generate secure filename
        val fileName = "${UUID.randomUUID()}.$extension"

        // Validate path and write file
        val targetDir = getDirectory(directory)
        val targetFile = File(targetDir, fileName)
        
        require(targetFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
            "Invalid file path detected"
        }
        
        targetFile.writeBytes(bytes)
        fileName
    }

    /**
     * Deletes a file from the specified upload directory.
     */
    fun delete(directory: String, fileName: String?): Boolean {
        if (fileName.isNullOrBlank()) return false
        
        return try {
            val targetDir = getDirectory(directory)
            val targetFile = File(targetDir, fileName)
            
            // Verify path is within upload directory
            if (!targetFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                return false
            }
            
            targetFile.delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deletes a profile image.
     */
    fun deleteProfileImage(fileName: String?): Boolean = delete(PROFILE_DIR, fileName)

    /**
     * Deletes a product image.
     */
    fun deleteProductImage(fileName: String?): Boolean = delete(PRODUCT_DIR, fileName)

    /**
     * Gets the public URL for an uploaded file.
     */
    fun getPublicUrl(directory: String, fileName: String): String =
        "/uploads/$directory/$fileName"

    /**
     * Gets URL helpers for different file types.
     */
    fun getProfileImageUrl(fileName: String) = getPublicUrl(PROFILE_DIR, fileName)
    fun getProductImageUrl(fileName: String) = getPublicUrl(PRODUCT_DIR, fileName)
    fun getShopImageUrl(fileName: String) = getPublicUrl(SHOP_DIR, fileName)
    fun getRefundImageUrl(fileName: String) = getPublicUrl(REFUND_DIR, fileName)
    fun getCategoryImageUrl(fileName: String) = getPublicUrl(CATEGORY_DIR, fileName)

    /**
     * Gets the upload directory with path traversal prevention.
     */
    private fun getDirectory(subDirectory: String): File {
        val base = File(uploadBaseDir, subDirectory).canonicalFile
        val root = File(uploadBaseDir).canonicalFile
        
        require(base.canonicalPath.startsWith(root.canonicalPath)) {
            "Invalid upload directory configuration"
        }
        
        return base
    }

    /**
     * Ensures an upload directory exists.
     */
    private fun ensureDirectoryExists(subDirectory: String) {
        val dir = getDirectory(subDirectory)
        if (!dir.exists()) dir.mkdirs()
    }
}

