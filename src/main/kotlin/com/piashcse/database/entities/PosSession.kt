package com.piashcse.database.entities

import com.piashcse.constants.PosSessionStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.database.entities.base.currentUtc
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal

object PosSessionTable : BaseIdTable("pos_session") {
    val shopId = reference("shop_id", ShopTable.id).index()
    val warehouseId = reference("warehouse_id", WarehouseTable.id).nullable()
    val openedBy = reference("opened_by", UserTable.id)
    val status = enumerationByName("status", 20, PosSessionStatus::class).default(PosSessionStatus.OPEN).index()
    val openingFloat = decimal("opening_float", 12, 2).default(BigDecimal.ZERO)
    val cashSales = decimal("cash_sales", 12, 2).default(BigDecimal.ZERO)
    val cardSales = decimal("card_sales", 12, 2).default(BigDecimal.ZERO)
    val expectedCash = decimal("expected_cash", 12, 2).nullable()
    val countedCash = decimal("counted_cash", 12, 2).nullable()
    val variance = decimal("variance", 12, 2).nullable()
    val openedAt = datetime("opened_at").clientDefault { currentUtc() }
    val closedAt = datetime("closed_at").nullable()
    val notes = text("notes").nullable()
    val closeNotes = text("close_notes").nullable()
}

class PosSessionDAO(id: EntityID<String>) : BaseEntity(id, PosSessionTable) {
    companion object : BaseEntityClass<PosSessionDAO>(PosSessionTable, PosSessionDAO::class.java)

    var shopId by PosSessionTable.shopId
    var warehouseId by PosSessionTable.warehouseId
    var openedBy by PosSessionTable.openedBy
    var status by PosSessionTable.status
    var openingFloat by PosSessionTable.openingFloat
    var cashSales by PosSessionTable.cashSales
    var cardSales by PosSessionTable.cardSales
    var expectedCash by PosSessionTable.expectedCash
    var countedCash by PosSessionTable.countedCash
    var variance by PosSessionTable.variance
    var openedAt by PosSessionTable.openedAt
    var closedAt by PosSessionTable.closedAt
    var notes by PosSessionTable.notes
    var closeNotes by PosSessionTable.closeNotes
}
