@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.piashcse.plugin

import com.piashcse.constants.AppConstants
import com.piashcse.feature.analytics.analyticsAdminRoutes
import com.piashcse.feature.analytics.analyticsRoutes
import com.piashcse.feature.apikey.apiKeyRoutes
import com.piashcse.feature.audit_log.auditLogAdminRoutes
import com.piashcse.feature.auth.authAdminRoutes
import com.piashcse.feature.auth.authRoutes
import com.piashcse.feature.b2b.b2bAccountRoutes
import com.piashcse.feature.b2b.b2bAdminRoutes
import com.piashcse.feature.b2b.b2bPurchaseOrderRoutes
import com.piashcse.feature.b2b.b2bQuoteRoutes
import com.piashcse.feature.b2b.b2bSellerPurchaseOrderRoutes
import com.piashcse.feature.b2b.b2bSellerQuoteRoutes
import com.piashcse.feature.brand.brandAdminRoutes
import com.piashcse.feature.brand.brandRoutes
import com.piashcse.feature.cart.cartRoutes
import com.piashcse.feature.checkout.checkoutRoutes
import com.piashcse.feature.cms.cmsAdminRoutes
import com.piashcse.feature.cms.cmsRoutes
import com.piashcse.feature.cms.seoAdminRoutes
import com.piashcse.feature.cms.seoRoutes
import com.piashcse.feature.cms.seoSiteRoutes
import com.piashcse.feature.consent.consentAdminRoutes
import com.piashcse.feature.consent.consentRoutes
import com.piashcse.feature.coupon.couponAdminRoutes
import com.piashcse.feature.coupon.couponRoutes
import com.piashcse.feature.dashboard.dashboardAdminRoutes
import com.piashcse.feature.dashboard.dashboardSellerRoutes
import com.piashcse.feature.featureflag.featureFlagRoutes
import com.piashcse.feature.fraud.chargebackAdminRoutes
import com.piashcse.feature.fraud.fraudAdminRoutes
import com.piashcse.feature.fraud.paymentRiskAdminRoutes
import com.piashcse.feature.fx.fxAdminRoutes
import com.piashcse.feature.i18n.i18nRoutes
import com.piashcse.feature.i18n.productTranslationAdminRoutes
import com.piashcse.feature.inventory.inventoryAdminRoutes
import com.piashcse.feature.inventory.inventorySellerRoutes
import com.piashcse.feature.invoice.orderDocumentRoutes
import com.piashcse.feature.kyc.adminKycRoutes
import com.piashcse.feature.kyc.sellerKycRoutes
import com.piashcse.feature.loyalty.giftCardAdminRoutes
import com.piashcse.feature.loyalty.referralRoutes
import com.piashcse.feature.loyalty.walletAdminRoutes
import com.piashcse.feature.loyalty.walletRoutes
import com.piashcse.feature.notification.notificationRoutes
import com.piashcse.feature.omnichannel.pickupActionRoutes
import com.piashcse.feature.omnichannel.pickupSellerListRoutes
import com.piashcse.feature.omnichannel.posSellerRoutes
import com.piashcse.feature.omnichannel.storeLocatorRoutes
import com.piashcse.feature.order.orderAdminRoutes
import com.piashcse.feature.order.orderRoutes
import com.piashcse.feature.order.orderSellerRoutes
import com.piashcse.feature.outbox.outboxAdminRoutes
import com.piashcse.feature.payment.paymentAdminRoutes
import com.piashcse.feature.payment.paymentRoutes
import com.piashcse.feature.payout.payoutAdminRoutes
import com.piashcse.feature.payout.payoutSellerRoutes
import com.piashcse.feature.policy.policyAdminRoutes
import com.piashcse.feature.policy.policyRoutes
import com.piashcse.feature.privacy.privacyRoutes
import com.piashcse.feature.product.productAdminRoutes
import com.piashcse.feature.product.productRoutes
import com.piashcse.feature.product.productSellerRoutes
import com.piashcse.feature.product_category.productCategoryAdminRoutes
import com.piashcse.feature.product_category.productCategoryRoutes
import com.piashcse.feature.product_sub_category.productSubCategoryAdminRoutes
import com.piashcse.feature.product_sub_category.productSubCategoryRoutes
import com.piashcse.feature.profile.profileRoutes
import com.piashcse.feature.promotion.promotionAdminRoutes
import com.piashcse.feature.recommendation.recommendationRoutes
import com.piashcse.feature.refund_request.refundAdminRoutes
import com.piashcse.feature.refund_request.refundRequestRoutes
import com.piashcse.feature.refund_request.refundSellerRoutes
import com.piashcse.feature.review_rating.reviewAdminRoutes
import com.piashcse.feature.review_rating.reviewRatingRoutes
import com.piashcse.feature.rma.rmaAdminRoutes
import com.piashcse.feature.rma.rmaRoutes
import com.piashcse.feature.rma.rmaSellerRoutes
import com.piashcse.feature.seller.commissionAdminRoutes
import com.piashcse.feature.seller.commissionSellerRoutes
import com.piashcse.feature.seller.disputeAdminRoutes
import com.piashcse.feature.seller.disputeRoutes
import com.piashcse.feature.seller.sellerOwnReviewRoutes
import com.piashcse.feature.seller.sellerPublicRoutes
import com.piashcse.feature.seller.sellerReviewAdminRoutes
import com.piashcse.feature.seller.sellerReviewRoutes
import com.piashcse.feature.shipping.orderShipmentRoutes
import com.piashcse.feature.shipping.shipmentRoutes
import com.piashcse.feature.shipping_method.shippingMethodAdminRoutes
import com.piashcse.feature.shop.shopAdminRoutes
import com.piashcse.feature.shop.shopRoutes
import com.piashcse.feature.shop.shopSellerRoutesV1
import com.piashcse.feature.shop_category.shopCategoryAdminRoutes
import com.piashcse.feature.shop_category.shopCategoryRoutes
import com.piashcse.feature.stockalert.stockAlertRoutes
import com.piashcse.feature.subscription.digitalLibraryRoutes
import com.piashcse.feature.subscription.subscriptionAdminRoutes
import com.piashcse.feature.subscription.subscriptionPlanAdminRoutes
import com.piashcse.feature.subscription.subscriptionPlanRoutes
import com.piashcse.feature.subscription.subscriptionRoutes
import com.piashcse.feature.tax.productTaxClassAdminRoutes
import com.piashcse.feature.tax.taxAdminRoutes
import com.piashcse.feature.tax.taxClassAdminRoutes
import com.piashcse.feature.tax.taxExemptionAdminRoutes
import com.piashcse.feature.tax.taxZoneAdminRoutes
import com.piashcse.feature.warehouse.warehouseSellerRoutes
import com.piashcse.feature.wishlist.wishListRoutes
import com.piashcse.utils.common.ApiError
import com.piashcse.utils.common.MessageResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/** Minimal liveness payload — service/infra detail stays out of the public contract. */
@Serializable
private data class HealthStatusResponse(val status: String, val version: String)

