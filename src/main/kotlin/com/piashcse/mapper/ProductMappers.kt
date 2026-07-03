package com.piashcse.mapper

import com.piashcse.database.entities.ProductDAO
import com.piashcse.model.response.ProductResponse

fun ProductDAO.toProductResponse() = ProductResponse(
    id = id.value,
    userId = userId.value,
    shopId = shopId?.value,
    categoryId = categoryId.value,
    subCategoryId = subCategoryId?.value,
    brandId = brandId?.value,
    name = name,
    description = description,
    sku = sku,
    barcode = barcode,
    weight = weight?.toDouble(),
    dimensions = dimensions,
    minOrderQuantity = minOrderQuantity,
    stockQuantity = stockQuantity,
    price = price.toPlainString(),
    discountPrice = discountPrice?.toPlainString(),
    discountPercentage = discountPercentage?.toDouble(),
    videoLink = videoLink,
    hotDeal = hotDeal,
    featured = featured,
    bestSeller = bestSeller,
    newProduct = newProduct,
    freeShipping = freeShipping,
    images = imageUrls,
    status = status,
    viewCount = viewCount,
    rating = rating.toDouble(),
    totalReviews = totalReviews,
    totalSales = totalSales,
)
