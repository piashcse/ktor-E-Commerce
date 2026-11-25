package com.piashcse.plugin

import com.piashcse.constants.Permission
import com.piashcse.feature.analytics.analyticsAdminRoutes
import com.piashcse.feature.audit_log.auditLogAdminRoutes
import com.piashcse.feature.auth.authAdminRoutes
import com.piashcse.feature.b2b.b2bAdminRoutes
import com.piashcse.feature.brand.brandAdminRoutes
import com.piashcse.feature.cms.cmsAdminRoutes
import com.piashcse.feature.cms.seoAdminRoutes
import com.piashcse.feature.consent.consentAdminRoutes
import com.piashcse.feature.coupon.couponAdminRoutes
import com.piashcse.feature.dashboard.adminQueueRoutes
import com.piashcse.feature.dashboard.dashboardAdminRoutes
import com.piashcse.feature.featureflag.featureFlagRoutes
import com.piashcse.feature.fraud.chargebackAdminRoutes
import com.piashcse.feature.fraud.fraudAdminRoutes
import com.piashcse.feature.fraud.paymentRiskAdminRoutes
import com.piashcse.feature.fx.fxAdminRoutes
import com.piashcse.feature.i18n.productTranslationAdminRoutes
import com.piashcse.feature.inventory.inventoryAdminRoutes
import com.piashcse.feature.kyc.adminKycRoutes
import com.piashcse.feature.loyalty.giftCardAdminRoutes
import com.piashcse.feature.loyalty.walletAdminRoutes
import com.piashcse.feature.order.orderAdminRoutes
import com.piashcse.feature.outbox.outboxAdminRoutes
import com.piashcse.feature.payment.paymentAdminRoutes
import com.piashcse.feature.payout.payoutAdminRoutes
import com.piashcse.feature.policy.policyAdminRoutes
import com.piashcse.feature.product.productAdminRoutes
import com.piashcse.feature.product_category.productCategoryAdminRoutes
import com.piashcse.feature.product_sub_category.productSubCategoryAdminRoutes
import com.piashcse.feature.promotion.promotionAdminRoutes
import com.piashcse.feature.rbac.adminRbacRoutes
import com.piashcse.feature.refund_request.refundAdminRoutes
import com.piashcse.feature.review_rating.reviewAdminRoutes
import com.piashcse.feature.rma.rmaAdminRoutes
import com.piashcse.feature.seller.commissionAdminRoutes
import com.piashcse.feature.seller.disputeAdminRoutes
import com.piashcse.feature.seller.sellerReviewAdminRoutes
import com.piashcse.feature.shipping_method.shippingMethodAdminRoutes
import com.piashcse.feature.shop.shopAdminRoutes
import com.piashcse.feature.shop_category.shopCategoryAdminRoutes
import com.piashcse.feature.subscription.subscriptionAdminRoutes
import com.piashcse.feature.subscription.subscriptionPlanAdminRoutes
import com.piashcse.feature.support.supportAdminRoutes
import com.piashcse.feature.tax.productTaxClassAdminRoutes
import com.piashcse.feature.tax.taxAdminRoutes
import com.piashcse.feature.tax.taxClassAdminRoutes
import com.piashcse.feature.tax.taxExemptionAdminRoutes
import com.piashcse.feature.tax.taxZoneAdminRoutes
import com.piashcse.feature.user.adminUserRoutes
import com.piashcse.feature.webhook.webhookAdminRoutes
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.server.routing.openapi.describe

