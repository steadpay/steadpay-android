package io.steadpay.core

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
        {"status":"active","entitlements":{"powered_by_watermark":true,"custom_domain":false,"downstream_webhooks":false},"card_update_url":"https://app.steadpay.io/update-card"}
    """.trimIndent()

    @Test fun returnsActiveResponseOn200() {
        enqueue(200, goodBody)
        val result = fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", client)
        assertEquals(SteadpayStatus.Active, result.status)
        assertEquals(true, result.entitlements?.poweredByWatermark)
        assertEquals("https://app.steadpay.io/update-card", result.cardUpdateUrl)
    }

    @Test fun returnsFailOpenOn402() {
        enqueue(402, "{}")
        val result = fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", client)
        assertEquals(SteadpayStatus.Active, result.status)
        assertEquals(false, result.entitlements?.poweredByWatermark)
        assertNull(result.cardUpdateUrl)
    }

    @Test(expected = SteadpayApiError::class)
    fun throwsUnauthorizedOn401() {
        enqueue(401, "{}")
        fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", client)
    }

    @Test(expected = SteadpayApiError::class)
    fun throwsTenantNotFoundOn404() {
        enqueue(404, "{}")
        fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", client)
    }

    @Test fun throwsUnexpectedStatusOn500() {
        enqueue(500, "{}")
        try {
            fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", client)
            throw AssertionError("Expected SteadpayApiError")
        } catch (e: SteadpayApiError) {
            assertEquals("unexpected_status_500", e.code)
        }
    }

    @Test fun sendsCorrectAuthorizationHeader() {
        enqueue(200, goodBody)
        fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_live_abc", client)
        val request = server.takeRequest()
        assertEquals("Bearer pk_live_abc", request.getHeader("Authorization"))
    }

    @Test fun sendsCorrectEndpointPath() {
        enqueue(200, goodBody)
        fetchSubscriberStatus(baseUrl, "acme", "cus_123", "pk_test", client)
        val request = server.takeRequest()
        assert(request.path?.contains("/api/subscriber-status/acme") == true) {
            "Path was: ${request.path}"
        }
        assert(request.path?.contains("stripe_customer_id=cus_123") == true) {
            "Path was: ${request.path}"
        }
    }
}
