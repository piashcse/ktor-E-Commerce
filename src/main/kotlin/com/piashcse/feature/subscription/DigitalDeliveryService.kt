package com.piashcse.feature.subscription

import com.piashcse.constants.SubscriptionStatus
import com.piashcse.database.entities.DigitalEntitlementDAO
import com.piashcse.database.entities.DigitalEntitlementTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SubscriptionDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.service.AppMetrics
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import java.util.UUID

@Serializable
data class DigitalLibraryItem(
    val entitlementId: String,
    val productId: String,
    val productName: String,
    val fileName: String?,
    val downloadToken: String,
    val downloadCount: Int,
    val maxDownloads: Int,
    val remainingDownloads: Int,
    val expiresAt: String?,
    val active: Boolean,
    val grantedByOrder: String?,
    val grantedBySubscription: String?,
)

@Serializable
data class DigitalDownloadResponse(
    val downloadUrl: String,
    val fileName: String?,
    val remainingDownloads: Int,
)

/** Grants and serves access to digital products. */
class DigitalDeliveryService {
    /** Must run inside the payment transaction after the order is fully paid. */
    fun grantForOrderTx(order: OrderDAO) {
        val items = OrderItemDAO.itemsForOrder(order.id)
        items.groupBy { it.productId.value }.forEach { (productId, lines) ->
            val product = ProductDAO.findById(productId) ?: return@forEach
            if (!product.isDigital) return@forEach
            val exists =
                DigitalEntitlementDAO.find {
                    (DigitalEntitlementTable.userId eq order.userId) and
                        (DigitalEntitlementTable.productId eq product.id) and
                        (DigitalEntitlementTable.orderId eq order.id)
                }.any()
            if (exists) return@forEach
            val token = UUID.randomUUID().toString()
            DigitalEntitlementDAO.new {
                this.userId = order.userId
                this.productId = product.id
                this.orderId = order.id
                this.subscriptionId = null
                this.downloadToken = token
                this.downloadUrl = product.digitalAssetUrl ?: "local://digital/${product.id.value}"
                this.fileName = product.digitalFileName ?: product.name
                this.maxDownloads = 5
                this.expiresAt = currentUtc().plusYears(1)
            }
            // Keep the snapshot flag accurate for orders created before this feature.
            lines.forEach { it.isDigital = true }
        }
    }

    /** Must run inside the subscription transaction. */
    fun grantSubscriptionTx(
        userId: String,
        productId: String,
        subscriptionId: String,
    ) {
        val product = ProductDAO.findById(productId) ?: return
        if (!product.isDigital) return
        val subscription =
            SubscriptionDAO.findById(subscriptionId) ?: return
        val existing =
            DigitalEntitlementDAO.find {
                (DigitalEntitlementTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable)) and
                    (DigitalEntitlementTable.productId eq product.id) and
                    (DigitalEntitlementTable.subscriptionId eq subscription.id)
            }.firstOrNull()
        if (existing != null) {
            existing.expiresAt = subscription.currentPeriodEnd
            return
        }
        DigitalEntitlementDAO.new {
            this.userId = userId.entityID(com.piashcse.database.entities.UserTable)
            this.productId = product.id
            this.orderId = null
            this.subscriptionId = subscription.id
            this.downloadToken = UUID.randomUUID().toString()
            this.downloadUrl = product.digitalAssetUrl ?: "local://digital/${product.id.value}"
            this.fileName = product.digitalFileName ?: product.name
            this.maxDownloads = 50
            this.expiresAt = subscription.currentPeriodEnd
        }
    }

    /** Revokes order-based digital access after cancellation/full refund. */
    fun revokeOrderTx(orderId: String) {
        DigitalEntitlementDAO.find {
            DigitalEntitlementTable.orderId eq orderId.entityID(com.piashcse.database.entities.OrderTable)
        }.forEach { it.delete() }
    }

    suspend fun library(userId: String): List<DigitalLibraryItem> =
        query {
            DigitalEntitlementDAO.find {
                DigitalEntitlementTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable)
            }.map { it.toLibraryItem() }
        }

    suspend fun download(
        token: String,
        userId: String,
    ): DigitalDownloadResponse =
        query {
            val entitlement =
                DigitalEntitlementDAO.find { DigitalEntitlementTable.downloadToken eq token }
                    .forUpdate()
                    .firstOrNull() ?: token.throwNotFound("Download")
            if (entitlement.userId.value != userId) throw ForbiddenException("This download belongs to another account")
            if (!entitlement.isActive() || !subscriptionActive(entitlement.subscriptionId?.value)) {
                throw ValidationException("Download is expired or has reached its limit")
            }
            entitlement.downloadCount += 1
            AppMetrics.digitalDownloads.increment()
            DigitalDownloadResponse(
                downloadUrl = entitlement.downloadUrl,
                fileName = entitlement.fileName,
                remainingDownloads = (entitlement.maxDownloads - entitlement.downloadCount).coerceAtLeast(0),
            )
        }

    private fun subscriptionActive(subscriptionId: String?): Boolean {
        if (subscriptionId == null) return true
        val subscription = SubscriptionDAO.findById(subscriptionId) ?: return false
        return subscription.status == SubscriptionStatus.ACTIVE && subscription.currentPeriodEnd.isAfter(currentUtc())
    }

    private fun DigitalEntitlementDAO.toLibraryItem(): DigitalLibraryItem {
        val product = ProductDAO.findById(productId.value)
        return DigitalLibraryItem(
            entitlementId = id.value,
            productId = productId.value,
            productName = product?.name ?: productId.value,
            fileName = fileName,
            downloadToken = downloadToken,
            downloadCount = downloadCount,
            maxDownloads = maxDownloads,
            remainingDownloads = (maxDownloads - downloadCount).coerceAtLeast(0),
            expiresAt = expiresAt?.toUtcString(),
            active = isActive() && subscriptionActive(subscriptionId?.value),
            grantedByOrder = orderId?.value,
            grantedBySubscription = subscriptionId?.value,
        )
    }
}
