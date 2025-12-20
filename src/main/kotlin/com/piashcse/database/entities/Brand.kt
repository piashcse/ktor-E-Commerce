package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.Brand
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object BrandTable : BaseIdTable("brand") {
    val name = text("name")
    val logo = text("logo").nullable()
}

class BrandDAO(id: EntityID<String>) : BaseEntity(id, BrandTable) {
    companion object : BaseEntityClass<BrandDAO>(BrandTable, BrandDAO::class.java)

    var name by BrandTable.name
    var logo by BrandTable.logo
    fun response() = Brand(id.value, name, logo)
}