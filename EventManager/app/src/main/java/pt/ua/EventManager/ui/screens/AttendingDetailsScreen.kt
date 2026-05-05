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
fun AttendingDetailsScreen(
    event: Event?, 
    onBack: () -> Unit,
    onScanQR: () -> Unit
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
                title = { Text("Attending Details", fontWeight = FontWeight.Bold, fontSize = 26.sp ) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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

            // ── Check-in Section ───────────────────────────────────────────
            if (isHappening) {
                Column {
                    Text("Check-in", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onScanQR,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Event QR Code", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (isUpcoming) {
                Surface(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Check-in will be available when the event starts.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Column {
                Text("Event Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                CommonInfoRow(Icons.Default.CalendarToday, dateString)
                CommonInfoRow(Icons.Default.LocationOn, event.address)
                CommonInfoRow(Icons.Default.Groups, "${event.participantsUids.size} people joining")
            }

            // ── Chat Section ───────────────────────────────────────────────
            EventChatSection(eventId = event.id, hostUid = event.organizerUid)

            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD00000)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Leave Event", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