@OptIn(ExperimentalKtorApi::class)
fun Application.configureRoute() {
    routing {
        seoSiteRoutes()
        get("/") { call.respondRedirect("/swagger") }.hide()
        get("/health") {
            call.respond(
                HealthStatusResponse(status = "UP", version = AppConstants.APP_VERSION),
            )
        }.describe {
            summary = "Service health"
            description = "Version and overall status"
            tag(ApiTags.HEALTH)
        }
        get("/health/live") {
            call.respond(
                MessageResponse("UP"),
            )
        }.describe {
            summary = "Liveness probe"
            tag(ApiTags.HEALTH)
        }
        get("/health/ready") {
            val dbOk =
                runCatching {
                    withTimeout(3_000) {
                        withContext(Dispatchers.IO) {
                            transaction {
                                exec("SELECT 1")
                            }
                        }
                    }
                }.isSuccess
            if (dbOk) {
                call.respond(MessageResponse("READY"))
            } else {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    ApiError(
                        message = "Database unreachable",
                        code = "SERVICE_UNAVAILABLE",
                        requestId = runCatching { call.requestId() }.getOrNull(),
                    ),
                )
            }
        }.describe {
            summary = "Readiness probe"
            description = "Checks database connectivity"
            tag(ApiTags.HEALTH)
        }

        route("/api") {
            route("v1") {
                customerRoutes()
                sellerRoutes()
                adminRoutes()
            }
        }
    }
}

