package com.piashcse.feature.payout

import com.piashcse.constants.PayoutStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PayoutRequestDAO
import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.SellerPayoutTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import org.jetbrains.exposed.v1.core.inList

internal fun SellerPayoutDAO.toPayoutResponse(
    orderNumbers: Map<String, String> = emptyMap(),
    sellerEmails: Map<String, String> = emptyMap(),
) = PayoutResponse(
    id = id.value,
    sellerId = sellerId.value,
    orderId = orderId.value,
    subTotal = subTotal.toPlainString(),
    commissionAmount = commissionAmount.toPlainString(),
    payoutAmount = payoutAmount.toPlainString(),
    currency = currency,
    status = status,
    payoutRequestId = payoutRequestId?.value,
    rejectionReason = rejectionReason,
    orderNumber = orderNumbers[orderId.value],
    sellerEmail = sellerEmails[sellerId.value],
    allowedActions =
        when (status) {
            PayoutStatus.REQUESTED.name -> listOf("APPROVE", "REJECT")
            PayoutStatus.APPROVED.name -> listOf("PAY", "REJECT")
            else -> emptyList()
        },
    requestedAt = requestedAt?.toUtcString(),
    approvedAt = approvedAt?.toUtcString(),
    rejectedAt = rejectedAt?.toUtcString(),
    paidAt = paidAt?.toUtcString(),
    createdAt = createdAt.toUtcString(),
)

internal fun enrichPayouts(
    page: PaginatedResponse<PayoutResponse>,
    includeSellerEmail: Boolean,
): PaginatedResponse<PayoutResponse> {
    if (page.data.isEmpty()) return page
    val orderNumbers =
        OrderDAO.find { OrderTable.id inList page.data.map { it.orderId }.distinct().map { it.entityID(OrderTable) } }
            .associate { it.id.value to it.orderNumber }
    val sellerEmails =
        if (includeSellerEmail) {
            val sellerIds = page.data.map { it.sellerId }.distinct().map { it.entityID(SellerTable) }
            val sellerUsers =
                SellerDAO.find { SellerTable.id inList sellerIds }
                    .associate { it.id.value to it.userId.value }
            UserDAO.find { UserTable.id inList sellerUsers.values.distinct().map { it.entityID(UserTable) } }
                .associate { it.id.value to it.email }
                .let { emails -> sellerUsers.mapValues { (_, userId) -> emails[userId].orEmpty() } }
        } else {
            emptyMap()
        }
    return page.copy(data = page.data.map { it.copy(orderNumber = orderNumbers[it.orderId], sellerEmail = sellerEmails[it.sellerId]) })
}

internal fun PayoutRequestDAO.toPayoutRequestResponse(
    sellerEmails: Map<String, String> = emptyMap(),
    rowCount: Int = 0,
) = PayoutRequestResponse(
    id = id.value,
    sellerId = sellerId.value,
    sellerEmail = sellerEmails[sellerId.value],
    amount = amount.toPlainString(),
    currency = currency,
    status = status,
    rejectionReason = rejectionReason,
    rowCount = rowCount,
    allowedActions =
        when (status) {
            PayoutStatus.REQUESTED.name -> listOf("APPROVE", "REJECT")
            PayoutStatus.APPROVED.name -> listOf("PAY", "REJECT")
            else -> emptyList()
        },
    createdAt = createdAt.toUtcString(),
)

internal fun payoutRequestResponses(
    requests: List<PayoutRequestDAO>,
    includeSellerEmail: Boolean,
): List<PayoutRequestResponse> {
    if (requests.isEmpty()) return emptyList()
    val counts =
        SellerPayoutDAO.find {
            SellerPayoutTable.payoutRequestId inList requests.map { it.id }
        }.groupingBy { it.payoutRequestId?.value }.eachCount()
    val emails =
        if (includeSellerEmail) {
            val sellerUsers =
                SellerDAO.find { SellerTable.id inList requests.map { it.sellerId }.distinct() }
                    .associate { it.id.value to it.userId.value }
            UserDAO.find { UserTable.id inList sellerUsers.values.distinct().map { it.entityID(UserTable) } }
                .associate { it.id.value to it.email }
                .let { byUser -> sellerUsers.mapValues { (_, userId) -> byUser[userId].orEmpty() } }
        } else {
            emptyMap()
        }
    return requests.map { it.toPayoutRequestResponse(emails, counts[it.id.value] ?: 0) }
}
