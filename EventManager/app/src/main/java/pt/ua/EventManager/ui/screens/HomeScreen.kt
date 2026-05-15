package pt.ua.EventManager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import pt.ua.EventManager.R
import pt.ua.EventManager.data.Event
import pt.ua.EventManager.ui.viewmodels.EventViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNotificationsClick: () -> Unit = {},
    onEventClick: (Event) -> Unit = {},
    viewModel: EventViewModel = viewModel()
) {
    val allEvents by viewModel.events.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val hasUnread = notifications.any { !it.isRead }
    
    val currentTime = System.currentTimeMillis()
    // Show only active public events
    val publicEvents = allEvents.filter { it.isPublic && it.endTimestamp > currentTime }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 12.dp)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Discover",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Events",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 36.sp
                        )
                    }

                    // Notification bell with blue dot indicator
                    Box {
                        IconButton(
                            onClick = onNotificationsClick,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsNone,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        if (hasUnread) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-2).dp, y = 2.dp)
                                    .background(Color(0xFF3B82F6), CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Subtle separator
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)
        ) {
            if (publicEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(vertical = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                "No active events yet",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Check back soon for upcoming events.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            items(publicEvents) { event ->
                EventCard(event, onClick = { onEventClick(event) })
            }
        }
    }
}

@Composable
fun EventCard(event: Event, onClick: () -> Unit) {
    val currentTime = System.currentTimeMillis()
    val isHappening = currentTime in event.timestamp..event.endTimestamp
    val isUpcoming = currentTime < event.timestamp

    val statusText = when {
        isHappening -> "Live Now"
        isUpcoming -> {
            val diff = event.timestamp - currentTime
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            if (days > 0) "In $days days" else if (hours > 0) "In ${hours}h" else "Soon"
        }
        else -> "Ended"
    }

    val statusColor = when {
        isHappening -> Color(0xFF16A34A)
        isUpcoming -> MaterialTheme.colorScheme.primary
        else -> Color(0xFF9CA3AF)
    }

    val sdf = SimpleDateFormat("MMM dd • h:mm a", Locale.getDefault())
    val dateString = sdf.format(Date(event.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            // Cover image
            AsyncImage(
                model = event.imageUrl
                    ?: "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_launcher_background)
            )

            // Gradient overlay — lighter at top, richer at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.4f to Color.Black.copy(alpha = 0.1f),
                            1f to Color.Black.copy(alpha = 0.82f)
                        )
                    )
            )

            // Live dot indicator for ongoing events
            if (isHappening) {
                Box(
                    modifier = Modifier
                        .padding(14.dp)
                        .align(Alignment.TopEnd)
                        .background(Color(0xFF16A34A), CircleShape)
                        .size(10.dp)
                )
            }

            // Category chip — top left
            Surface(
                modifier = Modifier
                    .padding(14.dp)
                    .align(Alignment.TopStart),
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = event.category.uppercase(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }

            // Bottom content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status pill
                Surface(
                    color = statusColor.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = event.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 24.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.White.copy(alpha = 0.75f)
                            )
                            Text(
                                dateString,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.People,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.White.copy(alpha = 0.75f)
                            )
                            Text(
                                "${event.participantsUids.size} going",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    // Organizer pill
                    Text(
                        text = "by ${event.organizerName}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
