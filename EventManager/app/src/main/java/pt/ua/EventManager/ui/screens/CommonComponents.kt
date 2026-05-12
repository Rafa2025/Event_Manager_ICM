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
import androidx.compose.ui.window.PopupProperties
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.delay

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
    
    var predictions by remember { mutableStateOf(listOf<AutocompletePrediction>()) }
    var expanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Implementação de Debounce e Busca via Side-Effect (Architect Pattern)
    LaunchedEffect(address) {
        if (address.length >= 3) {
            delay(500) // Debounce de 500ms
            
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setQuery(address)
                .setCountries("PT") // Otimização regional (Portugal)
                .build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    predictions = response.autocompletePredictions
                    expanded = predictions.isNotEmpty()
                    errorMsg = null
                }
                .addOnFailureListener { exception ->
                    if (exception is ApiException) {
                        val statusCode = exception.statusCode
                        Log.e("Places", "Status Code: $statusCode - ${exception.statusMessage}")
                        
                        // Mapeamento detalhado para facilitar a depuração no telemóvel
                        errorMsg = when(statusCode) {
                            9011 -> "Erro 9011: SHA-1 ou Package Name não autorizado."
                            9010 -> "Erro 9010: API Key inválida ou restrita."
                            else -> "Erro Google ($statusCode): ${exception.statusMessage}"
                        }
                    } else {
                        Log.e("Places", "Prediction error: ${exception.message}")
                        errorMsg = "Erro: ${exception.message}"
                    }
                    expanded = false
                    predictions = emptyList()
                }
        } else {
            predictions = emptyList()
            expanded = false
            if (address.isEmpty()) errorMsg = null
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = address,
            onValueChange = { onAddressChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search location...") },
            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
            isError = errorMsg != null,
            supportingText = {
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f),
            properties = PopupProperties(focusable = false)
        ) {
            predictions.forEach { prediction ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = prediction.getFullText(null).toString(),
                            fontSize = 14.sp,
                            maxLines = 2
                        ) 
                    },
                    onClick = {
                        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
                        val fetchPlaceRequest = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)

                        placesClient.fetchPlace(fetchPlaceRequest)
                            .addOnSuccessListener { fetchResponse ->
                                val place = fetchResponse.place
                                val latLng = place.latLng
                                if (latLng != null) {
                                    onLocationSelected(
                                        place.address ?: place.name ?: prediction.getFullText(null).toString(),
                                        GeoPoint(latLng.latitude, latLng.longitude)
                                    )
                                }
                                expanded = false
                            }
                            .addOnFailureListener { e ->
                                Log.e("Places", "Fetch Place Error: ${e.message}")
                                expanded = false
                            }
                    }
                )
            }
        }
    }
}
