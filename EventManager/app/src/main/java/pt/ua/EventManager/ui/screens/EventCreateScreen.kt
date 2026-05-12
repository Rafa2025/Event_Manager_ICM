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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.google.firebase.firestore.GeoPoint
import pt.ua.EventManager.data.Event
import pt.ua.EventManager.ui.viewmodels.EventViewModel
import pt.ua.EventManager.ui.viewmodels.UserViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import androidx.compose.ui.window.PopupProperties

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

    // Form States
    var eventName by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Meetup") }

    val sdfDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
    val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())

    var dateText by rememberSaveable { mutableStateOf("mm/dd/yy") }
    var startTimeText by rememberSaveable { mutableStateOf("--:-- --") }
    var endTimeText by rememberSaveable { mutableStateOf("--:-- --") }
    var address by rememberSaveable { mutableStateOf("") }
    var minPeople by rememberSaveable { mutableStateOf("5") }
    var maxPeople by rememberSaveable { mutableStateOf("30") }
    var foodOption by rememberSaveable { mutableStateOf("None") }
    var isPrivate by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    var timestamp by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var endTimestamp by rememberSaveable { mutableLongStateOf(System.currentTimeMillis() + 3600000) }
    
    var lat by rememberSaveable { mutableDoubleStateOf(0.0) }
    var lng by rememberSaveable { mutableDoubleStateOf(0.0) }
    val eventLocation = GeoPoint(lat, lng)

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
        lat = 0.0
        lng = 0.0
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

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

            // Chamada do componente de localização corrigida
            LocationAutocompleteField(
                address = address,
                onAddressChange = { newValue: String -> address = newValue },
                onLocationSelected = { selectedAddress: String, location: GeoPoint ->
                    address = selectedAddress
                    lat = location.latitude
                    lng = location.longitude
                },
                modifier = fieldModifier
            )

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

