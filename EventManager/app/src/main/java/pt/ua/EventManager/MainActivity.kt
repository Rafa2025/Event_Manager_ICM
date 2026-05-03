package pt.ua.EventManager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Places SDK
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

                val allScreens = navBarItems + Screen.Notifications + Screen.EventDetails + Screen.HostingDetails + Screen.AttendingDetails

                val pagerState = rememberPagerState(pageCount = { allScreens.size })
                val scope = rememberCoroutineScope()
                
                var previousPageIndex by remember { mutableIntStateOf(0) }
                var selectedEvent by remember { mutableStateOf<Event?>(null) }
                var selectedMyEvent by remember { mutableStateOf<Event?>(null) }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                        ) {
                            navBarItems.forEachIndexed { index, screen ->
                                val isSelected = if (pagerState.currentPage >= navBarItems.size) {
                                    index == previousPageIndex
                                } else {
                                    pagerState.currentPage == index
                                }

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
                                        scope.launch {
                                            pagerState.scrollToPage(index)
                                        }
                                    },
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
                ) { innerPadding ->
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.padding(innerPadding),
                        userScrollEnabled = false
                    ) { page ->
                        val onNotificationsClick = {
                            previousPageIndex = pagerState.currentPage
                            scope.launch {
                                pagerState.scrollToPage(allScreens.indexOf(Screen.Notifications))
                            }
                        }

                        when (allScreens[page]) {
                            Screen.Home -> HomeScreen(
                                onNotificationsClick = { onNotificationsClick() },
                                onEventClick = { event ->
                                    selectedEvent = event
                                    previousPageIndex = pagerState.currentPage
                                    scope.launch {
                                        pagerState.scrollToPage(allScreens.indexOf(Screen.EventDetails))
                                    }
                                }
                            )
                            Screen.EventMap -> EventMapScreen(
                                onNotificationsClick = { onNotificationsClick() },
                                onEventClick = { event ->
                                    selectedEvent = event
                                    previousPageIndex = pagerState.currentPage
                                    scope.launch {
                                        pagerState.scrollToPage(allScreens.indexOf(Screen.EventDetails))
                                    }
                                }
                            )
                            Screen.EventCreate -> {
                                if (currentUser == null) {
                                    LoginRequiredScreen(
                                        title = "Create Event",
                                        message = "You need to be logged in to create an event.",
                                        onLoginClick = {
                                            scope.launch {
                                                pagerState.scrollToPage(navBarItems.indexOf(Screen.Profile))
                                            }
                                        }
                                    )
                                } else {
                                    EventCreateScreen(onNotificationsClick = { onNotificationsClick() })
                                }
                            }
                            Screen.MyEvents -> {
                                if (currentUser == null) {
                                    LoginRequiredScreen(
                                        title = "My Events",
                                        message = "Log in to see your hosted and joined events.",
                                        onLoginClick = {
                                            scope.launch {
                                                pagerState.scrollToPage(navBarItems.indexOf(Screen.Profile))
                                            }
                                        }
                                    )
                                } else {
                                    MyEventsScreen(
                                        onNotificationsClick = { onNotificationsClick() },
                                        onEventClick = { event, isHosting ->
                                            selectedMyEvent = event
                                            previousPageIndex = pagerState.currentPage
                                            scope.launch {
                                                val route = if (isHosting) Screen.HostingDetails else Screen.AttendingDetails
                                                pagerState.scrollToPage(allScreens.indexOf(route))
                                            }
                                        }
                                    )
                                }
                            }
                            Screen.Profile -> {
                                if (currentUser == null) {
                                    LoginScreen(userViewModel = userViewModel) {
                                        // On login success, we stay on this page which will now show ProfileScreen
                                    }
                                } else {
                                    ProfileScreen(onNotificationsClick = { onNotificationsClick() }, userViewModel = userViewModel)
                                }
                            }
                            Screen.Notifications -> NotificationsScreen(onBack = {
                                scope.launch {
                                    pagerState.scrollToPage(previousPageIndex)
                                }
                            })
                            Screen.EventDetails -> EventDetailsScreen(
                                event = selectedEvent,
                                onBack = {
                                    scope.launch {
                                        pagerState.scrollToPage(previousPageIndex)
                                    }
                                }
                            )
                            Screen.HostingDetails -> HostingDetailsScreen(
                                event = selectedMyEvent,
                                onBack = {
                                    scope.launch {
                                        pagerState.scrollToPage(previousPageIndex)
                                    }
                                }
                            )
                            Screen.AttendingDetails -> AttendingDetailsScreen(
                                event = selectedMyEvent,
                                onBack = {
                                    scope.launch {
                                        pagerState.scrollToPage(previousPageIndex)
                                    }
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
