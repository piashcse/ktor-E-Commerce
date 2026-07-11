package com.piashcse.feature.product

import com.piashcse.constants.InventoryStatus
import com.piashcse.constants.Message
import com.piashcse.constants.ProductStatus
import com.piashcse.database.entities.*
import com.piashcse.mapper.toProductResponse
import com.piashcse.model.request.ProductRequest
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.request.ProductWithFilterRequest
import com.piashcse.model.request.UpdateProductRequest
import com.piashcse.model.response.FacetCount
import com.piashcse.model.response.ProductResponse
import com.piashcse.model.response.SearchFacets
import com.piashcse.model.response.SearchResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.common.PaginationMetadata
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.NotFoundException
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Connection
import java.sql.PreparedStatement

class ProductRepositoryImpl : ProductRepository {

    private fun requireSeller(userId: String) {
        if (findSellerByUserId(userId) == null) throw NotFoundException(Message.Errors.SELLER_REQUIRED)
    }

    private fun generateSKU(name: String) =
        name.replace(Regex("[^a-zA-Z0-9]"), "").take(6).uppercase() +
            System.currentTimeMillis().toString().takeLast(6)

    private fun calcDiscountPct(price: Double, discountPrice: Double?): BigDecimal? =
        if (discountPrice != null && discountPrice < price)
            BigDecimal.valueOf((price - discountPrice) / price * 100).setScale(2, RoundingMode.HALF_UP)
        else null

