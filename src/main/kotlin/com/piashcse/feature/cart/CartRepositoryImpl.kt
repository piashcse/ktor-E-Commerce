package com.piashcse.feature.cart

import com.piashcse.constants.Message
import com.piashcse.database.entities.*
import com.piashcse.feature.config.StorefrontSettings
import com.piashcse.feature.tax.TaxService
import com.piashcse.feature.tax.TaxTotals
import com.piashcse.mapper.toCartItemSummary
import com.piashcse.mapper.toCartResponse
import com.piashcse.mapper.toProductResponse
import com.piashcse.model.response.CartItemSummary
import com.piashcse.model.response.CartSummaryResponse
import com.piashcse.model.response.ProductResponse
import com.piashcse.service.PricingService
import com.piashcse.utils.common.Money
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal

class CartRepositoryImpl : CartRepository {
    private val taxService = TaxService()

    private fun requireCartParams(
        userId: String,
        productId: String,
        quantity: Int? = null,
    ) {
        userId.requireNotBlank("User ID")
        productId.requireNotBlank("Product ID")
        if (quantity != null && quantity <= 0) throw ValidationException(Message.Validation.notPositive("Quantity"))
    }

    private fun variantOp(variantId: String?) =
        if (variantId == null) {
            CartItemTable.variantId.isNull()
        } else {
            CartItemTable.variantId eq variantId.entityID(ProductVariantTable)
        }

    override suspend fun createCart(
        userId: String,
        productId: String,
        quantity: Int,
        variantId: String?,
    ): Cart =
        query {
            requireCartParams(userId, productId, quantity)

            // Advisory stock check only: read-then-write here is inherently racy (TOCTOU).
            // The authoritative check runs at order creation with a row-locked
            // effectiveStock(forUpdate = true) read, which re-validates before decrementing.
            val (product, variant) = requireCartSelection(productId, quantity, variantId = variantId)

            val existing =
                CartItemDAO.find {
                    (CartItemTable.userId eq userId) and
                        (CartItemTable.productId eq productId) and variantOp(variantId)
                }.singleOrNull()
            existing?.let { item ->
                // Merge duplicate adds instead of rejecting them, then re-check stock
                // against the combined quantity.
                val merged = item.quantity + quantity
                requireProductWithStock(productId, merged, variantId = variantId)
                item.quantity = merged
                return@query item.toCartResponse(product.toProductResponse(), variant?.toResponse())
            }

            try {
                CartItemDAO.new {
                    this.userId = userId.entityID(UserTable)
                    this.productId = productId.entityID(ProductTable)
                    this.variantId = variant?.id
                    this.quantity = quantity
                }.toCartResponse(product.toProductResponse(), variant?.toResponse())
            } catch (e: Exception) {
                // Unique constraint race: concurrent adds → merge into the winner.
                if ((e.message ?: "").contains("duplicate", ignoreCase = true)) {
                    val winner =
                        CartItemDAO.find {
                            (CartItemTable.userId eq userId) and
                                (CartItemTable.productId eq productId) and variantOp(variantId)
                        }.singleOrNull() ?: throw e
                    winner.quantity += quantity
                    winner.toCartResponse(product.toProductResponse(), variant?.toResponse())
                } else {
                    throw e
                }
            }
        }

