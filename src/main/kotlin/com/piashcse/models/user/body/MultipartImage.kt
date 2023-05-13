package com.piashcse.models.user.body

import com.papsign.ktor.openapigen.content.type.multipart.FormDataRequest
import com.papsign.ktor.openapigen.content.type.multipart.NamedFileInputStream
import com.papsign.ktor.openapigen.content.type.multipart.PartEncoding
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@FormDataRequest
data class MultipartImage(@PartEncoding("image/*") val file: NamedFileInputStream){
    fun validation(){
        validate(this){
            validate(MultipartImage::file).isNotNull()
        }
    }
}