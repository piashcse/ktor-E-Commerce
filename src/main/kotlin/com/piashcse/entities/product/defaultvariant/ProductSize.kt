package com.piashcse.entities.product.defaultvariant

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ProductSizeTable : BaseIntIdTable("product_size") {
    val name = text("name")
}

class ProductSizeEntity(id: EntityID<String>) : BaseIntEntity(id, ProductSizeTable) {
    companion object : BaseIntEntityClass<ProductSizeEntity>(ProductSizeTable)

    var name by ProductSizeTable.name
    fun response() = ProductSize(id.value, name)
}

data class ProductSize(val id: String, val size: String)