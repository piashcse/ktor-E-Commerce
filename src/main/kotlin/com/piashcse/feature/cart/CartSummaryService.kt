package com.piashcse.feature.cart

import com.piashcse.database.entities.*
import com.piashcse.feature.config.StorefrontSettings
import com.piashcse.feature.tax.TaxService
import com.piashcse.feature.tax.TaxTotals
import com.piashcse.mapper.toCartItemSummary
import com.piashcse.model.response.CartItemSummary
import com.piashcse.model.response.CartSummaryResponse
import com.piashcse.service.PricingService
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import java.math.BigDecimal

/** Cart item/stock loading, tax estimate and shipping summary. */
class CartSummaryService(
    private val taxService: TaxService = TaxService(),
) {
    suspend fun getCartSummary(userId: String): CartSummaryResponse =
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
