package com.piashcse.utils.extension

import kotlinx.datetime.*
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

inline fun <reified T : Any> T.nullProperties(callBack: (list: List<String>) -> Unit) {
    val allNullData = mutableListOf<String>()
    for (prop in T::class.memberProperties) {
        if (prop.get(this) == null) {
            allNullData.add(prop.name)
        }
    }
    if (allNullData.size > 0) {
        callBack.invoke(allNullData)
    }
}

fun currentTimeInUTC(): LocalDateTime {
    val currentMoment: Instant = Clock.System.now()
    return currentMoment.toLocalDateTime(TimeZone.UTC)

}

fun datetimeInSystemZone(): LocalDateTime {
    val currentMoment: Instant = Clock.System.now()
    return currentMoment.toLocalDateTime(TimeZone.currentSystemDefault())
}

fun ResultRow.toMap(): Map<String, Any?> {
    val mutableMap = mutableMapOf<String, Any?>()
    val dataList = this::class.memberProperties.find { it.name == "data" }?.apply {
        isAccessible = true
    }?.call(this) as Array<*>
    fieldIndex.entries.forEach { entry ->
        val column = entry.key as Column<*>
        mutableMap[column.name] = dataList[entry.value]
    }
    return mutableMap
}

fun String.fileExtension(): String {
    return this.substring(this.lastIndexOf("."))
}

fun String.orderStatusCode(): Int {
    return when (this) {
        OrderStatus.PENDING.name.lowercase() -> 0
        OrderStatus.CONFIRMED.name.lowercase() -> 1
        OrderStatus.PAID.name.lowercase() -> 2
        OrderStatus.DELIVERED.name.lowercase() -> 3
        OrderStatus.CANCELED.name.lowercase() -> 4
        OrderStatus.RECEIVED.name.lowercase() -> 5
        else -> 0
    }
}
enum class OrderStatus {
    PENDING, CONFIRMED, PAID, DELIVERED, CANCELED, RECEIVED
}