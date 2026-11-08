package com.piashcse.utils.common

import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.castTo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import java.time.LocalDateTime
import java.util.Base64

@Serializable
data class CursorPage<T>(
    val data: List<T>,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val limit: Int,
)

data class CursorKey(
    val createdAt: LocalDateTime,
    val id: String,
)

object CursorCodec {
    fun encode(
        createdAt: LocalDateTime,
        id: String,
    ): String {
        val raw = "$createdAt|$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    fun decode(cursor: String): CursorKey? =
        runCatching {
            val raw = String(Base64.getUrlDecoder().decode(cursor))
            val separator = raw.lastIndexOf('|')
            if (separator <= 0) return@runCatching null
            CursorKey(
                createdAt = LocalDateTime.parse(raw.substring(0, separator)),
                id = raw.substring(separator + 1),
            )
        }.getOrNull()
}

/**
 * Keyset pagination over `(createdAt DESC, id DESC)`. Fetches one extra row to
 * detect `hasMore` without a separate COUNT query.
 */
fun <T> Query.cursorPage(
    limit: Int,
    cursor: String?,
    createdAt: Column<LocalDateTime>,
    id: Column<EntityID<String>>,
    mapper: (ResultRow) -> T,
): CursorPage<T> {
    val pageSize = limit.coerceIn(1, 100)
    val key =
        cursor?.let { CursorCodec.decode(it) ?: throw ValidationException("Invalid pagination cursor") }

    if (key != null) {
        andWhere {
            (createdAt less key.createdAt) or
                ((createdAt eq key.createdAt) and (id.castTo(VarCharColumnType(50)) less key.id))
        }
    }

    val rows =
        orderBy(createdAt to SortOrder.DESC, id to SortOrder.DESC)
            .limit(pageSize + 1)
            .toList()
    val pageRows = rows.take(pageSize)
    val nextCursor =
        if (rows.size > pageSize && pageRows.isNotEmpty()) {
            val last = pageRows.last()
            CursorCodec.encode(last[createdAt], last[id].value)
        } else {
            null
        }
    return CursorPage(
        data = pageRows.map(mapper),
        nextCursor = nextCursor,
        hasMore = rows.size > pageSize,
        limit = pageSize,
    )
}
