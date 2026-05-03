package pt.ua.EventManager.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import pt.ua.EventManager.R
import pt.ua.EventManager.data.Event
import pt.ua.EventManager.ui.viewmodels.EventViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    event: Event?,
    onBack: () -> Unit,
    viewModel: EventViewModel = viewModel()
) {
    if (event == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Event not found")
        }
        return
    }

    val context = LocalContext.current
    val currentUserId = viewModel.currentUserUid
    val isParticipant = event.participantsUids.contains(currentUserId)
    val isOrganizer = event.organizerUid == currentUserId
    
    var isLoading by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
    val dateString = sdf.format(Date(event.timestamp))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details", fontWeight = FontWeight.Bold, fontSize = 26.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            val shareMessage = """
                                Check out this event: ${event.title}
                                Date: $dateString
                                Location: ${event.address}
                                Join us here: https://eventmanager.ua.pt/event/${event.id}
                            """.trimIndent()
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Event"))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
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
        ) {
            AsyncImage(
                model = event.imageUrl ?: "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                contentDescription = "Event cover image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_launcher_background)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.title,
                        modifier = Modifier.weight(1f),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (!event.isPublic) {
                        Surface(
                            color = Color(0xFF6366F1).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1))
                        ) {
                            Text(
                                "Private",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color(0xFF6366F1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DetailInfoRow(Icons.Default.Schedule, dateString)
                DetailInfoRow(Icons.Default.Place, event.address)
                
                val attendingText = if (event.maxParticipants != null) {
                    "${event.participantsUids.size}/${event.maxParticipants} attending"
                } else {
                    "${event.participantsUids.size} attending"
                }
                DetailInfoRow(Icons.Default.Groups, attendingText)

                Spacer(modifier = Modifier.height(24.dp))

                Text("Host", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DetailAvatar(text = event.organizerName.take(1).uppercase(), color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(event.organizerName, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Description", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    event.description.ifEmpty { "No description provided." },
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Gray,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (!isOrganizer) {
                    Button(
                        onClick = {
                            if (currentUserId == null) {
                                Toast.makeText(context, "Please log in to join events", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            isLoading = true
                            if (isParticipant) {
                                viewModel.leaveEvent(event.id) { success, error ->
                                    isLoading = false
                                    if (success) {
                                        Toast.makeText(context, "You left the event", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, error ?: "Failed to leave event", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                viewModel.joinEvent(event) { success, error ->
                                    isLoading = false
                                    if (success) {
                                        Toast.makeText(context, "Successfully joined!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, error ?: "Failed to join event", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isParticipant) Color.LightGray else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            if (isParticipant) "Leave Event" else "Join Event",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isParticipant) Color.DarkGray else Color.White
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        enabled = false
                    ) {
                        Text("You are the host", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 16.sp, color = Color.DarkGray)
    }
}

@Composable
private fun DetailAvatar(text: String, color: Color) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
