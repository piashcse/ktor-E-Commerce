package com.example.utils.extension

import kotlinx.datetime.*
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

inline fun <reified T : Any> nullProperties(data: T, callBack: (list: List<String>) -> Unit) {
    val allNullData = mutableListOf<String>()
    for (prop in T::class.memberProperties) {
        if (prop.get(data) == null) {
            allNullData.add(prop.name)
        }
    }
    callBack.invoke(allNullData)
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