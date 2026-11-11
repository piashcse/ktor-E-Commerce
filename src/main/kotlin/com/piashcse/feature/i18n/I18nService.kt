package com.piashcse.feature.i18n

import com.piashcse.constants.Localization
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

@Serializable
data class LocaleResponse(
    val code: String,
    val language: String,
    val englishName: String,
    val defaultCurrency: String,
)

@Serializable
data class MessagesResponse(
    val locale: String,
    val messages: Map<String, String>,
)

/**
 * Lightweight in-process i18n catalog for API messages plus locale-aware
 * money/date formatting. Unknown messages fall back to the supplied English text.
 */
class I18nService {
    private val catalogs: Map<String, Map<String, String>> =
        mapOf(
            "en" to
                mapOf(
                    "validation_failed" to "Validation failed",
                    "unauthorized" to "Authentication required",
                    "forbidden" to "Insufficient permissions",
                    "not_found" to "Not found",
                    "internal_error" to "Internal server error",
                    "product_not_found" to "Product not found",
                    "order_not_found" to "Order not found",
                    "cart_empty" to "Cart is empty",
                    "invalid_credentials" to "Invalid email or password",
                    "account_not_verified" to "Account not verified",
                    "account_deactivated" to "Account has been deactivated",
                    "gateway_failed" to "Payment could not be processed",
                    "order_placed" to "Order placed successfully",
                    "subtotal" to "Subtotal",
                    "shipping" to "Shipping",
                    "tax" to "Tax",
                    "discount" to "Discount",
                    "total" to "Total",
                    "wallet" to "Store credit",
                    "gift_card" to "Gift card",
                ),
            "es" to
                mapOf(
                    "validation_failed" to "Error de validación",
                    "unauthorized" to "Autenticación requerida",
                    "forbidden" to "Permisos insuficientes",
                    "not_found" to "No encontrado",
                    "internal_error" to "Error interno del servidor",
                    "product_not_found" to "Producto no encontrado",
                    "order_not_found" to "Pedido no encontrado",
                    "cart_empty" to "El carrito está vacío",
                    "invalid_credentials" to "Correo o contraseña inválidos",
                    "account_not_verified" to "Cuenta no verificada",
                    "account_deactivated" to "La cuenta ha sido desactivada",
                    "gateway_failed" to "No se pudo procesar el pago",
                    "order_placed" to "Pedido realizado correctamente",
                    "subtotal" to "Subtotal",
                    "shipping" to "Envío",
                    "tax" to "Impuestos",
                    "discount" to "Descuento",
                    "total" to "Total",
                    "wallet" to "Saldo de tienda",
                    "gift_card" to "Tarjeta regalo",
                ),
            "fr" to
                mapOf(
                    "validation_failed" to "Échec de la validation",
                    "unauthorized" to "Authentification requise",
                    "forbidden" to "Autorisations insuffisantes",
                    "not_found" to "Introuvable",
                    "internal_error" to "Erreur interne du serveur",
                    "product_not_found" to "Produit introuvable",
                    "order_not_found" to "Commande introuvable",
                    "cart_empty" to "Le panier est vide",
                    "invalid_credentials" to "E-mail ou mot de passe invalide",
                    "account_not_verified" to "Compte non vérifié",
                    "account_deactivated" to "Le compte a été désactivé",
                    "gateway_failed" to "Le paiement n'a pas pu être traité",
                    "order_placed" to "Commande passée avec succès",
                    "subtotal" to "Sous-total",
                    "shipping" to "Livraison",
                    "tax" to "Taxes",
                    "discount" to "Remise",
                    "total" to "Total",
                    "wallet" to "Crédit boutique",
                    "gift_card" to "Carte cadeau",
                ),
            "bn" to
                mapOf(
                    "validation_failed" to "যাচাইকরণ ব্যর্থ হয়েছে",
                    "unauthorized" to "প্রমাণীকরণ প্রয়োজন",
                    "forbidden" to "অপর্যাপ্ত অনুমতি",
                    "not_found" to "পাওয়া যায়নি",
                    "internal_error" to "অভ্যন্তরীণ সার্ভার ত্রুটি",
                    "product_not_found" to "পণ্য পাওয়া যায়নি",
                    "order_not_found" to "অর্ডার পাওয়া যায়নি",
                    "cart_empty" to "কার্ট খালি",
                    "invalid_credentials" to "ভুল ইমেইল বা পাসওয়ার্ড",
                    "account_not_verified" to "অ্যাকাউন্ট যাচাই করা হয়নি",
                    "account_deactivated" to "অ্যাকাউন্ট নিষ্ক্রিয় করা হয়েছে",
                    "gateway_failed" to "পেমেন্ট প্রক্রিয়া করা যায়নি",
                    "order_placed" to "অর্ডার সফলভাবে সম্পন্ন হয়েছে",
                    "subtotal" to "উপমোট",
                    "shipping" to "শিপিং",
                    "tax" to "কর",
                    "discount" to "ছাড়",
                    "total" to "মোট",
                    "wallet" to "স্টোর ক্রেডিট",
                    "gift_card" to "গিফট কার্ড",
                ),
            "hi" to
                mapOf(
                    "validation_failed" to "सत्यापन विफल रहा",
                    "unauthorized" to "प्रमाणीकरण आवश्यक है",
                    "forbidden" to "अपर्याप्त अनुमतियाँ",
                    "not_found" to "नहीं मिला",
                    "internal_error" to "आंतरिक सर्वर त्रुटि",
                    "product_not_found" to "उत्पाद नहीं मिला",
                    "order_not_found" to "ऑर्डर नहीं मिला",
                    "cart_empty" to "कार्ट खाली है",
                    "invalid_credentials" to "अमान्य ईमेल या पासवर्ड",
                    "account_not_verified" to "खाता सत्यापित नहीं है",
                    "account_deactivated" to "खाता निष्क्रिय कर दिया गया है",
                    "gateway_failed" to "भुगतान संसाधित नहीं हो सका",
                    "order_placed" to "ऑर्डर सफलतापूर्वक दिया गया",
                    "subtotal" to "उप-योग",
                    "shipping" to "शिपिंग",
                    "tax" to "कर",
                    "discount" to "छूट",
                    "total" to "कुल",
                    "wallet" to "स्टोर क्रेडिट",
                    "gift_card" to "गिफ्ट कार्ड",
                ),
        )

