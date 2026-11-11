package com.piashcse.database.entities

import com.piashcse.constants.DisputeStatus
import com.piashcse.constants.DisputeType
import com.piashcse.constants.ReviewStatus
import com.piashcse.database.entities.base.BaseEntity
import com.piashcse.database.entities.base.BaseEntityClass
import com.piashcse.database.entities.base.BaseIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime

object SellerReviewTable : BaseIdTable("seller_review") {
    val sellerId = reference("seller_id", SellerTable.id).index()
    val shopId = reference("shop_id", ShopTable.id).nullable()
    val userId = reference("user_id", UserTable.id)
    val orderId = reference("order_id", OrderTable.id)
    val rating = integer("rating")
    val comment = text("comment").nullable()
    val status = enumerationByName("status", 20, ReviewStatus::class).default(ReviewStatus.ACTIVE)

    init {
        uniqueIndex("seller_review_user_order_unique", userId, orderId)
    }
}

class SellerReviewDAO(id: EntityID<String>) : BaseEntity(id, SellerReviewTable) {
    companion object : BaseEntityClass<SellerReviewDAO>(SellerReviewTable, SellerReviewDAO::class.java)

    var sellerId by SellerReviewTable.sellerId
    var shopId by SellerReviewTable.shopId
    var userId by SellerReviewTable.userId
    var orderId by SellerReviewTable.orderId
    var rating by SellerReviewTable.rating
    var comment by SellerReviewTable.comment
    var status by SellerReviewTable.status
}

object SellerDisputeTable : BaseIdTable("seller_dispute") {
    val orderId = reference("order_id", OrderTable.id).nullable()
    val sellerId = reference("seller_id", SellerTable.id).nullable()
    val userId = reference("user_id", UserTable.id).index()
    val type = enumerationByName("type", 30, DisputeType::class)
    val subject = varchar("subject", 200)
    val description = text("description")
    val status = enumerationByName("status", 30, DisputeStatus::class).default(DisputeStatus.OPEN).index()
    val resolution = text("resolution").nullable()
    val resolvedBy = reference("resolved_by", UserTable.id).nullable()
    val resolvedAt = datetime("resolved_at").nullable()
}

class SellerDisputeDAO(id: EntityID<String>) : BaseEntity(id, SellerDisputeTable) {
    companion object : BaseEntityClass<SellerDisputeDAO>(SellerDisputeTable, SellerDisputeDAO::class.java)

    var orderId by SellerDisputeTable.orderId
    var sellerId by SellerDisputeTable.sellerId
    var userId by SellerDisputeTable.userId
    var type by SellerDisputeTable.type
    var subject by SellerDisputeTable.subject
    var description by SellerDisputeTable.description
    var status by SellerDisputeTable.status
    var resolution by SellerDisputeTable.resolution
    var resolvedBy by SellerDisputeTable.resolvedBy
    var resolvedAt by SellerDisputeTable.resolvedAt
}

/** Specificity: seller > shop > category; highest priority wins within a scope. */
object CommissionRuleTable : BaseIdTable("commission_rule") {
    val name = varchar("name", 150)
    val sellerId = reference("seller_id", SellerTable.id).nullable()
    val shopId = reference("shop_id", ShopTable.id).nullable()
    val categoryId = reference("category_id", ProductCategoryTable.id).nullable()
    val rate = decimal("rate", 5, 2)
    val priority = integer("priority").default(0)
    val isActive = bool("is_active").default(true)
}

class CommissionRuleDAO(id: EntityID<String>) : BaseEntity(id, CommissionRuleTable) {
    companion object : BaseEntityClass<CommissionRuleDAO>(CommissionRuleTable, CommissionRuleDAO::class.java)

    var name by CommissionRuleTable.name
    var sellerId by CommissionRuleTable.sellerId
    var shopId by CommissionRuleTable.shopId
    var categoryId by CommissionRuleTable.categoryId
    var rate by CommissionRuleTable.rate
    var priority by CommissionRuleTable.priority
    var isActive by CommissionRuleTable.isActive
}
