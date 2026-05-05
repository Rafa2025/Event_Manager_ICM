package pt.ua.EventManager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.ua.EventManager.data.Event
import pt.ua.EventManager.data.Notification
import pt.ua.EventManager.ui.viewmodels.EventViewModel
import pt.ua.EventManager.ui.viewmodels.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToEventDetails: (Event) -> Unit,
    eventViewModel: EventViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val notifications by eventViewModel.notifications.collectAsState()

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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Updates",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Notifications",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 36.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        },
        bottomBar = {
            if (notifications.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = { eventViewModel.clearAllNotifications() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear All",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Clear All Notifications", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "No notifications yet",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "We'll let you know when something happens.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        eventViewModel = eventViewModel,
                        userViewModel = userViewModel,
                        onNavigateToFriends = onNavigateToFriends,
                        onNavigateToEventDetails = onNavigateToEventDetails
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    eventViewModel: EventViewModel,
    userViewModel: UserViewModel,
    onNavigateToFriends: () -> Unit,
    onNavigateToEventDetails: (Event) -> Unit
) {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val timeString = sdf.format(Date(notification.timestamp))
    
    val allEvents by eventViewModel.events.collectAsState()
    val event = allEvents.find { it.id == notification.eventId }
    val isAlreadyJoined = event?.participantsUids?.contains(eventViewModel.currentUserUid) == true
    
    val currentUser by userViewModel.currentUser.collectAsState()
    val isFriend = currentUser?.friends?.contains(notification.senderUid) == true
    val isPending = currentUser?.friendRequestsReceived?.contains(notification.senderUid) == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (!notification.isRead) {
                    eventViewModel.markNotificationAsRead(notification.id)
                }
                when (notification.type) {
                    "friend_request" -> onNavigateToFriends()
                    "event_invite" -> event?.let { onNavigateToEventDetails(it) }
                }
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 0.dp else 4.dp),
        border = if (!notification.isRead) 
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) 
        else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Read/Unread dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (notification.isRead) Color.Transparent else MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (notification.type) {
                            "event_invite" -> "Event Invitation"
                            "friend_request" -> "Friend Request"
                            else -> "Notification"
                        },
                        fontWeight = if (notification.isRead) FontWeight.Bold else FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (notification.isRead) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                    
                    if (!notification.isRead) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "NEW",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (notification.type) {
                        "event_invite" -> "${notification.senderName} invited you to '${notification.eventTitle}'"
                        "friend_request" -> "${notification.senderName} wants to be your friend"
                        else -> "You have a new update"
                    },
                    fontSize = 14.sp,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                
                if (notification.type == "event_invite" && event != null && !isAlreadyJoined) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            eventViewModel.joinEvent(event) { _, _ -> }
                            eventViewModel.markNotificationAsRead(notification.id)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Join Event", fontSize = 12.sp)
                    }
                } else if (notification.type == "friend_request" && isPending) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                userViewModel.acceptFriendRequest(notification.senderUid)
                                eventViewModel.markNotificationAsRead(notification.id)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Accept", fontSize = 12.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = {
                                userViewModel.declineFriendRequest(notification.senderUid)
                                eventViewModel.markNotificationAsRead(notification.id)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Decline", fontSize = 12.sp)
                        }
                    }
                } else if (notification.type == "event_invite" && isAlreadyJoined) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Joined", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else if (notification.type == "friend_request" && isFriend) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Friends", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = timeString.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (notification.isRead) 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) 
                    else 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