    private val englishToKey =
        catalogs.getValue("en").entries.associate { (key, value) -> value.lowercase() to key }

    fun locales(): List<LocaleResponse> =
        Localization.LOCALES.map {
            LocaleResponse(
                code = it.code,
                language = it.language,
                englishName = it.englishName,
                defaultCurrency = it.currency,
            )
        }

    /** Resolves an Accept-Language header (with q-values) or a raw locale tag. */
    fun resolve(raw: String?): String {
        if (raw.isNullOrBlank()) return Localization.DEFAULT_LOCALE
        return raw.split(',')
            .mapNotNull { part ->
                val pieces = part.trim().split(';')
                val tag = pieces.firstOrNull()?.trim() ?: return@mapNotNull null
                val quality =
                    pieces.drop(1)
                        .firstOrNull { it.trim().startsWith("q=") }
                        ?.trim()
                        ?.removePrefix("q=")
                        ?.toDoubleOrNull()
                        ?: 1.0
                val base = tag.lowercase().substringBefore('-').substringBefore('_')
                if (base in Localization.CODES) quality to base else null
            }
            .maxByOrNull { it.first }
            ?.second
            ?: Localization.DEFAULT_LOCALE
    }

    fun translate(
        locale: String,
        textOrKey: String,
    ): String {
        val normalized = Localization.normalize(locale)
        val key = catalogs.getValue(normalized).keys.firstOrNull { it == textOrKey } ?: englishToKey[textOrKey.lowercase()]
        if (key == null) return textOrKey
        return catalogs[normalized]?.get(key) ?: catalogs.getValue("en").getOrDefault(key, textOrKey)
    }

    fun messages(locale: String): MessagesResponse {
        val normalized = Localization.normalize(locale)
        val english = catalogs.getValue("en")
        val values = catalogs.getValue(normalized)
        return MessagesResponse(
            locale = normalized,
            messages = english.keys.associateWith { key -> values[key] ?: english.getValue(key) },
        )
    }

    fun formatMoney(
        amount: BigDecimal,
        currency: String,
        locale: String,
    ): String {
        val tag = Localization.normalize(locale)
        val numberFormat =
            runCatching {
                NumberFormat.getCurrencyInstance(Locale.forLanguageTag(tag)).apply {
                    this.currency = Currency.getInstance(currency.uppercase())
                }
            }.getOrElse {
                NumberFormat.getCurrencyInstance(Locale.US).apply {
                    this.currency = Currency.getInstance(currency.uppercase())
                }
            }
        return numberFormat.format(amount)
    }

    fun formatDate(
        value: LocalDateTime,
        locale: String,
    ): String =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Locale.forLanguageTag(Localization.normalize(locale)))
            .format(value)
}
