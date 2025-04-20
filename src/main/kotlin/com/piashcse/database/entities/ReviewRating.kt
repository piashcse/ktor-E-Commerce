package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import org.jetbrains.exposed.dao.id.EntityID

object ReviewRatingTable : BaseIntIdTable("review_rating") {
    val userId = reference("user_id", UserTable.id)
    val productId = reference("product_id", ProductTable.id)
    val reviewText = varchar("review_text", 500)
    val rating = integer("rating").check { it.between(1, 5) }
}

class ReviewRatingDAO(id: EntityID<String>) : BaseIntEntity(id, ReviewRatingTable) {
    companion object : BaseIntEntityClass<ReviewRatingDAO>(ReviewRatingTable)

    var userId by ReviewRatingTable.userId
    var productId by ReviewRatingTable.productId
    var reviewText by ReviewRatingTable.reviewText
    var rating by ReviewRatingTable.rating

    fun response() = ReviewRating(
        id = id.value,
        userId = userId.value,
        productId = productId.value,
        reviewText = reviewText,
        rating = rating,
    )
}

data class ReviewRating(
    val id: String,
    val userId: String,
    val productId: String,
    val reviewText: String,
    val rating: Int
)