package pt.ua.EventManager.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.firebase.firestore.GeoPoint

@Composable
fun CommonInfoRow(icon: ImageVector, text: String, color: Color = Color.Gray) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = color, fontSize = 15.sp)
    }
}

@Composable
fun CommonAvatar(text: String, color: Color, size: Int = 40) {
    Surface(
        modifier = Modifier.size(size.dp),
        shape = CircleShape,
        color = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontSize = (size/2.5).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LocationAutocompleteField(
    address: String,
    onAddressChange: (String) -> Unit,
    onLocationSelected: (String, GeoPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val placesClient = remember { Places.createClient(context) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var shouldFetchSuggestions by remember { mutableStateOf(true) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = address,
            onValueChange = {
                onAddressChange(it)
                if (shouldFetchSuggestions) {
                    if (it.length >= 2) {
                        val request = FindAutocompletePredictionsRequest.builder()
                            .setSessionToken(sessionToken)
                            .setQuery(it)
                            .build()
                        placesClient.findAutocompletePredictions(request)
                            .addOnSuccessListener { response ->
                                predictions = response.autocompletePredictions
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Places", "Prediction error", exception)
                            }
                    } else {
                        predictions = emptyList()
                    }
                } else {
                    shouldFetchSuggestions = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search location...") },
            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        if (predictions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                predictions.forEach { prediction ->
                    ListItem(
                        headlineContent = {
                            Text(
                                prediction.getFullText(null).toString(),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.clickable {
                            val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
                            val request = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
                            placesClient.fetchPlace(request).addOnSuccessListener { response ->
                                val place = response.place
                                val selectedAddress = place.address ?: place.name ?: ""
                                shouldFetchSuggestions = false
                                onLocationSelected(
                                    selectedAddress,
                                    GeoPoint(place.latLng?.latitude ?: 0.0, place.latLng?.longitude ?: 0.0)
                                )
                                predictions = emptyList()
                            }.addOnFailureListener { exception ->
                                Log.e("Places", "Fetch place error", exception)
                            }
                        }
                    )
                }
            }
        }
    }
}
