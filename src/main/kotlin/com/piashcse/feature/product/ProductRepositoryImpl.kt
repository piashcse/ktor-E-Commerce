package com.piashcse.feature.product

import com.piashcse.constants.InventoryStatus
import com.piashcse.constants.Message
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.*
import com.piashcse.mapper.toProductResponse
import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.model.response.ProductResponse
import com.piashcse.model.response.SearchResponse
import com.piashcse.model.response.SearchSuggestionsResponse
import com.piashcse.model.response.SuggestionItem
import com.piashcse.utils.common.AppClock
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.common.emptyPage
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.NotFoundException
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

class ProductRepositoryImpl : ProductRepository {
    private fun requireSeller(userId: String): SellerDAO =
        try {
            requireSellerByUserId(userId)
        } catch (e: ForbiddenException) {
            throw NotFoundException(e.message ?: Message.Errors.SELLER_REQUIRED)
        }

    private fun generateSKU(name: String) =
        name.replace(Regex("[^a-zA-Z0-9]"), "").take(6).uppercase() +
            AppClock.now().toEpochMilli().toString().takeLast(6)

    private fun calcDiscountPct(
        price: BigDecimal,
        discountPrice: BigDecimal?,
    ): BigDecimal? =
        if (discountPrice != null && discountPrice < price) {
            Money.discountPercent(price, discountPrice)
        } else {
            null
        }

    private fun Query.applyProductFilters(filter: ProductWithFilterRequest): Query {
        filter.categoryId?.let { andWhere { ProductTable.categoryId eq it.entityID(ProductCategoryTable) } }
        filter.subCategoryId?.let { andWhere { ProductTable.subCategoryId eq it.entityID(ProductSubCategoryTable) } }
        filter.brandId?.let { andWhere { ProductTable.brandId eq it.entityID(BrandTable) } }
        filter.shopId?.let { andWhere { ProductTable.shopId eq it.entityID(ShopTable) } }
        filter.minPrice?.let { andWhere { ProductTable.price greaterEq Money.of(it) } }
        filter.maxPrice?.let { andWhere { ProductTable.price lessEq Money.of(it) } }
        filter.minRating?.let { andWhere { ProductTable.rating greaterEq BigDecimal.valueOf(it) } }
        filter.freeShipping?.let { andWhere { ProductTable.freeShipping eq it } }
        filter.onSale?.let { onSale ->
            andWhere {
                if (onSale) {
                    ProductTable.discountPrice.isNotNull()
                } else {
                    ProductTable.discountPrice.isNull()
                }
            }
        }
        filter.hotDeal?.let { andWhere { ProductTable.hotDeal eq it } }
        filter.featured?.let { andWhere { ProductTable.featured eq it } }
        if (filter.inStockOnly == true) {
            andWhere {
                (ProductTable.status eq ProductStatus.ACTIVE) and
                    (
                        ProductTable.id inSubQuery
                            InventoryTable.select(InventoryTable.productId).where {
                                InventoryTable.stockQuantity greater 0
                            }
                    )
            }
        }
        val sortOrder = if (filter.sortOrder?.lowercase() == "asc") SortOrder.ASC else SortOrder.DESC
        when (filter.sortBy?.lowercase()) {
            "price" -> orderBy(ProductTable.price to sortOrder)
            "name" -> orderBy(ProductTable.name to sortOrder)
            "best-selling" -> orderBy(ProductTable.totalSales to sortOrder)
            "top-rated" -> orderBy(ProductTable.rating to sortOrder)
            "relevance" -> applyRelevanceSort(sortOrder)
            else -> orderBy(ProductTable.createdAt to sortOrder)
        }
        return this
    }

