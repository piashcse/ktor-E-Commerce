package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable

import org.jetbrains.exposed.dao.id.EntityID

object BrandTable : BaseIntIdTable("brand") {
    val name = text("name")
    val logo = text("logo").nullable()
}

class BrandDAO(id: EntityID<String>) : BaseIntEntity(id, BrandTable) {
    companion object : BaseIntEntityClass<BrandDAO>(BrandTable)

    var name by BrandTable.name
    var logo by BrandTable.logo
    fun response() = Brand(id.value, name, logo)
}

data class Brand(val id: String, val name: String, val logo: String?)