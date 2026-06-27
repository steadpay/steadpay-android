package io.gatlio.core

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
            "Your payment failed. We'll retry on {date} — please ensure funds are available.",
            "Your payment failed. Final retry on {date} — add funds or your access will be restricted."),
        "bank_hold" to WarningVariant(
            "Your payment was held by your bank. We'll retry on {date} — you may want to contact them.",
            "Your payment was held by your bank. Final retry on {date} — contact your bank or your access will be restricted."),
        "processing_error" to WarningVariant(
            "Your payment failed due to a temporary issue. We'll retry on {date}.",
            "Your payment failed. Final retry on {date} — your access will be restricted if it fails."),
        "card_issue" to WarningVariant(
            "Your payment failed. We'll retry on {date}, but your saved card may need updating.",
            "Your payment failed. Final retry on {date} — update your card or your access will be restricted."),
    ),
    "fr" to mapOf(
        "insufficient_funds" to WarningVariant(
            "Votre paiement a échoué. Nous réessaierons le {date} — veuillez vous assurer que des fonds suffisants sont disponibles.",
            "Votre paiement a échoué. Dernier essai le {date} — ajoutez des fonds ou votre accès sera restreint."),
        "bank_hold" to WarningVariant(
            "Votre paiement a été bloqué par votre banque. Nous réessaierons le {date} — vous pouvez la contacter.",
            "Votre paiement a été bloqué par votre banque. Dernier essai le {date} — contactez votre banque ou votre accès sera restreint."),
        "processing_error" to WarningVariant(
            "Votre paiement a échoué en raison d'un problème temporaire. Nous réessaierons le {date}.",
            "Votre paiement a échoué. Dernier essai le {date} — votre accès sera restreint en cas d'échec."),
        "card_issue" to WarningVariant(
            "Votre paiement a échoué. Nous réessaierons le {date}, mais votre carte enregistrée devra peut-être être mise à jour.",
            "Votre paiement a échoué. Dernier essai le {date} — votre carte doit probablement être mise à jour ou votre accès sera restreint."),
    ),
    "es" to mapOf(
        "insufficient_funds" to WarningVariant(
            "Tu pago falló. Volveremos a intentarlo el {date} — asegúrate de que haya fondos suficientes.",
            "Tu pago falló. Último intento el {date} — añade fondos o tu acceso se restringirá."),
        "bank_hold" to WarningVariant(
            "Tu banco retuvo el pago. Volveremos a intentarlo el {date} — quizás quieras contactarles.",
            "Tu banco retuvo el pago. Último intento el {date} — contacta con tu banco o tu acceso se restringirá."),
        "processing_error" to WarningVariant(
            "Tu pago falló por un problema temporal. Volveremos a intentarlo el {date}.",
            "Tu pago falló. Último intento el {date} — tu acceso se restringirá si falla."),
        "card_issue" to WarningVariant(
            "Tu pago falló. Volveremos a intentarlo el {date}, pero es posible que tu tarjeta guardada deba actualizarse.",
            "Tu pago falló. Último intento el {date} — actualiza tu tarjeta o tu acceso se restringirá."),
    ),
    "de" to mapOf(
        "insufficient_funds" to WarningVariant(
            "Ihre Zahlung ist fehlgeschlagen. Wir versuchen es am {date} erneut — bitte stellen Sie sicher, dass ausreichend Guthaben verfügbar ist.",
            "Ihre Zahlung ist fehlgeschlagen. Letzter Versuch am {date} — laden Sie Guthaben auf oder Ihr Zugang wird eingeschränkt."),
        "bank_hold" to WarningVariant(
            "Ihre Zahlung wurde von Ihrer Bank zurückgehalten. Wir versuchen es am {date} erneut — Sie können sich an Ihre Bank wenden.",
            "Ihre Zahlung wurde von Ihrer Bank zurückgehalten. Letzter Versuch am {date} — wenden Sie sich an Ihre Bank oder Ihr Zugang wird eingeschränkt."),
        "processing_error" to WarningVariant(
            "Ihre Zahlung ist aufgrund eines vorübergehenden Problems fehlgeschlagen. Wir versuchen es am {date} erneut.",
            "Ihre Zahlung ist fehlgeschlagen. Letzter Versuch am {date} — andernfalls wird Ihr Zugang eingeschränkt."),
        "card_issue" to WarningVariant(
            "Ihre Zahlung ist fehlgeschlagen. Wir versuchen es am {date} erneut, aber Ihre gespeicherte Karte muss möglicherweise aktualisiert werden.",
            "Ihre Zahlung ist fehlgeschlagen. Letzter Versuch am {date} — aktualisieren Sie Ihre Karte oder Ihr Zugang wird eingeschränkt."),
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
