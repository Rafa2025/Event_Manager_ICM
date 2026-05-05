package pt.ua.EventManager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.places.api.Places
import kotlinx.coroutines.launch
import pt.ua.EventManager.data.Event
import pt.ua.EventManager.ui.navigation.Screen
import pt.ua.EventManager.ui.screens.*
import pt.ua.EventManager.ui.theme.EventManagerTheme
import pt.ua.EventManager.ui.viewmodels.UserViewModel
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.toMutableStateList

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyCGatbKuPNH7AD0asDTywWzGfZg0DA73w8")
        }

        enableEdgeToEdge()
        setContent {
            val userViewModel: UserViewModel = viewModel()
            val currentUser by userViewModel.currentUser.collectAsState()

            EventManagerTheme {
                val navBarItems = listOf(
                    Screen.Home,
                    Screen.EventMap,
                    Screen.EventCreate,
                    Screen.MyEvents,
                    Screen.Profile
                )

                val allScreens = remember {
                    navBarItems + listOf(
                        Screen.Notifications, 
                        Screen.EventDetails, 
                        Screen.HostingDetails, 
                        Screen.AttendingDetails,
                        Screen.QRCode,
                        Screen.QRScanner,
                        Screen.ParticipantsList,
                        Screen.EventEdit,
                        Screen.UserSearch,
                        Screen.Friends,
                        Screen.EventHistory
                    )
                }

                var currentScreenIndex by rememberSaveable { mutableIntStateOf(0) }

                val pagerState = rememberPagerState(
                    initialPage = currentScreenIndex,
                    pageCount = { allScreens.size }
                )
                val scope = rememberCoroutineScope()
                
                // Track navigation history and current logical state
                val navigationStack = rememberSaveable(saver = listSaver(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() }
                )) { mutableStateListOf<Int>() }

                
                var selectedEvent by rememberSaveable { mutableStateOf<Event?>(null) }
                var selectedMyEvent by rememberSaveable { mutableStateOf<Event?>(null) }

                val navigateTo = { nextIndex: Int ->
                    if (currentScreenIndex != nextIndex) {
                        // Prevent adding the same screen multiple times in a row
                        if (navigationStack.isEmpty() || navigationStack.last() != currentScreenIndex) {
                            navigationStack.add(currentScreenIndex)
                        }
                        currentScreenIndex = nextIndex
                        scope.launch {
                            pagerState.scrollToPage(nextIndex)
                        }
                    }
                }

                val navigateBack = {
                    if (navigationStack.isNotEmpty()) {
                        val lastIndex = navigationStack.removeAt(navigationStack.size - 1)
                        if (lastIndex in allScreens.indices) { // Extra safety check
                            currentScreenIndex = lastIndex
                            scope.launch {
                                pagerState.scrollToPage(lastIndex)
                            }
                        }
                    }
                }

                // Handle system back button
                BackHandler(enabled = navigationStack.isNotEmpty()) {
                    navigateBack()
                }

                Scaffold(
                    bottomBar = {
                        // Use currentScreenIndex for stable UI state
                        if (currentScreenIndex < navBarItems.size) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp,
                            ) {
                                navBarItems.forEachIndexed { index, screen ->
                                    val isSelected = currentScreenIndex == index

                                    NavigationBarItem(
                                        icon = {
                                            val iconModifier = Modifier.size(24.dp)
                                            when (val icon = screen.icon) {
                                                is ImageVector -> Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    modifier = iconModifier
                                                )
                                                is Int -> Icon(
                                                    painter = painterResource(id = icon),
                                                    contentDescription = null,
                                                    modifier = iconModifier
                                                )
                                                else -> {}
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = screen.title,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        selected = isSelected,
                                        alwaysShowLabel = false,
                                        onClick = {
                                            if (!isSelected) {
                                                navigationStack.clear() // Reset history when switching main tabs
                                                currentScreenIndex = index
                                                scope.launch {
                                                    pagerState.scrollToPage(index)
                                                }
                                            }
                                        } ,
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = Color.Gray,
                                            unselectedTextColor = Color.Gray,
                                            indicatorColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.padding(if (currentScreenIndex < navBarItems.size) innerPadding else PaddingValues(0.dp)),
                        userScrollEnabled = false
                    ) { page ->
                        when (allScreens[page]) {
                            Screen.Home -> HomeScreen(
                                onNotificationsClick = { navigateTo(allScreens.indexOf(Screen.Notifications)) },
                                onEventClick = { event ->
                                    selectedEvent = event
                                    navigateTo(allScreens.indexOf(Screen.EventDetails))
                                }
                            )
                            Screen.EventMap -> EventMapScreen(
                                onNotificationsClick = { navigateTo(allScreens.indexOf(Screen.Notifications)) },
                                onEventClick = { event ->
                                    selectedEvent = event
                                    navigateTo(allScreens.indexOf(Screen.EventDetails))
                                }
                            )
                            Screen.EventCreate -> {
                                if (currentUser == null) {
                                    LoginRequiredScreen(
                                        title = "Create Event",
                                        message = "You need to be logged in to create an event.",
                                        onLoginClick = {
                                            navigationStack.clear()
                                            val profileIndex = navBarItems.indexOf(Screen.Profile)
                                            currentScreenIndex = profileIndex
                                            scope.launch {
                                                pagerState.scrollToPage(profileIndex)
                                            }
                                        }
                                    )
                                } else {
                                    EventCreateScreen(onNotificationsClick = { navigateTo(allScreens.indexOf(Screen.Notifications)) })
                                }
                            }
                            Screen.MyEvents -> {
                                if (currentUser == null) {
                                    LoginRequiredScreen(
                                        title = "My Events",
                                        message = "Log in to see your hosted and joined events.",
                                        onLoginClick = {
                                            navigationStack.clear()
                                            val profileIndex = navBarItems.indexOf(Screen.Profile)
                                            currentScreenIndex = profileIndex
                                            scope.launch {
                                                pagerState.scrollToPage(profileIndex)
                                            }
                                        }
                                    )
                                } else {
                                    MyEventsScreen(
                                        onNotificationsClick = { navigateTo(allScreens.indexOf(Screen.Notifications)) },
                                        onEventClick = { event, isHosting ->
                                            selectedMyEvent = event
                                            val route = if (isHosting) Screen.HostingDetails else Screen.AttendingDetails
                                            navigateTo(allScreens.indexOf(route))
                                        }
                                    )
                                }
                            }
                            Screen.Profile -> {
                                if (currentUser == null) {
                                    LoginScreen(userViewModel = userViewModel) {
                                        // Stay on page
                                    }
                                } else {
                                    ProfileScreen(
                                        onNotificationsClick = { navigateTo(allScreens.indexOf(Screen.Notifications)) },
                                        onSearchUsersClick = { navigateTo(allScreens.indexOf(Screen.UserSearch)) },
                                        onFriendsClick = { navigateTo(allScreens.indexOf(Screen.Friends)) },
                                        onEventHistoryClick = { navigateTo(allScreens.indexOf(Screen.EventHistory)) },
                                        userViewModel = userViewModel
                                    )
                                }
                            }
                            Screen.Notifications -> NotificationsScreen(
                                onBack = { navigateBack() },
                                onNavigateToFriends = { navigateTo(allScreens.indexOf(Screen.Friends)) },
                                onNavigateToEventDetails = { event ->
                                    selectedEvent = event
                                    navigateTo(allScreens.indexOf(Screen.EventDetails))
                                }
                            )
                            Screen.EventDetails -> EventDetailsScreen(
                                event = selectedEvent,
                                onBack = { navigateBack() }
                            )
                            Screen.HostingDetails -> HostingDetailsScreen(
                                event = selectedMyEvent,
                                onBack = { navigateBack() },
                                onShowQR = {
                                    navigateTo(allScreens.indexOf(Screen.QRCode))
                                },
                                onParticipantsClick = {
                                    navigateTo(allScreens.indexOf(Screen.ParticipantsList))
                                },
                                onEditClick = {
                                    navigateTo(allScreens.indexOf(Screen.EventEdit))
                                }
                            )
                            Screen.AttendingDetails -> AttendingDetailsScreen(
                                event = selectedMyEvent,
                                onBack = { navigateBack() },
                                onScanQR = {
                                    navigateTo(allScreens.indexOf(Screen.QRScanner))
                                }
                            )
                            Screen.QRCode -> EventQRCodeScreen(
                                event = selectedMyEvent,
                                onBack = { navigateBack() }
                            )
                            Screen.QRScanner -> QRScannerScreen(
                                event = selectedMyEvent,
                                onBack = { navigateBack() },
                                onScanResult = { success ->
                                    if (success) {
                                        navigateBack() // Automatically go back to AttendingDetails on success
                                    }
                                }
                            )
                            Screen.ParticipantsList -> ParticipantsListScreen(
                                event = selectedMyEvent,
                                onBack = { navigateBack() },
                                userViewModel = userViewModel
                            )
                            Screen.EventEdit -> EventEditScreen(
                                event = selectedMyEvent,
                                onBack = { navigateBack() }
                            )
                            Screen.UserSearch -> UserSearchScreen(
                                onBack = { navigateBack() },
                                userViewModel = userViewModel
                            )
                            Screen.Friends -> FriendsScreen(
                                onBack = { navigateBack() },
                                onSearchClick = { navigateTo(allScreens.indexOf(Screen.UserSearch)) },
                                userViewModel = userViewModel
                            )
                            Screen.EventHistory -> EventHistoryScreen(
                                onBack = { navigateBack() },
                                onEventClick = { event ->
                                    selectedMyEvent = event
                                    // Navigate to details depending on role, but for history we can use a generic one or just the existing ones
                                    val isHosting = event.organizerUid == currentUser?.uid
                                    val route = if (isHosting) Screen.HostingDetails else Screen.AttendingDetails
                                    navigateTo(allScreens.indexOf(route))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginRequiredScreen(title: String, message: String, onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Go to Login", fontWeight = FontWeight.Bold)
        }
    }
}
