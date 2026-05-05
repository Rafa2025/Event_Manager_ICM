package pt.ua.EventManager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import pt.ua.EventManager.R
import pt.ua.EventManager.data.Event
import pt.ua.EventManager.ui.viewmodels.EventViewModel
import pt.ua.EventManager.ui.viewmodels.UserViewModel
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
    onEditClick: () -> Unit,
    eventViewModel: EventViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    if (event == null) return

    val currentTime = System.currentTimeMillis()
    val isHappening = currentTime in event.timestamp..event.endTimestamp
    val isUpcoming = currentTime < event.timestamp
    
    val currentUser by userViewModel.currentUser.collectAsState()
    var showInviteDialog by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
    val dateString = sdf.format(Date(event.timestamp))

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Management",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Hosting",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 36.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                AsyncImage(
                    model = event.imageUrl ?: "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                    contentDescription = "Event cover image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_launcher_background)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.6f to Color.Black.copy(alpha = 0.7f)
                            )
                        )
                )

                Surface(
                    modifier = Modifier.padding(16.dp).align(Alignment.BottomStart),
                    color = (if (isHappening) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val statusText = when {
                        isHappening -> "LIVE NOW"
                        isUpcoming -> {
                            val diff = event.timestamp - currentTime
                            val days = TimeUnit.MILLISECONDS.toDays(diff)
                            if (days > 0) "IN $days DAYS" else "SOON"
                        }
                        else -> "ENDED"
                    }
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text(
                        text = event.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 32.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = (if (event.isPublic) "Public Event" else "Private Event").uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp
                    )
                }

                Column {
                    Text("Tools", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AestheticManagementButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.People,
                            text = "Participants",
                            onClick = onParticipantsClick
                        )
                        AestheticManagementButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.PersonAdd,
                            text = "Invite Friends",
                            onClick = { showInviteDialog = true }
                        )
                        AestheticManagementButton(
                            modifier = Modifier.weight(1f),
                            icon = if (isHappening) Icons.Default.QrCode else Icons.Default.QrCodeScanner,
                            text = if (isHappening) "Check-in QR" else "QR Locked",
                            enabled = isHappening,
                            onClick = onShowQR
                        )
                    }
                    if (!isHappening && isUpcoming) {
                        Text(
                            "Check-in QR unlocks when event starts.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 10.dp, start = 4.dp)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Info",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AestheticInfoRow(Icons.Default.CalendarToday, dateString)
                        AestheticInfoRow(Icons.Default.LocationOn, event.address)
                        AestheticInfoRow(Icons.Default.Groups, "${event.participantsUids.size} people joined")
                    }
                }

                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    EventChatSection(eventId = event.id, hostUid = event.organizerUid)
                }

                Button(
                    onClick = { /* Cancel Event logic */ },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.1f),
                        contentColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Event", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showInviteDialog) {
        InviteFriendsDialog(
            friends = currentUser?.friends ?: emptyList(),
            onInvite = { friendUid ->
                eventViewModel.inviteFriendToEvent(friendUid, event, currentUser?.name ?: "Host")
            },
            onDismiss = { showInviteDialog = false },
            userViewModel = userViewModel
        )
    }
}

@Composable
fun InviteFriendsDialog(
    friends: List<String>,
    onInvite: (String) -> Unit,
    onDismiss: () -> Unit,
    userViewModel: UserViewModel
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Friends", fontWeight = FontWeight.Bold) },
        text = {
            if (friends.isEmpty()) {
                Text("No friends to invite yet.")
            } else {
                Box(modifier = Modifier.heightIn(max = 300.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(friends) { friendUid ->
                            var friendName by remember { mutableStateOf("Loading...") }
                            var isInvited by remember { mutableStateOf(false) }
                            
                            LaunchedEffect(friendUid) {
                                userViewModel.getUserName(friendUid) { friendName = it }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(friendName, modifier = Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        onInvite(friendUid)
                                        isInvited = true
                                    },
                                    enabled = !isInvited,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(if (isInvited) "Invited" else "Invite", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun AestheticManagementButton(
    modifier: Modifier,
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.5f else 0.2f),
        border = if (enabled) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
            )
        }
    }
}
