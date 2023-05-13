package com.example.routing

import com.example.controller.BrandController
import com.example.models.PagingData
import com.example.models.bands.AddBrand
import com.example.models.bands.DeleteBrand
import com.example.models.bands.UpdateBrand
import com.example.models.user.body.JwtTokenBody
import com.example.plugins.RoleManagement
import com.example.utils.ApiResponse
import com.example.utils.Response
import com.example.utils.authenticateWithJwt
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
            put<UpdateBrand, Response, Unit, JwtTokenBody> { updateParams, _ ->
                updateParams.validation()
                respond(ApiResponse.success(brandController.updateBrand(updateParams), HttpStatusCode.OK))
            }
            delete<DeleteBrand, Response, JwtTokenBody> { deleteParams ->
                deleteParams.validation()
                respond(ApiResponse.success(brandController.deleteBrand(deleteParams), HttpStatusCode.OK))
            }
        }
    }
}