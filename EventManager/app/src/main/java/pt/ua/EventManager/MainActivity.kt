package pt.ua.EventManager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pt.ua.EventManager.ui.navigation.Screen
import pt.ua.EventManager.ui.screens.*
import pt.ua.EventManager.ui.theme.EventManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EventManagerTheme {
                val navBarItems = listOf(
                    Screen.Home,
                    Screen.EventMap,
                    Screen.EventCreate,
                    Screen.MyEvents,
                    Screen.Profile
                )

                // Screens that are not in the bottom bar
                val allScreens = navBarItems + Screen.Notifications + Screen.EventDetails + Screen.HostingDetails + Screen.AttendingDetails

                val pagerState = rememberPagerState(pageCount = { allScreens.size })
                val scope = rememberCoroutineScope()
                
                // Track the previous page to return to when closing notifications or details
                var previousPageIndex by remember { mutableIntStateOf(0) }
                
                // State to hold the currently selected event for the details screen
                var selectedEvent by remember { mutableStateOf<Event?>(null) }
                var selectedMyEvent by remember { mutableStateOf<MyEvent?>(null) }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                        ) {
                            navBarItems.forEachIndexed { index, screen ->
                                // If we are on a "hidden" screen, highlight the tab we came from
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
                            Screen.EventMap -> EventMapScreen(onNotificationsClick = { onNotificationsClick() })
                            Screen.EventCreate -> EventCreateScreen(onNotificationsClick = { onNotificationsClick() })
                            Screen.MyEvents -> MyEventsScreen(
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
                            Screen.Profile -> ProfileScreen(onNotificationsClick = { onNotificationsClick() })
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
