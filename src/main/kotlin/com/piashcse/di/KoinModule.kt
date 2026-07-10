package com.piashcse.di

import com.piashcse.feature.audit_log.AuditLogRepository
import com.piashcse.feature.audit_log.AuditLogRepositoryImpl
import com.piashcse.feature.audit_log.AuditLogService
import com.piashcse.feature.auth.AuthRepository
import com.piashcse.feature.auth.AuthRepositoryImpl
import com.piashcse.feature.auth.PasswordManagementService
import com.piashcse.feature.auth.TokenManagementService
import com.piashcse.feature.auth.UserAuthenticationService
import com.piashcse.feature.brand.BrandRepository
import com.piashcse.feature.brand.BrandRepositoryImpl
import com.piashcse.feature.brand.BrandService
import com.piashcse.feature.cart.CartRepository
import com.piashcse.feature.cart.CartRepositoryImpl
import com.piashcse.feature.cart.CartService
import com.piashcse.feature.consent.ConsentRepository
import com.piashcse.feature.consent.ConsentRepositoryImpl
import com.piashcse.feature.consent.ConsentService
import com.piashcse.feature.coupon.CouponRepository
import com.piashcse.feature.coupon.CouponRepositoryImpl
import com.piashcse.feature.coupon.CouponService
import com.piashcse.feature.dashboard.DashboardRepository
import com.piashcse.feature.dashboard.DashboardRepositoryImpl
import com.piashcse.feature.dashboard.DashboardService
import com.piashcse.feature.inventory.InventoryRepository
import com.piashcse.feature.inventory.InventoryRepositoryImpl
import com.piashcse.feature.inventory.InventoryService
import com.piashcse.feature.order.CheckoutOrchestrator
import com.piashcse.feature.order.OrderManagementService
import com.piashcse.feature.order.OrderQueryService
import com.piashcse.feature.order.OrderRepository
import com.piashcse.feature.order.OrderRepositoryImpl
import com.piashcse.feature.payment.PaymentRepository
import com.piashcse.feature.payment.PaymentRepositoryImpl
import com.piashcse.feature.payment.PaymentService
import com.piashcse.feature.policy.PolicyRepository
import com.piashcse.feature.policy.PolicyRepositoryImpl
import com.piashcse.feature.policy.PolicyService
import com.piashcse.feature.product.ProductCatalogService
import com.piashcse.feature.product.ProductCrudService
import com.piashcse.feature.product.ProductQueryService
import com.piashcse.feature.product.ProductRepository
import com.piashcse.feature.product.ProductRepositoryImpl
import com.piashcse.feature.product_category.ProductCategoryRepository
import com.piashcse.feature.product_category.ProductCategoryRepositoryImpl
import com.piashcse.feature.product_category.ProductCategoryService
import com.piashcse.feature.product_sub_category.ProductSubCategoryRepository
import com.piashcse.feature.product_sub_category.ProductSubCategoryRepositoryImpl
import com.piashcse.feature.product_sub_category.ProductSubCategoryService
import com.piashcse.feature.profile.ProfileRepository
import com.piashcse.feature.profile.ProfileRepositoryImpl
import com.piashcse.feature.profile.ProfileService
import com.piashcse.feature.refund_request.RefundRequestRepository
import com.piashcse.feature.refund_request.RefundRequestRepositoryImpl
import com.piashcse.feature.refund_request.RefundRequestService
import com.piashcse.feature.review_rating.ReviewRatingRepository
import com.piashcse.feature.review_rating.ReviewRatingRepositoryImpl
import com.piashcse.feature.review_rating.ReviewRatingService
import com.piashcse.feature.shipping_address.ShippingAddressRepository
import com.piashcse.feature.shipping_address.ShippingAddressRepositoryImpl
import com.piashcse.feature.shipping_address.ShippingAddressService
import com.piashcse.feature.shipping_method.ShippingMethodRepository
import com.piashcse.feature.shipping_method.ShippingMethodRepositoryImpl
import com.piashcse.feature.shipping_method.ShippingMethodService
import com.piashcse.feature.shop.ShopRepository
import com.piashcse.feature.shop.ShopRepositoryImpl
import com.piashcse.feature.shop.ShopService
import com.piashcse.feature.shop_category.ShopCategoryRepository
import com.piashcse.feature.shop_category.ShopCategoryRepositoryImpl
import com.piashcse.feature.shop_category.ShopCategoryService
import com.piashcse.feature.wishlist.WishListRepository
import com.piashcse.feature.wishlist.WishListRepositoryImpl
import com.piashcse.feature.wishlist.WishListService
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

        // Services — unchanged (single-repo delegates)
        single { AuditLogService(get()) }
        single { BrandService(get()) }
        single { CartService(get()) }
        single { ConsentService(get()) }
        single { CouponService(get()) }
        single { DashboardService(get()) }
        single { InventoryService(get()) }
        single { PaymentService(get()) }
        single { PolicyService(get()) }
        single { ProductCategoryService(get()) }
        single { ProductSubCategoryService(get()) }
        single { ProfileService(get()) }
        single { RefundRequestService(get()) }
        single { ReviewRatingService(get()) }
        single { ShippingAddressService(get()) }
        single { ShippingMethodService(get()) }
        single { ShopService(get()) }
        single { ShopCategoryService(get()) }
        single { WishListService(get()) }

        // Split auth services
        single { UserAuthenticationService(get()) }
        single { PasswordManagementService(get()) }
        single { TokenManagementService(get()) }

        // Split product services
        single { ProductCatalogService(get()) }
        single { ProductQueryService(get()) }
        single { ProductCrudService(get()) }

        // Split order services
        single { CheckoutOrchestrator(get()) }
        single { OrderManagementService(get()) }
        single { OrderQueryService(get()) }

        // Non-DI services
        single { UploadService }
    }
