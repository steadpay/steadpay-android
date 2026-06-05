package io.arcta.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val metrics = listOf(
    "MRR" to "\$12,840",
    "Active Users" to "1,204",
    "Churn Rate" to "2.4%",
    "Conversions" to "8.7%",
)

private val barRatios = listOf(0.4f, 0.6f, 0.5f, 0.8f, 0.65f, 0.9f, 0.75f)
private val events = listOf(
    "New subscriber" to "2m ago",
    "Plan upgrade" to "1h ago",
    "Trial started" to "3h ago",
)

@Composable
fun ArctaContent() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { MetricGrid() }
        item { BarChart() }
        item { RecentEvents() }
    }
}

@Composable
private fun MetricGrid() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.chunked(2).forEach { pair ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (label, value) ->
                    Box(
                        Modifier.fillMaxWidth().background(Color(0xFFF8F8FA), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
                            Spacer(Modifier.height(4.dp))
                            Text(label, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarChart() {
    val maxBarHeight = 80.dp
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Revenue — last 7 days", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
        Row(Modifier.height(maxBarHeight), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            barRatios.forEach { ratio ->
                Box(
                    Modifier.weight(1f).fillMaxHeight(ratio)
                        .background(Color(0xFF1A1A2E), RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
private fun RecentEvents() {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text("Recent Events", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A2E))
        Spacer(Modifier.height(8.dp))
        events.forEach { (label, time) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, fontSize = 14.sp, color = Color(0xFF333333))
                Text(time, fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}
