package com.piashcse.feature.product

import com.piashcse.constants.ProductStatus
import com.piashcse.model.request.ProductSearchRequest
import com.piashcse.model.response.FacetCount
import com.piashcse.model.response.SearchFacets
import com.piashcse.utils.db.bindParams
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.Connection

/**
 * PostgreSQL full-text/trigram search and facet queries.
 *
 * Kept out of the Exposed repository so raw JDBC stays isolated and the
 * search SQL has a single place to evolve.
 */
internal object ProductSearchDao {
    private const val RELEVANCE_FORMULA =
        "((COALESCE(p.total_sales, 0) * 0.4) + (COALESCE(p.view_count, 0) * 0.3) + " +
            "(COALESCE(p.discount_percentage, 0) * 0.3)) %s"

    fun queryIds(
        request: ProductSearchRequest,
        useTrigram: Boolean,
    ): Pair<List<String>, Int> {
        val dir = if (request.sortOrder?.lowercase() == "asc") "ASC" else "DESC"
        val threshold = 0.15

        // Sold-out products stay discoverable so shoppers can request a restock
        // alert; `inStockOnly` narrows the listing to purchasable rows.
        val whereClauses = mutableListOf<String>()
        val whereParams = mutableListOf<Any>()
        if (request.inStockOnly == true) {
            whereClauses.add("p.status = ?")
            whereParams.add(ProductStatus.ACTIVE.name)
            whereClauses.add(
                "EXISTS (SELECT 1 FROM inventory i WHERE i.product_id = p.id AND i.stock_quantity > 0)",
            )
        } else {
            whereClauses.add("p.status IN (?, ?)")
            whereParams.add(ProductStatus.ACTIVE.name)
            whereParams.add(ProductStatus.OUT_OF_STOCK.name)
        }
        whereClauses.add("p.deleted_at IS NULL")
        whereClauses.add("p.shop_id IN (SELECT id FROM shop WHERE status = 'APPROVED')")

        if (useTrigram) {
            whereClauses.add(
                "(similarity(p.name, ?) > ? OR similarity(COALESCE(p.description, ''), ?) > ? " +
                    "OR p.search_vector @@ websearch_to_tsquery('english', ?))",
            )
            whereParams.add(request.name)
            whereParams.add(threshold)
            whereParams.add(request.name)
            whereParams.add(threshold)
            whereParams.add(request.name)
        } else {
            whereClauses.add("(p.name ILIKE ? OR p.description ILIKE ? OR p.search_vector @@ websearch_to_tsquery('english', ?))")
            whereParams.add("%${request.name}%")
            whereParams.add("%${request.name}%")
            whereParams.add(request.name)
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
        if (request.minRating != null) {
            whereClauses.add("p.rating >= ?")
            whereParams.add(request.minRating)
        }
        val whereSql = whereClauses.joinToString(" AND ")

        val isRelevance = request.sortBy?.lowercase() !in listOf("price", "newest", "best-selling", "top-rated")
        val orderClause =
            when (request.sortBy?.lowercase()) {
                "price" -> "p.price $dir"
                "newest" -> "p.created_at $dir"
                "best-selling" -> "p.total_sales $dir"
                "top-rated" -> "p.rating $dir"
                else -> {
                    val composite = RELEVANCE_FORMULA.format(dir)
                    if (useTrigram) {
                        "ts_rank_cd(p.search_vector, websearch_to_tsquery('english', ?)) DESC, " +
                            "GREATEST(similarity(p.name, ?), similarity(COALESCE(p.description, ''), ?)) DESC, $composite"
                    } else {
                        "ts_rank_cd(p.search_vector, websearch_to_tsquery('english', ?)) DESC, $composite"
                    }
                }
            }
        val orderParams =
            if (!isRelevance) {
                emptyList<Any>()
            } else if (useTrigram) {
                listOf<Any>(request.name, request.name, request.name)
            } else {
                listOf<Any>(request.name)
            }

        val countSql = "SELECT COUNT(*) FROM product p WHERE $whereSql"
        val dataSql = "SELECT p.id FROM product p WHERE $whereSql ORDER BY $orderClause LIMIT ? OFFSET ?"

        val conn = TransactionManager.current().connection.connection as Connection
        var totalCount = 0

        conn.prepareStatement(countSql).use { stmt ->
            stmt.bindParams(whereParams)
            val rs = stmt.executeQuery()
            if (rs.next()) totalCount = rs.getInt(1)
        }

        val ids = mutableListOf<String>()
        conn.prepareStatement(dataSql).use { stmt ->
            var nextIdx = stmt.bindParams(whereParams)
            nextIdx = stmt.bindParams(orderParams, nextIdx)
            stmt.setInt(whereParams.size + orderParams.size + 1, request.limit)
            stmt.setInt(whereParams.size + orderParams.size + 2, request.offset)
            val rs = stmt.executeQuery()
            while (rs.next()) ids.add(rs.getString("id"))
        }

        return ids to totalCount
    }

    fun facets(
        term: String,
        request: ProductSearchRequest,
        useTrigram: Boolean,
    ): SearchFacets {
        val statusName = ProductStatus.ACTIVE.name

        val matchClause =
            if (useTrigram && term.length >= 3) {
                "(similarity(p.name, ?) > ? OR similarity(COALESCE(p.description, ''), ?) > ? " +
                    "OR p.search_vector @@ websearch_to_tsquery('english', ?))"
            } else {
                "(p.name ILIKE ? OR p.description ILIKE ? OR p.search_vector @@ websearch_to_tsquery('english', ?))"
            }
        val matchParams =
            if (useTrigram && term.length >= 3) {
                listOf<Any>(term, 0.15, term, 0.15, term)
            } else {
                listOf<Any>("%$term%", "%$term%", term)
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
        if (request.minRating != null) {
            extraClauses.add("p.rating >= ?")
            extraParams.add(request.minRating)
        }
        if (request.inStockOnly == true) {
            extraClauses.add("EXISTS (SELECT 1 FROM inventory i WHERE i.product_id = p.id AND i.stock_quantity > 0)")
        }

        val extraSql = if (extraClauses.isNotEmpty()) " AND ${extraClauses.joinToString(" AND ")}" else ""
        val statusSql =
            if (request.inStockOnly == true) {
                "p.status = '$statusName'"
            } else {
                "p.status IN ('$statusName', '${ProductStatus.OUT_OF_STOCK.name}')"
            }
        val baseWhere =
            "$statusSql AND p.deleted_at IS NULL " +
                "AND p.shop_id IN (SELECT id FROM shop WHERE status = 'APPROVED') AND $matchClause$extraSql"

        val categorySql =
            """
            SELECT c.id, c.name, COUNT(p.id) as cnt
            FROM product p
            JOIN category c ON c.id = p.category_id
            WHERE $baseWhere
            GROUP BY c.id, c.name
            ORDER BY cnt DESC
            LIMIT 20
            """.trimIndent()
        val brandSql =
            """
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
                stmt.bindParams(allParams)
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
}
