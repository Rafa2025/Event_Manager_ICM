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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.ua.EventManager.data.Event
import pt.ua.EventManager.data.ChatMessage
import pt.ua.EventManager.data.Notification
import java.util.UUID

class EventViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private var notificationsListener: ListenerRegistration? = null

    val currentUserUid: String? get() = auth.currentUser?.uid

    init {
        Log.d("EventViewModel", "Initializing EventViewModel. Current user: $currentUserUid")
        fetchEvents()
        setupNotificationsListener()
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
                            val event = doc.toObject(Event::class.java)?.copy(id = doc.id)
                            if (event != null) {
                                fetchedEvents.add(event)
                            }
                        } catch (ex: Exception) {
                            Log.e("EventViewModel", "Error parsing event document: ${doc.id}", ex)
                        }
                    }
                    _events.value = fetchedEvents
                }
            }
    }

    private fun setupNotificationsListener() {
        val uid = currentUserUid ?: return
        notificationsListener?.remove()
        notificationsListener = db.collection("users").document(uid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("EventViewModel", "Notifications listener error", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Notification::class.java)?.copy(id = doc.id)
                    }
                    _notifications.value = list
                }
            }
    }

    fun getHostingEvents(allEvents: List<Event>): List<Event> {
        val uid = currentUserUid ?: return emptyList()
        return allEvents.filter { it.organizerUid == uid }
    }

    fun getActiveHostingEvents(allEvents: List<Event>): List<Event> {
        val uid = currentUserUid ?: return emptyList()
        val currentTime = System.currentTimeMillis()
        return allEvents.filter { it.organizerUid == uid && it.endTimestamp > currentTime }
    }

    fun getAttendingEvents(allEvents: List<Event>): List<Event> {
        val uid = currentUserUid ?: return emptyList()
        return allEvents.filter { it.participantsUids.contains(uid) }
    }

    fun getActiveAttendingEvents(allEvents: List<Event>): List<Event> {
        val uid = currentUserUid ?: return emptyList()
        val currentTime = System.currentTimeMillis()
        return allEvents.filter { it.participantsUids.contains(uid) && it.endTimestamp > currentTime }
    }

    fun getCompletedEvents(allEvents: List<Event>): List<Event> {
        val uid = currentUserUid ?: return emptyList()
        val currentTime = System.currentTimeMillis()
        return allEvents.filter { 
            (it.organizerUid == uid || it.participantsUids.contains(uid)) && 
            it.endTimestamp <= currentTime 
        }
    }

    fun inviteFriendToEvent(friendUid: String, event: Event, senderName: String) {
        val currentUid = currentUserUid ?: return
        
        // 1. Update Event's invited list
        db.collection("events").document(event.id)
            .update("invitedUids", FieldValue.arrayUnion(friendUid))

        // 2. Send Notification to friend
        val notification = Notification(
            type = "event_invite",
            senderUid = currentUid,
            senderName = senderName,
            eventId = event.id,
            eventTitle = event.title,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        
        db.collection("users").document(friendUid)
            .collection("notifications")
            .add(notification)
    }

    fun markNotificationAsRead(notificationId: String) {
        val uid = currentUserUid ?: return
        if (notificationId.isEmpty()) return
        
        db.collection("users").document(uid)
            .collection("notifications").document(notificationId)
            .update("isRead", true)
            .addOnFailureListener { e ->
                Log.e("EventViewModel", "Error marking notification as read: $notificationId", e)
            }
    }

    fun clearAllNotifications() {
        val uid = currentUserUid ?: return
        db.collection("users").document(uid)
            .collection("notifications")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
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

    fun updateEvent(event: Event, newImageUri: Uri?, onComplete: (Boolean, String?) -> Unit) {
        if (event.id.isEmpty()) {
            onComplete(false, "Invalid event ID")
            return
        }

        if (newImageUri != null) {
            uploadImage(newImageUri) { imageUrl ->
                if (imageUrl != null) {
                    val updatedEvent = event.copy(imageUrl = imageUrl)
                    updateInFirestore(updatedEvent, onComplete)
                } else {
                    onComplete(false, "Image upload failed.")
                }
            }
        } else {
            updateInFirestore(event, onComplete)
        }
    }

    private fun updateInFirestore(event: Event, onComplete: (Boolean, String?) -> Unit) {
        db.collection("events").document(event.id).set(event)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.localizedMessage ?: "Firestore update error")
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

    fun checkInEvent(eventId: String, onComplete: (Boolean, String?) -> Unit) {
        val uid = currentUserUid
        if (uid == null) {
            onComplete(false, "User not logged in")
            return
        }

        db.collection("events").document(eventId)
            .update("checkedInUids", FieldValue.arrayUnion(uid))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.localizedMessage ?: "Error checking in")
                }
            }
    }

    // Chat functionality
    fun sendMessage(eventId: String, messageText: String, senderName: String) {
        val uid = currentUserUid ?: return
        val chatMessage = ChatMessage(
            senderUid = uid,
            senderName = senderName,
            message = messageText
        )
        db.collection("events").document(eventId)
            .collection("chat")
            .add(chatMessage)
    }

    fun getChatMessages(eventId: String): StateFlow<List<ChatMessage>> {
        val messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
        db.collection("events").document(eventId)
            .collection("chat")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    messagesFlow.value = snapshot.toObjects(ChatMessage::class.java)
                }
            }
        return messagesFlow
    }

    override fun onCleared() {
        super.onCleared()
        notificationsListener?.remove()
    }
}
