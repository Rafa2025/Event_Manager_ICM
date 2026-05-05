package pt.ua.EventManager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import pt.ua.EventManager.R

sealed class Screen(val route: String, val title: String, val icon: Any) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object MyEvents : Screen("my_events", "Events", R.drawable.baseline_event_24)
    object EventMap : Screen("event_map",  "Map", R.drawable.outline_map_24)
    object EventCreate : Screen("event_create", "Create", Icons.Default.Add)
    object Profile : Screen("profile", "Profile",Icons.Default.Person)

    object Notifications : Screen("notifications", "Notifications", Icons.Default.Notifications)
    object EventDetails : Screen("event_details", "Event Details", Icons.Default.Info)
    object HostingDetails : Screen("hosting_details", "Hosting Details", Icons.Default.Info)
    object AttendingDetails : Screen("attending_details", "Attending Details", Icons.Default.Info)
    
    object QRCode : Screen("qr_code", "Event QR", Icons.Default.QrCode)
    object QRScanner : Screen("qr_scanner", "Scanner", Icons.Default.QrCodeScanner)
    object ParticipantsList : Screen("participants_list", "Participants", Icons.Default.People)
    object EventEdit : Screen("event_edit", "Edit Event", Icons.Default.Edit)
}

val navItems = listOf(
    Screen.Home,
    Screen.EventMap,
    Screen.EventCreate,
    Screen.MyEvents,
    Screen.Profile,
    Screen.Notifications,
    Screen.EventDetails,
    Screen.HostingDetails,
    Screen.AttendingDetails,
    Screen.QRCode,
    Screen.QRScanner,
    Screen.ParticipantsList,
    Screen.EventEdit
)
