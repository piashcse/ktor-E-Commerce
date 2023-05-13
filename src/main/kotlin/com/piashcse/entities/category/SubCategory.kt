package com.piashcse.entities.category

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object SubCategoryTable : BaseIntIdTable("sub_category") {
    val categoryId = reference("category_id", CategoryTable.id)
    val subCategoryName = text("sub_category_name")
    val image = text("image").nullable()
}

class SubCategoryEntity(id: EntityID<String>) : BaseIntEntity(id, SubCategoryTable) {
    companion object : BaseIntEntityClass<SubCategoryEntity>(SubCategoryTable)

    var categoryId by SubCategoryTable.categoryId
    var subCategoryName by SubCategoryTable.subCategoryName
    var image by SubCategoryTable.image
    fun subCategoryResponse() = SubCategoryResponse(id.value, categoryId.value, subCategoryName, image)
}

data class SubCategoryResponse(val id: String, val categoryId: String, val subCategoryName: String, val image: String?)