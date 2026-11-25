package com.piashcse.service

import com.piashcse.config.DotEnvConfig
import com.piashcse.constants.Message
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
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
    private val log = LoggerFactory.getLogger(UploadService::class.java)
    private val directoryLocks = java.util.concurrent.ConcurrentHashMap<String, Any>()

    // Upload configuration
    private val uploadBaseDir = DotEnvConfig.uploadDir

    // File size limit (applies to all image types)
    private const val MAX_FILE_SIZE = 5L * 1024 * 1024 // 5 MB
    private const val MAX_STORAGE_PER_DIR = 500L * 1024 * 1024 // 500 MB per upload directory

    // Allowed types
    private val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
    private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")

    // Route-level pre-upload allowlist (ProductRoutes/ProfileRoutes): deliberately
    // stricter than ALLOWED_MIME_TYPES above — GIF is rejected at the edge even
    // though the service core would store it. Keep in sync with the 5 MB cap.
    val ROUTE_IMAGE_MIME_ALLOWLIST = setOf("image/jpeg", "image/png", "image/webp")
    const val ROUTE_IMAGE_MAX_BYTES = 5L * 1024 * 1024 // 5 MB

    private val INVALID_FILENAME_REGEX = Regex("""[^\w.\-]""")

    init {
        // Create all upload directories on startup
        UploadPurpose.entries.forEach { ensureDirectoryExists(it.directory) }
    }

    /**
     * Uploads a file to [purpose]'s directory with the shared 5 MB limit.
     */
    suspend fun upload(
        purpose: UploadPurpose,
        file: PartData.FileItem,
        preloadedBytes: ByteArray? = null,
    ): String = upload(file, purpose.directory, MAX_FILE_SIZE, purpose.label, preloadedBytes)

    /**
     * Checks if the magic bytes of the file match the given extension.
     */
    private fun isValidMagicBytes(
        bytes: ByteArray,
        extension: String,
    ): Boolean {
        if (bytes.size < 4) return false
        return when (extension) {
            "jpg", "jpeg" -> {
                bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
            }
            "png" -> {
                bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
            }
            "webp" -> {
                bytes.size >= 12 &&
                    bytes.decodeToString(0, 4) == "RIFF" &&
                    bytes.decodeToString(8, 12) == "WEBP"
            }
            "gif" -> {
                bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()
            }
            else -> false
        }
    }

    /**
     * Route-level pre-validation for image uploads. Called by ProductRoutes /
     * ProfileRoutes BEFORE the UploadService upload call: rejects disallowed MIME
     * types and over-size payloads early with ValidationException (400). Returns
     * the file bytes so the multipart stream is consumed exactly once — pass them
     * to the matching upload* overload as [preloadedBytes].
     */
    suspend fun readAndValidateImagePart(
        part: PartData.FileItem,
        purpose: String,
    ): ByteArray {
        val mimeType = part.contentType?.toString()?.lowercase()
        if (mimeType == null || mimeType !in ROUTE_IMAGE_MIME_ALLOWLIST) {
            throw ValidationException(Message.Upload.invalidMimeType(purpose, mimeType ?: "unknown"))
        }
        val bytes =
            withContext(Dispatchers.IO) {
                part.streamProvider().use { input ->
                    readBounded(input, ROUTE_IMAGE_MAX_BYTES, purpose)
                }
            }
        if (bytes.isEmpty()) throw ValidationException(Message.Upload.EMPTY_FILE)
        return bytes
    }

    /** Reads at most [maxBytes] and aborts before allocating more than the limit. */
    private fun readBounded(
        input: InputStream,
        maxBytes: Long,
        purpose: String,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) {
                throw ValidationException(
                    Message.Upload.fileTooLarge((maxBytes / (1024 * 1024)).toInt(), purpose),
                )
            }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    /**
     * Core upload logic with validation and security.
     */
    private suspend fun upload(
        file: PartData.FileItem,
        directory: String,
        maxSize: Long,
        purpose: String,
        preloadedBytes: ByteArray? = null,
    ): String =
        withContext(Dispatchers.IO) {
            // Validate filename
            val originalName =
                file.originalFileName
                    ?: throw ValidationException(Message.Upload.fileNameRequired(purpose))

            // Sanitize filename: strip null bytes, path separators, control characters
            val sanitizedName =
                originalName
                    .replace(INVALID_FILENAME_REGEX, "_")
                    .replace(Regex("""[\x00/\\:]"""), "_")
                    .replace(Regex("""\p{Cntrl}"""), "")
                    .trim()
                    .take(255)
                    .ifEmpty { throw ValidationException(Message.Upload.fileNameRequired(purpose)) }

            // Extract and validate extension
            val extension = sanitizedName.substringAfterLast('.', "").lowercase()
            if (extension !in ALLOWED_EXTENSIONS) {
                throw ValidationException(Message.Upload.invalidFileType(purpose, ALLOWED_EXTENSIONS.joinToString(", ")))
            }

            // Validate MIME type (if present)
            file.contentType?.toString()?.lowercase()?.let { mimeType ->
                if (mimeType !in ALLOWED_MIME_TYPES) {
                    throw ValidationException(Message.Upload.invalidMimeType(purpose, mimeType))
                }
            }

            // Read file bytes (reuse route pre-validated bytes when provided so the
            // multipart stream is consumed exactly once). Always enforce the limit
            // while streaming, never after buffering the whole body.
            val bytes =
                preloadedBytes ?: file.streamProvider().use { input ->
                    readBounded(input, maxSize, purpose)
                }

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

            // Validate path and storage quota
            val targetDir = getDirectory(directory)
            val targetFile = File(targetDir, fileName)

            if (!targetFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                throw ValidationException(Message.Upload.INVALID_FILE_PATH)
            }

            val lock = directoryLocks.computeIfAbsent(targetDir.canonicalPath) { Any() }
            synchronized(lock) {
                val dirSize = targetDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                if (dirSize + bytes.size > MAX_STORAGE_PER_DIR) {
                    throw ValidationException(Message.Upload.storageQuotaExceeded(purpose))
                }
                targetFile.writeBytes(bytes)
            }
            AsyncWorker.enqueue(BackgroundTask.ProcessImage(targetFile.absolutePath))
            fileName
        }

    private val allowedDirectories = UploadPurpose.entries.map { it.directory }.toSet()

    /**
     * Deletes a file from the specified upload directory.
     */
    fun delete(
        directory: String,
        fileName: String?,
    ): Boolean {
        if (fileName.isNullOrBlank()) return false
        if (directory !in allowedDirectories) return false
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) return false

        return try {
            val targetDir = getDirectory(directory)
            val targetFile = File(targetDir, fileName)

            // Verify path is within upload directory
            if (!targetFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                return false
            }

            targetFile.delete()
        } catch (e: Exception) {
            log.warn("Failed to delete upload '$directory/$fileName'", e)
            false
        }
    }

    fun delete(
        purpose: UploadPurpose,
        fileName: String?,
    ): Boolean = delete(purpose.directory, fileName)

    /**
     * Gets the public URL for an uploaded file.
     */
    fun getPublicUrl(
        directory: String,
        fileName: String,
    ): String = "/uploads/$directory/$fileName"

    fun getPublicUrl(
        purpose: UploadPurpose,
        fileName: String,
    ): String = getPublicUrl(purpose.directory, fileName)

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
