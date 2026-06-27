package io.steadpay.core

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SteadpayApiError(val code: String) : Exception(code)

// Single shared client for callers that don't supply their own.
// SteadpayController always passes its own httpClient, so this is only
// used by direct callers (e.g. tests or one-off utilities).
internal val defaultFetchClient: OkHttpClient = OkHttpClient.Builder()
    .callTimeout(10, TimeUnit.SECONDS)
    .build()

private val failOpen = SteadpayState(
    status = SteadpayStatus.Active,
    cardUpdateUrl = null,
    entitlements = Entitlements(
        poweredByWatermark = false,
        customDomain = false,
        downstreamWebhooks = false,
    ),
)

fun fetchSubscriberStatus(
    baseUrl: String,
    tenantSlug: String,
    customerId: String,
    publishableKey: String,
    hmac: String,
    client: OkHttpClient = defaultFetchClient,
): SteadpayState {
    val encodedSlug = java.net.URLEncoder.encode(tenantSlug, "UTF-8")
    val encodedCustomer = java.net.URLEncoder.encode(customerId, "UTF-8")
    val url = "$baseUrl/api/subscriber-status/$encodedSlug?stripe_customer_id=$encodedCustomer"

    val request = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $publishableKey")
        .header("X-Steadpay-HMAC", hmac)
        .build()

    val response = client.newCall(request).execute()
    val code = response.code

    if (code == 402) return failOpen
    if (code == 401) throw SteadpayApiError("unauthorized")
    if (code == 404) throw SteadpayApiError("tenant_not_found")
    if (code != 200) throw SteadpayApiError("unexpected_status_$code")

    val body = response.body?.string() ?: throw SteadpayApiError("empty_body")
    val json = JSONObject(body)
    val ent = json.getJSONObject("entitlements")

    return SteadpayState(
        status = SteadpayStatus.values().firstOrNull { it.name.lowercase() == json.getString("status") }
            ?: SteadpayStatus.Error,
        cardUpdateUrl = if (json.isNull("card_update_url")) null else json.getString("card_update_url"),
        entitlements = Entitlements(
            poweredByWatermark = ent.getBoolean("powered_by_watermark"),
            customDomain = ent.getBoolean("custom_domain"),
            downstreamWebhooks = ent.getBoolean("downstream_webhooks"),
        ),
        declineCategory = json.optStringOrNull("decline_category"),
        nextRetryAt = json.optStringOrNull("next_retry_at"),
        isFinalRetry = json.optBoolean("is_final_retry", false),
        lockoutReason = json.optStringOrNull("lockout_reason"),
    )
}

// JSONObject.optString returns "" (not null) for missing/null keys, which would
// be miscategorized — return a real null instead.
private fun JSONObject.optStringOrNull(key: String): String? =
    if (!has(key) || isNull(key)) null else getString(key)
