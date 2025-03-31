package com.piashcse.utils.extension


fun String.fileExtension(): String {
    return this.substring(this.lastIndexOf("."))
}