    override suspend fun getCartItems(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<Cart> =
        query {
            val base = CartItemTable.selectAll().andWhere { CartItemTable.userId eq userId }
            base.paginateWithPreload(limit, offset, rowMapper = { it }) { rows ->
                val productIds = rows.map { it[CartItemTable.productId] }
                val products =
                    if (productIds.isNotEmpty()) {
                        ProductDAO.find { ProductTable.id inList productIds }.associateBy { it.id.value }
                    } else {
                        emptyMap()
                    }
                val imagesMap =
                    if (products.isNotEmpty()) {
                        ProductImageDAO.imagesForProducts(products.keys.map { it.entityID(ProductTable) })
                    } else {
                        emptyMap()
                    }
                val variantIds = rows.mapNotNull { it[CartItemTable.variantId]?.value }.distinct()
                val variants =
                    if (variantIds.isNotEmpty()) {
                        variantResponses(
                            ProductVariantDAO.find {
                                ProductVariantTable.id inList variantIds.map { it.entityID(ProductVariantTable) }
                            }.toList(),
                        ).associateBy { it.id }
                    } else {
                        emptyMap()
                    }
                rows.map { row ->
                    val product =
                        products[row[CartItemTable.productId].value]
                            ?: row[CartItemTable.productId].value.throwNotFound("Product")
                    val item = CartItemDAO.wrapRow(row)
                    val variant = item.variantId?.value?.let { variants[it] }
                    item.toCartResponse(
                        product.toProductResponse(imagesMap[product.id.value]),
                        variant,
                    )
                }
            }
        }

    override suspend fun updateCartQuantity(
        userId: String,
        productId: String,
        quantity: Int,
        variantId: String?,
    ): Cart? =
        query {
            userId.requireNotBlank("User ID")
            productId.requireNotBlank("Product ID")

            val cartItem = requireCartItem(userId, productId, variantId)

            // Single non-positive path: quantity <= 0 deletes the line item (no throw branch).
            if (quantity <= 0) {
                cartItem.delete()
                return@query null
            }

            // Advisory stock check only: read-then-write here is inherently racy (TOCTOU) —
            // concurrent checkouts can oversell between this read and order placement.
            // The authoritative check runs at order creation with a row-locked
            // effectiveStock(forUpdate = true) read, which re-validates before decrementing.
            val (product, variant) =
                requireCartSelection(cartItem.productId.value, quantity, variantId = cartItem.variantId?.value)
            cartItem.quantity = quantity

            cartItem.toCartResponse(product.toProductResponse(), variant?.toResponse())
        }

    override suspend fun removeCartItem(
        userId: String,
        productId: String,
        variantId: String?,
    ): ProductResponse =
        query {
            requireCartParams(userId, productId)

            val cartItem = requireCartItem(userId, productId, variantId)

            val product = requireProduct(cartItem.productId.value)
            cartItem.delete()
            product.toProductResponse()
        }

    override suspend fun clearCart(userId: String): Boolean =
        query {
            userId.requireNotBlank("User ID")
            CartItemTable.deleteWhere { CartItemTable.userId eq userId }
            true
        }

    override suspend fun getCartSummary(userId: String): CartSummaryResponse =
        query {
            val catalog = loadCartCatalog(userId)

            val lines = catalog.items.map { Money.of(it.price) to it.quantity }
            val subtotal = PricingService.subtotal(lines)
            // Use the same tax engine as checkout, keyed to the user's default
            // address when available. With no address it falls back to the
            // configured default tax rule instead of a second hardcoded rate.
            val address =
                ShippingAddressDAO.find { ShippingAddressTable.userId eq userId.entityID(UserTable) }
                    .orderBy(ShippingAddressTable.isDefault to SortOrder.DESC)
                    .firstOrNull()
            var taxTotals = TaxTotals()
            catalog.cartItems.forEach { cartItem ->
                val product = catalog.products[cartItem.productId.value] ?: return@forEach
                val variant = cartItem.variantId?.value?.let { catalog.variantsById[it] }
                val unitPrice = PricingService.unitPriceUsd(product, variant)
                val lineTotal = Money.unitTotal(unitPrice, cartItem.quantity)
                val quote =
                    taxService.resolve(
                        country = address?.country.orEmpty(),
                        region = address?.state,
                        categoryId = product.categoryId.value,
                        taxClassId = product.taxClassId?.value,
                        userId = userId,
                    )
                taxTotals = taxTotals.add(lineTotal, quote)
            }
            val taxTotalsNormalized = taxTotals.normalized()

            // Shipping estimate: cheapest method × number of shops that still
            // need shipping, waived once the platform threshold is reached.
            val physicalShops =
                catalog.products.values
                    .filter { !it.isDigital }
                    .groupBy { it.shopId?.value }
                    .count { (_, shopProducts) -> shopProducts.any { !it.freeShipping } }
            val cartShopIds = catalog.products.values.mapNotNull { it.shopId?.value }.distinct()
            val cheapestMethod =
                ShippingMethodDAO.all()
                    .filter { method ->
                        method.shopId == null || method.shopId?.value in cartShopIds
                    }
                    .minByOrNull { it.price }
            val freeShipping = StorefrontSettings.freeShippingReached(subtotal)
            val shippingUsd =
                if (freeShipping || physicalShops == 0 || cheapestMethod == null) {
                    BigDecimal.ZERO.setScale(2)
                } else {
                    Money.scale2(cheapestMethod.price.multiply(BigDecimal(physicalShops)))
                }
            val total = Money.scale2(subtotal.add(taxTotalsNormalized.exclusiveTax).add(shippingUsd))

            val shopSummaries =
                catalog.items
                    .groupBy { it.shopId }
                    .map { (shopId, shopItems) ->
                        com.piashcse.model.response.CartShopSummary(
                            shopId = shopId,
                            shopName = shopItems.firstOrNull()?.shopName,
                            subtotal =
                                Money.scale2(
                                    shopItems.fold(BigDecimal.ZERO) { acc, item ->
                                        acc.add(Money.of(item.price).multiply(BigDecimal(item.quantity)))
                                    },
                                ).toPlainString(),
                            itemCount = shopItems.sumOf { it.quantity },
                        )
                    }

            CartSummaryResponse(
                items = catalog.items,
                subtotal = subtotal.toPlainString(),
                estimatedTax = taxTotalsNormalized.tax.toPlainString(),
                itemCount = catalog.cartItems.sumOf { it.quantity },
                total = total.toPlainString(),
                estimatedShipping = shippingUsd.toPlainString(),
                freeShippingThreshold = Money.str(StorefrontSettings.freeShippingThreshold),
                freeShippingRemaining = Money.str(StorefrontSettings.freeShippingRemaining(subtotal)),
                shops = shopSummaries,
            )
        }

    private data class CartCatalog(
        val cartItems: List<CartItemDAO>,
        val products: Map<String, ProductDAO>,
        val variantsById: Map<String, ProductVariantDAO>,
        val items: List<CartItemSummary>,
    )

    private fun loadCartCatalog(userId: String): CartCatalog {
        val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
        val products =
            ProductDAO.find {
                ProductTable.id inList cartItems.map { it.productId.value }.distinct()
            }.associateBy { it.id.value }
        val productEntityIds = products.keys.map { it.entityID(ProductTable) }
        val imagesMap =
            if (products.isNotEmpty()) {
                ProductImageDAO.imagesForProducts(productEntityIds)
            } else {
                emptyMap()
            }
        val variantIds = cartItems.mapNotNull { it.variantId?.value }.distinct()
        val variantsById =
            if (variantIds.isNotEmpty()) {
                ProductVariantDAO.find {
                    ProductVariantTable.id inList variantIds.map { it.entityID(ProductVariantTable) }
                }.associateBy { it.id.value }
            } else {
                emptyMap()
            }
        val shopIds = products.values.mapNotNull { it.shopId?.value }.distinct()
        val activeWarehouseIds =
            if (shopIds.isEmpty()) {
                emptySet()
            } else {
                WarehouseDAO.find {
                    (WarehouseTable.shopId inList shopIds.map { it.entityID(ShopTable) }) and
                        (WarehouseTable.isActive eq true)
                }.map { it.id.value }.toSet()
            }
        val inventoryRows =
            if (products.isNotEmpty()) {
                InventoryDAO.find { InventoryTable.productId inList productEntityIds }
                    .filter { it.warehouseId.value in activeWarehouseIds }
            } else {
                emptyList()
            }

        fun stockFor(
            productId: String,
            variantId: String?,
        ): Int =
            inventoryRows
                .filter {
                    it.productId.value == productId && it.variantId?.value == variantId
                }.sumOf { it.stockQuantity }
        val shops =
            if (shopIds.isNotEmpty()) {
                ShopDAO.find { ShopTable.id inList shopIds }.associateBy { it.id.value }
            } else {
                emptyMap()
            }

        val items =
            cartItems.mapNotNull { cartItem ->
                val product = products[cartItem.productId.value] ?: return@mapNotNull null
                val variant = cartItem.variantId?.value?.let { variantsById[it] }
                val unitPrice = PricingService.unitPriceUsd(product, variant)
                cartItem.toCartItemSummary(
                    product = product,
                    unitPrice = unitPrice,
                    image = variant?.imageUrl ?: imagesMap[product.id.value]?.firstOrNull(),
                    stockQuantity = stockFor(product.id.value, variant?.id?.value),
                    shopName = product.shopId?.value?.let { shops[it]?.name },
                    variant = variant,
                )
            }

        return CartCatalog(cartItems, products, variantsById, items)
    }
}
