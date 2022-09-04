package com.example.entities.product.defaultvariant

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object ProductSizeTable : BaseIntIdTable("product_size") {
    val name = text("name")
}

class ProductSizeEntity(id: EntityID<String>) : BaseIntEntity(id, ProductSizeTable) {
    companion object : BaseIntEntityClass<ProductSizeEntity>(ProductSizeTable)

    var name by ProductSizeTable.name
    fun response() = ProductSize(id.value, name)
}

data class ProductSize(val id: String, val size: String)