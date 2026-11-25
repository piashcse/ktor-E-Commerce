package com.piashcse.feature.order

import com.piashcse.constants.AppConstants
import com.piashcse.constants.Message
import com.piashcse.feature.config.StorefrontSettings
import com.piashcse.feature.coupon.CouponRules
import com.piashcse.feature.coupon.toSnapshot
import com.piashcse.feature.i18n.I18nService
import com.piashcse.feature.loyalty.LoyaltyService
import com.piashcse.feature.promotion.PromotionService
import com.piashcse.feature.tax.TaxQuote
import com.piashcse.feature.tax.TaxService
import com.piashcse.feature.tax.TaxTotals
import com.piashcse.model.request.CheckoutRequest
import com.piashcse.model.response.CheckoutSummaryResponse
import com.piashcse.model.response.TaxLineResponse
import com.piashcse.service.FxService
import com.piashcse.service.PricingService
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.query
import com.piashcse.utils.validator.ValidationException
import java.math.BigDecimal
import java.math.RoundingMode

/** Builds the server-owned checkout quote shown before order placement. */
internal class CheckoutPreviewService(
    private val fxService: FxService,
    private val taxService: TaxService,
    private val promotionService: PromotionService,
    private val loyaltyService: LoyaltyService,
) {
    private val i18n = I18nService()

    private data class TaxPreview(
        val tax: BigDecimal,
        val exclusiveTax: BigDecimal,
        val lines: List<TaxLineResponse>,
    )

    private data class CouponPreviewResult(
        val response: CheckoutSummaryResponse,
        val eligibleProductIds: Set<String>?,
    )

    /** Order-currency subtotal using the same unit rounding as placeOrder. */
    private fun checkoutSubtotal(
        context: CheckoutContext,
        fxRate: BigDecimal,
    ): BigDecimal =
        Money.scale2(
            context.cartItems.fold(BigDecimal.ZERO) { acc, cartItem ->
                val product = context.productsMap[cartItem.productId.value] ?: return@fold acc
                val variant = cartItem.variantId?.value?.let(context.variantsMap::get)
                val unit = PricingService.toCurrency(PricingService.unitPriceUsd(product, variant), fxRate)
                acc.add(Money.unitTotal(unit, cartItem.quantity))
            },
        )

    private fun checkoutSubtotalUsd(context: CheckoutContext): BigDecimal =
        PricingService.subtotal(
            context.cartItems.map { cartItem ->
                val product =
                    context.productsMap[cartItem.productId.value]
                        ?: throw ValidationException(Message.Orders.PRODUCT_NOT_FOUND)
                val variant = cartItem.variantId?.value?.let(context.variantsMap::get)
                PricingService.unitPriceUsd(product, variant) to cartItem.quantity
            },
        )

    private fun checkoutShipping(
        context: CheckoutContext,
        subUsd: BigDecimal,
        fxRate: BigDecimal,
    ): BigDecimal {
        val shopsWithShipping =
            context.cartItems
                .mapNotNull { context.productsMap[it.productId.value] }
                .filter { !it.isDigital }
                .groupBy { it.shopId?.value }
                .count { (_, products) -> products.any { !it.freeShipping } }
        return if (context.pickupWarehouse != null ||
            shopsWithShipping == 0 ||
            StorefrontSettings.freeShippingReached(subUsd)
        ) {
            BigDecimal.ZERO.setScale(2)
        } else {
            PricingService.toCurrency(PricingService.shipping(context.shippingMethod.price, shopsWithShipping), fxRate)
        }
    }

    private fun checkoutTaxPreview(
        context: CheckoutContext,
        userId: String,
        businessTaxId: String?,
        fxRate: BigDecimal,
    ): TaxPreview {
        var taxTotals = TaxTotals()
        val lines = mutableListOf<TaxLineResponse>()
        context.cartItems.forEach { cartItem ->
            val product =
                context.productsMap[cartItem.productId.value]
                    ?: throw ValidationException(Message.Orders.PRODUCT_NOT_FOUND)
            val variant = cartItem.variantId?.value?.let(context.variantsMap::get)
            val unit = PricingService.toCurrency(PricingService.unitPriceUsd(product, variant), fxRate)
            val line = Money.unitTotal(unit, cartItem.quantity)
            val quote =
                taxService.resolve(
                    country = context.taxCountry,
                    region = context.taxRegion,
                    categoryId = product.categoryId.value,
                    taxClassId = product.taxClassId?.value,
                    userId = userId,
                    businessTaxId = businessTaxId,
                )
            val amounts = taxService.apply(line, quote)
            taxTotals = taxTotals.add(amounts, quote)
            lines +=
                TaxLineResponse(
                    productId = product.id.value,
                    taxClassId = quote.taxClassId,
                    rate = quote.rate.toPlainString(),
                    inclusive = quote.inclusive,
                    netAmount = amounts.net.toPlainString(),
                    taxAmount = amounts.tax.toPlainString(),
                    grossAmount = amounts.gross.toPlainString(),
                )
        }
        val totals = taxTotals.normalized()
        return TaxPreview(totals.tax, totals.exclusiveTax, lines)
    }

    private fun couponPreview(
        response: CheckoutSummaryResponse,
        code: String,
        userId: String,
        context: CheckoutContext,
        sub: BigDecimal,
        ship: BigDecimal,
        exclusiveTax: BigDecimal,
        fxRate: BigDecimal,
    ): CouponPreviewResult {
        // A mistyped promo code must never blank the whole totals panel:
        // report validity inline and keep the undiscounted totals.
        return try {
            val snapshot = CouponCheckoutSupport.load(code, userId = userId).toSnapshot()
            val lines = CouponCheckoutSupport.lines(context.cartItems, context.productsMap, context.variantsMap)
            val eligibleUsd = CouponRules.eligibleSubtotalUsd(snapshot, lines)
            CouponRules.eligibilityError(snapshot, eligibleUsd)?.let { throw ValidationException(it) }
            val discount = PricingService.toCurrency(CouponRules.discountUsd(snapshot, eligibleUsd), fxRate)
            val total = Money.scale2(sub.add(ship).add(exclusiveTax).subtract(discount).max(BigDecimal.ZERO))
            val eligibleProductIds =
                context.productsMap.values
                    .filter { product ->
                        CouponRules.matches(snapshot, product.shopId?.value, product.categoryId.value)
                    }.map { product -> product.id.value }
                    .toSet()
            CouponPreviewResult(
                response.copy(
                    couponCode = code,
                    couponValid = true,
                    discountAmount = discount.toPlainString(),
                    total = total.toPlainString(),
                ),
                eligibleProductIds,
            )
        } catch (error: ValidationException) {
            CouponPreviewResult(
                response.copy(
                    couponCode = code,
                    couponValid = false,
                    couponError = error.message ?: "Coupon is not valid",
                ),
                null,
            )
        }
    }

    /** Coupons/promotions reduce the taxable base; refresh line tax before redemptions. */
    private fun applyDiscountToTax(
        response: CheckoutSummaryResponse,
        taxLines: List<TaxLineResponse>,
        eligibleProductIds: Set<String>?,
        sub: BigDecimal,
    ): CheckoutSummaryResponse {
        val discount = BigDecimal(response.discountAmount)
        if (discount <= BigDecimal.ZERO) return response
        val discountBase =
            if (eligibleProductIds == null) {
                sub
            } else {
                taxLines
                    .filter { it.productId in eligibleProductIds }
                    .fold(BigDecimal.ZERO) { acc, line -> acc.add(BigDecimal(line.grossAmount)) }
            }
        var recalculatedTax = BigDecimal.ZERO
        var recalculatedExclusive = BigDecimal.ZERO
        val recalculatedLines =
            taxLines.map { line ->
                if (eligibleProductIds != null && line.productId !in eligibleProductIds) {
                    recalculatedTax = recalculatedTax.add(BigDecimal(line.taxAmount))
                    if (!line.inclusive) recalculatedExclusive = recalculatedExclusive.add(BigDecimal(line.taxAmount))
                    return@map line
                }
                val gross = BigDecimal(line.grossAmount)
                val share =
                    if (discountBase > BigDecimal.ZERO) {
                        Money.proportionalSplit(discount, gross, discountBase)
                    } else {
                        BigDecimal.ZERO
                    }
                val amounts =
                    taxService.apply(
                        gross.subtract(share).max(BigDecimal.ZERO),
                        TaxQuote(
                            rate = BigDecimal(line.rate),
                            inclusive = line.inclusive,
                            taxClassId = line.taxClassId,
                        ),
                    )
                recalculatedTax = recalculatedTax.add(amounts.tax)
                if (!line.inclusive) recalculatedExclusive = recalculatedExclusive.add(amounts.tax)
                line.copy(
                    netAmount = amounts.net.toPlainString(),
                    taxAmount = amounts.tax.toPlainString(),
                    grossAmount = amounts.gross.toPlainString(),
                )
            }
        return response.copy(
            taxAmount = Money.scale2(recalculatedTax).toPlainString(),
            taxLines = recalculatedLines,
            total =
                Money.scale2(
                    sub.subtract(discount).max(BigDecimal.ZERO)
                        .add(BigDecimal(response.shippingCost))
                        .add(recalculatedExclusive),
                ).toPlainString(),
        )
    }

    private fun applyWalletPreview(
        response: CheckoutSummaryResponse,
        request: CheckoutRequest,
        userId: String,
        fxRate: BigDecimal,
    ): CheckoutSummaryResponse {
        val preview =
            loyaltyService.redemptionPreviewTx(
                userId = userId,
                points = request.usePoints,
                walletCreditUsd = request.walletCredit,
                giftCardCode = request.giftCardCode,
            )
        val responseTotal = BigDecimal(response.total)
        val totalUsd = responseTotal.divide(fxRate, 2, RoundingMode.HALF_UP)
        val walletUsd = preview.pointsUsd.add(preview.creditUsd).min(totalUsd)
        val giftUsd = preview.giftCardUsd.min(totalUsd.subtract(walletUsd).max(BigDecimal.ZERO))
        val walletDiscount = PricingService.toCurrency(walletUsd, fxRate).min(responseTotal)
        val giftDiscount = PricingService.toCurrency(giftUsd, fxRate).min(responseTotal.subtract(walletDiscount))
        return response.copy(
            walletDiscount = walletDiscount.toPlainString(),
            giftCardDiscount = giftDiscount.toPlainString(),
            total =
                Money.scale2(
                    responseTotal.subtract(walletDiscount).subtract(giftDiscount).max(BigDecimal.ZERO),
                ).toPlainString(),
            pointsBalance = preview.pointsAvailable,
            storeCreditBalance = preview.creditAvailable.toPlainString(),
        )
    }

    private fun formatSummary(
        response: CheckoutSummaryResponse,
        currency: String,
        checkoutRequest: CheckoutRequest,
        fxRate: BigDecimal,
        subUsd: BigDecimal,
    ): CheckoutSummaryResponse {
        val locale = com.piashcse.constants.Localization.normalize(checkoutRequest.locale)
        return response.copy(
            locale = locale,
            freeShippingThreshold =
                PricingService.toCurrency(StorefrontSettings.freeShippingThreshold, fxRate).toPlainString(),
            freeShippingRemaining =
                PricingService.toCurrency(StorefrontSettings.freeShippingRemaining(subUsd), fxRate).toPlainString(),
            formattedSubTotal = i18n.formatMoney(BigDecimal(response.subTotal), currency, locale),
            formattedShipping = i18n.formatMoney(BigDecimal(response.shippingCost), currency, locale),
            formattedTax = i18n.formatMoney(BigDecimal(response.taxAmount), currency, locale),
            formattedDiscount = i18n.formatMoney(BigDecimal(response.discountAmount), currency, locale),
            formattedTotal = i18n.formatMoney(BigDecimal(response.total), currency, locale),
        )
    }

    suspend fun summary(
        userId: String,
        checkoutRequest: CheckoutRequest,
    ): CheckoutSummaryResponse {
        val currency = checkoutRequest.currency.uppercase()
        val fxRate = if (currency != AppConstants.DEFAULT_CURRENCY) fxService.rateTo(currency) else BigDecimal.ONE
        return query {
            val context = CheckoutContextLoader.load(userId, checkoutRequest)
            val subUsd = checkoutSubtotalUsd(context)
            val sub = checkoutSubtotal(context, fxRate)
            val ship = checkoutShipping(context, subUsd, fxRate)
            val taxPreview = checkoutTaxPreview(context, userId, checkoutRequest.businessTaxId, fxRate)

            var response =
                CheckoutSummaryResponse(
                    subTotal = sub.toPlainString(),
                    shippingCost = ship.toPlainString(),
                    taxAmount = taxPreview.tax.toPlainString(),
                    taxInclusive = taxPreview.lines.any { it.inclusive },
                    total = Money.scale2(sub.add(ship).add(taxPreview.exclusiveTax)).toPlainString(),
                    itemCount = context.cartItems.sumOf { it.quantity },
                    currency = currency,
                    taxLines = taxPreview.lines,
                )

            var eligibleCouponProductIds: Set<String>? = null
            val couponCode = checkoutRequest.couponCode
            if (couponCode == null) {
                promotionService.evaluateTx(subUsd, context.itemsByShop.keys)?.let { promotion ->
                    val discount = PricingService.toCurrency(promotion.discount, fxRate)
                    val discountedShipping = if (promotion.freeShipping) BigDecimal.ZERO.setScale(2) else ship
                    response =
                        response.copy(
                            shippingCost = discountedShipping.toPlainString(),
                            discountAmount = discount.toPlainString(),
                            total =
                                Money.scale2(
                                    sub.add(discountedShipping)
                                        .add(taxPreview.exclusiveTax)
                                        .subtract(discount)
                                        .max(BigDecimal.ZERO),
                                ).toPlainString(),
                        )
                }
            } else {
                val preview =
                    couponPreview(
                        response,
                        couponCode,
                        userId,
                        context,
                        sub,
                        ship,
                        taxPreview.exclusiveTax,
                        fxRate,
                    )
                response = preview.response
                eligibleCouponProductIds = preview.eligibleProductIds
            }

            response = applyDiscountToTax(response, taxPreview.lines, eligibleCouponProductIds, sub)
            response = applyWalletPreview(response, checkoutRequest, userId, fxRate)
            formatSummary(response, currency, checkoutRequest, fxRate, subUsd)
        }
    }
}
