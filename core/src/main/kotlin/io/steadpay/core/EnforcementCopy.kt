package io.steadpay.core

// Context-aware enforcement copy (#041).
//
// Mirrors the web enforcement snippet's copy tables so the mobile experience is
// identical: decline-category-driven warning copy (no card-update CTA on soft
// declines) and lockout copy differentiated by lockout reason × decline category.

data class EnforcementContext(
    val declineCategory: String? = null,
    val nextRetryAt: String? = null,
    val isFinalRetry: Boolean = false,
    val lockoutReason: String? = null,
)

data class EnforcementCopy(
    val message: String,
    /** Card-update CTA label. Always null in warning state (#041). */
    val cta: String?,
)

private val SUPPORTED_LOCALES = listOf("en", "fr", "es", "de")

/** Validates a raw locale string down to a supported language code, else English. */
fun resolveLocale(locale: String?): String {
    val loc = (locale ?: "en").lowercase()
    val code = if (loc.length >= 2) loc.substring(0, 2) else loc
    return if (code in SUPPORTED_LOCALES) code else "en"
}

private val MONTH_NAMES = mapOf(
    "en" to listOf("January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"),
    "fr" to listOf("janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre"),
    "es" to listOf("enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"),
    "de" to listOf("Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember"),
)

/**
 * Formats an ISO-8601 timestamp into a locale-appropriate long date. The leading
 * UTC calendar date is used so the rendered day is deterministic across devices.
 */
fun formatRetryDate(iso: String?, locale: String): String {
    if (iso.isNullOrEmpty() || iso.length < 10) return ""
    return try {
        val datePart = iso.substring(0, 10) // YYYY-MM-DD
        val year = datePart.substring(0, 4).toInt()
        val month = datePart.substring(5, 7).toInt()
        val day = datePart.substring(8, 10).toInt()
        val loc = resolveLocale(locale)
        val name = MONTH_NAMES.getValue(loc)[month - 1]
        when (loc) {
            "fr" -> "$day $name $year"
            "es" -> "$day de $name de $year"
            "de" -> "$day. $name $year"
            else -> "$name $day, $year"
        }
    } catch (e: Exception) {
        ""
    }
}

private data class WarningVariant(val normal: String, val last: String)

