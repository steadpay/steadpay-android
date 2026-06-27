package io.gatlio.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.gatlio.core.Entitlements

@Composable
fun LockoutScreen(
    poweredByWatermark: Boolean,
    entitlements: Entitlements?,
    message: String,
    cta: String,
    onTriggerCardUpdate: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111111)),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CardIcon()
            Spacer(Modifier.height(36.dp))
            Text(
                text = "Payment method declined",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 15.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onTriggerCardUpdate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            ) {
                Text(
                    text = cta,
                    color = Color(0xFF111111),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (poweredByWatermark || entitlements?.poweredByWatermark == true) {
            Text(
                text = "Powered by Gatlio",
                fontSize = 12.sp,
                color = Color(0xFF444444),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun CardIcon() {
    Box {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1A1A1A)),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(11.dp)
                        .background(Color.White.copy(alpha = 0.1f)),
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(width = 14.dp, height = 10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                )
            }
        }
        Box(
            modifier = Modifier
                .offset(x = 54.dp, y = (-8).dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444)),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
