package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

/** A physical stock location belonging to a shop. */
object WarehouseTable : BaseIdTable("warehouse") {
    val shopId = reference("shop_id", ShopTable.id).index()
    val name = varchar("name", 100)
    val code = varchar("code", 50)
    val address = text("address").nullable()
    val city = varchar("city", 100).nullable()
    val country = varchar("country", 3).nullable()
    val isActive = bool("is_active").default(true)
    val latitude = decimal("latitude", 9, 6).nullable()
    val longitude = decimal("longitude", 9, 6).nullable()
    val phone = varchar("phone", 30).nullable()
    val openingHours = varchar("opening_hours", 100).nullable()
    val supportsPickup = bool("supports_pickup").default(false)
    val supportsPos = bool("supports_pos").default(false)

    init {
        uniqueIndex("warehouse_shop_code_unique", shopId, code)
    }
}

class WarehouseDAO(id: EntityID<String>) : BaseEntity(id, WarehouseTable) {
    companion object : BaseEntityClass<WarehouseDAO>(WarehouseTable, WarehouseDAO::class.java)

    var shopId by WarehouseTable.shopId
    var name by WarehouseTable.name
    var code by WarehouseTable.code
    var address by WarehouseTable.address
    var city by WarehouseTable.city
    var country by WarehouseTable.country
    var isActive by WarehouseTable.isActive
    var latitude by WarehouseTable.latitude
    var longitude by WarehouseTable.longitude
    var phone by WarehouseTable.phone
    var openingHours by WarehouseTable.openingHours
    var supportsPickup by WarehouseTable.supportsPickup
    var supportsPos by WarehouseTable.supportsPos
}
