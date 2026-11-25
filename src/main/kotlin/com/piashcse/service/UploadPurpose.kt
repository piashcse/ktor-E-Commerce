package com.piashcse.service

/**
 * Upload destinations. Directory names, user-facing labels and the legacy
 * helper names used by routes live in one place so adding a purpose does not
 * require editing several upload/delete/URL wrappers.
 */
enum class UploadPurpose(
    val directory: String,
    val label: String,
) {
    PROFILE("profile-images", "profile image"),
    PRODUCT("product-images", "product image"),
    SHOP("shop-images", "shop image"),
    REFUND("refund-images", "refund image"),
    SUPPORT("support-attachments", "support attachment"),
    CATEGORY("category-images", "category image"),
    BRAND("brand-images", "brand image"),
}
