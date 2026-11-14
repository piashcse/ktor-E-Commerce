package com.piashcse.feature.checkout

import at.favre.lib.crypto.bcrypt.BCrypt
import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.constants.ProductStatus
import com.piashcse.constants.UserType
import com.piashcse.database.entities.CartItemDAO
import com.piashcse.database.entities.CartItemTable
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductVariantDAO
import com.piashcse.database.entities.ShippingAddressDAO
import com.piashcse.database.entities.UserDAO
import com.piashcse.database.entities.UserProfileDAO
import com.piashcse.database.entities.UserTable
import com.piashcse.feature.order.OrderRepository
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.request.GuestCheckoutRequest
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.extension.entityID
import com.piashcse.utils.extension.query
import com.piashcse.utils.extension.throwNotFound
import com.piashcse.utils.validator.ValidationException
import org.jetbrains.exposed.v1.core.eq
import java.util.UUID

/**
 * Guest checkout reuses the normal order pipeline:
 * 1. find/create an unverified guest CUSTOMER for the email,
 * 2. create a temporary cart + shipping address,
 * 3. call the same `placeOrder` service used by signed-in customers.
 *
 * If the email belongs to a verified account the request is rejected so guests
 * can never place orders into someone else's account. A guest can later claim
 * the account by registering and verifying the OTP (isGuest is cleared).
 */
class GuestCheckoutService(
    private val orderRepository: OrderRepository,
) {
    suspend fun placeOrder(request: GuestCheckoutRequest): List<OrderResponse> {
        val normalizedEmail = request.email.trim().lowercase()

        val (guestUserId, addressId) =
            query {
                val existingUsers = UserDAO.find { UserTable.email eq normalizedEmail }.toList()
                if (existingUsers.any { it.isVerified }) {
                    throw ValidationException("This email is registered. Please sign in to place the order.")
                }
                val guest =
                    existingUsers.find { it.userType == UserType.CUSTOMER }?.also {
                        if (!it.isGuest) {
                            throw ValidationException("This email is registered. Please sign in to place the order.")
                        }
                    } ?: UserDAO.new {
                        this.email = normalizedEmail
                        userType = UserType.CUSTOMER
                        password =
                            BCrypt.withDefaults().hashToString(
                                AppConstants.BCRYPT_COST,
                                UUID.randomUUID().toString().toCharArray(),
                            )
                        isGuest = true
                        isVerified = false
                    }.also { user ->
                        UserProfileDAO.new {
                            userId = user.id
                            firstName = request.firstName
                            lastName = request.lastName
                            mobile = request.phoneNumber
                        }
                    }

                CartItemDAO.find { CartItemTable.userId eq guest.id }.forEach { it.delete() }
                request.items.forEach { item ->
                    val product =
                        ProductDAO.findById(item.productId) ?: item.productId.throwNotFound("Product")
                    if (product.status != ProductStatus.ACTIVE) {
                        throw ValidationException(Message.Products.OUT_OF_STOCK)
                    }
                    val variant =
                        item.variantId?.takeIf { it.isNotBlank() }?.let { variantId ->
                            val row =
                                ProductVariantDAO.findById(variantId)
                                    ?: variantId.throwNotFound("Product variant")
                            if (row.productId.value != product.id.value) {
                                throw ValidationException("Variant does not belong to this product")
                            }
                            if (row.status != ProductStatus.ACTIVE) {
                                throw ValidationException(Message.Products.OUT_OF_STOCK)
                            }
                            row
                        }
                    CartItemDAO.new {
                        userId = guest.id
                        productId = product.id
                        this.variantId = variant?.id
                        quantity = item.quantity
                    }
                }

                val address =
                    ShippingAddressDAO.new {
                        userId = guest.id
                        firstName = request.firstName
                        lastName = request.lastName
                        this.email = normalizedEmail
                        phoneNumber = request.phoneNumber
                        streetAddress = request.streetAddress
                        city = request.city
                        state = request.state
                        country = request.country
                        zipCode = request.postCode
                        isDefault = true
                    }
                guest.id.value to address.id.value
            }

        return try {
            orderRepository.placeOrder(
                guestUserId,
                CheckoutRequest(
                    shippingAddressId = addressId,
                    shippingMethodId = request.shippingMethodId,
                    paymentMethod = request.paymentMethod,
                    notes = request.notes,
                    idempotencyKey = request.idempotencyKey,
                    couponCode = request.couponCode,
                    currency = request.currency,
                ),
            )
        } finally {
            // Clear on success, replay and failure so no half-built guest cart remains.
            query {
                CartItemDAO.find { CartItemTable.userId eq guestUserId.entityID(UserTable) }
                    .forEach { it.delete() }
            }
        }
    }
}
