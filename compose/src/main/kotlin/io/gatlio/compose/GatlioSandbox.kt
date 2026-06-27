package io.gatlio.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.gatlio.core.CallbackName
import io.gatlio.core.EnforcementContext
import io.gatlio.core.Entitlements
import io.gatlio.core.GatlioStatus
import io.gatlio.core.computeTransition
import io.gatlio.core.lockoutCopy
import io.gatlio.core.resolveLocale
import io.gatlio.core.warningCopy

@Composable
fun GatlioSandbox(
    onLockout: (() -> Unit)? = null,
    onWarning: (() -> Unit)? = null,
    onActive: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    lockoutScreen: (@Composable (triggerCardUpdate: () -> Unit, entitlements: Entitlements?, message: String, cta: String) -> Unit)? = null,
    warningBanner: (@Composable (dismissWarning: () -> Unit, message: String) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val sandboxLocale = resolveLocale(java.util.Locale.getDefault().language)
    val sampleRetryAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        .format(java.util.Date(System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000))
    val sampleLockoutCopy = lockoutCopy(
        EnforcementContext(declineCategory = "card_issue", lockoutReason = "hard_decline"),
        sandboxLocale,
    )
    val sampleWarningMessage = warningCopy(
        EnforcementContext(declineCategory = "insufficient_funds", nextRetryAt = sampleRetryAt),
        sandboxLocale,
    ).message
    var currentStatus by remember { mutableStateOf(GatlioStatus.Active) }
    var sheetOpen by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val log = remember { mutableStateListOf<String>() }
    var lastStatus by remember { mutableStateOf<GatlioStatus?>(GatlioStatus.Active) }

    // Reset dismissed whenever status leaves Warning
    LaunchedEffect(currentStatus) {
        if (currentStatus != GatlioStatus.Warning) dismissed = false
    }

    val changeStatus: (GatlioStatus) -> Unit = { next ->
        if (next == GatlioStatus.Error) {
            if (currentStatus != GatlioStatus.Error) {
                onError?.invoke(RuntimeException("sandbox_error"))
                log.add(0, "onError(sandbox_error)")
                if (log.size > 5) log.removeAt(log.size - 1)
                currentStatus = GatlioStatus.Error
                lastStatus = GatlioStatus.Error
            }
        } else {
            val cbName = computeTransition(lastStatus, next, false)
            currentStatus = next
            lastStatus = next
            if (cbName != null) {
                when (cbName) {
                    CallbackName.OnLockout -> onLockout?.invoke()
                    CallbackName.OnWarning -> onWarning?.invoke()
                    CallbackName.OnActive -> onActive?.invoke()
                    CallbackName.OnRecovered -> Unit
                }
                log.add(0, "${cbName.name}()")
                if (log.size > 5) log.removeAt(log.size - 1)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Render gate content inline based on currentStatus
        when (currentStatus) {
            GatlioStatus.Lockout -> {
                if (lockoutScreen != null) {
                    lockoutScreen({ }, null, sampleLockoutCopy.message, sampleLockoutCopy.cta ?: "")
                } else {
                    LockoutScreen(
                        poweredByWatermark = true,
                        entitlements = null,
                        message = sampleLockoutCopy.message,
                        cta = sampleLockoutCopy.cta ?: "",
                        onTriggerCardUpdate = {},
                    )
                }
            }
            else -> {
                Column {
                    if (currentStatus == GatlioStatus.Warning && !dismissed) {
                        if (warningBanner != null) {
                            warningBanner({ dismissed = true }, sampleWarningMessage)
                        } else {
                            WarningBanner(message = sampleWarningMessage, onDismiss = { dismissed = true })
                        }
                    }
                    content()
                }
            }
        }

        // DEV badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .sizeIn(minWidth = 44.dp, minHeight = 44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1A1A2E),
                modifier = Modifier
                    .width(64.dp)
                    .height(28.dp)
                    .clickable { sheetOpen = true }
                    .semantics { testTag = "sandbox-dev-badge" },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "DEV",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        // Control panel overlay — Column is a direct BoxScope child so pills are
        // immediately reachable in the semantic tree without an intermediate Box.
        if (sheetOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.54f))
                    .clickable { sheetOpen = false },
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .background(Color(0xFF1A1A2E), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(16.dp),
            ) {
                    Text(
                        "GATLIO SANDBOX",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Status: ${currentStatus.name}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    // Status pills row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            GatlioStatus.Active,
                            GatlioStatus.Warning,
                            GatlioStatus.Lockout,
                            GatlioStatus.Error,
                        ).forEach { status ->
                            val isSelected = currentStatus == status
                            val pillColor = when (status) {
                                GatlioStatus.Active  -> Color(0xFF22C55E)
                                GatlioStatus.Warning -> Color(0xFFF59E0B)
                                GatlioStatus.Lockout -> Color(0xFFEF4444)
                                else                   -> Color(0xFF6B7280)
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) pillColor else Color.Transparent,
                                        RoundedCornerShape(20.dp),
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(20.dp))
                                    .clickable {
                                        changeStatus(status)
                                        if (status != GatlioStatus.Error) sheetOpen = false
                                    }
                                    .semantics { testTag = "sandbox-pill-${status.name.lowercase()}" }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    status.name,
                                    color = if (isSelected) Color(0xFF111111) else Color(0xFF888888),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Log entries
                    if (log.isNotEmpty()) {
                        Text("Recent callbacks:", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(4.dp))
                        log.forEach { entry ->
                            Text(
                                entry,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        "onRecovered requires a real card update — test against a live Gatlio environment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { testTag = "sandbox-recovered-note" },
                    )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LockoutPreview() {
    LockoutScreen(
        poweredByWatermark = true,
        entitlements = null,
        message = "Your payment method needs to be updated to restore access.",
        cta = "Update card",
        onTriggerCardUpdate = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun WarningPreview() {
    WarningBanner(message = "We'll retry on June 20, 2026. Please ensure sufficient funds are available.", onDismiss = {})
}
