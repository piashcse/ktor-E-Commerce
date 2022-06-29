package com.example.models.shop

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam

data class GetShopCategory(@QueryParam("offset") val offset: Int, @QueryParam("limit") val limit: Int)
