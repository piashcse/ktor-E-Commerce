package com.piashcse.utils.db

import java.math.BigDecimal
import java.sql.PreparedStatement

/** Shared JDBC binder for raw-SQL search/facet queries (was copy-pasted in ProductRepositoryImpl). */
fun PreparedStatement.bindParams(
    params: List<Any>,
    startIdx: Int = 1,
): Int {
    var idx = startIdx
    for (p in params) {
        when (p) {
            is Int -> setInt(idx++, p)
            is Long -> setLong(idx++, p)
            is String -> setString(idx++, p)
            is Double -> setBigDecimal(idx++, BigDecimal(p.toString()))
            is BigDecimal -> setBigDecimal(idx++, p)
            else -> setObject(idx++, p)
        }
    }
    return idx
}
