package com.piashcse.plugins

import com.piashcse.models.AddWisList
import com.piashcse.models.bands.AddBrand
import com.piashcse.models.cart.AddCart
import com.piashcse.models.category.AddProductCategory
import com.piashcse.models.order.AddOrder
import com.piashcse.models.product.request.AddProduct
import com.piashcse.models.shipping.AddShipping
import com.piashcse.models.shop.AddShop
import com.piashcse.models.shop.AddShopCategory
import com.piashcse.models.subcategory.AddProductSubCategory
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*

fun Application.configureRequestValidation() {
    install(RequestValidation) {
        validate<AddProductCategory> { productCategory ->
            productCategory.validation()
            ValidationResult.Valid
        }
        validate<AddProductSubCategory> { productSubCategory ->
            productSubCategory.validation()
            ValidationResult.Valid
        }
        validate<AddShop> { shop ->
            shop.validation()
            ValidationResult.Valid
        }
        validate<AddShopCategory> { shopCategory ->
            shopCategory.validation()
            ValidationResult.Valid
        }
        validate<AddBrand> { brand ->
            brand.validation()
            ValidationResult.Valid
        }
        validate<AddProduct> { product ->
            product.validation()
            ValidationResult.Valid
        }
        validate<AddShipping> { shipping ->
            shipping.validation()
            ValidationResult.Valid
        }
        validate<AddOrder> { order ->
            order.validation()
            ValidationResult.Valid
        }
        validate<AddCart> { cart ->
            cart.validation()
            ValidationResult.Valid
        }
        validate<AddWisList> { wishlist ->
            wishlist.validation()
            ValidationResult.Valid
        }
    }
}