package com.piashcse.database.entities

import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.base.BaseIntEntity
import com.piashcse.database.entities.base.BaseIntEntityClass
import com.piashcse.database.entities.base.BaseIntIdTable
import com.piashcse.model.response.Shop
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.datetime
import java.math.BigDecimal

object ShopTable : BaseIntIdTable("shop") {
    val userId = reference("user_id", UserTable.id)
    val categoryId = reference("category_id", ShopCategoryTable.id)
    val name = text("name")
    val description = text("description").nullable()
    val address = text("address").nullable()
    val phone = varchar("phone", 20).nullable()
    val email = varchar("email", 255).nullable()
    val logo = varchar("logo", 500).nullable()
    val coverImage = varchar("cover_image", 500).nullable()
    val status = enumerationByName<ShopStatus>("status", 50).default(ShopStatus.PENDING)
    val rating = decimal("rating", 3, 2).default(BigDecimal("0.00"))
    val totalReviews = integer("total_reviews").default(0)
    // createdAt and updatedAt are inherited from BaseIntIdTable, so we don't need to redeclare them
}

class ShopDAO(id: EntityID<String>) : BaseIntEntity(id, ShopTable) {
    companion object : BaseIntEntityClass<ShopDAO>(ShopTable, ShopDAO::class.java)

    var userId by ShopTable.userId
    var categoryId by ShopTable.categoryId
    var name by ShopTable.name
    var description by ShopTable.description
    var address by ShopTable.address
    var phone by ShopTable.phone
    var email by ShopTable.email
    var logo by ShopTable.logo
    var coverImage by ShopTable.coverImage
    var status by ShopTable.status
    var rating by ShopTable.rating
    var totalReviews by ShopTable.totalReviews

    fun shopResponse() = Shop(
        id = id.value,
        name = name,
        categoryId = categoryId.value,
        description = description,
        address = address,
        phone = phone,
        email = email,
        logo = logo,
        coverImage = coverImage,
        status = status,
        rating = rating,
        totalReviews = totalReviews,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

