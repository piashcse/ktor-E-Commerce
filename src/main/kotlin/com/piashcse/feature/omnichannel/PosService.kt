package com.piashcse.feature.omnichannel

import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.tax.TaxService
import com.piashcse.model.response.OrderResponse
import com.piashcse.service.DigitalDeliveryService
import com.piashcse.utils.common.PaginatedResponse

/** Facade over POS sales and sessions. */
class PosService(
    digitalDelivery: DigitalDeliveryService,
    taxService: TaxService,
    loyaltyService: LoyaltyService,
) {
    private val sales = PosSaleService(digitalDelivery, taxService, loyaltyService)
    private val sessions = PosSessionService()

    suspend fun posSale(
        sellerUserId: String,
        request: PosSaleRequest,
    ): OrderResponse = sales.posSale(sellerUserId, request)

    suspend fun posReceipt(
        sellerUserId: String,
        orderId: String,
    ): String = sales.posReceipt(sellerUserId, orderId)

    suspend fun posSales(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<OrderResponse> = sales.posSales(sellerUserId, limit, offset)

    suspend fun openPosSession(
        sellerUserId: String,
        request: PosSessionOpenRequest,
    ): PosSessionResponse = sessions.openPosSession(sellerUserId, request)

    suspend fun closePosSession(
        sellerUserId: String,
        sessionId: String,
        request: PosSessionCloseRequest,
    ): PosSessionResponse = sessions.closePosSession(sellerUserId, sessionId, request)

    suspend fun posSessionReport(
        sellerUserId: String,
        sessionId: String,
    ): String = sessions.posSessionReport(sellerUserId, sessionId)

    suspend fun posSessions(
        sellerUserId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<PosSessionResponse> = sessions.posSessions(sellerUserId, limit, offset)
}
