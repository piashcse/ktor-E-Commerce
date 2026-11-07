package com.piashcse.database.entities

import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID

/** One vote per user per review; prevents unbounded helpful-count inflation. */
object ReviewHelpfulVoteTable : BaseIdTable("review_helpful_vote") {
    val reviewId = reference("review_id", ReviewRatingTable.id).index()
    val userId = reference("user_id", UserTable.id).index()
    val helpful = bool("helpful")

    init {
        uniqueIndex("review_helpful_vote_unique", reviewId, userId)
    }
}

class ReviewHelpfulVoteDAO(id: EntityID<String>) : BaseEntity(id, ReviewHelpfulVoteTable) {
    companion object : BaseEntityClass<ReviewHelpfulVoteDAO>(ReviewHelpfulVoteTable, ReviewHelpfulVoteDAO::class.java)

    var reviewId by ReviewHelpfulVoteTable.reviewId
    var userId by ReviewHelpfulVoteTable.userId
    var helpful by ReviewHelpfulVoteTable.helpful
}
