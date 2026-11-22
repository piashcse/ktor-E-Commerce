package com.piashcse.feature.order

import com.piashcse.constants.FulfillmentType
import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.database.entities.CartItemDAO
import com.piashcse.database.entities.CartItemTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.database.entities.ProductVariantDAO
import com.piashcse.database.entities.ProductVariantTable
import com.piashcse.database.entities.ShippingAddressDAO
import com.piashcse.database.entities.ShippingMethodDAO
import com.piashcse.database.entities.ShopDAO
import com.piashcse.database.entities.ShopTable
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.WarehouseDAO
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import java.math.BigDecimal

/** Shipping snapshot resolved from the request (pickup or delivery). */
internal data class CheckoutShipping(
    val name: String,
    val price: BigDecimal,
    val shopId: String? = null,
)

/** Validated fulfillment inputs shared by order placement and checkout preview. */
internal data class CheckoutContext(
    val cartItems: List<CartItemDAO>,
    val shippingAddress: ShippingAddressDAO?,
    val contactEmail: String,
    val fullAddress: String,
    val shippingMethod: CheckoutShipping,
    val pickupWarehouse: WarehouseDAO?,
    val taxCountry: String,
    val taxRegion: String?,
    val productsMap: Map<String, ProductDAO>,
    val variantsMap: Map<String, ProductVariantDAO>,
    val itemsByShop: Map<String, List<CartItemDAO>>,
)

/**
 * Locks the customer cart and resolves every checkout input in one place.
 * Both placement and preview must use the same validation rules.
 */
internal object CheckoutContextLoader {
    fun load(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutContext {
        // Lock the cart for the duration of the checkout transaction. Two
        // concurrent placements must not both read (and fulfil) the same rows.
        val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.forUpdate().toList()
        if (cartItems.isEmpty()) throw ValidationException(Message.Cart.EMPTY_CART)

        val fulfillment = resolveFulfillment(userId, checkoutRequest)
        val (productsMap, variantsMap) = loadCartCatalog(cartItems)

        val itemsByShop =
            cartItems.groupBy {
                productsMap[it.productId.value]?.shopId?.value
                    ?: throw ValidationException(Message.Orders.productDoesNotBelongToShop(it.productId.value))
            }
        validateShopsApproved(itemsByShop.keys)
        fulfillment.shippingMethod.shopId?.let { methodShop ->
            if (itemsByShop.size != 1 || methodShop != itemsByShop.keys.first()) {
                throw ValidationException("Shipping method is not available for these shops")
            }
        }
        fulfillment.pickupWarehouse?.let { warehouse ->
            if (itemsByShop.size != 1) throw ValidationException("Pickup orders must contain items from one shop")
            if (warehouse.shopId.value != itemsByShop.keys.first()) {
                throw ValidationException("Pickup location does not belong to the selling shop")
            }
        }

        return CheckoutContext(
            cartItems = cartItems,
            shippingAddress = fulfillment.shippingAddress,
            contactEmail = fulfillment.contactEmail,
            fullAddress = fulfillment.fullAddress,
            shippingMethod = fulfillment.shippingMethod,
            pickupWarehouse = fulfillment.pickupWarehouse,
            taxCountry = fulfillment.taxCountry,
            taxRegion = fulfillment.taxRegion,
            productsMap = productsMap,
            variantsMap = variantsMap,
            itemsByShop = itemsByShop,
        )
    }

    private data class Fulfillment(
        val pickupWarehouse: WarehouseDAO?,
        val shippingAddress: ShippingAddressDAO?,
        val shippingMethod: CheckoutShipping,
        val fullAddress: String,
        val contactEmail: String,
        val taxCountry: String,
        val taxRegion: String?,
    )

    private fun resolveFulfillment(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): Fulfillment {
        val pickup = checkoutRequest.fulfillmentType == FulfillmentType.PICKUP
        val pickupWarehouse =
            if (pickup) {
                val id =
                    checkoutRequest.pickupWarehouseId?.takeIf { it.isNotBlank() }
                        ?: throw ValidationException("pickupWarehouseId is required for pickup orders")
                val warehouse =
                    WarehouseDAO.findById(id)
                        ?: throw ValidationException("Pickup location not found")
                if (!warehouse.isActive || !warehouse.supportsPickup) {
                    throw ValidationException("Pickup location is not available")
                }
                warehouse
            } else {
                null
            }

        val shippingAddress =
            if (pickup) {
                null
            } else {
                val address =
                    ShippingAddressDAO.findById(
                        checkoutRequest.shippingAddressId
                            ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND),
                    ) ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
                if (address.userId.value != userId) {
                    throw ValidationException(Message.Orders.SHIPPING_ADDRESS_UNAUTHORIZED)
                }
                address
            }
        val shippingMethod =
            if (pickup) {
                CheckoutShipping("PICKUP", BigDecimal.ZERO.setScale(2))
            } else {
                val method =
                    ShippingMethodDAO.findById(
                        checkoutRequest.shippingMethodId
                            ?: throw ValidationException(Message.Orders.SHIPPING_METHOD_NOT_FOUND),
                    ) ?: throw ValidationException(Message.Orders.SHIPPING_METHOD_NOT_FOUND)
                CheckoutShipping(method.name, method.price, method.shopId?.value)
            }
        val fullAddress =
            if (pickupWarehouse != null) {
                buildString {
                    append("Pickup at ${pickupWarehouse.name}\n")
                    append("${pickupWarehouse.address ?: ""}, ${pickupWarehouse.city ?: ""}\n")
                    append(pickupWarehouse.country ?: "")
                }
            } else {
                val address = shippingAddress ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
                buildString {
                    append("${address.firstName} ${address.lastName}\n")
                    append("${address.streetAddress}, ${address.city}, ${address.state ?: ""}\n")
                    append("${address.country}, ${address.zipCode}\n")
                    append("Phone: ${maskPhone(address.phoneNumber)}")
                }
            }
        val taxCountry =
            pickupWarehouse?.country ?: shippingAddress?.country
                ?: throw ValidationException(Message.Orders.SHIPPING_ADDRESS_NOT_FOUND)
        return Fulfillment(
            pickupWarehouse = pickupWarehouse,
            shippingAddress = shippingAddress,
            shippingMethod = shippingMethod,
            fullAddress = fullAddress,
            contactEmail = shippingAddress?.email ?: UserDAO.findById(userId)?.email.orEmpty(),
            taxCountry = taxCountry,
            taxRegion = if (pickup) null else shippingAddress?.state,
        )
    }

