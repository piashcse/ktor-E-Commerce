package com.piashcse.constants

/** Supported content/checkout locales. Codes are BCP-47 language tags. */
object Localization {
    const val DEFAULT_LOCALE = "en"

    data class LocaleOption(
        val code: String,
        val language: String,
        val currency: String,
        val englishName: String,
    )

    val LOCALES =
        listOf(
            LocaleOption("en", "English", "USD", "English"),
            LocaleOption("es", "Español", "EUR", "Spanish"),
            LocaleOption("fr", "Français", "EUR", "French"),
            LocaleOption("bn", "বাংলা", "BDT", "Bengali"),
            LocaleOption("hi", "हिन्दी", "INR", "Hindi"),
        )

    val CODES: Set<String> = LOCALES.map { it.code }.toSet()

    fun normalize(value: String?): String {
        if (value.isNullOrBlank()) return DEFAULT_LOCALE
        val base = value.trim().lowercase().substringBefore('-').substringBefore('_')
        return base.takeIf { it in CODES } ?: DEFAULT_LOCALE
    }

    fun requireValid(value: String): String {
        val base = value.trim().lowercase().substringBefore('-').substringBefore('_')
        require(base in CODES) { "Unsupported locale: $value" }
        return base
    }

    fun option(code: String): LocaleOption = LOCALES.first { it.code == normalize(code) }
}
