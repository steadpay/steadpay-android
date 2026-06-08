package io.steadpay.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

typealias FetchFn = (String, String, String, String) -> SteadpayState

class SteadpayController(
    val config: SteadpayConfig,
    val forcedStatus: SteadpayStatus? = null,
    private val callbacks: SteadpayCallbacks? = null,
    private val urlLauncher: ((String) -> Unit)? = null,
    fetch: FetchFn? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _stateFlow = MutableStateFlow(SteadpayState())
    val stateFlow: StateFlow<SteadpayState> = _stateFlow

    private val _dismissedFlow = MutableStateFlow(false)
    val dismissedFlow: StateFlow<Boolean> = _dismissedFlow

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private var pollingJob: Job? = null
    private var lastStatus: SteadpayStatus? = null
    private var isRecoveryPath = false

    // Shared across all poll calls for this controller instance.
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private val fetch: FetchFn = fetch
        ?: { apiBase, tenantSlug, customerId, publishableKey ->
            fetchSubscriberStatus(apiBase, tenantSlug, customerId, publishableKey, httpClient)
        }

    fun start() {
        if (forcedStatus != null) {
            _stateFlow.value = SteadpayState(
                status = forcedStatus,
                cardUpdateUrl = "https://example.com/update-card?forced=1",
                entitlements = Entitlements(
                    poweredByWatermark = true,
                    customDomain = true,
                    downstreamWebhooks = true,
                ),
            )
            return
        }
        startPolling()
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun triggerCardUpdate() {
        val url = _stateFlow.value.cardUpdateUrl ?: return
        if (!url.startsWith("https://")) return
        isRecoveryPath = true
        _dismissedFlow.value = false
        urlLauncher?.invoke(url)
        startPolling()
    }

    fun dismissWarning() {
        _dismissedFlow.value = true
    }

    fun dispose() {
        scope.cancel()
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch { pollLoop() }
    }

    private suspend fun pollLoop() {
        doPoll()
        if (_stateFlow.value.status == SteadpayStatus.Lockout) return

        while (true) {
            delay(config.pollIntervalMs)
            doPoll()
            if (_stateFlow.value.status == SteadpayStatus.Lockout) break
        }
    }

    private fun doPoll() {
        val wasRecovery = isRecoveryPath
        isRecoveryPath = false

        try {
            val state = fetch(config.apiBase, config.tenantSlug, config.customerId, config.publishableKey)
            val cbName = computeTransition(lastStatus, state.status, wasRecovery)
            _stateFlow.value = state
            lastStatus = state.status
            fireCallback(cbName)
        } catch (e: Throwable) {
            _stateFlow.value = SteadpayState(status = SteadpayStatus.Error)
            lastStatus = SteadpayStatus.Error
            callbacks?.onError?.invoke(e)
        }
    }

    private fun fireCallback(name: CallbackName?) {
        val id = config.customerId
        when (name) {
            CallbackName.OnLockout   -> callbacks?.onLockout?.invoke(id)
            CallbackName.OnWarning   -> callbacks?.onWarning?.invoke(id)
            CallbackName.OnActive    -> callbacks?.onActive?.invoke(id)
            CallbackName.OnRecovered -> callbacks?.onRecovered?.invoke(id)
            null -> Unit
        }
    }
}
