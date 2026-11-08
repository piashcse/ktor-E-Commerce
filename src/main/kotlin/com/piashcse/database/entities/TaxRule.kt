package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

/** Country/category tax rate. A NULL category is the country default. */
object TaxRuleTable : BaseIdTable("tax_rule") {
    val country = varchar("country", 3).index()
    val categoryId = reference("category_id", ProductCategoryTable.id).nullable()
    val rate = decimal("rate", 6, 4)
    val isActive = bool("is_active").default(true)
}

class TaxRuleDAO(id: EntityID<String>) : BaseEntity(id, TaxRuleTable) {
    companion object : BaseEntityClass<TaxRuleDAO>(TaxRuleTable, TaxRuleDAO::class.java)

    var country by TaxRuleTable.country
    var categoryId by TaxRuleTable.categoryId
    var rate by TaxRuleTable.rate
    var isActive by TaxRuleTable.isActive
}
