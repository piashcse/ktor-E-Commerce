package com.piashcse.entities.product.defaultvariant

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductColorTable : BaseIntIdTable("product_color") {
    val name = text("name")
}

class ProductColorEntity(id: EntityID<String>) : BaseIntEntity(id, ProductColorTable) {
    companion object : BaseIntEntityClass<ProductColorEntity>(ProductColorTable)

    var name by ProductColorTable.name
    fun response() = ProductColor(id.value, name)
}

data class ProductColor(val id: String, val color: String)