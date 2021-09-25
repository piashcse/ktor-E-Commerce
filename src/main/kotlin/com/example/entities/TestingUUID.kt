package com.example.entities

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object TestingUUID : UUIDTable("product_type_table", "product_uuid") {
    val name = text("name")
    val price = double("price")
    val image = text("image")
    val productStock = bool("productStock") }

class TestingUUIDEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TestingUUIDEntity>(TestingUUID)
    var name by TestingUUID.name
    var price by TestingUUID.price
    var image by TestingUUID.image
    var productStock by TestingUUID.productStock
}
