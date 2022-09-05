package com.example.models.user.body

import com.papsign.ktor.openapigen.content.type.multipart.FormDataRequest
import com.papsign.ktor.openapigen.content.type.multipart.NamedFileInputStream
import com.papsign.ktor.openapigen.content.type.multipart.PartEncoding

@FormDataRequest
data class MultipartImage(@PartEncoding("image/*") val file: NamedFileInputStream)