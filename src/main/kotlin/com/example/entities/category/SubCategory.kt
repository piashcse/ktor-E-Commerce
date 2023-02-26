package com.example.entities.category

import com.example.entities.base.BaseIntEntity
import com.example.entities.base.BaseIntEntityClass
import com.example.entities.base.BaseIntIdTable
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