internal fun Route.adminRoutes() {
    route("admin") {
        adminAuth {
            adminIdentityRoutes()
            adminCatalogRoutes()
            requirePermission(Permission.SHOP_MANAGE) {
                route("shops") { shopAdminRoutes() }.describe { tag(ApiTags.SHOP) }
            }
            requirePermission(Permission.SHOP_MANAGE) {
                route("sellers") { adminKycRoutes() }.describe { tag(ApiTags.KYC) }
            }
            requirePermission(Permission.INVENTORY_MANAGE) {
                route("inventories") { inventoryAdminRoutes() }.describe { tag(ApiTags.INVENTORY) }
            }
            requirePermission(Permission.ORDER_MANAGE) {
                route("orders") { orderAdminRoutes() }.describe { tag(ApiTags.ORDER) }
            }
            requirePermission(Permission.PAYMENT_MANAGE) {
                route("payments") {
                    paymentAdminRoutes()
                    paymentRiskAdminRoutes()
                }.describe { tag(ApiTags.PAYMENT) }
            }
            requirePermission(Permission.FRAUD_MANAGE) {
                route("fraud") { fraudAdminRoutes() }.describe { tag(ApiTags.FRAUD) }
                route("chargebacks") { chargebackAdminRoutes() }.describe { tag(ApiTags.FRAUD) }
            }
            requirePermission(Permission.PAYMENT_MANAGE) {
                route("gift-cards") { giftCardAdminRoutes() }.describe { tag(ApiTags.LOYALTY) }
                route("wallets") { walletAdminRoutes() }.describe { tag(ApiTags.LOYALTY) }
            }
            requirePermission(Permission.REVIEW_MANAGE) {
                route("seller-reviews") { sellerReviewAdminRoutes() }.describe { tag(ApiTags.SELLER) }
                route("reviews") { reviewAdminRoutes() }.describe { tag(ApiTags.REVIEW) }
            }
            requirePermission(Permission.SUPPORT_MANAGE) {
                route("disputes") { disputeAdminRoutes() }.describe { tag(ApiTags.SELLER) }
            }
            requirePermission(Permission.PAYOUT_MANAGE) {
                route("commission-rules") { commissionAdminRoutes() }.describe { tag(ApiTags.SELLER) }
            }
            requirePermission(Permission.SUBSCRIPTION_MANAGE) {
                route("subscription-plans") { subscriptionPlanAdminRoutes() }.describe { tag(ApiTags.SUBSCRIPTION) }
                route("subscriptions") { subscriptionAdminRoutes() }.describe { tag(ApiTags.SUBSCRIPTION) }
            }
            requirePermission(Permission.B2B_MANAGE) {
                route("b2b") { b2bAdminRoutes() }.describe { tag(ApiTags.B2B) }
            }
            requirePermission(Permission.CMS_MANAGE) {
                route("cms/pages") { cmsAdminRoutes() }.describe { tag(ApiTags.CONTENT) }
                route("seo") { seoAdminRoutes() }.describe { tag(ApiTags.CONTENT) }
            }
            requirePermission(Permission.SUPPORT_MANAGE) {
                route("support/tickets") { supportAdminRoutes() }.describe { tag(ApiTags.SUPPORT) }
            }
            requirePermission(Permission.REFUND_MANAGE) {
                route("refund-requests") { refundAdminRoutes() }.describe { tag(ApiTags.REFUND) }
            }
            requirePermission(Permission.REFUND_MANAGE) {
                route("rma-requests") { rmaAdminRoutes() }.describe { tag(ApiTags.RMA) }
            }
            requirePermission(Permission.POLICY_MANAGE) {
                route("policies") { policyAdminRoutes() }.describe { tag(ApiTags.POLICY) }
                route("policy-consents") { consentAdminRoutes() }.describe { tag(ApiTags.CONSENT) }
            }
            requirePermission(Permission.SHIPPING_MANAGE) {
                route("shipping-methods") { shippingMethodAdminRoutes() }.describe { tag(ApiTags.SHIPPING_METHOD) }
            }
            requirePermission(Permission.COUPON_MANAGE) {
                route("coupons") { couponAdminRoutes() }.describe { tag(ApiTags.COUPON) }
            }
            requirePermission(Permission.COUPON_MANAGE) {
                route("promotions") { promotionAdminRoutes() }.describe { tag(ApiTags.COUPON) }
            }
            requirePermission(Permission.DASHBOARD_VIEW) {
                route("dashboard") { dashboardAdminRoutes() }.describe { tag(ApiTags.DASHBOARD) }
            }
            requirePermission(Permission.DASHBOARD_VIEW) {
                route("queues") { adminQueueRoutes() }.describe { tag(ApiTags.DASHBOARD) }
            }
            requirePermission(Permission.DASHBOARD_VIEW) {
                route("analytics") { analyticsAdminRoutes() }.describe { tag(ApiTags.ANALYTICS) }
            }
            requirePermission(Permission.AUDIT_VIEW) {
                route("audit-logs") { auditLogAdminRoutes() }.describe { tag(ApiTags.AUDIT_LOG) }
            }
            requirePermission(Permission.PAYMENT_MANAGE) {
                route("webhooks") { webhookAdminRoutes() }.describe { tag(ApiTags.PAYMENT) }
            }
            requirePermission(Permission.PAYOUT_MANAGE) {
                route("payouts") { payoutAdminRoutes() }.describe { tag(ApiTags.PAYOUT) }
            }
            requirePermission(Permission.FX_MANAGE) {
                route("fx-rates") { fxAdminRoutes() }.describe { tag(ApiTags.FX) }
            }
            requirePermission(Permission.TAX_MANAGE) {
                route("tax-rules") { taxAdminRoutes() }.describe { tag(ApiTags.TAX) }
                route("tax-classes") { taxClassAdminRoutes() }.describe { tag(ApiTags.TAX) }
                route("tax-zones") { taxZoneAdminRoutes() }.describe { tag(ApiTags.TAX) }
                route("tax-exemptions") { taxExemptionAdminRoutes() }.describe { tag(ApiTags.TAX) }
            }
            requirePermission(Permission.FEATURE_FLAG_MANAGE) {
                route("feature-flags") { featureFlagRoutes() }.describe { tag(ApiTags.FEATURE_FLAGS) }
            }
            requirePermission(Permission.OUTBOX_MANAGE) {
                route("outbox") { outboxAdminRoutes() }.describe { tag(ApiTags.OUTBOX) }
            }
        }
    }
}

internal fun Route.adminIdentityRoutes() {
    requirePermission(Permission.USER_MANAGE) {
        route("auth") { authAdminRoutes() }.describe { tag(ApiTags.AUTH) }
    }
    route("users") { adminUserRoutes() }.describe { tag(ApiTags.AUTH) }
    route("rbac") { adminRbacRoutes() }.describe { tag(ApiTags.RBAC) }
}

internal fun Route.adminCatalogRoutes() {
    requirePermission(Permission.CATALOG_MANAGE) {
        route("brands") { brandAdminRoutes() }.describe { tag(ApiTags.BRAND) }
    }
    requirePermission(Permission.CATALOG_MANAGE) {
        route("product-categories") { productCategoryAdminRoutes() }.describe { tag(ApiTags.PRODUCT_CATEGORY) }
    }
    requirePermission(Permission.CATALOG_MANAGE) {
        route("product-subcategories") { productSubCategoryAdminRoutes() }.describe { tag(ApiTags.PRODUCT_SUB_CATEGORY) }
    }
    requirePermission(Permission.CATALOG_MANAGE) {
        route("shop-categories") { shopCategoryAdminRoutes() }.describe { tag(ApiTags.SHOP_CATEGORY) }
    }
    requirePermission(Permission.CATALOG_MANAGE) {
        route("products") {
            productAdminRoutes()
            productTaxClassAdminRoutes()
            productTranslationAdminRoutes()
        }.describe { tag(ApiTags.PRODUCT) }
    }
}
