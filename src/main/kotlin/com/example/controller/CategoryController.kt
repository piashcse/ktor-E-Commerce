package com.example.controller

import com.example.entities.*
import com.example.utils.AppConstants
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class CategoryController {
    fun insertCategory(userType: String, categoryName: String) = transaction {
        return@transaction if (userType == AppConstants.UserType.ADMIN) {
                ProductCategoryEntity.new (UUID.randomUUID().toString()){
                    productCategoryName = categoryName
                }.productCategoryResponse()
        } else null
    }
}