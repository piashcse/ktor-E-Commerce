package com.piashcse.feature.dashboard

import com.piashcse.constants.*
import com.piashcse.database.entities.*
import com.piashcse.model.response.*
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.readQuery
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Admin activity feed and operational work queues. */
internal object AdminDashboardService {
    suspend fun getRecentActivity(limit: Int?) =
        query {
            val max = (limit ?: 20).coerceIn(1, 50)
            val orders =
                OrderTable.selectAll().orderBy(OrderTable.createdAt to SortOrder.DESC).limit(max).map {
                    val dao = OrderDAO.wrapRow(it)
                    RecentActivityResponse(
                        dao.id.value,
                        "order",
                        "Order ${dao.orderNumber} created - \$${Money.str(dao.total)}",
                        dao.status.name.lowercase(),
                        dao.createdAt.toUtcString(),
                    )
                }
            val users =
                UserTable.selectAll().orderBy(UserTable.createdAt to SortOrder.DESC).limit(max).map {
                    val dao = UserDAO.wrapRow(it)
                    RecentActivityResponse(
                        dao.id.value,
                        "user",
                        "New ${dao.userType.name.lowercase()} registered: ${dao.email}",
                        if (dao.isVerified) "verified" else "unverified",
                        dao.createdAt.toUtcString(),
                    )
                }
            (orders + users).sortedByDescending { it.createdAt }.take(max)
        }

    suspend fun getAdminQueues() =
        readQuery {
            fun oldest(
                column: org.jetbrains.exposed.v1.core.Column<*>,
                query: org.jetbrains.exposed.v1.jdbc.Query,
            ): String? =
                query.orderBy(column to SortOrder.ASC)
                    .limit(1)
                    .firstOrNull()
                    ?.let { row -> (row[column] as? java.time.LocalDateTime)?.toUtcString() }

            val pendingShops = ShopTable.selectAll().where { ShopTable.status eq ShopStatus.PENDING }
            val pendingKyc = SellerTable.selectAll().where { SellerTable.kycStatus eq KycStatus.PENDING }
            val requestedPayouts = PayoutRequestTable.selectAll().where { PayoutRequestTable.status eq PayoutStatus.REQUESTED.name }
            val pendingRefunds = RefundRequestTable.selectAll().where { RefundRequestTable.status eq RefundStatus.PENDING }
            val requestedRmas = RmaTable.selectAll().where { RmaTable.status eq RmaStatus.REQUESTED }
            val openDisputes =
                SellerDisputeTable.selectAll().where {
                    SellerDisputeTable.status inList listOf(DisputeStatus.OPEN, DisputeStatus.UNDER_REVIEW)
                }
            val chargebacks =
                ChargebackTable.selectAll().where {
                    ChargebackTable.status inList listOf(ChargebackStatus.OPEN, ChargebackStatus.UNDER_REVIEW)
                }
            val failedWebhooks =
                PaymentWebhookEventTable.selectAll().where { PaymentWebhookEventTable.status eq WebhookEventStatus.FAILED }
            val deadLetters = OutboxDeadLetterTable.selectAll()
            val lowStock = InventoryTable.selectAll().where { InventoryTable.status eq InventoryStatus.LOW_STOCK }

            WorkQueuesResponse(
                listOf(
                    WorkQueueItem(
                        "shops",
                        "Pending shop approvals",
                        pendingShops.count(),
                        oldest(ShopTable.createdAt, pendingShops),
                        "/admin/shops/status?status=PENDING",
                    ),
                    WorkQueueItem(
                        "kyc",
                        "Pending KYC reviews",
                        pendingKyc.count(),
                        oldest(SellerTable.kycSubmittedAt, pendingKyc),
                        "/admin/sellers?status=PENDING",
                    ),
                    WorkQueueItem(
                        "payouts",
                        "Requested payouts",
                        requestedPayouts.count(),
                        oldest(PayoutRequestTable.createdAt, requestedPayouts),
                        "/admin/payouts/requests?status=REQUESTED",
                    ),
                    WorkQueueItem(
                        "refunds",
                        "Pending refunds",
                        pendingRefunds.count(),
                        oldest(RefundRequestTable.createdAt, pendingRefunds),
                        "/admin/refund-requests?status=PENDING",
                    ),
                    WorkQueueItem(
                        "rmas",
                        "Requested returns",
                        requestedRmas.count(),
                        oldest(RmaTable.createdAt, requestedRmas),
                        "/admin/rma-requests?status=REQUESTED",
                    ),
                    WorkQueueItem(
                        "disputes",
                        "Open disputes",
                        openDisputes.count(),
                        oldest(SellerDisputeTable.createdAt, openDisputes),
                        "/admin/disputes?status=OPEN",
                    ),
                    WorkQueueItem(
                        "chargebacks",
                        "Open chargebacks",
                        chargebacks.count(),
                        oldest(ChargebackTable.evidenceDueAt, chargebacks),
                        "/admin/chargebacks?status=OPEN",
                    ),
                    WorkQueueItem(
                        "webhooks",
                        "Failed webhooks",
                        failedWebhooks.count(),
                        oldest(PaymentWebhookEventTable.receivedAt, failedWebhooks),
                        "/admin/webhooks?status=FAILED",
                    ),
                    WorkQueueItem(
                        "outbox",
                        "Dead letters",
                        deadLetters.count(),
                        oldest(OutboxDeadLetterTable.createdAt, deadLetters),
                        "/admin/outbox/dead-letters",
                    ),
                    WorkQueueItem(
                        "inventory",
                        "Low-stock items",
                        lowStock.count(),
                        oldest(InventoryTable.updatedAt, lowStock),
                        "/admin/inventories?lowStock=true",
                    ),
                ),
            )
        }
}
