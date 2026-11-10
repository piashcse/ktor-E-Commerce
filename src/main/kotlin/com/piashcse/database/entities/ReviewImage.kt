package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ReviewImageTable : BaseIdTable("review_image") {
    val reviewId = reference("review_id", ReviewRatingTable.id).index()
    val imageUrl = varchar("image_url", 500)
    val sortOrder = integer("sort_order").default(0)
}

class ReviewImageDAO(id: EntityID<String>) : BaseEntity(id, ReviewImageTable) {
    companion object : BaseEntityClass<ReviewImageDAO>(ReviewImageTable, ReviewImageDAO::class.java)

    var reviewId by ReviewImageTable.reviewId
    var imageUrl by ReviewImageTable.imageUrl
    var sortOrder by ReviewImageTable.sortOrder
}
