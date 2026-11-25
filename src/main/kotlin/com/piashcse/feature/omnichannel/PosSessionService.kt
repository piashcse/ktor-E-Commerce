package com.piashcse.feature.omnichannel

import com.piashcse.constants.FulfillmentType
import com.piashcse.constants.PosSessionStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderTable
import com.piashcse.database.entities.PosSessionDAO
import com.piashcse.database.entities.PosSessionTable
import com.piashcse.database.entities.UserTable
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.database.entities.base.currentUtc
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.readQuery
import com.piashcse.utils.extension.requireSellerByUserId
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.extension.toPaginatedResponse
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

/** POS session opening, closing, listing and reporting. */
class PosSessionService {
    suspend fun openPosSession(
        sellerUserId: String,
        request: PosSessionOpenRequest,
    ): PosSessionResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            requireOperationalShop(shopId.value)
            val warehouse =
                request.warehouseId?.takeIf { it.isNotBlank() }?.let { warehouseId ->
                    val row =
                        WarehouseDAO.findById(warehouseId)
                            ?: throw ValidationException("Warehouse not found")
                    if (row.shopId.value != shopId.value) throw ForbiddenException("Warehouse belongs to another shop")
                    if (!row.supportsPos) throw ValidationException("Warehouse does not support POS")
                    row
                }
            val existing =
                PosSessionDAO.find {
                    (PosSessionTable.shopId eq shopId) and (PosSessionTable.status eq PosSessionStatus.OPEN)
                }.firstOrNull()
            if (existing != null) throw ValidationException("A POS session is already open for this shop")
            PosSessionDAO.new {
                this.shopId = shopId
                this.warehouseId = warehouse?.id
                this.openedBy = sellerUserId.entityID(UserTable)
                status = PosSessionStatus.OPEN
                openingFloat = Money.scale2(request.openingFloat)
                notes = request.notes
            }.toResponse()
        }

    suspend fun closePosSession(
        sellerUserId: String,
        sessionId: String,
        request: PosSessionCloseRequest,
    ): PosSessionResponse =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            val session =
                PosSessionDAO.find {
                    (PosSessionTable.id eq sessionId.entityID(PosSessionTable)) and
                        (PosSessionTable.shopId eq shopId)
                }.forUpdate().firstOrNull() ?: throw ValidationException("POS session not found")
            if (session.status != PosSessionStatus.OPEN) throw ValidationException("POS session is already closed")
            val expected = Money.scale2(session.openingFloat.add(session.cashSales))
            val counted = Money.scale2(request.countedCash)
            session.expectedCash = expected
            session.countedCash = counted
            session.variance = Money.scale2(counted.subtract(expected))
            session.status = PosSessionStatus.CLOSED
            session.closedAt = currentUtc()
            session.closeNotes = request.notes
            session.toResponse()
        }

    suspend fun posSessionReport(
        sellerUserId: String,
        sessionId: String,
    ): String =
        query {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            val session =
                PosSessionDAO.find {
                    (PosSessionTable.id eq sessionId.entityID(PosSessionTable)) and
                        (PosSessionTable.shopId eq shopId)
                }.firstOrNull() ?: sessionId.throwNotFound("POS session")
            val sales =
                OrderDAO.find {
                    (OrderTable.posSessionId eq session.id) and
                        (OrderTable.fulfillmentType eq FulfillmentType.POS)
                }.count()
            val expected = session.expectedCash ?: Money.scale2(session.openingFloat.add(session.cashSales))
            """
            <!doctype html>
            <html><head><meta charset="utf-8"><title>POS Session ${escapeHtml(session.id.value)}</title>
            <style>
              body{font-family:system-ui,sans-serif;max-width:520px;margin:24px auto;color:#111}
              h1{font-size:20px;text-align:center;margin:0 0 4px}.muted{color:#666;text-align:center;font-size:12px}
              table{width:100%;border-collapse:collapse;margin:18px 0;font-size:14px}
              td{padding:8px 4px;border-bottom:1px dashed #ddd}.num{text-align:right;font-variant-numeric:tabular-nums}
              .total{font-weight:700}.foot{text-align:center;font-size:11px;color:#666;margin-top:20px}
              @media print{body{margin:0}}
            </style></head><body>
            <h1>POS Session Report</h1>
            <div class="muted">${escapeHtml(session.id.value)} · ${escapeHtml(session.status.name)}</div>
            <table>
              <tr><td>Opened</td><td class="num">${escapeHtml(session.openedAt.toString())}</td></tr>
              <tr><td>Closed</td><td class="num">${session.closedAt?.toString() ?: "-"}</td></tr>
              <tr><td>Sales</td><td class="num">$sales</td></tr>
              <tr><td>Opening float</td><td class="num">${session.openingFloat.toPlainString()}</td></tr>
              <tr><td>Cash sales</td><td class="num">${session.cashSales.toPlainString()}</td></tr>
              <tr><td>Card sales</td><td class="num">${session.cardSales.toPlainString()}</td></tr>
              <tr class="total"><td>Expected cash</td><td class="num">${expected.toPlainString()}</td></tr>
              <tr><td>Counted cash</td><td class="num">${session.countedCash?.toPlainString() ?: "-"}</td></tr>
              <tr class="total"><td>Variance</td><td class="num">${session.variance?.toPlainString() ?: "-"}</td></tr>
            </table>
            <div class="foot">Generated from the POS ledger.</div>
            </body></html>
            """.trimIndent()
        }

    suspend fun posSessions(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PosSessionResponse> =
        readQuery {
            val seller = requireSellerByUserId(sellerUserId)
            val shopId = seller.shopId ?: throw ValidationException("Seller has no shop")
            PosSessionTable.selectAll()
                .where { PosSessionTable.shopId eq shopId }
                .orderBy(PosSessionTable.openedAt to SortOrder.DESC)
                .toPaginatedResponse(limit, offset) { PosSessionDAO.wrapRow(it).toResponse() }
        }
}
