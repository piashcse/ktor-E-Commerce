package com.piashcse.constants

/**
 * User-facing error messages organized by domain.
 * Industry standard: All error messages centralized here for consistency, i18n, and maintainability.
 *
 * Usage: throw ValidationException(Message.Validation.BLANK_FIELD("User ID"))
 *
 * Based on Stripe, GitHub, OpenAI best practices:
 * - Clear, actionable messages
 * - No technical jargon
 * - Consistent tone across all endpoints
 */
object Message {
    // ─── Validation Messages (Reusable with field names) ───────────────────
    object Validation {
        // Generic field validators
        fun blankField(fieldName: String) = "$fieldName cannot be blank"

        fun negativeValue(fieldName: String) = "$fieldName cannot be negative"

        fun notPositive(fieldName: String) = "$fieldName must be greater than 0"

        fun tooLong(
            fieldName: String,
            maxLength: Int,
        ) = "$fieldName cannot exceed $maxLength characters"

        fun invalidFormat(fieldName: String) = "Invalid $fieldName format"

        // Specific validation messages
        const val INVALID_EMAIL = "Invalid email address"
        const val WEAK_PASSWORD = "Password must be at least 8 characters with uppercase, lowercase, digit, and special character"
        const val INVALID_USER_TYPE = "Invalid user type. Must be one of: CUSTOMER, SELLER, ADMIN, SUPER_ADMIN"
        const val EMPTY_PASSWORD = "Password cannot be empty"
        const val EMPTY_ORDER_ITEMS = "Order must contain at least one item"
        const val FILE_NAME_REQUIRED = "File name is required"
        const val INVALID_FILE_TYPE = "Invalid file type. Allowed types: jpg, jpeg, png, gif, webp"
        const val FILE_REQUIRED = "No file uploaded"

        fun productNotFound(productId: String) = "Product with ID $productId not found"

        fun insufficientStock(
            productName: String,
            available: Int,
        ) = "Insufficient stock for $productName. Available: $available"

        const val RATING_OUT_OF_RANGE = "Rating must be between 1 and 5"

        fun invalidEnumValue(
            fieldName: String,
            value: String,
        ) = "Invalid $fieldName: $value"

        fun invalidOperation(
            operation: String,
            validOps: String,
        ) = "Invalid operation: $operation. Valid operations: $validOps"
    }

    // ─── Auth ──────────────────────────────────────────────────────────────
    object Auth {
        const val USER_EXISTS = "User already exists with this email"
        const val INVALID_CREDENTIALS = "Invalid email or password"
        const val ACCOUNT_NOT_VERIFIED = "Account not verified"
        const val ACCOUNT_DEACTIVATED = "Account has been deactivated"
        const val PASSWORD_CHANGE_SUCCESS = "Password changed successfully"
        const val PASSWORD_SAME = "New password cannot be the same as current password"

        const val OTP_SENT = "Verification code sent to your email"
        const val OTP_ALREADY_SENT = "Verification code already sent. Please wait before requesting a new one"
        const val OTP_INVALID = "Invalid or expired verification code"
        const val PASSWORD_RESET_SUCCESS = "Password reset successful"
        const val INVALID_REFRESH_TOKEN = "Invalid or expired refresh token"
        const val TOKEN_EXPIRED = "Refresh token expired or revoked"

        fun accountLocked(lockoutMinutes: Long) =
            "Account locked due to too many failed login attempts. Try again in $lockoutMinutes minutes"

        // Permissions
        fun insufficientPermissions(action: String) = "Insufficient permissions to $action"

        fun userNotFoundForRole(
            email: String,
            userType: String,
        ) = "User with email $email not found for $userType role"
    }

    // ─── Orders ────────────────────────────────────────────────────────────
    object Orders {
        const val NOT_FOUND = "Order not found"
        const val INVALID_STATUS = "Invalid order status"
        const val STATUS_NOT_ALLOWED = "You cannot set this order status"
        const val UNAUTHORIZED = "You do not have permission to update this order"
        const val PRODUCT_NOT_FOUND = "Product not found"
        const val OUT_OF_STOCK = "Product is out of stock"
        const val CANNOT_CANCEL = "Order cannot be cancelled in current status"
        const val CANCEL_REASON_REQUIRED = "Cancellation reason is required"
        const val SHOP_NOT_FOUND = "Shop not found"
        const val SHOP_INACTIVE = "Shop is not active. Cannot place order."
        const val INVALID_COUPON = "Invalid or inactive coupon code"
        const val COUPON_EXPIRED = "Coupon is expired or not yet active"
        const val COUPON_LIMIT_REACHED = "Coupon usage limit reached"
        const val SHIPPING_ADDRESS_NOT_FOUND = "Shipping address not found"
        const val SHIPPING_ADDRESS_UNAUTHORIZED = "Unauthorized shipping address"
        const val SHIPPING_METHOD_NOT_FOUND = "Shipping method not found"
        const val TOTAL_MISMATCH = "Order total mismatch"
        const val SELLER_PROFILE_NOT_FOUND = "Seller profile not found"
        const val NO_SHOP_ASSOCIATED = "No shop associated with seller"

        fun productDoesNotBelongToShop(productName: String) = "$productName does not belong to any shop"
        fun couponMinOrderAmount(amount: String) = "Order amount is below the minimum required for this coupon ($amount)"
    }

