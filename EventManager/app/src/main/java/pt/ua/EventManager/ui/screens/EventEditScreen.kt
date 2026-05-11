package pt.ua.EventManager.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    // Form states
    var eventName by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description) }
    var category by remember { mutableStateOf(event.category) }

    val sdfDate = SimpleDateFormat("MM/dd/yy", Locale.getDefault())
    val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())

    var dateText by remember { mutableStateOf(sdfDate.format(Date(event.timestamp))) }
    var startTimeText by remember { mutableStateOf(sdfTime.format(Date(event.timestamp))) }
    var endTimeText by remember { mutableStateOf(sdfTime.format(Date(event.endTimestamp))) }

    var address by remember { mutableStateOf(event.address) }
    var eventLocation by remember { mutableStateOf(event.location) }
    
    var minPeople by remember { mutableStateOf(event.minParticipants.toString()) }
    var maxPeople by remember { mutableStateOf(event.maxParticipants?.toString() ?: "") }
    var foodOption by remember { mutableStateOf(event.foodOption) }
    var isPrivate by remember { mutableStateOf(!event.isPublic) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var timestamp by remember { mutableLongStateOf(event.timestamp) }
    var endTimestamp by remember { mutableLongStateOf(event.endTimestamp) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedImageUri = uri }

    val datePickerDialog = DatePickerDialog(context, { _, year, month, day ->
        calendar.set(year, month, day)
        dateText = sdfDate.format(calendar.time)
        timestamp = calendar.timeInMillis
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    val startTimePickerDialog = TimePickerDialog(context, { _, h, m ->
        calendar.set(Calendar.HOUR_OF_DAY, h); calendar.set(Calendar.MINUTE, m)
        startTimeText = sdfTime.format(calendar.time); timestamp = calendar.timeInMillis
    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)

    val endTimePickerDialog = TimePickerDialog(context, { _, h, m ->
        endCalendar.set(Calendar.HOUR_OF_DAY, h); endCalendar.set(Calendar.MINUTE, m)
        endTimeText = sdfTime.format(endCalendar.time); endTimestamp = endCalendar.timeInMillis
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
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("MANAGEMENT", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.5.sp)
                        Text("Edit Event", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())
        ) {
            // Hero Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clickable { imagePickerLauncher.launch("image/*") }
            ) {
                AsyncImage(
                    model = selectedImageUri ?: event.imageUrl ?: "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80",
                    contentDescription = "Cover Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))

                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Photo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                // Section: Details
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("EVENT DETAILS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)

                    OutlinedTextField(
                        value = eventName,
                        onValueChange = { eventName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Event Name") },
                        shape = RoundedCornerShape(16.dp)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        label = { Text("Description") },
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Section: Logistics
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("LOGISTICS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)

                    // Category Selector
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Category") },
                            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                            shape = RoundedCornerShape(16.dp),
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, disabledBorderColor = MaterialTheme.colorScheme.outline)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("Meetup", "Dinner", "Party", "Workshop", "Other").forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expanded = false })
                            }
                        }
                    }

                    // Date Picker
                    Box {
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Event Date") },
                            leadingIcon = { Icon(Icons.Default.DateRange, null) },
                            shape = RoundedCornerShape(16.dp),
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { datePickerDialog.show() })
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = startTimeText, onValueChange = {}, label = { Text("Starts") },
                                leadingIcon = { Icon(Icons.Default.AccessTime, null) }, shape = RoundedCornerShape(16.dp),
                                readOnly = true, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline)
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { startTimePickerDialog.show() })
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = endTimeText, onValueChange = {}, label = { Text("Ends") },
                                leadingIcon = { Icon(Icons.Default.AccessTime, null) }, shape = RoundedCornerShape(16.dp),
                                readOnly = true, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledBorderColor = MaterialTheme.colorScheme.outline)
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { endTimePickerDialog.show() })
                        }
                    }
                }

                // Section: Location
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("LOCATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    
                    LocationAutocompleteField(
                        address = address,
                        onAddressChange = { address = it },
                        onLocationSelected = { selectedAddress, location ->
                            address = selectedAddress
                            eventLocation = location
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Section: Capacity
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = minPeople,
                        onValueChange = { if (it.all { char -> char.isDigit() }) minPeople = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Min Guest") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = maxPeople,
                        onValueChange = { if (it.all { char -> char.isDigit() }) maxPeople = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Max Guest") },
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Save Action
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Button(
                        onClick = {
                            if (eventName.isBlank() || address.isBlank()) {
                                Toast.makeText(context, "Missing required fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            val updatedEvent = event.copy(
                                title = eventName, description = description, category = category,
                                address = address, minParticipants = minPeople.toIntOrNull() ?: 0,
                                maxParticipants = maxPeople.toIntOrNull(), foodOption = foodOption,
                                isPublic = !isPrivate, timestamp = timestamp, endTimestamp = endTimestamp,
                                location = eventLocation
                            )
                            viewModel.updateEvent(updatedEvent, selectedImageUri) { success, error ->
                                isLoading = false
                                if (success) { Toast.makeText(context, "Event updated!", Toast.LENGTH_SHORT).show(); onBack() }
                                else { Toast.makeText(context, error ?: "Failed", Toast.LENGTH_SHORT).show() }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
