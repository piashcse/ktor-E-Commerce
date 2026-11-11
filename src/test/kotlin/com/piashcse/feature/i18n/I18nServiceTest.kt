package com.piashcse.feature.i18n

import com.piashcse.constants.Localization
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class I18nServiceTest {
    private val i18n = I18nService()

    @Test
    fun `resolve honours quality values and regional tags`() {
        assertEquals("es", i18n.resolve("es-MX,es;q=0.9,en;q=0.8"))
        assertEquals("fr", i18n.resolve("de;q=0.9,fr;q=0.8"))
        assertEquals("bn", i18n.resolve("bn-BD"))
        assertEquals("hi", i18n.resolve("hi-IN"))
        assertEquals("en", i18n.resolve("de-DE"))
        assertEquals("en", i18n.resolve(null))
    }

    @Test
    fun `translates known messages and falls back to English text`() {
        assertEquals("Producto no encontrado", i18n.translate("es", "Product not found"))
        assertEquals("Produit introuvable", i18n.translate("fr", "product_not_found"))
        assertEquals("পণ্য পাওয়া যায়নি", i18n.translate("bn", "Product not found"))
        assertEquals("Untranslated message", i18n.translate("es", "Untranslated message"))
    }

    @Test
    fun `message catalogs always contain every English key`() {
        val english = i18n.messages("en").messages
        val spanish = i18n.messages("es").messages
        assertEquals(english.keys, spanish.keys)
        assertTrue(spanish.values.none { it.isBlank() })
    }

    @Test
    fun `formats money and dates for the requested locale`() {
        val usd = i18n.formatMoney(BigDecimal("1234.56"), "USD", "en")
        assertTrue(usd.contains("1,234.56"))
        val eur = i18n.formatMoney(BigDecimal("1234.56"), "EUR", "fr")
        assertTrue(eur.contains("1") && eur.contains("234,56"))
        assertTrue(i18n.formatDate(LocalDateTime.of(2026, 5, 1, 12, 0), "fr").isNotBlank())
    }

    @Test
    fun `localization helpers normalize and validate`() {
        assertEquals("en", Localization.normalize(null))
        assertEquals("bn", Localization.normalize("bn-BD"))
        assertEquals("en", Localization.normalize("de-DE"))
        assertEquals("fr", Localization.requireValid("fr-FR"))
        assertEquals("BDT", Localization.option("bn").currency)
    }

    @Test
    fun `locale metadata exposes supported currencies`() {
        val locales = i18n.locales()
        assertEquals(5, locales.size)
        assertEquals("BDT", locales.first { it.code == "bn" }.defaultCurrency)
        assertEquals("INR", locales.first { it.code == "hi" }.defaultCurrency)
    }
}
