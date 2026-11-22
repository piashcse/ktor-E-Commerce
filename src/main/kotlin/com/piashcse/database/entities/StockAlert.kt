package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object StockAlertTable : BaseIdTable("stock_alert") {
    val userId = reference("user_id", UserTable.id).index()
    val productId = reference("product_id", ProductTable.id).index()
    val variantId = reference("variant_id", ProductVariantTable.id).nullable()
    val notifiedAt = datetime("notified_at").nullable()

    // Partial per-variant uniqueness is created by V57 in Flyway. Do not
    // declare the obsolete (user, product) index here: schema tooling would
    // recreate it and block alerts for a second variant.
}

class StockAlertDAO(id: EntityID<String>) : BaseEntity(id, StockAlertTable) {
    companion object : BaseEntityClass<StockAlertDAO>(StockAlertTable, StockAlertDAO::class.java)

    var userId by StockAlertTable.userId
    var productId by StockAlertTable.productId
    var variantId by StockAlertTable.variantId
    var notifiedAt by StockAlertTable.notifiedAt
}