    private fun Query.applyProductFilters(filter: ProductWithFilterRequest): Query {
        filter.categoryId?.let { andWhere { ProductTable.categoryId eq EntityID(it, ProductCategoryTable) } }
        filter.subCategoryId?.let { andWhere { ProductTable.subCategoryId eq EntityID(it, ProductSubCategoryTable) } }
        filter.brandId?.let { andWhere { ProductTable.brandId eq EntityID(it, BrandTable) } }
        filter.minPrice?.let { andWhere { ProductTable.price greaterEq BigDecimal.valueOf(it) } }
        filter.maxPrice?.let { andWhere { ProductTable.price lessEq BigDecimal.valueOf(it) } }
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

    private fun toProductPaginatedResponse(query: Query, limit: Int, offset: Int): PaginatedResponse<ProductResponse> {
        val (totalCount, rows) = query.toPaginatedList(limit, offset) { it }
        val data = withPreloadedImages(rows) { row, images ->
            ProductDAO.wrapRow(row).toProductResponse(images[ProductDAO.wrapRow(row).id.value])
        }
        return PaginatedResponse(data, PaginationMetadata(totalCount, limit, offset))
    }

    private fun withPreloadedImages(
        rows: List<ResultRow>,
        mapper: (ResultRow, Map<String, List<String>>) -> ProductResponse,
    ): List<ProductResponse> {
        val productIds = rows.map { ProductDAO.wrapRow(it).id }
        val imagesMap = ProductImageDAO.imagesForProducts(productIds)
        return rows.map { mapper(it, imagesMap) }
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
    ): ProductResponse = query {
        requireSeller(userId)
        if (shopId != null) {
            val shop = ShopDAO.findById(shopId) ?: shopId.throwNotFound("Shop")
            if (shop.userId.value != userId) throw ForbiddenException(Message.Products.NOT_SHOP_OWNER)
        }

        ProductDAO.new {
            this.userId = EntityID(userId, UserTable)
            this.shopId = shopId?.let { EntityID(shopId, ShopTable) }
            categoryId = EntityID(productRequest.categoryId, ProductCategoryTable)
            subCategoryId = productRequest.subCategoryId?.let { EntityID(it, ProductSubCategoryTable) }
            brandId = productRequest.brandId?.let { EntityID(it, BrandTable) }
            sku = generateSKU(productRequest.name)
            name = productRequest.name
            description = productRequest.description
            price = BigDecimal.valueOf(productRequest.price)
            discountPrice = productRequest.discountPrice?.let { BigDecimal.valueOf(it) }
            discountPercentage = calcDiscountPct(productRequest.price, productRequest.discountPrice)
            videoLink = productRequest.videoLink
            hotDeal = productRequest.hotDeal
            featured = productRequest.featured
            bestSeller = false
            newProduct = true
            freeShipping = productRequest.freeShipping ?: false
            status = ProductStatus.ACTIVE
        }.let { product ->
            product.setImages(productRequest.images)
            if (shopId != null) {
                InventoryDAO.new {
                    productId = product.id
                    this.shopId = EntityID(shopId, ShopTable)
                    stockQuantity = productRequest.stockQuantity
                    minimumStockLevel = 10
                    maximumStockLevel = 1000
                    status = InventoryStatus.fromStockLevel(productRequest.stockQuantity, 10)
                }
            }
            product.toProductResponse()
        }
    }

    override suspend fun updateProduct(
        userId: String,
        productId: String,
        updateProduct: UpdateProductRequest,
    ): ProductResponse = query {
        requireSeller(userId)
        val product = ProductDAO.findById(productId) ?: productId.throwNotFound("Product")
        product.verifyOwnership(userId, "product") { it.userId.value }

        product.apply {
            categoryId = updateProduct.categoryId?.let { EntityID(it, ProductCategoryTable) } ?: categoryId
            subCategoryId = updateProduct.subCategoryId?.let { EntityID(it, ProductSubCategoryTable) } ?: subCategoryId
            brandId = updateProduct.brandId?.let { EntityID(it, BrandTable) } ?: brandId
            name = updateProduct.name ?: name
            description = updateProduct.description ?: description
            price = updateProduct.price?.let { BigDecimal.valueOf(it) } ?: price
            discountPrice = updateProduct.discountPrice?.let { BigDecimal.valueOf(it) } ?: discountPrice
            videoLink = updateProduct.videoLink ?: videoLink
            hotDeal = updateProduct.hotDeal ?: hotDeal
            featured = updateProduct.featured ?: featured
            freeShipping = updateProduct.freeShipping ?: freeShipping
            if (updateProduct.images.isNotEmpty()) setImages(updateProduct.images)
        }.toProductResponse()
    }

    override suspend fun getProducts(filter: ProductWithFilterRequest): PaginatedResponse<ProductResponse> = query {
        toProductPaginatedResponse(ProductTable.selectAll().andWhere { ProductTable.status eq ProductStatus.ACTIVE }.applyProductFilters(filter), filter.limit, filter.offset)
    }

    override suspend fun getProductsByShop(shopId: String, filter: ProductWithFilterRequest): PaginatedResponse<ProductResponse> = query {
        toProductPaginatedResponse(ProductTable.selectAll().andWhere {
            (ProductTable.shopId eq shopId) and (ProductTable.status eq ProductStatus.ACTIVE)
        }.applyProductFilters(filter), filter.limit, filter.offset)
    }

    override suspend fun getProductsByUser(userId: String, filter: ProductWithFilterRequest): PaginatedResponse<ProductResponse> = query {
        toProductPaginatedResponse(ProductTable.selectAll().andWhere {
            (ProductTable.userId eq userId) and (ProductTable.status eq ProductStatus.ACTIVE)
        }.applyProductFilters(filter), filter.limit, filter.offset)
    }

    override suspend fun getProductDetail(productId: String): ProductResponse = query {
        ProductDAO.findById(productId)?.toProductResponse() ?: productId.throwNotFound("ProductResponse")
    }

    override suspend fun incrementViewCount(productId: String) = query {
        ProductDAO.findById(productId)?.apply { viewCount = viewCount + 1 }
        Unit
    }

    override suspend fun deleteProduct(userId: String, productId: String): String = query {
        requireSeller(userId)
        val product = ProductDAO.findById(productId) ?: productId.throwNotFound("Product")
        product.verifyOwnership(userId, "product") { it.userId.value }
        product.delete()
        productId
    }

    override suspend fun deleteProductAsAdmin(productId: String): String = query {
        val product = ProductDAO.findById(productId) ?: productId.throwNotFound("ProductResponse")
        product.delete()
        productId
    }

    override suspend fun searchProduct(searchRequest: ProductSearchRequest): SearchResponse = query {
        val useFuzzy = searchRequest.useFuzzy != false && searchRequest.name.length >= 3

        val (productIds, totalCount) = querySearchIds(searchRequest, useFuzzy)

        if (productIds.isEmpty()) {
            val facets = buildFacets(searchRequest.name, searchRequest, useFuzzy)
            return@query SearchResponse(
                products = emptyList(),
                metadata = PaginationMetadata(0, searchRequest.limit, searchRequest.offset),
                facets = facets,
            )
        }

        val idEntities = productIds.map { EntityID(it, ProductTable) }
        val rows = ProductTable.selectAll().andWhere { ProductTable.id inList idEntities }
        val rowsById = rows.associateBy { it[ProductTable.id].value }
        val orderedRows = productIds.mapNotNull { rowsById[it] }

        val products = withPreloadedImages(orderedRows) { row, images ->
            ProductDAO.wrapRow(row).toProductResponse(images[ProductDAO.wrapRow(row).id.value])
        }

        val facets = buildFacets(searchRequest.name, searchRequest, useFuzzy)

        SearchResponse(
            products = products,
            metadata = PaginationMetadata(totalCount.toLong(), searchRequest.limit, searchRequest.offset),
            facets = facets,
        )
    }

    private fun querySearchIds(request: ProductSearchRequest, useTrigram: Boolean): Pair<List<String>, Int> {
        val dir = if (request.sortOrder?.lowercase() == "asc") "ASC" else "DESC"
        val threshold = 0.15

        val whereClauses = mutableListOf("p.status = ?")
        val whereParams = mutableListOf<Any>(ProductStatus.ACTIVE.name)

        if (useTrigram) {
            whereClauses.add("similarity(p.name, ?) > ?")
            whereParams.add(request.name)
            whereParams.add(threshold)
        } else {
            whereClauses.add("p.name ILIKE ?")
            whereParams.add("%${request.name}%")
        }

        if (!request.categoryId.isNullOrEmpty()) {
            whereClauses.add("p.category_id = ?")
            whereParams.add(request.categoryId)
        }
        if (!request.brandId.isNullOrEmpty()) {
            whereClauses.add("p.brand_id = ?")
            whereParams.add(request.brandId)
        }
        if (request.minPrice != null) {
            whereClauses.add("p.price >= ?")
            whereParams.add(request.minPrice)
        }
        if (request.maxPrice != null) {
            whereClauses.add("p.price <= ?")
            whereParams.add(request.maxPrice)
        }

        val whereSql = whereClauses.joinToString(" AND ")

        val isRelevance = request.sortBy?.lowercase() !in listOf("price", "newest", "best-selling", "top-rated")
        val orderClause = when (request.sortBy?.lowercase()) {
            "price" -> "p.price $dir"
            "newest" -> "p.created_at $dir"
            "best-selling" -> "p.total_sales $dir"
            "top-rated" -> "p.rating $dir"
            else -> {
                val composite = "((COALESCE(p.total_sales, 0) * 0.4) + (COALESCE(p.view_count, 0) * 0.3) + (COALESCE(p.discount_percentage, 0) * 0.3)) $dir"
                if (useTrigram) "similarity(p.name, ?) DESC, $composite" else composite
            }
        }
        val orderParams = if (useTrigram && isRelevance) listOf<Any>(request.name) else emptyList<Any>()

        val countSql = "SELECT COUNT(*) FROM product p WHERE $whereSql"
        val dataSql = "SELECT p.id FROM product p WHERE $whereSql ORDER BY $orderClause LIMIT ? OFFSET ?"

        val conn = TransactionManager.current().connection.connection as Connection
        var totalCount = 0

        fun setParams(stmt: PreparedStatement, params: List<Any>, startIdx: Int = 1) {
            var idx = startIdx
            for (p in params) {
                when (p) {
                    is Int -> stmt.setInt(idx++, p)
                    is String -> stmt.setString(idx++, p)
                    is Double -> stmt.setDouble(idx++, p)
                }
            }
        }

        conn.prepareStatement(countSql).use { stmt ->
            setParams(stmt, whereParams)
            val rs = stmt.executeQuery()
            if (rs.next()) totalCount = rs.getInt(1)
        }

        val ids = mutableListOf<String>()
        conn.prepareStatement(dataSql).use { stmt ->
            setParams(stmt, whereParams)
            setParams(stmt, orderParams, whereParams.size + 1)
            stmt.setInt(whereParams.size + orderParams.size + 1, request.limit)
            stmt.setInt(whereParams.size + orderParams.size + 2, request.offset)
            val rs = stmt.executeQuery()
            while (rs.next()) ids.add(rs.getString("id"))
        }

        return ids to totalCount
    }

    private fun buildFacets(
        term: String,
        request: ProductSearchRequest,
        useTrigram: Boolean,
    ): SearchFacets {
        val statusName = ProductStatus.ACTIVE.name

        val matchClause = if (useTrigram && term.length >= 3) {
            "similarity(p.name, ?) > ?"
        } else {
            "p.name LIKE ?"
        }
        val matchParams = if (useTrigram && term.length >= 3) {
            listOf<Any>(term, 0.15)
        } else {
            listOf<Any>("%${term.replace("'", "''")}%")
        }

        val extraClauses = mutableListOf<String>()
        val extraParams = mutableListOf<Any>()
        if (!request.categoryId.isNullOrEmpty()) {
            extraClauses.add("p.category_id = ?")
            extraParams.add(request.categoryId)
        }
        if (!request.brandId.isNullOrEmpty()) {
            extraClauses.add("p.brand_id = ?")
            extraParams.add(request.brandId)
        }
        if (request.minPrice != null) {
            extraClauses.add("p.price >= ?")
            extraParams.add(request.minPrice)
        }
        if (request.maxPrice != null) {
            extraClauses.add("p.price <= ?")
            extraParams.add(request.maxPrice)
        }

        val extraSql = if (extraClauses.isNotEmpty()) " AND ${extraClauses.joinToString(" AND ")}" else ""
        val baseWhere = "p.status = '$statusName' AND $matchClause$extraSql"

        val categorySql = """
            SELECT c.id, c.name, COUNT(p.id) as cnt
            FROM product p
            JOIN category c ON c.id = p.category_id
            WHERE $baseWhere
            GROUP BY c.id, c.name
            ORDER BY cnt DESC
            LIMIT 20
        """.trimIndent()
        val brandSql = """
            SELECT b.id, b.name, COUNT(p.id) as cnt
            FROM product p
            JOIN brand b ON b.id = p.brand_id
            WHERE $baseWhere AND p.brand_id IS NOT NULL
            GROUP BY b.id, b.name
            ORDER BY cnt DESC
            LIMIT 20
        """.trimIndent()

        val categories = mutableListOf<FacetCount>()
        val brands = mutableListOf<FacetCount>()
        val conn = TransactionManager.current().connection.connection as Connection
        val allParams = matchParams + extraParams

        fun executeFacetQuery(sql: String): List<FacetCount> {
            val results = mutableListOf<FacetCount>()
            conn.prepareStatement(sql).use { stmt ->
                var idx = 1
                for (p in allParams) {
                    when (p) {
                        is Int -> stmt.setInt(idx++, p)
                        is String -> stmt.setString(idx++, p)
                        is Double -> stmt.setDouble(idx++, p)
                    }
                }
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        results.add(FacetCount(rs.getString("id"), rs.getString("name"), rs.getLong("cnt")))
                    }
                }
            }
            return results
        }

        return SearchFacets(executeFacetQuery(categorySql), executeFacetQuery(brandSql))
    }

