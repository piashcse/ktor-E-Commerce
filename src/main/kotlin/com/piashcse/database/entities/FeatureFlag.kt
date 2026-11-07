package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

/** Runtime feature flags managed by super admins. */
object FeatureFlagTable : BaseIdTable("feature_flag") {
    val key = varchar("key", 100).uniqueIndex()
    val enabled = bool("enabled").default(false)
    val description = text("description").nullable()
}

class FeatureFlagDAO(id: EntityID<String>) : BaseEntity(id, FeatureFlagTable) {
    companion object : BaseEntityClass<FeatureFlagDAO>(FeatureFlagTable, FeatureFlagDAO::class.java)

    var key by FeatureFlagTable.key
    var enabled by FeatureFlagTable.enabled
    var description by FeatureFlagTable.description
}
