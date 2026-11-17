package com.piashcse.utils.extension

import com.piashcse.utils.common.Csv
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respondTextWriter

/**
 * Streams a CSV by paging the source until exhausted, so exports are not capped
 * at an arbitrary row limit and do not load the whole dataset into memory.
 */
suspend fun <T> ApplicationCall.respondCsv(
    filename: String,
    header: List<String>,
    pageSize: Int = 1_000,
    fetch: suspend (limit: Int, offset: Int) -> List<T>,
    row: (T) -> List<Any?>,
) {
    response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"$filename\"")
    respondTextWriter(ContentType.parse("text/csv")) {
        write(Csv.row(header) + "\n")
        var offset = 0
        while (true) {
            val page = fetch(pageSize, offset)
            page.forEach { write(Csv.row(row(it)) + "\n") }
            if (page.size < pageSize) break
            offset += pageSize
        }
    }
}
