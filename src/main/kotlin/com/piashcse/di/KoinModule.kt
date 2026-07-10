package com.piashcse.di

import com.piashcse.feature.audit_log.AuditLogService
import com.piashcse.feature.auth.AuthService
import com.piashcse.feature.brand.BrandService
import com.piashcse.feature.cart.CartService
import com.piashcse.feature.consent.ConsentService
import com.piashcse.feature.coupon.CouponService
import com.piashcse.feature.dashboard.DashboardService
import com.piashcse.feature.inventory.InventoryService
import com.piashcse.feature.order.OrderService
import com.piashcse.feature.payment.PaymentService
import com.piashcse.feature.policy.PolicyService
import com.piashcse.feature.product.ProductService
import com.piashcse.feature.product_category.ProductCategoryService
import com.piashcse.feature.product_sub_category.ProductSubCategoryService
import com.piashcse.feature.profile.ProfileService
import com.piashcse.feature.refund_request.RefundRequestService
import com.piashcse.feature.review_rating.ReviewRatingService
import com.piashcse.feature.shipping_address.ShippingAddressService
import com.piashcse.feature.shipping_method.ShippingMethodService
import com.piashcse.feature.shop.ShopService
import com.piashcse.feature.shop_category.ShopCategoryService
import com.piashcse.feature.wishlist.WishListService
import com.piashcse.service.CacheService
import com.piashcse.service.UploadService
import org.koin.dsl.module

val serviceModule =
    module {
        single { AuditLogService() }
        single { AuthService() }
        single { BrandService() }
        single { CartService() }
        single { OrderService() }
        single { ProductService(CacheService.cache) }
        single { ProductCategoryService() }
        single { ProductSubCategoryService() }
        single { ShippingAddressService() }
        single { ShippingMethodService() }
        single { ShopService() }
        single { ShopCategoryService() }
        single { CouponService() }
        single { DashboardService() }
        single { ProfileService() }
        single { WishListService() }
        single { PaymentService() }
        single { ReviewRatingService() }
        single { PolicyService() }
        single { ConsentService() }
        single { InventoryService() }
        single { RefundRequestService() }
        single { UploadService }
    }