    override suspend fun getProductsByCategory(categoryId: String): PaginatedResponse<ProductResponse> = query {
        val condition: Op<Boolean> = (ProductTable.categoryId eq categoryId) and (ProductTable.status eq ProductStatus.ACTIVE)
        val count = ProductDAO.count(condition)
        val products = ProductDAO.find(condition).orderBy(ProductTable.createdAt to SortOrder.DESC).toList()
        val imagesMap = if (products.isNotEmpty()) ProductImageDAO.imagesForProducts(products.map { it.id }) else emptyMap()
        val data = products.map { it.toProductResponse(imagesMap[it.id.value]) }
        PaginatedResponse(data, PaginationMetadata(count, data.size, 0))
    }

    override suspend fun getFeaturedProducts(): PaginatedResponse<ProductResponse> = query {
        val condition: Op<Boolean> = (ProductTable.featured eq true) and (ProductTable.status eq ProductStatus.ACTIVE)
        val count = ProductDAO.count(condition)
        val products = ProductDAO.find(condition).orderBy(ProductTable.createdAt to SortOrder.DESC).toList()
        val imagesMap = if (products.isNotEmpty()) ProductImageDAO.imagesForProducts(products.map { it.id }) else emptyMap()
        val data = products.map { it.toProductResponse(imagesMap[it.id.value]) }
        PaginatedResponse(data, PaginationMetadata(count, data.size, 0))
    }

