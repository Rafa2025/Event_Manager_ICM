package pt.ua.EventManager.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditScreen(
    event: Event?,
    onBack: () -> Unit,
    viewModel: EventViewModel = viewModel()
) {
    if (event == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Event not found")
        }
        return
    }

    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = event.timestamp }
    val endCalendar = Calendar.getInstance().apply { timeInMillis = event.endTimestamp }
    
    // Places API states
    val placesClient = remember { Places.createClient(context) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var eventLocation by remember { mutableStateOf(event.location) }

    // Form states initialized with existing event data
    var eventName by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description) }
    var category by remember { mutableStateOf(event.category) }
    
    val sdfDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
    val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    var dateText by remember { mutableStateOf(sdfDate.format(Date(event.timestamp))) }
    var startTimeText by remember { mutableStateOf(sdfTime.format(Date(event.timestamp))) }
    var endTimeText by remember { mutableStateOf(sdfTime.format(Date(event.endTimestamp))) }
    
    var address by remember { mutableStateOf(event.address) }
    var minPeople by remember { mutableStateOf(event.minParticipants.toString()) }
    var maxPeople by remember { mutableStateOf(event.maxParticipants?.toString() ?: "") }
    var foodOption by remember { mutableStateOf(event.foodOption) }
    var isPrivate by remember { mutableStateOf(!event.isPublic) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    var timestamp by remember { mutableLongStateOf(event.timestamp) }
    var endTimestamp by remember { mutableLongStateOf(event.endTimestamp) }

    // Search logic
    fun fetchAddressSuggestions(query: String) {
        if (query.length < 2) {
            predictions = emptyList()
            return
        }
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setQuery(query)
            .build()
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response -> predictions = response.autocompletePredictions }
            .addOnFailureListener { Log.e("Places", "Prediction fetching failed", it) }
    }

    fun fetchPlaceDetails(prediction: AutocompletePrediction) {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val request = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                address = place.address ?: place.name ?: ""
                place.latLng?.let { eventLocation = GeoPoint(it.latitude, it.longitude) }
                predictions = emptyList()
            }
            .addOnFailureListener { Log.e("Places", "Place details fetching failed", it) }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    val datePickerDialog = DatePickerDialog(context, { _, year, month, day ->
        calendar.set(year, month, day)
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
            TopAppBar(
                title = { Text("Edit Event", fontWeight = FontWeight.Bold, fontSize = 26.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedImageUri ?: event.imageUrl ?: "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                    contentDescription = "Cover Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, "Change Image", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledBorderColor = Color.LightGray.copy(alpha = 0.5f),
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = eventName,
                onValueChange = { eventName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Event Name") },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                label = { Text("Description") },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            // Category
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Category") },
                    trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                    shape = RoundedCornerShape(12.dp),
                    readOnly = true,
                    colors = textFieldColors,
                    enabled = false
                )
                Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("Meetup", "Dinner", "Party", "Workshop", "Other").forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expanded = false })
                    }
                }
            }

            // Date & Time
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Date") },
                    leadingIcon = { Icon(Icons.Default.DateRange, null) },
                    shape = RoundedCornerShape(12.dp),
                    readOnly = true,
                    colors = textFieldColors,
                    enabled = false
                )
                Box(modifier = Modifier.matchParentSize().clickable { datePickerDialog.show() })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = startTimeText,
                        onValueChange = {},
                        label = { Text("Start") },
                        leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        colors = textFieldColors,
                        enabled = false
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { startTimePickerDialog.show() })
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = endTimeText,
                        onValueChange = {},
                        label = { Text("End") },
                        leadingIcon = { Icon(Icons.Default.AccessTime, null) },
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        colors = textFieldColors,
                        enabled = false
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { endTimePickerDialog.show() })
                }
            }

            // Location
            Column {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; fetchAddressSuggestions(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Location") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
                if (predictions.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        predictions.forEach { prediction ->
                            ListItem(
                                headlineContent = { Text(prediction.getFullText(null).toString()) },
                                modifier = Modifier.clickable { fetchPlaceDetails(prediction) }
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = minPeople,
                    onValueChange = { if (it.all { it.isDigit() }) minPeople = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Min People") },
                    colors = textFieldColors
                )
                OutlinedTextField(
                    value = maxPeople,
                    onValueChange = { if (it.all { it.isDigit() }) maxPeople = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Max People") },
                    colors = textFieldColors
                )
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = {
                        if (eventName.isBlank() || address.isBlank()) {
                            Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        val updatedEvent = event.copy(
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
                        viewModel.updateEvent(updatedEvent, selectedImageUri) { success, error ->
                            isLoading = false
                            if (success) {
                                Toast.makeText(context, "Event updated!", Toast.LENGTH_SHORT).show()
                                onBack()
                            } else {
                                Toast.makeText(context, error ?: "Update failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