private val WARNING: Map<String, Map<String, WarningVariant>> = mapOf(
    "en" to mapOf(
        "insufficient_funds" to WarningVariant(
            "We'll retry on {date}. Please ensure sufficient funds are available.",
            "This is our final retry on {date}. Please add funds — your access will be restricted if it fails."),
        "bank_hold" to WarningVariant(
            "We'll retry on {date}. You may want to contact your bank.",
            "This is our final retry on {date}. Please contact your bank — your access will be restricted if it fails."),
        "processing_error" to WarningVariant(
            "There was a temporary processing issue. We'll retry on {date}.",
            "This is our final retry on {date}. Your access will be restricted if it fails."),
        "card_issue" to WarningVariant(
            "We'll retry on {date}, but your saved card may need updating to go through.",
            "This is our final retry on {date}. Your saved card likely needs updating — your access will be restricted if it fails."),
    ),
    "fr" to mapOf(
        "insufficient_funds" to WarningVariant(
            "Nous réessaierons le {date}. Veuillez vous assurer que des fonds suffisants sont disponibles.",
            "Ceci est notre dernier essai le {date}. Veuillez ajouter des fonds — votre accès sera restreint en cas d'échec."),
        "bank_hold" to WarningVariant(
            "Nous réessaierons le {date}. Vous pouvez contacter votre banque.",
            "Ceci est notre dernier essai le {date}. Veuillez contacter votre banque — votre accès sera restreint en cas d'échec."),
        "processing_error" to WarningVariant(
            "Un problème temporaire de traitement est survenu. Nous réessaierons le {date}.",
            "Ceci est notre dernier essai le {date}. Votre accès sera restreint en cas d'échec."),
        "card_issue" to WarningVariant(
            "Nous réessaierons le {date}, mais votre carte enregistrée devra peut-être être mise à jour.",
            "Ceci est notre dernier essai le {date}. Votre carte enregistrée doit probablement être mise à jour — votre accès sera restreint en cas d'échec."),
    ),
    "es" to mapOf(
        "insufficient_funds" to WarningVariant(
            "Volveremos a intentarlo el {date}. Asegúrate de que haya fondos suficientes disponibles.",
            "Este es nuestro último intento el {date}. Añade fondos — tu acceso se restringirá si falla."),
        "bank_hold" to WarningVariant(
            "Volveremos a intentarlo el {date}. Quizás quieras contactar con tu banco.",
            "Este es nuestro último intento el {date}. Contacta con tu banco — tu acceso se restringirá si falla."),
        "processing_error" to WarningVariant(
            "Hubo un problema temporal de procesamiento. Volveremos a intentarlo el {date}.",
            "Este es nuestro último intento el {date}. Tu acceso se restringirá si falla."),
        "card_issue" to WarningVariant(
            "Volveremos a intentarlo el {date}, pero es posible que tu tarjeta guardada deba actualizarse.",
            "Este es nuestro último intento el {date}. Probablemente debas actualizar tu tarjeta guardada — tu acceso se restringirá si falla."),
    ),
    "de" to mapOf(
        "insufficient_funds" to WarningVariant(
            "Wir versuchen es am {date} erneut. Bitte stellen Sie sicher, dass ausreichend Guthaben verfügbar ist.",
            "Dies ist unser letzter Versuch am {date}. Bitte laden Sie Guthaben auf — andernfalls wird Ihr Zugang eingeschränkt."),
        "bank_hold" to WarningVariant(
            "Wir versuchen es am {date} erneut. Sie können sich an Ihre Bank wenden.",
            "Dies ist unser letzter Versuch am {date}. Bitte wenden Sie sich an Ihre Bank — andernfalls wird Ihr Zugang eingeschränkt."),
        "processing_error" to WarningVariant(
            "Es gab ein vorübergehendes Verarbeitungsproblem. Wir versuchen es am {date} erneut.",
            "Dies ist unser letzter Versuch am {date}. Andernfalls wird Ihr Zugang eingeschränkt."),
        "card_issue" to WarningVariant(
            "Wir versuchen es am {date} erneut, aber Ihre gespeicherte Karte muss möglicherweise aktualisiert werden.",
            "Dies ist unser letzter Versuch am {date}. Ihre gespeicherte Karte muss wahrscheinlich aktualisiert werden — andernfalls wird Ihr Zugang eingeschränkt."),
    ),
)

private val WARNING_FALLBACK = mapOf(
    "en" to "Your payment failed. We'll retry automatically — please keep your payment method up to date.",
    "fr" to "Votre paiement a échoué. Nous réessaierons automatiquement — veuillez garder votre moyen de paiement à jour.",
    "es" to "Tu pago falló. Volveremos a intentarlo automáticamente — mantén tu método de pago actualizado.",
    "de" to "Ihre Zahlung ist fehlgeschlagen. Wir versuchen es automatisch erneut — bitte halten Sie Ihre Zahlungsmethode aktuell.",
)

