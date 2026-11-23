package com.piashcse.plugin

import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiDocsTest {
    @Test
    fun `operation ids are derived from method and path`() {
        assertEquals("getApiV1Orders", OpenApiDocs.operationIdFor("get", "/api/v1/orders"))
        assertEquals(
            "getApiV1OrdersByOrderId",
            OpenApiDocs.operationIdFor("get", "/api/v1/orders/{orderId}"),
        )
        assertEquals(
            "postSellerPayouts",
            OpenApiDocs.operationIdFor("post", "seller/payouts/"),
        )
    }

    @Test
    fun `hyphenated and underscored segments are camel cased`() {
        assertEquals(
            "getAdminAuditLogs",
            OpenApiDocs.operationIdFor("get", "/admin/audit-logs"),
        )
        assertEquals(
            "getProductsByProductIdVariants",
            OpenApiDocs.operationIdFor("get", "/products/{product_id}/variants"),
        )
    }
}
