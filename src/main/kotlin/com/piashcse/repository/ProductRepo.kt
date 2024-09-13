package com.piashcse.repository

import com.piashcse.entities.product.ImageUrl
import com.piashcse.entities.product.Product
import com.piashcse.models.product.request.AddProduct
import com.piashcse.models.product.request.ProductWithFilter
import com.piashcse.models.product.request.UpdateProduct

interface ProductRepo {
    suspend fun addProduct(userId: String, addProduct: AddProduct): Product
    suspend fun updateProduct(userId: String, productId: String, updateProduct: UpdateProduct):Product
    suspend fun getProducts(productQuery: ProductWithFilter): List<Product>
    suspend fun getProductById(userId: String, productQuery: ProductWithFilter):List<Product>
    suspend fun productDetail(productId: String): Product
    suspend fun deleteProduct(userId: String, productId: String) :String
    suspend fun uploadProductImage(userId: String, productId: String, productImage: String): ImageUrl
}