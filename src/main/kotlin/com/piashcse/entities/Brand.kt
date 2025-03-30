package com.piashcse.entities

import com.piashcse.entities.base.BaseIntEntity
import com.piashcse.entities.base.BaseIntEntityClass
import com.piashcse.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object BrandTable : BaseIntIdTable("brand") {
    val name = text("name")
    val logo = text("logo").nullable()
}

class BrandEntity(id: EntityID<String>) : BaseIntEntity(id, BrandTable) {
    companion object : BaseIntEntityClass<BrandEntity>(BrandTable)

    var name by BrandTable.name
    var logo by BrandTable.logo
    fun response() = Brand(id.value, name, logo)
}

data class Brand(val id: String, val name: String, val logo: String?)