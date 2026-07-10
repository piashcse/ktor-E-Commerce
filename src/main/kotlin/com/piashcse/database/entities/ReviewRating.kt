package com.piashcse.database.entities

import com.piashcse.constants.ReviewStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import com.piashcse.model.response.ReviewRatingResponse
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.dao.id.EntityID

object ReviewRatingTable : BaseIdTable("review_rating") {
    val userId = reference("user_id", UserTable.id).index()
    val productId = reference("product_id", ProductTable.id).index()
    val reviewText = varchar("review_text", 500)
    val rating = integer("rating").check { it.between(1, 5) }
    val title = varchar("title", 200).nullable()
    val isVerifiedPurchase = bool("is_verified_purchase").default(false)
    val helpfulCount = integer("helpful_count").default(0)
    val notHelpfulCount = integer("not_helpful_count").default(0)
    val status = enumerationByName("status", 20, ReviewStatus::class).default(ReviewStatus.ACTIVE)
    // createdAt and updatedAt are inherited from BaseIdTable
}

class ReviewRatingDAO(id: EntityID<String>) : BaseEntity(id, ReviewRatingTable) {
    companion object : BaseEntityClass<ReviewRatingDAO>(ReviewRatingTable, ReviewRatingDAO::class.java)

    var userId by ReviewRatingTable.userId
    var productId by ReviewRatingTable.productId
    var reviewText by ReviewRatingTable.reviewText
    var rating by ReviewRatingTable.rating
    var title by ReviewRatingTable.title
    var isVerifiedPurchase by ReviewRatingTable.isVerifiedPurchase
    var helpfulCount by ReviewRatingTable.helpfulCount
    var notHelpfulCount by ReviewRatingTable.notHelpfulCount
    var status by ReviewRatingTable.status

    fun response() =
        ReviewRatingResponse(
            id = id.value,
            userId = userId.value,
            productId = productId.value,
            reviewText = reviewText,
            rating = rating,
        )
}
