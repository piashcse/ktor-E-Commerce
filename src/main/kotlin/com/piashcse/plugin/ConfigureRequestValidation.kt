package com.piashcse.plugin

import com.piashcse.model.request.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*

fun Application.configureRequestValidation() {
    install(RequestValidation) {
        validate<LoginRequest> { login ->
            login.validation()
            ValidationResult.Valid
        }
        validate<RegisterRequest> { register ->
            register.validation()
            ValidationResult.Valid
        }
        validate<ProductSearchRequest> { search ->
            search.validation()
            ValidationResult.Valid
        }
        validate<ProductCategoryRequest> { productCategory ->
            productCategory.validation()
            ValidationResult.Valid
        }
        validate<ProductSubCategoryRequest> { productSubCategory ->
            productSubCategory.validation()
            ValidationResult.Valid
        }
        validate<ShopRequest> { shop ->
            shop.validation()
            ValidationResult.Valid
        }
        validate<ShopCategoryRequest> { shopCategory ->
            shopCategory.validation()
            ValidationResult.Valid
        }
        validate<BrandRequest> { brand ->
            brand.validation()
            ValidationResult.Valid
        }
        validate<ProductRequest> { product ->
            product.validation()
            ValidationResult.Valid
        }
        validate<ShippingRequest> { shipping ->
            shipping.validation()
            ValidationResult.Valid
        }
        validate<OrderRequest> { order ->
            order.validation()
            ValidationResult.Valid
        }
        validate<CartRequest> { cart ->
            cart.validation()
            ValidationResult.Valid
        }
        validate<WishListRequest> { wishlist ->
            wishlist.validation()
            ValidationResult.Valid
        }
        validate<PaymentRequest> { payment ->
            payment.validation()
            ValidationResult.Valid
        }
    }
}