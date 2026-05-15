package pt.ua.EventManager.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.firebase.firestore.GeoPoint
import pt.ua.EventManager.data.Event
import pt.ua.EventManager.ui.viewmodels.EventViewModel
import pt.ua.EventManager.ui.viewmodels.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCreateScreen(
    onNotificationsClick: () -> Unit = {},
    viewModel: EventViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val endCalendar = Calendar.getInstance()
    val currentUser by userViewModel.currentUser.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val hasUnread = notifications.any { !it.isRead }

    // Places API states
    val placesClient = remember { Places.createClient(context) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var eventLocation by remember { mutableStateOf(GeoPoint(0.0, 0.0)) }

    // Form States
    var eventName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Meetup") }
    
    val sdfDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
    val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    var dateText by remember { mutableStateOf("mm/dd/yy") }
    var startTimeText by remember { mutableStateOf("--:-- --") }
    var endTimeText by remember { mutableStateOf("--:-- --") }
    var address by remember { mutableStateOf("") }
    var minPeople by remember { mutableStateOf("5") }
    var maxPeople by remember { mutableStateOf("30") }
    var foodOption by remember { mutableStateOf("None") }
    var isPrivate by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var endTimestamp by remember { mutableLongStateOf(System.currentTimeMillis() + 3600000) }

    // Helper to reset fields
    fun resetFields() {
        eventName = ""
        description = ""
        category = "Meetup"
        dateText = "mm/dd/yy"
        startTimeText = "--:-- --"
        endTimeText = "--:-- --"
        address = ""
        minPeople = "5"
        maxPeople = "30"
        foodOption = "None"
        isPrivate = false
        selectedImageUri = null
        eventLocation = GeoPoint(0.0, 0.0)
    }

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    // Dialogs
    val datePickerDialog = DatePickerDialog(context, { _, year, month, day ->
        calendar.set(year, month, day)
        endCalendar.set(year, month, day)
        dateText = sdfDate.format(calendar.time)
        timestamp = calendar.timeInMillis
        if (endTimestamp < timestamp) endTimestamp = timestamp + 3600000
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    val startTimePickerDialog = TimePickerDialog(context, { _, h, m ->
        calendar.set(Calendar.HOUR_OF_DAY, h)
        calendar.set(Calendar.MINUTE, m)
        startTimeText = sdfTime.format(calendar.time)
        timestamp = calendar.timeInMillis
    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)

    val endTimePickerDialog = TimePickerDialog(context, { _, h, m ->
        endCalendar.set(Calendar.HOUR_OF_DAY, h)
        endCalendar.set(Calendar.MINUTE, m)
        endTimeText = sdfTime.format(endCalendar.time)
        endTimestamp = endCalendar.timeInMillis
    }, endCalendar.get(Calendar.HOUR_OF_DAY), endCalendar.get(Calendar.MINUTE), false)

    // Places API Helpers
    fun fetchAddressSuggestions(query: String) {
        if (query.length < 2) { predictions = emptyList(); return }
        val request = FindAutocompletePredictionsRequest.builder().setSessionToken(sessionToken).setQuery(query).build()
        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response -> 
            predictions = response.autocompletePredictions 
        }
    }

    fun fetchPlaceDetails(prediction: AutocompletePrediction) {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val request = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            address = place.address ?: place.name ?: ""
            place.latLng?.let { eventLocation = GeoPoint(it.latitude, it.longitude) }
            predictions = emptyList()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 12.dp)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Planning",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "New Event",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 36.sp
                        )
                    }

                    Box {
                        IconButton(
                            onClick = onNotificationsClick,
                            modifier = Modifier
                                .size(44.dp)
                                .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsNone,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        if (hasUnread) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-2).dp, y = 2.dp)
                                    .background(Color(0xFF3B82F6), CircleShape)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Cover Image Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(color = Color.LightGray.copy(alpha = 0.5f), style = stroke)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Add Cover Image", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Input Fields Aesthetic
            val fieldModifier = Modifier.fillMaxWidth()
            val fieldColors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = eventName,
                onValueChange = { eventName = it },
                modifier = fieldModifier,
                placeholder = { Text("Event Title") },
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Description") },
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors
            )

            // Category Dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            Box(modifier = fieldModifier) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Category") },
                    trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    readOnly = true
                )
                Box(modifier = Modifier.matchParentSize().clickable { categoryExpanded = true })
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    listOf("Meetup", "Dinner", "Party", "Workshop", "Other").forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { category = cat; categoryExpanded = false }
                        )
                    }
                }
            }

            // Date Picker Field
            Box(modifier = fieldModifier) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Date") },
                    leadingIcon = { Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    readOnly = true
                )
                Box(modifier = Modifier.matchParentSize().clickable { datePickerDialog.show() })
            }

            // Time Pickers Row
            Row(modifier = fieldModifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = startTimeText,
                        onValueChange = {},
                        placeholder = { Text("Start Time") },
                        leadingIcon = { Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        readOnly = true
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { startTimePickerDialog.show() })
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = endTimeText,
                        onValueChange = {},
                        placeholder = { Text("End Time") },
                        leadingIcon = { Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.primary) },
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        readOnly = true
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { endTimePickerDialog.show() })
                }
            }

            // Location with Suggestions
            Column(modifier = fieldModifier) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; fetchAddressSuggestions(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search location...") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors
                )
                if (predictions.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        predictions.forEach { prediction ->
                            ListItem(
                                headlineContent = { Text(prediction.getFullText(null).toString(), fontSize = 14.sp) },
                                modifier = Modifier.clickable { fetchPlaceDetails(prediction) }
                            )
                        }
                    }
                }
            }

            // Participants Row
            Row(modifier = fieldModifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minPeople,
                    onValueChange = { if (it.all { c -> c.isDigit() }) minPeople = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Min People") },
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = maxPeople,
                    onValueChange = { if (it.all { c -> c.isDigit() }) maxPeople = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Max People") },
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors
                )
            }

            // Food Options
            Column {
                Text("Food & Drinks", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Food", "Drinks", "Both", "None").forEach { option ->
                        val isSelected = foodOption == option
                        Surface(
                            modifier = Modifier.clickable { foodOption = option },
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Private Event Card
            Card(
                modifier = fieldModifier,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Private Event", fontWeight = FontWeight.Bold)
                        Text("Only invited people can join", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                }
            }

            // Create Button
            Button(
                onClick = {
                    if (eventName.isBlank() || address.isBlank() || dateText == "mm/dd/yy") {
                        Toast.makeText(context, "Fill required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    val newEvent = Event(
                        title = eventName,
                        description = description,
                        category = category,
                        address = address,
                        minParticipants = minPeople.toIntOrNull() ?: 0,
                        maxParticipants = maxPeople.toIntOrNull(),
                        foodOption = foodOption,
                        isPublic = !isPrivate,
                        timestamp = timestamp,
                        endTimestamp = endTimestamp,
                        location = eventLocation
                    )
                    viewModel.createEvent(newEvent, currentUser?.name ?: "Anonymous", selectedImageUri) { success, _ ->
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "Event Created!", Toast.LENGTH_SHORT).show()
                            resetFields()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Create Event", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}