private fun Route.customerRoutes() {
    route("auth") { authRoutes() }.describe { tag(ApiTags.AUTH) }
    route("profile") { profileRoutes() }.describe { tag(ApiTags.PROFILE) }
    route("shops") { shopRoutes() }.describe { tag(ApiTags.SHOP) }
    route("brands") { brandRoutes() }.describe { tag(ApiTags.BRAND) }
    route("product-categories") { productCategoryRoutes() }.describe { tag(ApiTags.PRODUCT_CATEGORY) }
    route("product-subcategories") { productSubCategoryRoutes() }.describe { tag(ApiTags.PRODUCT_SUB_CATEGORY) }
    route("shop-categories") { shopCategoryRoutes() }.describe { tag(ApiTags.SHOP_CATEGORY) }
    route("products") { productRoutes() }.describe { tag(ApiTags.PRODUCT) }
    route("reviews") { reviewRatingRoutes() }.describe { tag(ApiTags.REVIEW) }
    route("carts") { cartRoutes() }.describe { tag(ApiTags.CART) }
    route("wishlists") { wishListRoutes() }.describe { tag(ApiTags.WISHLIST) }
    route("stock-alerts") { stockAlertRoutes() }.describe { tag(ApiTags.PRODUCT) }
    route("checkout") { checkoutRoutes() }.describe { tag(ApiTags.CHECKOUT) }
    route("orders") {
        orderRoutes()
        orderDocumentRoutes()
        orderShipmentRoutes()
    }.describe { tag(ApiTags.ORDER) }
    route("payments") { paymentRoutes() }.describe { tag(ApiTags.PAYMENT) }
    route("shipments") { shipmentRoutes() }.describe { tag(ApiTags.SHIPMENT) }
    route("wallet") { walletRoutes() }.describe { tag(ApiTags.LOYALTY) }
    route("referrals") { referralRoutes() }.describe { tag(ApiTags.LOYALTY) }
    route("i18n") { i18nRoutes() }.describe { tag(ApiTags.LOCALIZATION) }
    route("analytics") { analyticsRoutes() }.describe { tag(ApiTags.ANALYTICS) }
    route("sellers") { sellerPublicRoutes() }.describe { tag(ApiTags.SELLER) }
    route("seller-reviews") { sellerReviewRoutes() }.describe { tag(ApiTags.SELLER) }
    route("disputes") { disputeRoutes() }.describe { tag(ApiTags.SELLER) }
    route("subscription-plans") { subscriptionPlanRoutes() }.describe { tag(ApiTags.SUBSCRIPTION) }
    route("subscriptions") { subscriptionRoutes() }.describe { tag(ApiTags.SUBSCRIPTION) }
    route("library") { digitalLibraryRoutes() }.describe { tag(ApiTags.SUBSCRIPTION) }
    route("b2b/account") { b2bAccountRoutes() }.describe { tag(ApiTags.B2B) }
    route("b2b/quotes") { b2bQuoteRoutes() }.describe { tag(ApiTags.B2B) }
    route("b2b/purchase-orders") { b2bPurchaseOrderRoutes() }.describe { tag(ApiTags.B2B) }
    route("stores") { storeLocatorRoutes() }.describe { tag(ApiTags.OMNICHANNEL) }
    route("cms") { cmsRoutes() }.describe { tag(ApiTags.CONTENT) }
    route("seo") { seoRoutes() }.describe { tag(ApiTags.CONTENT) }
    route("policies") { policyRoutes() }.describe { tag(ApiTags.POLICY) }
    route("policy-consents") { consentRoutes() }.describe { tag(ApiTags.CONSENT) }
    route("refund-requests") { refundRequestRoutes() }.describe { tag(ApiTags.REFUND) }
    route("rma-requests") { rmaRoutes() }.describe { tag(ApiTags.RMA) }
    route("recommendations") { recommendationRoutes() }.describe { tag(ApiTags.RECOMMENDATION) }
    route("coupons") { couponRoutes() }.describe { tag(ApiTags.COUPON) }
    route("notifications") { notificationRoutes() }.describe { tag(ApiTags.NOTIFICATION) }
    route("privacy") { privacyRoutes() }.describe { tag(ApiTags.PRIVACY) }
    route("api-keys") { apiKeyRoutes() }.describe { tag(ApiTags.API_KEYS) }
}

