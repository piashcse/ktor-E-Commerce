package com.piashcse.feature.omnichannel

import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.PosSessionDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException

internal fun requireOperationalShop(shopId: String) {
    val shop =
        ShopDAO.findById(shopId)
            ?: throw ValidationException("Shop not found")
    if (shop.status != ShopStatus.APPROVED) {
        throw ForbiddenException("Shop is not operational")
    }
}

internal fun escapeHtml(value: String): String =
    value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;")

internal fun PosSessionDAO.toResponse() =
    PosSessionResponse(
        id = id.value,
        shopId = shopId.value,
        warehouseId = warehouseId?.value,
        openedBy = openedBy.value,
        status = status,
        openingFloat = Money.str(openingFloat),
        cashSales = Money.str(cashSales),
        cardSales = Money.str(cardSales),
        expectedCash = expectedCash?.let(Money::str),
        countedCash = countedCash?.let(Money::str),
        variance = variance?.let(Money::str),
        openedAt = openedAt.toUtcString(),
        closedAt = closedAt?.toUtcString(),
        notes = notes,
        closeNotes = closeNotes,
    )
