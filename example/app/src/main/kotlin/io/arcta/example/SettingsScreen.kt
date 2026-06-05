package io.arcta.example

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        val items = listOf("Plan" to "Growth", "Renewal" to "Jul 1, 2026", "Account" to "demo@arcta.io")
        androidx.compose.foundation.lazy.LazyColumn(contentPadding = padding) {
            items.forEach { (label, value) ->
                item {
                    ListItem(
                        headlineContent = { Text(label) },
                        trailingContent = { Text(value) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
