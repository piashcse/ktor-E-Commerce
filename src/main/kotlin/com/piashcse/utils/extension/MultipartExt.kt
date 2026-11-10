package com.piashcse.utils.extension

import com.piashcse.constants.Message
import com.piashcse.service.UploadService
import com.piashcse.utils.validator.ValidationException
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart

/**
 * Reads the first file part from a multipart request, validates it as an image,
 * uploads it and returns the public URL.
 *
 * Every image-upload endpoint used to repeat the same receive → validate →
 * upload → dispose block. Keeping it here guarantees consistent disposal and
 * the same "no file uploaded" 400 across modules.
 *
 * @param purpose human-readable label used in validation messages
 * @param upload backend upload function (product/brand/shop/...)
 * @param publicUrl maps the stored filename to the public URL
 */
suspend fun ApplicationCall.receiveSingleImage(
    purpose: String,
    upload: suspend (PartData.FileItem, ByteArray) -> String,
    publicUrl: (String) -> String,
): String {
    var imageUrl: String? = null
    receiveMultipart().forEachPart { part ->
        try {
            if (part is PartData.FileItem) {
                val bytes = UploadService.readAndValidateImagePart(part, purpose)
                imageUrl = publicUrl(upload(part, bytes))
            }
        } finally {
            part.dispose()
        }
    }
    return imageUrl ?: throw ValidationException(Message.Validation.FILE_REQUIRED)
}
