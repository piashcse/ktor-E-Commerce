package com.piashcse.routing

import com.piashcse.controller.BrandController
import com.piashcse.models.PagingData
import com.piashcse.models.bands.AddBrand
import com.piashcse.models.bands.DeleteBrand
import com.piashcse.models.bands.UpdateBrand
import com.piashcse.models.user.body.JwtTokenBody
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.Response
import com.piashcse.utils.authenticateWithJwt
import com.papsign.ktor.openapigen.route.path.auth.delete
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.path.auth.post
import com.papsign.ktor.openapigen.route.path.auth.put
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*

fun NormalOpenAPIRoute.brandRouting(brandController: BrandController) {
    route("brand") {
        authenticateWithJwt(RoleManagement.SELLER.role) {
            post<Unit, Response, AddBrand, JwtTokenBody>(
                exampleRequest = AddBrand(
                    brandName = "Easy"
                )
            ) { _, brandBody ->
                brandBody.validation()
                respond(
                    ApiResponse.success(
                        brandController.createBrand(brandBody), HttpStatusCode.OK
                    )
                )
            }
            get<PagingData, Response, JwtTokenBody> { pagingData ->
                pagingData.validation()
                respond(ApiResponse.success(brandController.getBrand(pagingData), HttpStatusCode.OK))
            }
            put<UpdateBrand, Response, Unit, JwtTokenBody> { params, _ ->
                params.validation()
                respond(ApiResponse.success(brandController.updateBrand(params), HttpStatusCode.OK))
            }
            delete<DeleteBrand, Response, JwtTokenBody> { params ->
                params.validation()
                respond(ApiResponse.success(brandController.deleteBrand(params), HttpStatusCode.OK))
            }
        }
    }
}