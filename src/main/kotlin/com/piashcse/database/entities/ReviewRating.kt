package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import com.piashcse.model.response.ReviewRating
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object ReviewRatingTable : BaseIntIdTable("review_rating") {
    val userId = reference("user_id", UserTable.id)
    val productId = reference("product_id", ProductTable.id)
    val reviewText = varchar("review_text", 500)
    val rating = integer("rating").check { it.between(1, 5) }
    val title = varchar("title", 200).nullable() // Added review title
    val isVerifiedPurchase = bool("is_verified_purchase").default(false) // Whether user actually purchased the product
    val helpfulCount = integer("helpful_count").default(0) // Number of helpful votes
    val notHelpfulCount = integer("not_helpful_count").default(0) // Number of not helpful votes
    val status = varchar("status", 20).default("active") // Review status: active, hidden, flagged
    // createdAt and updatedAt are inherited from BaseIntIdTable
}

class ReviewRatingDAO(id: EntityID<String>) : BaseIntEntity(id, ReviewRatingTable) {
    companion object : BaseIntEntityClass<ReviewRatingDAO>(ReviewRatingTable, ReviewRatingDAO::class.java)

    var userId by ReviewRatingTable.userId
    var productId by ReviewRatingTable.productId
    var reviewText by ReviewRatingTable.reviewText
    var rating by ReviewRatingTable.rating
    var title by ReviewRatingTable.title
    var isVerifiedPurchase by ReviewRatingTable.isVerifiedPurchase
    var helpfulCount by ReviewRatingTable.helpfulCount
    var notHelpfulCount by ReviewRatingTable.notHelpfulCount
    var status by ReviewRatingTable.status

    fun response() = ReviewRating(
        id = id.value,
        userId = userId.value,
        productId = productId.value,
        reviewText = reviewText,
        rating = rating,
    )
}