package com.example.entities.product.defaultvariant

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
import com.example.entities.product.ProductTable
import com.example.entities.product.ProductTable.defaultExpression
import com.example.entities.product.ProductTable.nullable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object ProductColorTable : BaseIntIdTable("product_color") {
    val name = text("name")
}

class ProductColorEntity(id: EntityID<String>) : BaseIntEntity(id, ProductColorTable) {
    companion object : BaseIntEntityClass<ProductColorEntity>(ProductColorTable)

    var name by ProductColorTable.name
    fun response() = ProductColor(id.value, name)
}

data class ProductColor(val id: String, val color: String)