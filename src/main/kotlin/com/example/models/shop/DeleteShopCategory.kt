package com.example.models.shop

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam

data class DeleteShopCategory(@QueryParam("shopCategoryId") val shopCategoryId: String)
