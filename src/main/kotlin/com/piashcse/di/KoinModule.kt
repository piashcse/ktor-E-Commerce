package com.piashcse.di

import com.piashcse.feature.audit_log.AuditLogRepository
import com.piashcse.feature.audit_log.AuditLogRepositoryImpl
import com.piashcse.feature.auth.AuthRepository
import com.piashcse.feature.auth.AuthRepositoryImpl
import com.piashcse.feature.auth.UserAuthenticationService
import com.piashcse.feature.brand.BrandRepository
import com.piashcse.feature.brand.BrandRepositoryImpl
import com.piashcse.feature.cart.CartRepository
import com.piashcse.feature.cart.CartRepositoryImpl
import com.piashcse.feature.consent.ConsentRepository
import com.piashcse.feature.consent.ConsentRepositoryImpl
import com.piashcse.feature.coupon.CouponRepository
import com.piashcse.feature.coupon.CouponRepositoryImpl
import com.piashcse.feature.dashboard.DashboardRepository
import com.piashcse.feature.dashboard.DashboardRepositoryImpl
import com.piashcse.feature.inventory.InventoryRepository
import com.piashcse.feature.inventory.InventoryRepositoryImpl
import com.piashcse.feature.order.OrderRepository
import com.piashcse.feature.order.OrderRepositoryImpl
import com.piashcse.feature.payment.PaymentRepository
import com.piashcse.feature.payment.PaymentRepositoryImpl
import com.piashcse.feature.policy.PolicyRepository
import com.piashcse.feature.policy.PolicyRepositoryImpl
import com.piashcse.feature.product.ProductCatalogService
import com.piashcse.feature.product.ProductCrudService
import com.piashcse.feature.product.ProductRepository
import com.piashcse.feature.product.ProductRepositoryImpl
import com.piashcse.feature.product_category.ProductCategoryRepository
import com.piashcse.feature.product_category.ProductCategoryRepositoryImpl
import com.piashcse.feature.product_sub_category.ProductSubCategoryRepository
import com.piashcse.feature.product_sub_category.ProductSubCategoryRepositoryImpl
import com.piashcse.feature.profile.ProfileRepository
import com.piashcse.feature.profile.ProfileRepositoryImpl
import com.piashcse.feature.profile.ProfileService
import com.piashcse.feature.refund_request.RefundRequestRepository
import com.piashcse.feature.refund_request.RefundRequestRepositoryImpl
import com.piashcse.feature.review_rating.ReviewRatingRepository
import com.piashcse.feature.review_rating.ReviewRatingRepositoryImpl
import com.piashcse.feature.shipping_address.ShippingAddressRepository
import com.piashcse.feature.shipping_address.ShippingAddressRepositoryImpl
import com.piashcse.feature.shipping_method.ShippingMethodRepository
import com.piashcse.feature.shipping_method.ShippingMethodRepositoryImpl
import com.piashcse.feature.shop.ShopRepository
import com.piashcse.feature.shop.ShopRepositoryImpl
import com.piashcse.feature.shop_category.ShopCategoryRepository
import com.piashcse.feature.shop_category.ShopCategoryRepositoryImpl
import com.piashcse.feature.wishlist.WishListRepository
import com.piashcse.feature.wishlist.WishListRepositoryImpl
import com.piashcse.service.UploadService
import org.koin.dsl.module

val serviceModule =
    module {
        // Repositories
        single<AuthRepository> { AuthRepositoryImpl() }
        single<AuditLogRepository> { AuditLogRepositoryImpl() }
        single<BrandRepository> { BrandRepositoryImpl() }
        single<CartRepository> { CartRepositoryImpl() }
        single<ConsentRepository> { ConsentRepositoryImpl() }
        single<CouponRepository> { CouponRepositoryImpl() }
        single<DashboardRepository> { DashboardRepositoryImpl() }
        single<InventoryRepository> { InventoryRepositoryImpl() }
        single<OrderRepository> { OrderRepositoryImpl() }
        single<PaymentRepository> { PaymentRepositoryImpl() }
        single<PolicyRepository> { PolicyRepositoryImpl() }
        single<ProductRepository> { ProductRepositoryImpl() }
        single<ProductCategoryRepository> { ProductCategoryRepositoryImpl() }
        single<ProductSubCategoryRepository> { ProductSubCategoryRepositoryImpl() }
        single<ProfileRepository> { ProfileRepositoryImpl() }
        single<RefundRequestRepository> { RefundRequestRepositoryImpl() }
        single<ReviewRatingRepository> { ReviewRatingRepositoryImpl() }
        single<ShippingAddressRepository> { ShippingAddressRepositoryImpl() }
        single<ShippingMethodRepository> { ShippingMethodRepositoryImpl() }
        single<ShopRepository> { ShopRepositoryImpl() }
        single<ShopCategoryRepository> { ShopCategoryRepositoryImpl() }
        single<WishListRepository> { WishListRepositoryImpl() }

        // Services with business logic
        single { ProfileService(get()) }

        // Split auth services
        single { UserAuthenticationService(get()) }

        // Split product services
        single { ProductCatalogService(get()) }
        single { ProductCrudService(get()) }

        // Non-DI services
        single { UploadService }
    }
