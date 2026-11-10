package com.piashcse.feature.stockalert

import com.piashcse.constants.ProductStatus
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductVariantDAO
import com.piashcse.database.entities.StockAlertDAO
import com.piashcse.database.entities.StockAlertTable
import com.piashcse.database.entities.UserTable
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq

@Serializable
data class StockAlertRequest(
    val productId: String,
    val variantId: String? = null,
)

@Serializable
data class StockAlertResponse(
    val id: String,
    val productId: String,
    val variantId: String?,
    val notifiedAt: String?,
    val createdAt: String?,
)

/** Customer back-in-stock subscriptions; notifications are delivered by the outbox subscriber. */
class StockAlertService {
    suspend fun subscribe(
        userId: String,
        request: StockAlertRequest,
    ): StockAlertResponse =
        query {
            val product = ProductDAO.findById(request.productId) ?: request.productId.throwNotFound("Product")
            if (product.deletedAt != null || product.status != ProductStatus.ACTIVE) {
                throw ValidationException("Cannot subscribe to an inactive product")
            }
            request.variantId?.let { variantId ->
                val variant = ProductVariantDAO.findById(variantId) ?: variantId.throwNotFound("Variant")
                if (variant.productId.value != product.id.value) {
                    throw ValidationException("Variant does not belong to this product")
                }
            }

            val userEntityId = userId.entityID(UserTable)
            val existing =
                StockAlertDAO.find {
                    (StockAlertTable.userId eq userEntityId) and (StockAlertTable.productId eq product.id)
                }.firstOrNull()

            val alert =
                existing?.apply {
                    variantId = request.variantId?.entityID(com.piashcse.database.entities.ProductVariantTable)
                    notifiedAt = null
                } ?: StockAlertDAO.new {
                    this.userId = userEntityId
                    this.productId = product.id
                    variantId = request.variantId?.entityID(com.piashcse.database.entities.ProductVariantTable)
                }
            alert.toResponse()
        }

    suspend fun unsubscribe(
        userId: String,
        productId: String,
    ): Boolean =
        query {
            val alert =
                StockAlertDAO.find {
                    (StockAlertTable.userId eq userId.entityID(UserTable)) and
                        (StockAlertTable.productId eq productId.entityID(com.piashcse.database.entities.ProductTable))
                }.firstOrNull() ?: return@query false
            alert.delete()
            true
        }

    suspend fun list(userId: String): List<StockAlertResponse> =
        query {
            StockAlertDAO.find { StockAlertTable.userId eq userId.entityID(UserTable) }
                .orderBy(StockAlertTable.createdAt to org.jetbrains.exposed.v1.core.SortOrder.DESC)
                .map { it.toResponse() }
        }

    private fun StockAlertDAO.toResponse() =
        StockAlertResponse(
            id = id.value,
            productId = productId.value,
            variantId = variantId?.value,
            notifiedAt = notifiedAt?.toUtcString(),
            createdAt = createdAt.toUtcString(),
        )
}