// Lockout copy: locale → reason → category (with a per-reason "_default").
private val LOCKOUT: Map<String, Map<String, Map<String, String>>> = mapOf(
    "en" to mapOf(
        "hard_decline" to mapOf(
            "card_issue" to "Your payment method needs to be updated to restore access.",
            "bank_hold" to "Your payment was declined by your bank. Please update your payment method or contact your bank.",
            "_default" to "Your payment method needs to be updated to restore access."),
        "retry_exhausted" to mapOf(
            "insufficient_funds" to "We were unable to process your payment after multiple attempts. Please add funds or update your payment method.",
            "_default" to "We were unable to process your payment after multiple attempts. Please update your payment method or contact your bank."),
    ),
    "fr" to mapOf(
        "hard_decline" to mapOf(
            "card_issue" to "Votre moyen de paiement doit être mis à jour pour rétablir l'accès.",
            "bank_hold" to "Votre paiement a été refusé par votre banque. Veuillez mettre à jour votre moyen de paiement ou contacter votre banque.",
            "_default" to "Votre moyen de paiement doit être mis à jour pour rétablir l'accès."),
        "retry_exhausted" to mapOf(
            "insufficient_funds" to "Nous n'avons pas pu traiter votre paiement après plusieurs tentatives. Veuillez ajouter des fonds ou mettre à jour votre moyen de paiement.",
            "_default" to "Nous n'avons pas pu traiter votre paiement après plusieurs tentatives. Veuillez mettre à jour votre moyen de paiement ou contacter votre banque."),
    ),
    "es" to mapOf(
        "hard_decline" to mapOf(
            "card_issue" to "Tu método de pago debe actualizarse para restaurar el acceso.",
            "bank_hold" to "Tu banco rechazó el pago. Actualiza tu método de pago o contacta con tu banco.",
            "_default" to "Tu método de pago debe actualizarse para restaurar el acceso."),
        "retry_exhausted" to mapOf(
            "insufficient_funds" to "No pudimos procesar tu pago después de varios intentos. Añade fondos o actualiza tu método de pago.",
            "_default" to "No pudimos procesar tu pago después de varios intentos. Actualiza tu método de pago o contacta con tu banco."),
    ),
    "de" to mapOf(
        "hard_decline" to mapOf(
            "card_issue" to "Ihre Zahlungsmethode muss aktualisiert werden, um den Zugang wiederherzustellen.",
            "bank_hold" to "Ihre Zahlung wurde von Ihrer Bank abgelehnt. Bitte aktualisieren Sie Ihre Zahlungsmethode oder wenden Sie sich an Ihre Bank.",
            "_default" to "Ihre Zahlungsmethode muss aktualisiert werden, um den Zugang wiederherzustellen."),
        "retry_exhausted" to mapOf(
            "insufficient_funds" to "Wir konnten Ihre Zahlung nach mehreren Versuchen nicht verarbeiten. Bitte laden Sie Guthaben auf oder aktualisieren Sie Ihre Zahlungsmethode.",
            "_default" to "Wir konnten Ihre Zahlung nach mehreren Versuchen nicht verarbeiten. Bitte aktualisieren Sie Ihre Zahlungsmethode oder wenden Sie sich an Ihre Bank."),
    ),
)

private val CTA = mapOf(
    "en" to "Update card",
    "fr" to "Mettre à jour la carte",
    "es" to "Actualizar tarjeta",
    "de" to "Karte aktualisieren",
)

/** Decline-specific warning copy. Never carries a card-update CTA (#041). */
fun warningCopy(ctx: EnforcementContext, locale: String): EnforcementCopy {
    val loc = resolveLocale(locale)
    val variant = ctx.declineCategory?.let { WARNING.getValue(loc)[it] }
    val date = formatRetryDate(ctx.nextRetryAt, loc)
    val template = when {
        variant == null -> WARNING_FALLBACK.getValue(loc)
        ctx.isFinalRetry -> variant.last
        else -> variant.normal
    }
    return EnforcementCopy(template.replace("{date}", date), null)
}

/**
 * Lockout copy differentiated by lockout reason × decline category, with the
 * localized Update card CTA.
 */
fun lockoutCopy(ctx: EnforcementContext, locale: String): EnforcementCopy {
    val loc = resolveLocale(locale)
    val reasons = LOCKOUT.getValue(loc)
    val group = reasons[ctx.lockoutReason ?: "hard_decline"] ?: reasons.getValue("hard_decline")
    val message = (ctx.declineCategory?.let { group[it] }) ?: group.getValue("_default")
    return EnforcementCopy(message, CTA.getValue(loc))
}
