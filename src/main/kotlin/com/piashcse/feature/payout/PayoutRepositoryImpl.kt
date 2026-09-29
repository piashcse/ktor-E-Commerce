package com.piashcse.feature.payout

import com.piashcse.database.entities.SellerDAO
import com.piashcse.database.entities.SellerPayoutDAO
import com.piashcse.database.entities.SellerPayoutTable
import com.piashcse.database.entities.SellerTable
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDateTime
import java.time.ZoneOffset

class PayoutRepositoryImpl : PayoutRepository {
    private fun SellerPayoutDAO.toResponse() =
        PayoutResponse(
            id = id.value,
            sellerId = sellerId.value,
            orderId = orderId.value,
            subTotal = subTotal.toPlainString(),
            commissionAmount = commissionAmount.toPlainString(),
            payoutAmount = payoutAmount.toPlainString(),
            status = status,
            createdAt = createdAt.toString(),
        )

    override suspend fun sellerPayouts(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ) = query {
        val seller =
            SellerDAO.find { SellerTable.userId eq sellerUserId }.firstOrNull()
                ?: return@query PaginatedResponse(
                    emptyList(),
                    PaginationMetadata(0, limit, offset),
                )
        SellerPayoutTable.selectAll().andWhere { SellerPayoutTable.sellerId eq seller.id }
            .also { it.orderBy(SellerPayoutTable.createdAt to SortOrder.DESC) }
            .toPaginatedResponse(limit, offset) { SellerPayoutDAO.wrapRow(it).toResponse() }
    }

    override suspend fun allPayouts(
        limit: Int,
        offset: Int,
        status: String?,
    ) = query {
        SellerPayoutTable.selectAll()
            .also { q ->
                status?.let { q.andWhere { SellerPayoutTable.status eq it.uppercase() } }
                q.orderBy(SellerPayoutTable.createdAt to SortOrder.DESC)
            }
            .toPaginatedResponse(limit, offset) { SellerPayoutDAO.wrapRow(it).toResponse() }
    }

    override suspend fun markPaid(payoutId: String) =
        query {
            val payout = SellerPayoutDAO.findById(payoutId) ?: payoutId.throwNotFound("Payout")
            payout.status = "PAID"
            payout.paidAt = LocalDateTime.now(ZoneOffset.UTC)
            payout.toResponse()
        }
}
