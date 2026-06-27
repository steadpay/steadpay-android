package io.gatlio.core

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FetchSubscriberStatusTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var baseUrl: String

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient()
        baseUrl = server.url("/").toString().trimEnd('/')
    }

    @After fun tearDown() { server.shutdown() }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    private val goodBody = """
        {"status":"active","entitlements":{"powered_by_watermark":true,"custom_domain":false,"downstream_webhooks":false},"card_update_url":"https://app.gatlio.io/update-card"}
    """.trimIndent()

    @Test fun returnsActiveResponseOn200() {
        enqueue(200, goodBody)
        val result = fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", "test_hmac", client)
        assertEquals(GatlioStatus.Active, result.status)
        assertEquals(true, result.entitlements?.poweredByWatermark)
        assertEquals("https://app.gatlio.io/update-card", result.cardUpdateUrl)
    }

    @Test fun parsesContextAwareCopyFields() {
        enqueue(200, """
            {"status":"warning","entitlements":{"powered_by_watermark":true,"custom_domain":false,"downstream_webhooks":false},"card_update_url":"https://app.gatlio.io/update-card","decline_category":"insufficient_funds","next_retry_at":"2026-06-20T12:00:00Z","is_final_retry":true,"lockout_reason":null}
        """.trimIndent())
        val result = fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", "test_hmac", client)
        assertEquals("insufficient_funds", result.declineCategory)
        assertEquals("2026-06-20T12:00:00Z", result.nextRetryAt)
        assertEquals(true, result.isFinalRetry)
        assertNull(result.lockoutReason)
    }

    @Test fun contextFieldsDefaultWhenAbsent() {
        enqueue(200, goodBody)
        val result = fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", "test_hmac", client)
        assertNull(result.declineCategory)
        assertNull(result.nextRetryAt)
        assertEquals(false, result.isFinalRetry)
        assertNull(result.lockoutReason)
    }

    @Test fun returnsFailOpenOn402() {
        enqueue(402, "{}")
        val result = fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", "test_hmac", client)
        assertEquals(GatlioStatus.Active, result.status)
        assertEquals(false, result.entitlements?.poweredByWatermark)
        assertNull(result.cardUpdateUrl)
    }

    @Test(expected = GatlioApiError::class)
    fun throwsUnauthorizedOn401() {
        enqueue(401, "{}")
        fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", "test_hmac", client)
    }

    @Test(expected = GatlioApiError::class)
    fun throwsTenantNotFoundOn404() {
        enqueue(404, "{}")
        fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", "test_hmac", client)
    }

    @Test fun throwsUnexpectedStatusOn500() {
        enqueue(500, "{}")
        try {
            fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", "test_hmac", client)
            throw AssertionError("Expected GatlioApiError")
        } catch (e: GatlioApiError) {
            assertEquals("unexpected_status_500", e.code)
        }
    }

    @Test fun sendsCorrectAuthorizationHeader() {
        enqueue(200, goodBody)
        fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_live_abc", "test_hmac", client)
        val request = server.takeRequest()
        assertEquals("Bearer pk_live_abc", request.getHeader("Authorization"))
    }

    @Test fun sendsCorrectEndpointPath() {
        enqueue(200, goodBody)
        fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", "test_hmac", client)
        val request = server.takeRequest()
        assert(request.path?.contains("/api/subscriber-status/acme") == true) {
            "Path was: ${request.path}"
        }
        assert(request.path?.contains("stripe_customer_id=cus_123") == true) {
            "Path was: ${request.path}"
        }
        assert(request.path?.contains("hmac=") == false) {
            "HMAC must not appear in URL, was: ${request.path}"
        }
    }

    @Test fun sendsHmacInHeader() {
        enqueue(200, goodBody)
        fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", "test_hmac", client)
        val request = server.takeRequest()
        assertEquals("test_hmac", request.getHeader("X-Gatlio-HMAC"))
    }

    @Test fun defaultFetchClientHasCallTimeout() {
        assertEquals(10_000L, defaultFetchClient.callTimeoutMillis.toLong())
    }
}

class GatlioConfigValidationTest {
    @Test(expected = IllegalArgumentException::class)
    fun rejectsHttpApiBase() {
        GatlioConfig(
            apiBase = "http://app.gatlio.io",
            tenantSlug = "acme",
            customerId = "cus_123",
            publishableKey = "pk_live_abc",
            hmac = "test_hmac",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyTenantSlug() {
        GatlioConfig(
            apiBase = "https://app.gatlio.io",
            tenantSlug = "",
            customerId = "cus_123",
            publishableKey = "pk_live_abc",
            hmac = "test_hmac",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPkPublishableKey() {
        GatlioConfig(
            apiBase = "https://app.gatlio.io",
            tenantSlug = "acme",
            customerId = "cus_123",
            publishableKey = "sk_live_abc",
            hmac = "test_hmac",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyHmac() {
        GatlioConfig(
            apiBase = "https://app.gatlio.io",
            tenantSlug = "acme",
            customerId = "cus_123",
            publishableKey = "pk_live_abc",
            hmac = "",
        )
    }
}

class TriggerCardUpdateSecurityTest {
    @Test fun doesNotLaunchNonHttpsUrl() {
        var launched: String? = null
        val controller = GatlioController(
            config(),
            fetch = { _, _, _, _, _ ->
                GatlioState(
                    status = GatlioStatus.Lockout,
                    cardUpdateUrl = "javascript:alert(1)",
                    entitlements = Entitlements(poweredByWatermark = false, customDomain = false, downstreamWebhooks = false),
                )
            },
            urlLauncher = { url -> launched = url },
        )
        controller.start()
        Thread.sleep(200) // let coroutine poll complete
        controller.triggerCardUpdate()
        assertNull(launched)
        controller.dispose()
    }
}

private fun config() = GatlioConfig(
    apiBase = "https://app.gatlio.io",
    tenantSlug = "acme",
    customerId = "cus_123",
    publishableKey = "pk_live_abc",
    hmac = "test_hmac",
)
