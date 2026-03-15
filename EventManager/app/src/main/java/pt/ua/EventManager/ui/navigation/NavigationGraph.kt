package pt.ua.EventManager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import pt.ua.EventManager.ui.screens.*

@Composable
fun NavigationGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination = Screen.Home.route, modifier = modifier) {
        composable(Screen.Home.route) { HomeScreen() }
        composable(Screen.EventMap.route) { EventMapScreen() }
        composable(Screen.EventCreate.route) { EventCreateScreen() }
        composable(Screen.MyEvents.route) { MyEventsScreen() }
        composable(Screen.Profile.route) { ProfileScreen() }
    }
}
