package com.piashcse.di

import com.piashcse.controller.*
import com.piashcse.modules.auth.service.AuthController
import com.piashcse.modules.brand.service.BrandController
import com.piashcse.modules.cart.service.CartController
import com.piashcse.modules.consent.service.ConsentController
import com.piashcse.modules.order.service.OrderController
import com.piashcse.modules.payment.service.PaymentController
import com.piashcse.modules.policy.service.PolicyController
import com.piashcse.modules.product.service.ProductController
import com.piashcse.modules.productcategory.service.ProductCategoryController
import com.piashcse.modules.productsubcategory.service.ProductSubCategoryController
import com.piashcse.modules.profile.service.ProfileController
import com.piashcse.modules.review_rating.service.ReviewRatingController
import com.piashcse.modules.shipping.service.ShippingController
import com.piashcse.modules.shop.service.ShopController
import com.piashcse.modules.shopcategory.service.ShopCategoryController
import com.piashcse.modules.wishlist.service.WishListController
import org.koin.dsl.module

val controllerModule = module {
    single { BrandController() }
    single { CartController() }
    single { OrderController() }
    single { OrderController() }
    single { ProductController() }
    single { ProductCategoryController() }
    single { ProductSubCategoryController() }
    single { ShippingController() }
    single { ShopController() }
    single { ShopCategoryController() }
    single { AuthController() }
    single { ProfileController() }
    single { WishListController() }
    single { PaymentController() }
    single { ReviewRatingController() }
    single { PolicyController() }
    single { ConsentController() }
}