    private fun loadCartCatalog(cartItems: List<CartItemDAO>): Pair<Map<String, ProductDAO>, Map<String, ProductVariantDAO>> {
        val productsMap =
            ProductDAO.find {
                ProductTable.id inList cartItems.map { it.productId.value }.distinct()
            }.associateBy { it.id.value }
        val variantIds = cartItems.mapNotNull { it.variantId?.value }.distinct()
        val variantsMap =
            if (variantIds.isNotEmpty()) {
                ProductVariantDAO.find {
                    ProductVariantTable.id inList variantIds.map { it.entityID(ProductVariantTable) }
                }.associateBy { it.id.value }
            } else {
                emptyMap()
            }
        return productsMap to variantsMap
    }

    private fun validateShopsApproved(shopIds: Set<String>) {
        val shops =
            if (shopIds.isNotEmpty()) {
                ShopDAO.find { ShopTable.id inList shopIds.map { it.entityID(ShopTable) } }.associateBy { it.id.value }
            } else {
                emptyMap()
            }
        shopIds.forEach { shopId ->
            val shop = shops[shopId] ?: throw ValidationException(Message.Orders.SHOP_NOT_FOUND)
            if (shop.status != ShopStatus.APPROVED) {
                throw ValidationException(Message.Orders.SHOP_INACTIVE)
            }
        }
    }

    // PII minimization: never persist the full phone number in the denormalized
    // shippingAddress snapshot — keep only the last 4 digits for support lookup.
    private fun maskPhone(phoneNumber: String): String {
        val digits = phoneNumber.filter { it.isDigit() }
        if (digits.length <= 4) return "***"
        return "***" + digits.takeLast(4)
    }
}
