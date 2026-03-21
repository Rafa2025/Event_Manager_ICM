package pt.ua.EventManager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventMapScreen(onNotificationsClick: () -> Unit = {}) {
    var searchQuery by remember { mutableStateOf("") }
    val categories = listOf("All", "Party", "Dinner", "Meetup", "Sports", "Music")
    var selectedCategory by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text ="Event Map",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )
                },
                actions = {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(Icons.Default.Notifications, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Search and Filters
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search events or locations...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        val isSelected = category == selectedCategory
                        Surface(
                            onClick = { selectedCategory = category },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = category,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = if (isSelected) Color.White else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Map Placeholder
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F1F1))) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridSize = 40.dp.toPx()
                    for (x in 0..(size.width / gridSize).toInt()) {
                        drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(x * gridSize, 0f), Offset(x * gridSize, size.height), strokeWidth = 1f)
                    }
                    for (y in 0..(size.height / gridSize).toInt()) {
                        drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(0f, y * gridSize), Offset(size.width, y * gridSize), strokeWidth = 1f)
                    }
                }

                // Markers as seen in the images
                MapMarker(Modifier.align(Alignment.Center).offset(x = (-60).dp, y = (-100).dp))
                MapMarker(Modifier.align(Alignment.Center).offset(x = 20.dp, y = (-40).dp))
                MapMarker(Modifier.align(Alignment.Center).offset(x = 80.dp, y = 40.dp))
                MapMarker(Modifier.align(Alignment.Center).offset(x = 150.dp, y = 120.dp))
            }
        }
    }
}

@Composable
fun MapMarker(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.LocationOn,
        contentDescription = null,
        modifier = modifier.size(36.dp),
        tint = MaterialTheme.colorScheme.primary
    )
}
