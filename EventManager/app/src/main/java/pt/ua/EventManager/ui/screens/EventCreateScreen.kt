package pt.ua.EventManager.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCreateScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text ="Create Event",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cover Image Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                val stroke = Stroke(width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(color = Color.LightGray, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AccountBox, // Placeholder for "Add Image" icon
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF4A4A4A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Add Cover Image", color = Color(0xFF4A4A4A), fontWeight = FontWeight.Medium)
                }
            }

            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Event Name") },
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("Description") },
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = "Category",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                readOnly = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = "mm/dd/yy",
                    onValueChange = {},
                    modifier = Modifier.weight(1f),
                    label = { Text("Date") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    readOnly = true
                )
                OutlinedTextField(
                    value = "--:-- --",
                    onValueChange = {},
                    modifier = Modifier.weight(1f),
                    label = { Text("Time") },
                    leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) }, // Should be time icon
                    shape = RoundedCornerShape(12.dp),
                    readOnly = true
                )
            }

            OutlinedTextField(
                value = "Enter address or pick on map",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Location") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                readOnly = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = "5",
                    onValueChange = {},
                    modifier = Modifier.weight(1f),
                    label = { Text("Min People") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = "30",
                    onValueChange = {},
                    modifier = Modifier.weight(1f),
                    label = { Text("Max People") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) // Should be food icon
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Food & Drinks", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip("Food")
                    FilterChip("Drinks")
                    FilterChip("Both")
                    FilterChip("None", isSelected = true)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Private Event", fontWeight = FontWeight.Bold)
                        Text("Only invited people can join", fontSize = 12.sp, color = Color.Gray)
                    }
                    var checked by remember { mutableStateOf(false) }
                    Switch(checked = checked, onCheckedChange = { checked = it })
                }
            }

            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Create Event", color = Color.Gray)
            }
        }
    }
}

@Composable
fun FilterChip(text: String, isSelected: Boolean = false) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Black else Color.LightGray,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = if (isSelected) Color.Black else Color.Gray)
    }
}
