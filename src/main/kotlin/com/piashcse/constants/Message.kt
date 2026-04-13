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
        fun tooLong(fieldName: String, maxLength: Int) = "$fieldName cannot exceed $maxLength characters"
        fun invalidFormat(fieldName: String) = "Invalid $fieldName format"

        // Specific validation messages
        const val INVALID_EMAIL = "Invalid email address"
        const val WEAK_PASSWORD = "Password must be at least 8 characters with uppercase, lowercase, digit, and special character"
        const val INVALID_USER_TYPE = "Invalid user type. Must be one of: CUSTOMER, SELLER, ADMIN, SUPER_ADMIN"
        const val EMPTY_PASSWORD = "Password cannot be empty"
        const val INVALID_ORDER_ITEMS = "Order must contain at least one item"
        const val FILE_NAME_REQUIRED = "File name is required"
        const val INVALID_FILE_TYPE = "Invalid file type. Allowed types: jpg, jpeg, png, gif, webp"
        const val FILE_REQUIRED = "No file uploaded"
        fun productNotFound(productId: String) = "Product with ID $productId not found"
        fun insufficientStock(productName: String, available: Int) = "Insufficient stock for $productName. Available: $available"
        fun invalidOperation(operation: String, validOps: String) = "Invalid operation: $operation. Valid operations: $validOps"
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
        
        // Permissions
        fun insufficientPermissions(action: String) = "Insufficient permissions to $action"
        fun userNotFoundForRole(email: String, userType: String) = "User with email $email not found for $userType role"
    }
    
    // ─── Orders ────────────────────────────────────────────────────────────
    object Orders {
        const val NOT_FOUND = "Order not found"
        const val INVALID_STATUS = "Invalid order status"
        const val STATUS_NOT_ALLOWED = "You cannot set this order status"
        const val UNAUTHORIZED = "You do not have permission to update this order"
        const val PRODUCT_NOT_FOUND = "Product not found"
        const val OUT_OF_STOCK = "Product is out of stock"
        fun productDoesNotBelongToShop(productName: String) = "$productName does not belong to any shop"
    }
    
    // ─── Products ──────────────────────────────────────────────────────────
    object Products {
        const val NOT_FOUND = "Product not found"
        const val UNAUTHORIZED = "You do not have permission to update this product"
        const val UNAUTHORIZED_ADD = "You do not have permission to add products to this shop"
        const val OUT_OF_STOCK = "Product is out of stock"
        const val INSUFFICIENT_STOCK = "Insufficient stock available"
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
    
    // ─── Cart ──────────────────────────────────────────────────────────────
    object Cart {
        const val PRODUCT_NOT_FOUND = "Product not found"
        const val EMPTY_CART = "Cart is empty"
    }
    
    // ─── Inventory ─────────────────────────────────────────────────────────
    object Inventory {
        const val NOT_FOUND = "Inventory record not found"
        const val NEGATIVE_STOCK = "Stock quantity cannot be negative"
        fun insufficientStock(available: Int, requested: Int) = "Insufficient stock. Available: $available, Requested: $requested"
        fun invalidOperation(operation: String) = "Invalid operation: $operation. Use add, subtract, or set"
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
    }
}

