package com.piashcse.service

import com.piashcse.config.DotEnvConfig
import com.piashcse.constants.SubscriptionStatus
import com.piashcse.database.entities.DigitalEntitlementDAO
import com.piashcse.database.entities.DigitalEntitlementTable
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.SubscriptionDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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
    /** Short-lived signed URL; the underlying asset URL is never exposed. */
    val downloadUrl: String,
    val fileName: String?,
    val remainingDownloads: Int,
    val expiresAt: String? = null,
)

/** Grants and serves access to digital products. */
class DigitalDeliveryService(
    private val signingSecret: String =
        DotEnvConfig.digitalUrlSecret.ifBlank {
            // Derive a separate key rather than reusing the JWT signing secret.
            com.piashcse.utils.common.hmacSha256Hex(DotEnvConfig.jwtSecret, "ktor-ecom:digital-download-signing")
        },
) {
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
            // A renewal starts a fresh download quota.
            existing.downloadCount = 0
            existing.maxDownloads = 50
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

    /** Keep access through the dunning grace window instead of ending at period end. */
    fun extendSubscriptionGraceTx(
        subscriptionId: String,
        until: java.time.LocalDateTime,
    ) {
        DigitalEntitlementDAO.find {
            DigitalEntitlementTable.subscriptionId eq subscriptionId.entityID(com.piashcse.database.entities.SubscriptionTable)
        }.forEach { entitlement ->
            if (entitlement.expiresAt?.isBefore(until) ?: true) {
                entitlement.expiresAt = until
            }
        }
    }

    /** Revokes subscription-based digital access after cancellation/expiry/dunning failure. */
    fun revokeSubscriptionTx(subscriptionId: String) {
        DigitalEntitlementDAO.find {
            DigitalEntitlementTable.subscriptionId eq subscriptionId.entityID(com.piashcse.database.entities.SubscriptionTable)
        }.forEach { it.delete() }
    }

    /** Revokes order-based digital access after cancellation/full refund. */
    fun revokeOrderTx(orderId: String) {
        DigitalEntitlementDAO.find {
            DigitalEntitlementTable.orderId eq orderId.entityID(com.piashcse.database.entities.OrderTable)
        }.forEach { it.delete() }
    }

    suspend fun library(userId: String): List<DigitalLibraryItem> =
        query {
            val entitlements =
                DigitalEntitlementDAO.find {
                    DigitalEntitlementTable.userId eq userId.entityID(com.piashcse.database.entities.UserTable)
                }.toList()
            val productIds = entitlements.map { it.productId }.distinct()
            val products =
                if (productIds.isEmpty()) {
                    emptyMap()
                } else {
                    ProductDAO.find { com.piashcse.database.entities.ProductTable.id inList productIds }
                        .associateBy { it.id.value }
                }
            val subscriptionIds = entitlements.mapNotNull { it.subscriptionId }.distinct()
            val activeSubscriptions =
                if (subscriptionIds.isEmpty()) {
                    emptySet()
                } else {
                    SubscriptionDAO.find {
                        (com.piashcse.database.entities.SubscriptionTable.id inList subscriptionIds) and
                            (com.piashcse.database.entities.SubscriptionTable.status eq SubscriptionStatus.ACTIVE) and
                            (com.piashcse.database.entities.SubscriptionTable.currentPeriodEnd greater currentUtc())
                    }.map { it.id.value }.toSet()
                }
            entitlements.map {
                it.toLibraryItem(
                    products[it.productId.value],
                    it.subscriptionId?.value?.let { id -> id in activeSubscriptions } ?: true,
                )
            }
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
            val expires = currentUtc().plusMinutes(5)
            val expiresEpoch = expires.toEpochSecond(java.time.ZoneOffset.UTC)
            val signature = sign(token, expiresEpoch)
            DigitalDownloadResponse(
                downloadUrl =
                    "${DotEnvConfig.publicBaseUrl}/api/v1/library/asset" +
                        "?token=${encode(token)}&expires=$expiresEpoch&signature=$signature",
                fileName = entitlement.fileName,
                remainingDownloads = (entitlement.maxDownloads - entitlement.downloadCount).coerceAtLeast(0),
                expiresAt = expires.toUtcString(),
            )
        }

    /** Validates a short-lived signed URL and returns the asset location for redirect. */
    suspend fun asset(
        token: String,
        expires: Long,
        signature: String,
    ): String =
        query {
            if (expires < currentUtc().toEpochSecond(java.time.ZoneOffset.UTC)) {
                throw ValidationException("Download link has expired")
            }
            val expected = sign(token, expires)
            if (!MessageDigest.isEqual(expected.toByteArray(), signature.toByteArray())) {
                throw ValidationException("Invalid download signature")
            }
            val entitlement =
                DigitalEntitlementDAO.find { DigitalEntitlementTable.downloadToken eq token }
                    .forUpdate()
                    .firstOrNull() ?: token.throwNotFound("Download")
            if (!entitlement.isActive() || !subscriptionActive(entitlement.subscriptionId?.value)) {
                throw ValidationException("Download is expired or has reached its limit")
            }
            // The actual asset fetch consumes the quota; minting the URL does not.
            entitlement.downloadCount += 1
            AppMetrics.digitalDownloads.increment()
            entitlement.downloadUrl
        }

    private fun sign(
        token: String,
        expires: Long,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(signingSecret.toByteArray(), "HmacSHA256"))
        return HexFormat.of().formatHex(mac.doFinal("$token:$expires".toByteArray()))
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8)

    private fun subscriptionActive(subscriptionId: String?): Boolean {
        if (subscriptionId == null) return true
        val subscription = SubscriptionDAO.findById(subscriptionId) ?: return false
        val now = currentUtc()
        if (subscription.status == SubscriptionStatus.ACTIVE && subscription.currentPeriodEnd.isAfter(now)) return true
        // Dunning grace: PAST_DUE retains access until graceUntil passes.
        return subscription.status == SubscriptionStatus.PAST_DUE &&
            subscription.graceUntil?.isAfter(now) == true
    }

    private fun DigitalEntitlementDAO.toLibraryItem(
        product: ProductDAO?,
        subscriptionIsActive: Boolean,
    ): DigitalLibraryItem =
        DigitalLibraryItem(
            entitlementId = id.value,
            productId = productId.value,
            productName = product?.name ?: productId.value,
            fileName = fileName,
            downloadToken = downloadToken,
            downloadCount = downloadCount,
            maxDownloads = maxDownloads,
            remainingDownloads = (maxDownloads - downloadCount).coerceAtLeast(0),
            expiresAt = expiresAt?.toUtcString(),
            active = isActive() && subscriptionIsActive,
            grantedByOrder = orderId?.value,
            grantedBySubscription = subscriptionId?.value,
        )
}
