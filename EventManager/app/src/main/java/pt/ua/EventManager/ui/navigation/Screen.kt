package pt.ua.EventManager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import pt.ua.EventManager.R

sealed class Screen(val route: String, val icon: Any) {
    object Home : Screen("home", Icons.Default.Home)
    object MyEvents : Screen("my_events",  R.drawable.baseline_event_24)
    object EventMap : Screen("event_map",  R.drawable.outline_map_24)
    object EventCreate : Screen("event_create",  Icons.Default.Add)
    object Profile : Screen("profile", Icons.Default.Person)
}

val navItems = listOf(
    Screen.Home,
    Screen.EventMap,
    Screen.EventCreate,
    Screen.MyEvents,
    Screen.Profile
)