    // ─── Products ──────────────────────────────────────────────────────────
    object Products {
        const val NOT_FOUND = "Product not found"
        const val UNAUTHORIZED = "You do not have permission to update this product"
        const val UNAUTHORIZED_ADD = "You do not have permission to add products to this shop"
        const val OUT_OF_STOCK = "Product is out of stock"
        const val INSUFFICIENT_STOCK = "Insufficient stock available"
        const val NOT_SHOP_OWNER = "You are not the owner of this shop"
    }

    // ─── Shops ─────────────────────────────────────────────────────────────
    object Shops {
        const val NOT_FOUND = "Shop not found"
        const val UNAUTHORIZED = "You do not have permission to update this shop"
        const val ALREADY_EXISTS = "User already has a shop"
        const val ALREADY_APPROVED = "Shop is already approved"
        const val ALREADY_SUSPENDED = "Shop is already suspended"

        fun invalidStatus(status: String) = "Invalid shop status: $status"
    }

    // ─── Brands ────────────────────────────────────────────────────────────
    object Brands {
        const val NOT_FOUND = "Brand not found"
        const val BLANK_NAME = "Brand name cannot be blank"

        fun nameTooLong(maxLength: Int) = "Brand name cannot exceed $maxLength characters"
    }

    // ─── Coupons ────────────────────────────────────────────────────────────
    object Coupons {
        const val NOT_FOUND = "Coupon not found"
    }

    // ─── Payments ───────────────────────────────────────────────────────────
    object Payments {
        const val ALREADY_PAID = "Order already fully paid"
        fun amountMismatch(paid: String, total: String) = "Payment amount ($paid) does not match order total ($total)"
    }

    // ─── Refunds ────────────────────────────────────────────────────────────
    object Refunds {
        const val NOT_FOUND = "Refund request not found"
        const val ITEM_NOT_FOUND = "Order item not found"
        const val ALREADY_EXISTS = "Refund request already exists for this item"
        const val INVALID_STATUS = "Invalid status. Must be one of: APPROVED, REJECTED, REFUNDED"
        const val MUST_BE_APPROVED = "Refund must be approved before shipping"
    }

    // ─── Upload ─────────────────────────────────────────────────────────────
    object Upload {
        const val EMPTY_FILE = "Uploaded file is empty"
        const val INVALID_FILE_PATH = "Invalid file path detected"
        const val INVALID_DIR_CONFIG = "Invalid upload directory configuration"
        fun fileNameRequired(purpose: String) = "File name is required for $purpose upload"
        fun invalidFileType(purpose: String, allowed: String) = "Invalid file type for $purpose. Allowed: $allowed"
        fun invalidMimeType(purpose: String, mime: String) = "Invalid MIME type for $purpose: $mime"
        fun fileTooLarge(mb: Int, purpose: String) = "File size exceeds ${mb}MB limit for $purpose upload"
        fun maliciousContent(ext: String) = "Malicious file content detected or file format does not match $ext extension"
        fun storageQuotaExceeded(purpose: String) = "Storage quota exceeded for $purpose uploads. Please free up space or contact support."
    }

    // ─── Inventory ──────────────────────────────────────────────────────────
    object Cart {
        const val PRODUCT_NOT_FOUND = "Product not found"
        const val EMPTY_CART = "Cart is empty"
    }

    // ─── Inventory ─────────────────────────────────────────────────────────
    object Inventory {
        const val NOT_FOUND = "Inventory record not found"
        const val NEGATIVE_STOCK = "Stock quantity cannot be negative"
        const val NEGATIVE_QUANTITY = "Quantity cannot be negative for set operation"

        fun insufficientStock(
            available: Int,
            requested: Int,
        ) = "Insufficient stock. Available: $available, Requested: $requested"

        fun invalidOperation(operation: String) = "Invalid operation: $operation. Use add, subtract, or set"
        fun quantityNotPositive(operation: String) = "Quantity must be positive for $operation operation"
    }

    // ─── Consent ───────────────────────────────────────────────────────────
    object Consent {
        const val NOT_FOUND = "Consent record not found"
        const val ALREADY_GIVEN = "Consent already given for this policy"
    }

    // ─── Policy ────────────────────────────────────────────────────────────
    object Policy {
        const val NOT_FOUND = "Policy not found"

        fun noActivePolicy(type: String) = "No active $type policy found"
    }

    // ─── Shipping ──────────────────────────────────────────────────────────
    object Shipping {
        const val NOT_FOUND = "Shipping record not found"
        const val INVALID_METHOD = "Invalid shipping method"
    }

    // ─── General Errors ────────────────────────────────────────────────────
    object Errors {
        const val INTERNAL = "Internal server error"
        const val UNAUTHORIZED = "Authentication required"
        const val FORBIDDEN = "Insufficient permissions"
        const val NOT_FOUND = "Resource not found"
        const val VALIDATION_FAILED = "Validation failed"
        const val EMAIL_FAILED = "Failed to send email"
        const val SELLER_REQUIRED = "User must be registered as a seller"
        const val MISSING_PARAMETER = "Missing required parameter: %s"
        const val NOT_OWNER = "You do not own this resource"
        fun notOwner(resourceName: String) = "You do not own this $resourceName"
        fun invalidParameter(name: String, value: String) = "Invalid $name: $value"
    }
}
