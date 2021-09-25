package com.example.utils

import kotlinx.datetime.*
import java.time.format.DateTimeFormatterBuilder
import kotlin.reflect.full.memberProperties

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