    override suspend fun getBestSellingProducts(): PaginatedResponse<ProductResponse> = query {
        val condition: Op<Boolean> = (ProductTable.bestSeller eq true) and (ProductTable.status eq ProductStatus.ACTIVE)
        val count = ProductDAO.count(condition)
        val products = ProductDAO.find(condition).orderBy(ProductTable.totalSales to SortOrder.DESC).limit(10).toList()
        val imagesMap = if (products.isNotEmpty()) ProductImageDAO.imagesForProducts(products.map { it.id }) else emptyMap()
        val data = products.map { it.toProductResponse(imagesMap[it.id.value]) }
        PaginatedResponse(data, PaginationMetadata(count, data.size, 0))
    }

    override suspend fun getHotDealProducts(): PaginatedResponse<ProductResponse> = query {
        val condition: Op<Boolean> = (ProductTable.hotDeal eq true) and (ProductTable.status eq ProductStatus.ACTIVE)
        val count = ProductDAO.count(condition)
        val products = ProductDAO.find(condition).orderBy(ProductTable.discountPercentage to SortOrder.DESC).limit(10).toList()
        val imagesMap = if (products.isNotEmpty()) ProductImageDAO.imagesForProducts(products.map { it.id }) else emptyMap()
        val data = products.map { it.toProductResponse(imagesMap[it.id.value]) }
        PaginatedResponse(data, PaginationMetadata(count, data.size, 0))
    }
}