private fun Route.sellerRoutes() {
    route("seller") {
        sellerAuth {
            route("shops") { shopSellerRoutesV1() }.describe { tag(ApiTags.SHOP) }
            route("products") { productSellerRoutes() }.describe { tag(ApiTags.PRODUCT) }
            route("inventories") { inventorySellerRoutes() }.describe { tag(ApiTags.INVENTORY) }
            route("warehouses") { warehouseSellerRoutes() }.describe { tag(ApiTags.WAREHOUSE) }
            route("kyc") { sellerKycRoutes() }.describe { tag(ApiTags.KYC) }
            route("orders") {
                orderSellerRoutes()
                pickupActionRoutes()
            }.describe { tag(ApiTags.ORDER) }
            route("pickup-orders") { pickupSellerListRoutes() }.describe { tag(ApiTags.OMNICHANNEL) }
            route("pos/sales") { posSellerRoutes() }.describe { tag(ApiTags.OMNICHANNEL) }
            route("refund-requests") { refundSellerRoutes() }.describe { tag(ApiTags.REFUND) }
            route("rma-requests") { rmaSellerRoutes() }.describe { tag(ApiTags.RMA) }
            route("dashboard") { dashboardSellerRoutes() }.describe { tag(ApiTags.DASHBOARD) }
            route("payouts") { payoutSellerRoutes() }.describe { tag(ApiTags.PAYOUT) }
            route("reviews") { sellerOwnReviewRoutes() }.describe { tag(ApiTags.SELLER) }
            route("commission") { commissionSellerRoutes() }.describe { tag(ApiTags.SELLER) }
            route("b2b/quotes") { b2bSellerQuoteRoutes() }.describe { tag(ApiTags.B2B) }
            route("b2b/purchase-orders") { b2bSellerPurchaseOrderRoutes() }.describe { tag(ApiTags.B2B) }
        }
    }
}

