package io.steadpay.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.steadpay.core.CallbackName
import io.steadpay.core.Entitlements
import io.steadpay.core.SteadpayStatus
import io.steadpay.core.computeTransition

@Composable
fun SteadpaySandbox(
    onLockout: (() -> Unit)? = null,
    onWarning: (() -> Unit)? = null,
    onActive: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    lockoutScreen: (@Composable (triggerCardUpdate: () -> Unit, entitlements: Entitlements?) -> Unit)? = null,
    warningBanner: (@Composable (triggerCardUpdate: () -> Unit, dismissWarning: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var currentStatus by remember { mutableStateOf(SteadpayStatus.Active) }
    var sheetOpen by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    val log = remember { mutableStateListOf<String>() }
    var lastStatus by remember { mutableStateOf<SteadpayStatus?>(SteadpayStatus.Active) }

    // Reset dismissed whenever status leaves Warning
    LaunchedEffect(currentStatus) {
        if (currentStatus != SteadpayStatus.Warning) dismissed = false
    }

    val changeStatus: (SteadpayStatus) -> Unit = { next ->
        if (next == SteadpayStatus.Error) {
            if (currentStatus != SteadpayStatus.Error) {
                onError?.invoke(RuntimeException("sandbox_error"))
                log.add(0, "onError(sandbox_error)")
                if (log.size > 5) log.removeAt(log.size - 1)
                currentStatus = SteadpayStatus.Error
                lastStatus = SteadpayStatus.Error
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
            SteadpayStatus.Lockout -> {
                if (lockoutScreen != null) {
                    lockoutScreen({ }, null)
                } else {
                    LockoutScreen(
                        poweredByWatermark = true,
                        entitlements = null,
                        onTriggerCardUpdate = {},
                    )
                }
            }
            else -> {
                Column {
                    if (currentStatus == SteadpayStatus.Warning && !dismissed) {
                        if (warningBanner != null) {
                            warningBanner({ }, { dismissed = true })
                        } else {
                            WarningBanner(onTriggerCardUpdate = {}, onDismiss = { dismissed = true })
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
                    .clickable { sheetOpen = true },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "DEV",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.semantics { testTag = "sandbox-dev-badge" },
                    )
                }
            }
        }

        // Control panel sheet
        if (sheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { sheetOpen = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(0.4f)
                        .padding(16.dp),
                ) {
                    Text(
                        "STEADPAY SANDBOX",
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
                            SteadpayStatus.Active,
                            SteadpayStatus.Warning,
                            SteadpayStatus.Lockout,
                            SteadpayStatus.Error,
                        ).forEach { status ->
                            Button(
                                onClick = {
                                    changeStatus(status)
                                    if (status != SteadpayStatus.Error) sheetOpen = false
                                },
                                modifier = Modifier.semantics { testTag = "sandbox-pill-${status.name.lowercase()}" },
                            ) {
                                Text(status.name)
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
                        "onRecovered requires a real card update — test against a live Steadpay environment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { testTag = "sandbox-recovered-note" },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LockoutPreview() {
    LockoutScreen(poweredByWatermark = true, entitlements = null, onTriggerCardUpdate = {})
}

@Preview(showBackground = true)
@Composable
private fun WarningPreview() {
    WarningBanner(onTriggerCardUpdate = {}, onDismiss = {})
}
