package com.piashcse.di

import com.piashcse.modules.auth.controller.AuthController
import com.piashcse.modules.brand.controller.BrandController
import com.piashcse.modules.cart.controller.CartController
import com.piashcse.modules.consent.controller.ConsentController
import com.piashcse.modules.order.controller.OrderController
import com.piashcse.modules.payment.controller.PaymentController
import com.piashcse.modules.policy.controller.PolicyController
import com.piashcse.modules.product.controller.ProductController
import com.piashcse.modules.productcategory.controller.ProductCategoryController
import com.piashcse.modules.productsubcategory.controller.ProductSubCategoryController
import com.piashcse.modules.profile.controller.ProfileController
import com.piashcse.modules.review_rating.controller.ReviewRatingController
import com.piashcse.modules.shipping.controller.ShippingController
import com.piashcse.modules.shop.controller.ShopController
import com.piashcse.modules.shopcategory.controller.ShopCategoryController
import com.piashcse.modules.wishlist.controller.WishListController
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