package com.piashcse.model.request

import com.piashcse.constants.FulfillmentType
import com.piashcse.constants.Localization
import com.piashcse.constants.PaymentMethod
import com.piashcse.database.entities.SupportedCurrencies
import com.piashcse.utils.validator.ValidationException
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate
import java.math.BigDecimal

@Serializable
data class CheckoutAttribution(
    val sessionId: String,
    val source: String? = null,
    val medium: String? = null,
    val campaign: String? = null,
    val referrer: String? = null,
)

@Serializable
data class CheckoutRequest(
    val shippingAddressId: String,
    val shippingMethodId: String,
    val paymentMethod: PaymentMethod? = null,
    val notes: String? = null,
    val idempotencyKey: String? = null,
    val couponCode: String? = null,
    val currency: String = "USD",
    /** Business tax id used to look up a B2B exemption. */
    val businessTaxId: String? = null,
    /** Loyalty points to redeem; 100 points = 1 USD. */
    val usePoints: Int = 0,
    /** Store credit to spend, in USD. */
    @Contextual val walletCredit: BigDecimal? = null,
    /** Gift card code; unused balance stays on the card. */
    val giftCardCode: String? = null,
    /** Checkout locale (en, es, fr, bn, hi); also used for formatted totals and emails. */
    val locale: String = Localization.DEFAULT_LOCALE,
    /** Optional first-touch attribution captured by the storefront. */
    val attribution: CheckoutAttribution? = null,
    /** DELIVERY (default) or PICKUP; POS sales use the dedicated POS endpoint. */
    val fulfillmentType: FulfillmentType = FulfillmentType.DELIVERY,
    val pickupWarehouseId: String? = null,
) {
    init {
        validate(this) {
            validate(CheckoutRequest::shippingAddressId).isNotNull().isNotEmpty()
            validate(CheckoutRequest::shippingMethodId).isNotNull().isNotEmpty()
        }
        SupportedCurrencies.requireValid(currency)
        if (locale.trim().lowercase().substringBefore('-').substringBefore('_') !in Localization.CODES) {
            throw ValidationException("Unsupported locale: $locale")
        }
    }
}
