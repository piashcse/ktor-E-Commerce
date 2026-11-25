package com.piashcse.plugin

import com.piashcse.feature.analytics.analyticsRoutes
import com.piashcse.feature.apikey.apiKeyRoutes
import com.piashcse.feature.auth.authRoutes
import com.piashcse.feature.b2b.b2bAccountRoutes
import com.piashcse.feature.b2b.b2bInvoiceRoutes
import com.piashcse.feature.b2b.b2bPurchaseOrderRoutes
import com.piashcse.feature.b2b.b2bQuoteRoutes
import com.piashcse.feature.brand.brandRoutes
import com.piashcse.feature.cart.cartRoutes
import com.piashcse.feature.checkout.checkoutRoutes
import com.piashcse.feature.cms.cmsRoutes
import com.piashcse.feature.cms.seoRoutes
import com.piashcse.feature.config.configRoutes
import com.piashcse.feature.consent.consentRoutes
import com.piashcse.feature.coupon.couponRoutes
import com.piashcse.feature.fx.fxPublicRoutes
import com.piashcse.feature.i18n.i18nRoutes
import com.piashcse.feature.invoice.orderDocumentRoutes
import com.piashcse.feature.loyalty.referralRoutes
import com.piashcse.feature.loyalty.walletRoutes
import com.piashcse.feature.notification.notificationRoutes
import com.piashcse.feature.omnichannel.storeLocatorRoutes
import com.piashcse.feature.order.orderRoutes
import com.piashcse.feature.payment.paymentRoutes
import com.piashcse.feature.policy.policyRoutes
import com.piashcse.feature.privacy.privacyRoutes
import com.piashcse.feature.product.productRoutes
import com.piashcse.feature.product_category.productCategoryRoutes
import com.piashcse.feature.product_sub_category.productSubCategoryRoutes
import com.piashcse.feature.profile.profileRoutes
import com.piashcse.feature.rbac.invitationRoutes
import com.piashcse.feature.rbac.meRoutes
import com.piashcse.feature.recommendation.recommendationRoutes
import com.piashcse.feature.refund_request.refundRequestRoutes
import com.piashcse.feature.review_rating.reviewRatingRoutes
import com.piashcse.feature.rma.rmaRoutes
import com.piashcse.feature.seller.disputeRoutes
import com.piashcse.feature.seller.sellerPublicRoutes
import com.piashcse.feature.seller.sellerReviewRoutes
import com.piashcse.feature.shipping.orderShipmentRoutes
import com.piashcse.feature.shipping.shipmentRoutes
import com.piashcse.feature.shop.shopRoutes
import com.piashcse.feature.shop_category.shopCategoryRoutes
import com.piashcse.feature.stockalert.stockAlertRoutes
import com.piashcse.feature.subscription.digitalLibraryRoutes
import com.piashcse.feature.subscription.subscriptionPlanRoutes
import com.piashcse.feature.subscription.subscriptionRoutes
import com.piashcse.feature.support.supportRoutes
import com.piashcse.feature.wishlist.wishlistRoutes
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.server.routing.openapi.describe

internal fun Route.customerRoutes() {
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
    route("wishlists") { wishlistRoutes() }.describe { tag(ApiTags.WISHLIST) }
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
    route("config") { configRoutes() }.describe { tag(ApiTags.CONTENT) }
    route("fx-rates") { fxPublicRoutes() }.describe { tag(ApiTags.FX) }
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
    route("b2b/invoices") { b2bInvoiceRoutes() }.describe { tag(ApiTags.B2B) }
    route("stores") { storeLocatorRoutes() }.describe { tag(ApiTags.OMNICHANNEL) }
    route("cms") { cmsRoutes() }.describe { tag(ApiTags.CONTENT) }
    route("seo") { seoRoutes() }.describe { tag(ApiTags.CONTENT) }
    route("support/tickets") { supportRoutes() }.describe { tag(ApiTags.SUPPORT) }
    route("policies") { policyRoutes() }.describe { tag(ApiTags.POLICY) }
    route("policy-consents") { consentRoutes() }.describe { tag(ApiTags.CONSENT) }
    route("refund-requests") { refundRequestRoutes() }.describe { tag(ApiTags.REFUND) }
    route("rma-requests") { rmaRoutes() }.describe { tag(ApiTags.RMA) }
    route("recommendations") { recommendationRoutes() }.describe { tag(ApiTags.RECOMMENDATION) }
    route("coupons") { couponRoutes() }.describe { tag(ApiTags.COUPON) }
    route("notifications") { notificationRoutes() }.describe { tag(ApiTags.NOTIFICATION) }
    route("privacy") { privacyRoutes() }.describe { tag(ApiTags.PRIVACY) }
    route("api-keys") { apiKeyRoutes() }.describe { tag(ApiTags.API_KEYS) }
    route("me") { meRoutes() }.describe { tag(ApiTags.RBAC) }
    route("invitations") { invitationRoutes() }.describe { tag(ApiTags.RBAC) }
}
