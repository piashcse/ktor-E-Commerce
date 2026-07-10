package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ProductCategoryTable : BaseIdTable("category") {
    val name = text("name")
    val image = text("image").nullable()
}

class ProductCategoryDAO(id: EntityID<String>) : BaseEntity(id, ProductCategoryTable) {
    companion object : BaseEntityClass<ProductCategoryDAO>(ProductCategoryTable, ProductCategoryDAO::class.java)

    var name by ProductCategoryTable.name
    val subCategories by ProductSubCategoryDAO referrersOn ProductSubCategoryTable.categoryId
    var image by ProductCategoryTable.image

}
