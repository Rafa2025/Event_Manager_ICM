package pt.ua.EventManager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
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
                val items = listOf(
                    Screen.Home,
                    Screen.EventMap,
                    Screen.EventCreate,
                    Screen.MyEvents,
                    Screen.Profile
                )
                val pagerState = rememberPagerState(pageCount = { items.size })
                val scope = rememberCoroutineScope()

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            items.forEachIndexed { index, screen ->
                                NavigationBarItem(
                                    icon = {
                                        when (val icon = screen.icon) {
                                            is ImageVector -> Icon(imageVector = icon, contentDescription = null)
                                            is Int -> Icon(painter = painterResource(id = icon), contentDescription = null)
                                            else -> {}
                                        }
                                    },
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.padding(innerPadding)
                    ) { page ->
                        when (items[page]) {
                            Screen.Home -> HomeScreen()
                            Screen.EventMap -> EventMapScreen()
                            Screen.EventCreate -> EventCreateScreen()
                            Screen.MyEvents -> MyEventsScreen()
                            Screen.Profile -> ProfileScreen()
                        }
                    }
                }
            }
        }
    }
}
