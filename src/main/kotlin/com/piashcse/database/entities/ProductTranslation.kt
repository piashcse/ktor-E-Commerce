package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ProductTranslationTable : BaseIdTable("product_translation") {
    val productId = reference("product_id", ProductTable.id).index()
    val locale = varchar("locale", 10).index()
    val name = text("name")
    val description = text("description")

    init {
        uniqueIndex("product_translation_product_locale_unique", productId, locale)
    }
}

class ProductTranslationDAO(id: EntityID<String>) : BaseEntity(id, ProductTranslationTable) {
    companion object : BaseEntityClass<ProductTranslationDAO>(ProductTranslationTable, ProductTranslationDAO::class.java)

    var productId by ProductTranslationTable.productId
    var locale by ProductTranslationTable.locale
    var name by ProductTranslationTable.name
    var description by ProductTranslationTable.description
}
