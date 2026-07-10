package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ShippingMethodTable : BaseIdTable("shipping_method") {
    val name = varchar("name", 50)
    val type = varchar("type", 50).nullable() // e.g., Standard, Express
    val price = double("price")
    val deliveryTime = varchar("delivery_time", 50).nullable() // e.g., 3-5 days
}

class ShippingMethodDAO(id: EntityID<String>) : BaseEntity(id, ShippingMethodTable) {
    companion object : BaseEntityClass<ShippingMethodDAO>(ShippingMethodTable, ShippingMethodDAO::class.java)

    var name by ShippingMethodTable.name
    var type by ShippingMethodTable.type
    var price by ShippingMethodTable.price
    var deliveryTime by ShippingMethodTable.deliveryTime

}
