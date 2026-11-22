package com.piashcse.feature.order

import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.utils.extension.escapeLike
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or

/**
 * Shared order lookup used by admin list screens (refunds, RMA, …) that accept
 * a free-text search over order number or contact email.
 */
object OrderSearch {
    /**
     * Resolves a search term to matching order ids.
     * Returns `null` when no search term was supplied, so callers can skip the filter.
     */
    fun matchingOrderIdsTx(search: String?): List<EntityID<String>>? =
        search?.takeIf { it.isNotBlank() }?.let { term ->
            val pattern = "%${term.trim().lowercase().escapeLike()}%"
            OrderDAO.find {
                (OrderTable.orderNumber.lowerCase() like pattern) or
                    (OrderTable.contactEmail.lowerCase() like pattern)
            }.map { it.id }
        }
}
