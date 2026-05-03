package pt.ua.EventManager.data

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Event(
    @DocumentId 
    val id: String = "", 
    val title: String = "",
    val description: String = "",
    val category: String = "Other",
    val location: GeoPoint = GeoPoint(0.0, 0.0), 
    val address: String = "", 
    val timestamp: Long = System.currentTimeMillis(), 
    val imageUrl: String? = null, 
    val organizerUid: String = "", 
    val organizerName: String = "Anonymous",
    val participantsUids: List<String> = emptyList(), 
    val minParticipants: Int = 0,
    val maxParticipants: Int? = null, 
    val foodOption: String = "None",
    
    @get:PropertyName("isPublic")
    @set:PropertyName("isPublic")
    var isPublic: Boolean = true
)
