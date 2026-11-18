package com.piashcse.database.entities

import com.piashcse.constants.KycStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

/** Append-only KYC decision history for seller and admin consoles. */
object KycStatusHistoryTable : BaseIdTable("kyc_status_history") {
    val sellerId = reference("seller_id", SellerTable.id).index()
    val status = enumerationByName("status", 20, KycStatus::class)
    val reason = text("reason").nullable()
    val actorId = reference("actor_id", UserTable.id).nullable()
}

class KycStatusHistoryDAO(id: EntityID<String>) : BaseEntity(id, KycStatusHistoryTable) {
    companion object : BaseEntityClass<KycStatusHistoryDAO>(KycStatusHistoryTable, KycStatusHistoryDAO::class.java)

    var sellerId by KycStatusHistoryTable.sellerId
    var status by KycStatusHistoryTable.status
    var reason by KycStatusHistoryTable.reason
    var actorId by KycStatusHistoryTable.actorId
}
