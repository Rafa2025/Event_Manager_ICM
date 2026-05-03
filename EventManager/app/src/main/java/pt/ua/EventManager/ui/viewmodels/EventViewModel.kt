package pt.ua.EventManager.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.ua.EventManager.data.Event
import java.util.UUID

class EventViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    val currentUserUid: String? get() = auth.currentUser?.uid

    init {
        Log.d("EventViewModel", "Initializing EventViewModel. Current user: $currentUserUid")
        fetchEvents()
    }

    private fun fetchEvents() {
        db.collection("events")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("EventViewModel", "Firestore snapshot listener error", e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val fetchedEvents = mutableListOf<Event>()
                    for (doc in snapshot.documents) {
                        try {
                            val event = Event(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                category = doc.getString("category") ?: "Other",
                                address = doc.getString("address") ?: "",
                                timestamp = parseTimestamp(doc.get("timestamp")),
                                imageUrl = doc.getString("imageUrl"),
                                organizerUid = doc.getString("organizerUid") ?: "",
                                organizerName = doc.getString("organizerName") ?: "Anonymous",
                                participantsUids = @Suppress("UNCHECKED_CAST") (doc.get("participantsUids") as? List<String>) ?: emptyList(),
                                minParticipants = doc.getLong("minParticipants")?.toInt() ?: 0,
                                maxParticipants = doc.getLong("maxParticipants")?.toInt(),
                                foodOption = doc.getString("foodOption") ?: "None",
                                isPublic = doc.getBoolean("isPublic") ?: true,
                                location = doc.getGeoPoint("location") ?: GeoPoint(0.0, 0.0)
                            )
                            fetchedEvents.add(event)
                        } catch (ex: Exception) {
                            Log.e("EventViewModel", "Error parsing event document: ${doc.id}", ex)
                        }
                    }
                    _events.value = fetchedEvents
                }
            }
    }

    private fun parseTimestamp(value: Any?): Long {
        return when (value) {
            is Long -> value
            is Double -> value.toLong()
            is Timestamp -> value.toDate().time
            is Map<*, *> -> (value["seconds"] as? Long)?.times(1000) ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
    }
    
    fun createEvent(event: Event, organizerName: String, imageUri: Uri?, onComplete: (Boolean, String?) -> Unit) {
        val uid = currentUserUid
        if (uid == null) {
            onComplete(false, "User not logged in")
            return
        }

        val eventWithHost = event.copy(
            organizerUid = uid,
            organizerName = organizerName
        )

        if (imageUri != null) {
            uploadImage(imageUri) { imageUrl ->
                if (imageUrl != null) {
                    val finalEvent = eventWithHost.copy(imageUrl = imageUrl)
                    saveToFirestore(finalEvent, onComplete)
                } else {
                    onComplete(false, "Image upload failed. Check Firebase Storage rules.")
                }
            }
        } else {
            saveToFirestore(eventWithHost, onComplete)
        }
    }

    private fun saveToFirestore(event: Event, onComplete: (Boolean, String?) -> Unit) {
        db.collection("events").add(event)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.localizedMessage ?: "Firestore error")
                }
            }
    }

    private fun uploadImage(uri: Uri, callback: (String?) -> Unit) {
        val storageRef = storage.reference.child("event_images/${UUID.randomUUID()}")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    callback(downloadUri.toString())
                }.addOnFailureListener { callback(null) }
            }
            .addOnFailureListener { callback(null) }
    }

    fun getHostingEvents(allEvents: List<Event>): List<Event> {
        val uid = currentUserUid ?: return emptyList()
        return allEvents.filter { it.organizerUid == uid }
    }

    fun getAttendingEvents(allEvents: List<Event>): List<Event> {
        val uid = currentUserUid ?: return emptyList()
        return allEvents.filter { it.participantsUids.contains(uid) }
    }

    fun joinEvent(event: Event, onComplete: (Boolean, String?) -> Unit) {
        val uid = currentUserUid
        if (uid == null) {
            onComplete(false, "User not logged in")
            return
        }

        if (event.organizerUid == uid) {
            onComplete(false, "You cannot join your own event as a participant")
            return
        }

        db.collection("events").document(event.id)
            .update("participantsUids", FieldValue.arrayUnion(uid))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.localizedMessage ?: "Error joining event")
                }
            }
    }

    fun leaveEvent(eventId: String, onComplete: (Boolean, String?) -> Unit) {
        val uid = currentUserUid
        if (uid == null) {
            onComplete(false, "User not logged in")
            return
        }

        db.collection("events").document(eventId)
            .update("participantsUids", FieldValue.arrayRemove(uid))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.localizedMessage ?: "Error leaving event")
                }
            }
    }

    fun deleteAllEvents(onComplete: (Boolean) -> Unit) {
        db.collection("events").get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().addOnCompleteListener { onComplete(it.isSuccessful) }
        }.addOnFailureListener { onComplete(false) }
    }
}
