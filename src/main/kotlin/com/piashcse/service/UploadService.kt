package com.piashcse.service

import com.piashcse.config.DotEnvConfig
import com.piashcse.constants.Message
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

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
    private val uploadBaseDir = DotEnvConfig.uploadDir

    // Upload directories
    private const val PROFILE_DIR = "profile-images"
    private const val PRODUCT_DIR = "product-images"
    private const val SHOP_DIR = "shop-images"
    private const val REFUND_DIR = "refund-images"
    private const val CATEGORY_DIR = "category-images"

    // File size limit (applies to all image types)
    private const val MAX_FILE_SIZE = 5L * 1024 * 1024 // 5 MB

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
    suspend fun uploadProfileImage(file: PartData.FileItem): String = upload(file, PROFILE_DIR, MAX_FILE_SIZE, "profile image")

    /**
     * Uploads a product image (5MB limit).
     */
    suspend fun uploadProductImage(file: PartData.FileItem): String = upload(file, PRODUCT_DIR, MAX_FILE_SIZE, "product image")

    /**
     * Uploads a shop image (5MB limit).
     */
    suspend fun uploadShopImage(file: PartData.FileItem): String = upload(file, SHOP_DIR, MAX_FILE_SIZE, "shop image")

    /**
     * Uploads a refund evidence image (5MB limit).
     */
    suspend fun uploadRefundImage(file: PartData.FileItem): String = upload(file, REFUND_DIR, MAX_FILE_SIZE, "refund image")

    /**
     * Uploads a category image (5MB limit).
     */
    suspend fun uploadCategoryImage(file: PartData.FileItem): String = upload(file, CATEGORY_DIR, MAX_FILE_SIZE, "category image")

    /**
     * Checks if the magic bytes of the file match the given extension.
     */
    private fun isValidMagicBytes(bytes: ByteArray, extension: String): Boolean {
        if (bytes.size < 4) return false
        return when (extension) {
            "jpg", "jpeg" -> {
                bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
            }
            "png" -> {
                bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
            }
            "webp" -> {
                bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()
            }
            "gif" -> {
                bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()
            }
            else -> false
        }
    }

    /**
     * Core upload logic with validation and security.
     */
    private suspend fun upload(
        file: PartData.FileItem,
        directory: String,
        maxSize: Long,
        purpose: String,
    ): String =
        withContext(Dispatchers.IO) {
            // Validate filename
            val originalName =
                file.originalFileName
                    ?: throw ValidationException(Message.Upload.fileNameRequired(purpose))

            // Extract and validate extension
            val extension = originalName.substringAfterLast('.', "").lowercase()
            if (extension !in ALLOWED_EXTENSIONS) {
                throw ValidationException(Message.Upload.invalidFileType(purpose, ALLOWED_EXTENSIONS.joinToString(", ")))
            }

            // Validate MIME type (if present)
            file.contentType?.toString()?.lowercase()?.let { mimeType ->
                if (mimeType !in ALLOWED_MIME_TYPES) {
                    throw ValidationException(Message.Upload.invalidMimeType(purpose, mimeType))
                }
            }

            // Read file bytes
            val bytes = file.streamProvider().readBytes()

            // Validate file size
            if (bytes.isEmpty()) throw ValidationException(Message.Upload.EMPTY_FILE)
            if (bytes.size > maxSize) {
                val maxSizeMB = maxSize / (1024 * 1024)
                throw ValidationException(Message.Upload.fileTooLarge(maxSizeMB.toInt(), purpose))
            }

            // Validate Magic Bytes signature
            if (!isValidMagicBytes(bytes, extension)) {
                throw ValidationException(Message.Upload.maliciousContent(extension))
            }

            // Generate secure filename
            val fileName = "${UUID.randomUUID()}.$extension"

            // Validate path and write file
            val targetDir = getDirectory(directory)
            val targetFile = File(targetDir, fileName)

            if (!targetFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                throw ValidationException(Message.Upload.INVALID_FILE_PATH)
            }

            targetFile.writeBytes(bytes)
            AsyncWorker.enqueue(BackgroundTask.ProcessImage(targetFile.absolutePath))
            fileName
        }

    /**
     * Deletes a file from the specified upload directory.
     */
    fun delete(
        directory: String,
        fileName: String?,
    ): Boolean {
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
     * Deletes a shop image.
     */
    fun deleteShopImage(fileName: String?): Boolean = delete(SHOP_DIR, fileName)

    /**
     * Deletes a category image.
     */
    fun deleteCategoryImage(fileName: String?): Boolean = delete(CATEGORY_DIR, fileName)

    /**
     * Deletes a refund image.
     */
    fun deleteRefundImage(fileName: String?): Boolean = delete(REFUND_DIR, fileName)

    /**
     * Gets the public URL for an uploaded file.
     */
    fun getPublicUrl(
        directory: String,
        fileName: String,
    ): String = "/uploads/$directory/$fileName"

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

        if (!base.canonicalPath.startsWith(root.canonicalPath)) {
            throw ValidationException(Message.Upload.INVALID_DIR_CONFIG)
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
