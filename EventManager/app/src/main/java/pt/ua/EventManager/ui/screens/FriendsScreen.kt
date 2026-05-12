package pt.ua.EventManager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.ua.EventManager.data.User
import pt.ua.EventManager.ui.viewmodels.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    userViewModel: UserViewModel = viewModel(),
    initialTab: Int = 0,
    onTabChange: (Int) -> Unit = {}
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 0.dp)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("COMMUNITY", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.5.sp)
                        Text("Friends", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.PersonAdd, "Search Users", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { 
                            selectedTab = 0 
                            onTabChange(0)
                        },
                        text = { Text("My Friends", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { 
                            selectedTab = 1 
                            onTabChange(1)
                        },
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Requests", fontWeight = FontWeight.Bold)
                                if ((currentUser?.friendRequestsReceived?.size ?: 0) > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text(currentUser?.friendRequestsReceived?.size.toString(), color = Color.White)
                                    }
                                }
                            }
                        }
                    )
                }
                
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (selectedTab == 0) {
                FriendsList(currentUser?.friends ?: emptyList(), userViewModel)
            } else {
                FriendRequestsList(currentUser?.friendRequestsReceived ?: emptyList(), userViewModel)
            }
        }
    }
}

@Composable
fun FriendsList(friendUids: List<String>, userViewModel: UserViewModel) {
    if (friendUids.isEmpty()) {
        EmptyFriendsState("You don't have any friends yet.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(friendUids) { uid ->
            FriendItem(uid, userViewModel)
        }
    }
}

@Composable
fun FriendRequestsList(requestUids: List<String>, userViewModel: UserViewModel) {
    if (requestUids.isEmpty()) {
        EmptyFriendsState("No pending friend requests.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requestUids) { uid ->
            RequestItem(uid, userViewModel)
        }
    }
}

@Composable
fun FriendItem(uid: String, userViewModel: UserViewModel) {
    var name by remember { mutableStateOf("Loading...") }
    var showUnfriendDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        userViewModel.getUserName(uid) { result ->
            name = result ?: "Unknown User"
        }
    }

    if (showUnfriendDialog) {
        AlertDialog(
            onDismissRequest = { showUnfriendDialog = false },
            title = { Text("Unfriend $name?") },
            text = { Text("Are you sure you want to remove $name from your friends list?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        userViewModel.removeFriend(uid)
                        showUnfriendDialog = false
                    }
                ) {
                    Text("Unfriend", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnfriendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Active Member", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { /* Message Friend */ }) {
                    Icon(Icons.Default.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showUnfriendDialog = true }) {
                    Icon(Icons.Default.PersonRemove, null, tint = Color(0xFFEF4444).copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun RequestItem(uid: String, userViewModel: UserViewModel) {
    var name by remember { mutableStateOf("Loading...") }
    LaunchedEffect(uid) {
        userViewModel.getUserName(uid) { result ->
            name = result ?: "Unknown User"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Wants to be friends", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { userViewModel.declineFriendRequest(uid) },
                    modifier = Modifier.size(36.dp).background(Color(0xFFEF4444).copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { userViewModel.acceptFriendRequest(uid) },
                    modifier = Modifier.size(36.dp).background(Color(0xFF16A34A).copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyFriendsState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.PeopleOutline,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