private fun Route.adminRoutes() {
    route("admin") {
        adminAuth {
            route("auth") { authAdminRoutes() }.describe { tag(ApiTags.AUTH) }
            route("brands") { brandAdminRoutes() }.describe { tag(ApiTags.BRAND) }
            route("product-categories") { productCategoryAdminRoutes() }.describe { tag(ApiTags.PRODUCT_CATEGORY) }
            route("product-subcategories") { productSubCategoryAdminRoutes() }.describe { tag(ApiTags.PRODUCT_SUB_CATEGORY) }
            route("shop-categories") { shopCategoryAdminRoutes() }.describe { tag(ApiTags.SHOP_CATEGORY) }
            route("shops") { shopAdminRoutes() }.describe { tag(ApiTags.SHOP) }
            route("sellers") { adminKycRoutes() }.describe { tag(ApiTags.KYC) }
            route("products") {
                productAdminRoutes()
                productTaxClassAdminRoutes()
                productTranslationAdminRoutes()
            }.describe { tag(ApiTags.PRODUCT) }
            route("inventories") { inventoryAdminRoutes() }.describe { tag(ApiTags.INVENTORY) }
            route("orders") { orderAdminRoutes() }.describe { tag(ApiTags.ORDER) }
            route("payments") {
                paymentAdminRoutes()
                paymentRiskAdminRoutes()
            }.describe { tag(ApiTags.PAYMENT) }
            route("fraud") { fraudAdminRoutes() }.describe { tag(ApiTags.FRAUD) }
            route("chargebacks") { chargebackAdminRoutes() }.describe { tag(ApiTags.FRAUD) }
            route("gift-cards") { giftCardAdminRoutes() }.describe { tag(ApiTags.LOYALTY) }
            route("wallets") { walletAdminRoutes() }.describe { tag(ApiTags.LOYALTY) }
            route("seller-reviews") { sellerReviewAdminRoutes() }.describe { tag(ApiTags.SELLER) }
            route("disputes") { disputeAdminRoutes() }.describe { tag(ApiTags.SELLER) }
            route("commission-rules") { commissionAdminRoutes() }.describe { tag(ApiTags.SELLER) }
            route("subscription-plans") { subscriptionPlanAdminRoutes() }.describe { tag(ApiTags.SUBSCRIPTION) }
            route("subscriptions") { subscriptionAdminRoutes() }.describe { tag(ApiTags.SUBSCRIPTION) }
            route("b2b") { b2bAdminRoutes() }.describe { tag(ApiTags.B2B) }
            route("cms/pages") { cmsAdminRoutes() }.describe { tag(ApiTags.CONTENT) }
            route("seo") { seoAdminRoutes() }.describe { tag(ApiTags.CONTENT) }
            route("refund-requests") { refundAdminRoutes() }.describe { tag(ApiTags.REFUND) }
            route("rma-requests") { rmaAdminRoutes() }.describe { tag(ApiTags.RMA) }
            route("policies") { policyAdminRoutes() }.describe { tag(ApiTags.POLICY) }
            route("policy-consents") { consentAdminRoutes() }.describe { tag(ApiTags.CONSENT) }
            route("shipping-methods") { shippingMethodAdminRoutes() }.describe { tag(ApiTags.SHIPPING_METHOD) }
            route("coupons") { couponAdminRoutes() }.describe { tag(ApiTags.COUPON) }
            route("promotions") { promotionAdminRoutes() }.describe { tag(ApiTags.COUPON) }
            route("reviews") { reviewAdminRoutes() }.describe { tag(ApiTags.REVIEW) }
            route("dashboard") { dashboardAdminRoutes() }.describe { tag(ApiTags.DASHBOARD) }
            route("analytics") { analyticsAdminRoutes() }.describe { tag(ApiTags.ANALYTICS) }
            route("audit-logs") { auditLogAdminRoutes() }.describe { tag(ApiTags.AUDIT_LOG) }
            route("payouts") { payoutAdminRoutes() }.describe { tag(ApiTags.PAYOUT) }
            route("fx-rates") { fxAdminRoutes() }.describe { tag(ApiTags.FX) }
            route("tax-rules") { taxAdminRoutes() }.describe { tag(ApiTags.TAX) }
            route("tax-classes") { taxClassAdminRoutes() }.describe { tag(ApiTags.TAX) }
            route("tax-zones") { taxZoneAdminRoutes() }.describe { tag(ApiTags.TAX) }
            route("tax-exemptions") { taxExemptionAdminRoutes() }.describe { tag(ApiTags.TAX) }
            route("feature-flags") { featureFlagRoutes() }.describe { tag(ApiTags.FEATURE_FLAGS) }
            route("outbox") { outboxAdminRoutes() }.describe { tag(ApiTags.OUTBOX) }
        }
    }
}
