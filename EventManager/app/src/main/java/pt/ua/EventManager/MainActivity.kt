package pt.ua.EventManager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

                // Notifications is a screen but not in the bottom bar
                val allScreens = navBarItems + Screen.Notifications

                val pagerState = rememberPagerState(pageCount = { allScreens.size })
                val scope = rememberCoroutineScope()
                
                // Track the previous page to return to when closing notifications
                var previousPageIndex by remember { mutableIntStateOf(0) }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                        ) {
                            navBarItems.forEachIndexed { index, screen ->
                                // If we are on Notifications screen, none of the items should be selected
                                val isSelected = if (pagerState.currentPage == allScreens.indexOf(Screen.Notifications)) {
                                    false
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
                                    alwaysShowLabel = false, // Labels will only appear when selected
                                    onClick = {
                                        scope.launch {
                                            // Using scrollToPage instead of animateScrollToPage to avoid sliding through intermediate pages
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
                        userScrollEnabled = false // Disable swiping to avoid accidental navigation to hidden screens
                    ) { page ->
                        val onNotificationsClick = {
                            previousPageIndex = pagerState.currentPage
                            scope.launch {
                                // Instant transition to notifications
                                pagerState.scrollToPage(allScreens.indexOf(Screen.Notifications))
                            }
                        }

                        when (allScreens[page]) {
                            Screen.Home -> HomeScreen(onNotificationsClick = { onNotificationsClick() })
                            Screen.EventMap -> EventMapScreen(onNotificationsClick = { onNotificationsClick() })
                            Screen.EventCreate -> EventCreateScreen(onNotificationsClick = { onNotificationsClick() })
                            Screen.MyEvents -> MyEventsScreen(onNotificationsClick = { onNotificationsClick() })
                            Screen.Profile -> ProfileScreen(onNotificationsClick = { onNotificationsClick() })
                            Screen.Notifications -> NotificationsScreen(onBack = {
                                scope.launch {
                                    // Instant transition back
                                    pagerState.scrollToPage(previousPageIndex)
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}
