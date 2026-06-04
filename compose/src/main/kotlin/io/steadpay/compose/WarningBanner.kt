package io.steadpay.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WarningBanner(
    onTriggerCardUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFFF59E0B)),
            contentAlignment = Alignment.Center,
        ) {
            Text("!", color = Color(0xFF111111), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Please update your payment method to avoid interruption.",
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = Color(0xFFD4D4D4),
            lineHeight = 18.sp,
            maxLines = 2,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = "Update now",
            modifier = Modifier.clickable(onClick = onTriggerCardUpdate),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF59E0B),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = "✕",
            modifier = Modifier.clickable(onClick = onDismiss),
            fontSize = 14.sp,
            color = Color(0xFF666666),
        )
    }
}
