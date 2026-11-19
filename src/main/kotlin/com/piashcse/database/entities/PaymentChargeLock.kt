package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * Short-lived distributed lease that serializes payment charging per order
 * across application instances. Expired leases are reclaimed by the next
 * attempt, so a crashed instance cannot block an order forever.
 */
object PaymentChargeLockTable : BaseIdTable("payment_charge_lock") {
    val orderId = reference("order_id", OrderTable.id).uniqueIndex()
    val owner = varchar("owner", 50)
    val lockedUntil = datetime("locked_until")
}

class PaymentChargeLockDAO(id: EntityID<String>) : BaseEntity(id, PaymentChargeLockTable) {
    companion object : BaseEntityClass<PaymentChargeLockDAO>(PaymentChargeLockTable, PaymentChargeLockDAO::class.java)

    var orderId by PaymentChargeLockTable.orderId
    var owner by PaymentChargeLockTable.owner
    var lockedUntil by PaymentChargeLockTable.lockedUntil
}
