package com.piashcse.plugin

import com.piashcse.feature.b2b.b2bSellerInvoiceRoutes
import com.piashcse.feature.b2b.b2bSellerPurchaseOrderRoutes
import com.piashcse.feature.b2b.b2bSellerQuoteRoutes
import com.piashcse.feature.coupon.sellerCouponRoutes
import com.piashcse.feature.dashboard.dashboardSellerRoutes
import com.piashcse.feature.dashboard.sellerReadinessRoutes
import com.piashcse.feature.inventory.inventorySellerRoutes
import com.piashcse.feature.kyc.sellerKycRoutes
import com.piashcse.feature.notification.sellerNotificationRoutes
import com.piashcse.feature.omnichannel.pickupActionRoutes
import com.piashcse.feature.omnichannel.pickupSellerListRoutes
import com.piashcse.feature.omnichannel.posSellerRoutes
import com.piashcse.feature.omnichannel.posSessionSellerRoutes
import com.piashcse.feature.order.orderSellerRoutes
import com.piashcse.feature.payout.payoutSellerRoutes
import com.piashcse.feature.product.productSellerRoutes
import com.piashcse.feature.promotion.sellerPromotionRoutes
import com.piashcse.feature.rbac.sellerMemberRoutes
import com.piashcse.feature.refund_request.refundSellerRoutes
import com.piashcse.feature.rma.rmaSellerRoutes
import com.piashcse.feature.seller.commissionSellerRoutes
import com.piashcse.feature.seller.sellerOwnReviewRoutes
import com.piashcse.feature.shipping_method.sellerShippingMethodRoutes
import com.piashcse.feature.shop.shopSellerRoutesV1
import com.piashcse.feature.warehouse.warehouseSellerRoutes
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.server.routing.openapi.describe

internal fun Route.sellerRoutes() {
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
            route("pos/sessions") { posSessionSellerRoutes() }.describe { tag(ApiTags.OMNICHANNEL) }
            route("refund-requests") { refundSellerRoutes() }.describe { tag(ApiTags.REFUND) }
            route("rma-requests") { rmaSellerRoutes() }.describe { tag(ApiTags.RMA) }
            route("dashboard") { dashboardSellerRoutes() }.describe { tag(ApiTags.DASHBOARD) }
            route("readiness") { sellerReadinessRoutes() }.describe { tag(ApiTags.DASHBOARD) }
            route("payouts") { payoutSellerRoutes() }.describe { tag(ApiTags.PAYOUT) }
            route("reviews") { sellerOwnReviewRoutes() }.describe { tag(ApiTags.SELLER) }
            route("notifications") { sellerNotificationRoutes() }.describe { tag(ApiTags.NOTIFICATION) }
            route("members") { sellerMemberRoutes() }.describe { tag(ApiTags.RBAC) }
            route("commission") { commissionSellerRoutes() }.describe { tag(ApiTags.SELLER) }
            route("coupons") { sellerCouponRoutes() }.describe { tag(ApiTags.COUPON) }
            route("promotions") { sellerPromotionRoutes() }.describe { tag(ApiTags.COUPON) }
            route("shipping-methods") { sellerShippingMethodRoutes() }.describe { tag(ApiTags.SHIPPING_METHOD) }
            route("b2b/quotes") { b2bSellerQuoteRoutes() }.describe { tag(ApiTags.B2B) }
            route("b2b/purchase-orders") { b2bSellerPurchaseOrderRoutes() }.describe { tag(ApiTags.B2B) }
            route("b2b/invoices") { b2bSellerInvoiceRoutes() }.describe { tag(ApiTags.B2B) }
        }
    }
}
