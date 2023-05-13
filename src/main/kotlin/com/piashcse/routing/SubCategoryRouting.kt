package com.piashcse.routing

import com.piashcse.controller.SubCategoryController
import com.piashcse.models.PagingData
import com.piashcse.models.subcategory.AddSubCategory
import com.piashcse.models.subcategory.DeleteSubCategory
import com.piashcse.models.subcategory.UpdateSubCategory
import com.piashcse.models.user.body.JwtTokenBody
import com.piashcse.plugins.RoleManagement
import com.piashcse.utils.ApiResponse
import com.piashcse.utils.Response
import com.piashcse.utils.authenticateWithJwt
import com.papsign.ktor.openapigen.route.path.auth.delete
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.path.auth.post
import com.papsign.ktor.openapigen.route.path.auth.put
import com.papsign.ktor.openapigen.route.path.normal.*
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*

fun NormalOpenAPIRoute.subCategoryRoute(subCategoryController: SubCategoryController) {
    route("sub-category") {
        authenticateWithJwt(RoleManagement.SELLER.role) {
            post<Unit, Response, AddSubCategory, JwtTokenBody>(
                exampleRequest = AddSubCategory(
                    categoryId = "8eabd62f-fbb2-4fad-b440-3060f2e12dbc", subCategoryName = "Shirt"
                )
            ) { _, addSubCategoryBody ->
                addSubCategoryBody.validation()
                respond(
                    ApiResponse.success(
                        subCategoryController.createSubCategory(addSubCategoryBody), HttpStatusCode.OK
                    )
                )
            }
            get<PagingData, Response, JwtTokenBody> { pagingData ->
                pagingData.validation()
                respond(ApiResponse.success(subCategoryController.getSubCategory(pagingData), HttpStatusCode.OK))
            }
            put<UpdateSubCategory, Response, Unit, JwtTokenBody> { updateParams, _ ->
                updateParams.validation()
                respond(ApiResponse.success(subCategoryController.updateSubCategory(updateParams), HttpStatusCode.OK))
            }
            delete<DeleteSubCategory, Response, JwtTokenBody> { deleteParams ->
                deleteParams.validation()
                respond(
                    ApiResponse.success(
                        subCategoryController.deleteSubCategory(deleteParams.subCategoryId), HttpStatusCode.OK
                    )
                )
            }
        }
    }
}