package pt.ua.EventManager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ua.EventManager.data.Event
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostingDetailsScreen(
    event: Event?, 
    onBack: () -> Unit,
    onShowQR: () -> Unit,
    onParticipantsClick: () -> Unit,
    onEditClick: () -> Unit
) {
    if (event == null) return

    val currentTime = System.currentTimeMillis()
    val isHappening = currentTime in event.timestamp..event.endTimestamp
    val isUpcoming = currentTime < event.timestamp
    val isEnded = currentTime > event.endTimestamp

    val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
    val dateString = sdf.format(Date(event.timestamp))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hosting Details", fontWeight = FontWeight.Bold, fontSize = 26.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Event Header Card ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(event.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val statusText = when {
                        isHappening -> "Happening Now!"
                        isUpcoming -> {
                            val diff = event.timestamp - currentTime
                            val hours = TimeUnit.MILLISECONDS.toHours(diff)
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                            val days = TimeUnit.MILLISECONDS.toDays(diff)
                            
                            if (days > 0) "Starts in $days days"
                            else if (hours > 0) "Starts in $hours h $minutes min"
                            else "Starts in $minutes min"
                        }
                        else -> "Event Ended"
                    }

                    Text(
                        statusText,
                        color = if (isHappening) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Management Buttons ─────────────────────────────────────────
            Column {
                Text("Management", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ManagementButton(Modifier.weight(1f), Icons.Default.People, "Participants") {
                        onParticipantsClick()
                    }
                    
                    // QR Code button is only enabled/visible when happening
                    ManagementButton(
                        modifier = Modifier.weight(1f), 
                        icon = if (isHappening) Icons.Default.QrCode else Icons.Default.QrCodeScanner, 
                        text = if (isHappening) "Check-in QR" else "QR Locked",
                        enabled = isHappening
                    ) {
                        onShowQR()
                    }
                }
                if (!isHappening && isUpcoming) {
                    Text(
                        "Check-in QR will be available when the event starts.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // ── Event Details ──────────────────────────────────────────────
            Column {
                Text("Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                CommonInfoRow(Icons.Default.CalendarToday, dateString)
                CommonInfoRow(Icons.Default.LocationOn, event.address)
                CommonInfoRow(Icons.Default.Groups, "${event.participantsUids.size} people joined")
            }

            // ── Chat Section ───────────────────────────────────────────────
            EventChatSection(eventId = event.id, hostUid = event.organizerUid)

            // ── Cancel Button ──────────────────────────────────────────────
            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD00000)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Cancel Event", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun ManagementButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null)
            Text(text, fontSize = 12.sp)
        }
    }
}

@Composable
fun CommonInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 16.sp)
    }
}
