package com.cashi.app.domain.model

/** Currencies the app lets the user send (the mockup's currency picker). */
enum class Currency(val code: String, val displayName: String, val symbol: String) {
    JOD("JOD", "Jordanian Dinar", "JD"),
    USD("USD", "US Dollar", "$");

    companion object {
        val Default = JOD

        fun fromCode(code: String): Currency? = entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
    }
}
