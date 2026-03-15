package pt.ua.EventManager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object MyEvents : Screen("my_events", "Events", Icons.Default.Check)
    object EventMap : Screen("event_map", "Map", Icons.Default.Star)
    object EventCreate : Screen("event_create", "Create", Icons.Default.Add)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

val navItems = listOf(
    Screen.Home,
    Screen.EventMap,
    Screen.EventCreate,
    Screen.MyEvents,
    Screen.Profile
)