    private fun toProductPaginatedResponse(
        query: Query,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ProductResponse> {
        val (totalCount, rows) = query.toPaginatedList(limit, offset) { it }
        val data =
            withPreloadedImages(rows) { row, images, stock ->
                val product = ProductDAO.wrapRow(row)
                product.toProductResponse(images[product.id.value], stockQuantity = stock[product.id.value] ?: 0)
            }
        return PaginatedResponse(data, PaginationMetadata(totalCount, limit, offset))
    }

    private fun withPreloadedImages(
        rows: List<ResultRow>,
        mapper: (ResultRow, Map<String, List<String>>, Map<String, Int>) -> ProductResponse,
    ): List<ProductResponse> {
        val productIds = rows.map { ProductDAO.wrapRow(it).id }
        val imagesMap = ProductImageDAO.imagesForProducts(productIds)
        val stockMap = stockByProductIds(productIds.map { it.value })
        return rows.map { mapper(it, imagesMap, stockMap) }
    }

    private fun Query.applyRelevanceSort(sortOrder: SortOrder): Query {
        return orderBy(ProductTable.totalSales to sortOrder)
            .orderBy(ProductTable.viewCount to SortOrder.DESC)
            .orderBy(ProductTable.discountPercentage to SortOrder.DESC)
    }

    override suspend fun createProduct(
        userId: String,
        shopId: String?,
        productRequest: ProductRequest,
    ): ProductResponse =
        query {
            val seller = requireSeller(userId)
            val resolvedShopId =
                shopId ?: seller.shopId?.value
                    ?: throw ValidationException(
                        "Create or get a shop approved before listing products",
                    )
            val shop = ShopDAO.findById(resolvedShopId) ?: resolvedShopId.throwNotFound("Shop")
            if (shop.userId.value != userId) throw ForbiddenException(Message.Products.NOT_SHOP_OWNER)
            if (shop.status != com.piashcse.constants.ShopStatus.APPROVED) {
                throw ValidationException("Shop must be approved before listing products")
            }

            ProductDAO.new {
                this.userId = userId.entityID(UserTable)
                this.shopId = resolvedShopId.entityID(ShopTable)
                categoryId = productRequest.categoryId.entityID(ProductCategoryTable)
                subCategoryId = productRequest.subCategoryId?.let { it.entityID(ProductSubCategoryTable) }
                brandId = productRequest.brandId?.let { it.entityID(BrandTable) }
                sku = productRequest.productCode?.takeIf { it.isNotBlank() } ?: generateSKU(productRequest.name)
                barcode = productRequest.barcode?.takeIf { it.isNotBlank() }
                weight = productRequest.weight?.let { BigDecimal.valueOf(it) }
                dimensions = productRequest.dimensions?.takeIf { it.isNotBlank() }
                minOrderQuantity = productRequest.minOrderQuantity.coerceAtLeast(1)
                name = productRequest.name
                description = productRequest.description
                price = Money.scale2(productRequest.price)
                discountPrice = productRequest.discountPrice?.let { Money.scale2(it) }
                discountPercentage = calcDiscountPct(productRequest.price, productRequest.discountPrice)
                videoLink = productRequest.videoLink
                // Merchandising flags are admin-curated, not seller self-service.
                hotDeal = false
                featured = false
                bestSeller = false
                newProduct = true
                freeShipping = productRequest.freeShipping
                isDigital = productRequest.isDigital
                digitalAssetUrl = productRequest.digitalAssetUrl?.takeIf { it.isNotBlank() }
                digitalFileName = productRequest.digitalFileName?.takeIf { it.isNotBlank() }
                status = productRequest.status ?: ProductStatus.ACTIVE
            }.let { product ->
                product.setImages(productRequest.images)
                if (!productRequest.isDigital) {
                    val warehouse =
                        WarehouseDAO.find {
                            (WarehouseTable.shopId eq resolvedShopId.entityID(ShopTable)) and
                                (WarehouseTable.isActive eq true)
                        }.firstOrNull()
                            ?: WarehouseDAO.new {
                                this.shopId = resolvedShopId.entityID(ShopTable)
                                name = "Default"
                                code = "DEFAULT"
                            }
                    InventoryDAO.new {
                        productId = product.id
                        this.shopId = resolvedShopId.entityID(ShopTable)
                        warehouseId = warehouse.id
                        stockQuantity = productRequest.stockQuantity
                        minimumStockLevel = 10
                        maximumStockLevel = 1000
                        status = InventoryStatus.fromStockLevel(productRequest.stockQuantity, 10)
                    }
                }
                product.toProductResponse(stockQuantity = productRequest.stockQuantity)
            }
        }

    private fun ProductDAO.applyUpdate(updateProduct: UpdateProductRequest) {
        categoryId = updateProduct.categoryId?.let { it.entityID(ProductCategoryTable) } ?: categoryId
        subCategoryId = updateProduct.subCategoryId?.let { it.entityID(ProductSubCategoryTable) } ?: subCategoryId
        brandId = updateProduct.brandId?.let { it.entityID(BrandTable) } ?: brandId
        name = updateProduct.name ?: name
        description = updateProduct.description ?: description
        updateProduct.productCode?.let { sku = it.takeIf { value -> value.isNotBlank() } ?: sku }
        updateProduct.barcode?.let { barcode = it.takeIf { value -> value.isNotBlank() } }
        updateProduct.weight?.let { weight = BigDecimal.valueOf(it) }
        updateProduct.dimensions?.let { dimensions = it.takeIf { value -> value.isNotBlank() } }
        updateProduct.minOrderQuantity?.let { minOrderQuantity = it.coerceAtLeast(1) }
        val effectivePrice = updateProduct.price?.let { Money.scale2(it) } ?: price
        val effectiveDiscount =
            when {
                updateProduct.clearDiscountPrice -> null
                updateProduct.discountPrice != null -> Money.scale2(updateProduct.discountPrice)
                else -> discountPrice
            }
        if (effectiveDiscount != null && effectiveDiscount >= effectivePrice) {
            throw ValidationException("Discount price must be lower than the regular price")
        }
        price = effectivePrice
        discountPrice = effectiveDiscount
        status = updateProduct.status ?: status
        discountPercentage = Money.discountPercent(price, discountPrice)
        videoLink = updateProduct.videoLink ?: videoLink
        // hotDeal/featured are admin-curated; seller updates cannot change them.
        freeShipping = updateProduct.freeShipping ?: freeShipping
        isDigital = updateProduct.isDigital ?: isDigital
        updateProduct.digitalAssetUrl?.let { digitalAssetUrl = it.takeIf { value -> value.isNotBlank() } }
        updateProduct.digitalFileName?.let { digitalFileName = it.takeIf { value -> value.isNotBlank() } }
        if (updateProduct.images.isNotEmpty()) setImages(updateProduct.images)
    }

    /** Legacy update path kept together for atomic validation. */
    override suspend fun updateProduct(
        userId: String,
        productId: String,
        updateProduct: UpdateProductRequest,
    ): ProductResponse =
        query {
            requireSeller(userId)
            val product = ProductDAO.findById(productId) ?: productId.throwNotFound("Product")
            product.verifyOwnership(userId, "product") { it.userId.value }

            product.applyUpdate(updateProduct)
            product.toProductResponse(
                stockQuantity = stockByProductIds(listOf(product.id.value))[product.id.value] ?: 0,
            )
        }

    override suspend fun getProducts(productQuery: ProductWithFilterRequest): PaginatedResponse<ProductResponse> =
        readQuery {
            toProductPaginatedResponse(
                ProductTable.selectAll().andWhere {
                    (ProductTable.status inList listOf(ProductStatus.ACTIVE, ProductStatus.OUT_OF_STOCK)) and
                        ProductTable.deletedAt.isNull() and
                        (ProductTable.shopId inSubQuery approvedShopIdsQuery())
                }.applyProductFilters(productQuery),
                productQuery.limit,
                productQuery.offset,
            )
        }

    private fun approvedShopIdsQuery() =
        ShopTable.select(ShopTable.id).where {
            ShopTable.status eq com.piashcse.constants.ShopStatus.APPROVED
        }

    override suspend fun getProductsByShop(
        shopId: String,
        productQuery: ProductWithFilterRequest,
    ): PaginatedResponse<ProductResponse> =
        query {
            val shop = ShopDAO.findById(shopId) ?: shopId.throwNotFound("Shop")
            if (shop.status != com.piashcse.constants.ShopStatus.APPROVED) {
                return@query emptyPage(productQuery.limit, productQuery.offset)
            }
            toProductPaginatedResponse(
                ProductTable.selectAll().andWhere {
                    (ProductTable.shopId eq shopId) and
                        (ProductTable.status inList listOf(ProductStatus.ACTIVE, ProductStatus.OUT_OF_STOCK)) and
                        ProductTable.deletedAt.isNull()
                }.applyProductFilters(productQuery),
                productQuery.limit,
                productQuery.offset,
            )
        }

    override suspend fun getProductsByUser(
        userId: String,
        productQuery: ProductWithFilterRequest,
    ): PaginatedResponse<ProductResponse> =
        query {
            toProductPaginatedResponse(
                ProductTable.selectAll().andWhere {
                    (ProductTable.userId eq userId) and
                        ProductTable.deletedAt.isNull()
                }.applyProductFilters(productQuery),
                productQuery.limit,
                productQuery.offset,
            )
        }

    override suspend fun getProductDetail(productId: String): ProductResponse =
        query {
            val product = ProductDAO.findById(productId) ?: productId.throwNotFound("ProductResponse")
            if (product.deletedAt != null) productId.throwNotFound("ProductResponse")
            product.shopId?.value?.let { shopId ->
                val shop = ShopDAO.findById(shopId) ?: productId.throwNotFound("ProductResponse")
                if (shop.status != com.piashcse.constants.ShopStatus.APPROVED) {
                    productId.throwNotFound("ProductResponse")
                }
            }
            val variantRows =
                ProductVariantDAO.find { ProductVariantTable.productId eq product.id }
                    .orderBy(ProductVariantTable.sortOrder to SortOrder.ASC)
                    .toList()
            product.toProductResponse(
                variants = variantResponses(variantRows, stockByVariantIds(variantRows.map { it.id.value })),
                stockQuantity = stockByProductIds(listOf(product.id.value))[product.id.value] ?: 0,
            )
        }

    override suspend fun incrementViewCount(productId: String) =
        query {
            ProductDAO.findById(productId)?.apply { viewCount = viewCount + 1 }
            Unit
        }

    override suspend fun deleteProduct(
        userId: String,
        productId: String,
    ): String =
        query {
            requireSeller(userId)
            val product = ProductDAO.findById(productId) ?: productId.throwNotFound("Product")
            product.verifyOwnership(userId, "product") { it.userId.value }
            product.softDelete()
            productId
        }

    override suspend fun deleteProductAsAdmin(productId: String): String =
        query {
            val product = ProductDAO.findById(productId) ?: productId.throwNotFound("ProductResponse")
            product.softDelete()
            productId
        }

    override suspend fun searchProduct(productQuery: ProductSearchRequest): SearchResponse =
        query {
            val useFuzzy = productQuery.useFuzzy != false && productQuery.name.length >= 3

            val (productIds, totalCount) = ProductSearchDao.queryIds(productQuery, useFuzzy)

            if (productIds.isEmpty()) {
                val facets = ProductSearchDao.facets(productQuery.name, productQuery, useFuzzy)
                return@query SearchResponse(
                    products = emptyList(),
                    metadata = PaginationMetadata(0, productQuery.limit, productQuery.offset),
                    facets = facets,
                )
            }

            val idEntities = productIds.map { it.entityID(ProductTable) }
            val rows = ProductTable.selectAll().andWhere { ProductTable.id inList idEntities }
            val rowsById = rows.associateBy { it[ProductTable.id].value }
            val orderedRows = productIds.mapNotNull { rowsById[it] }

            val products =
                withPreloadedImages(orderedRows) { row, images, stock ->
                    val product = ProductDAO.wrapRow(row)
                    product.toProductResponse(images[product.id.value], stockQuantity = stock[product.id.value] ?: 0)
                }

            val facets = ProductSearchDao.facets(productQuery.name, productQuery, useFuzzy)

            SearchResponse(
                products = products,
                metadata = PaginationMetadata(totalCount.toLong(), productQuery.limit, productQuery.offset),
                facets = facets,
            )
        }

    override suspend fun getSuggestions(
        query: String,
        limit: Int,
    ): SearchSuggestionsResponse =
        query {
            val q = query.trim()
            if (q.isBlank()) return@query SearchSuggestionsResponse(query, emptyList())
            val capped = limit.coerceIn(1, 10)
            val pattern = "%${q.lowercase()}%"

            val products =
                ProductTable.innerJoin(ShopTable)
                    .select(ProductTable.id, ProductTable.name)
                    .where {
                        (ProductTable.status eq ProductStatus.ACTIVE) and
                            ProductTable.deletedAt.isNull() and
                            (ShopTable.status eq ShopStatus.APPROVED) and
                            (ProductTable.name.lowerCase() like pattern)
                    }
                    .orderBy(ProductTable.createdAt to SortOrder.DESC)
                    .limit(capped)
                    .map { SuggestionItem(it[ProductTable.id].value, it[ProductTable.name], "PRODUCT") }

            val categories =
                ProductCategoryTable.select(ProductCategoryTable.id, ProductCategoryTable.name)
                    .where { ProductCategoryTable.name.lowerCase() like pattern }
                    .orderBy(ProductCategoryTable.name to SortOrder.ASC)
                    .limit(capped)
                    .map { SuggestionItem(it[ProductCategoryTable.id].value, it[ProductCategoryTable.name], "CATEGORY") }

            val brands =
                BrandTable.select(BrandTable.id, BrandTable.name)
                    .where { BrandTable.name.lowerCase() like pattern }
                    .orderBy(BrandTable.name to SortOrder.ASC)
                    .limit(capped)
                    .map { SuggestionItem(it[BrandTable.id].value, it[BrandTable.name], "BRAND") }

            SearchSuggestionsResponse(q, products + categories + brands)
        }

    override suspend fun getProductsByCategory(categoryId: String) =
        queryProducts(ProductTable.categoryId eq categoryId, ProductTable.createdAt to SortOrder.DESC)

    override suspend fun getFeaturedProducts() = queryProducts(ProductTable.featured eq true, ProductTable.createdAt to SortOrder.DESC)

    override suspend fun getBestSellingProducts() =
        queryProducts(ProductTable.bestSeller eq true, ProductTable.totalSales to SortOrder.DESC, limit = 10)

    override suspend fun getHotDealProducts() =
        queryProducts(ProductTable.hotDeal eq true, ProductTable.discountPercentage to SortOrder.DESC, limit = 10)

    private suspend fun queryProducts(
        condition: Op<Boolean>,
        orderBy: Pair<Column<*>, SortOrder>,
        limit: Int? = null,
    ): PaginatedResponse<ProductResponse> =
        query {
            val statusCondition =
                condition and
                    (ProductTable.status eq ProductStatus.ACTIVE) and
                    ProductTable.deletedAt.isNull() and
                    (ProductTable.shopId inSubQuery approvedShopIdsQuery())
            val count = ProductDAO.count(statusCondition)
            val products =
                ProductDAO.find(statusCondition)
                    .orderBy(orderBy)
                    .also { if (limit != null) it.limit(limit) }
                    .toList()
            val imagesMap = if (products.isNotEmpty()) ProductImageDAO.imagesForProducts(products.map { it.id }) else emptyMap()
            val stockMap = stockByProductIds(products.map { it.id.value })
            val data =
                products.map {
                    it.toProductResponse(imagesMap[it.id.value], stockQuantity = stockMap[it.id.value] ?: 0)
                }
            PaginatedResponse(data, PaginationMetadata(count, limit ?: data.size, 0))
        }
}
