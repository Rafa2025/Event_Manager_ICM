package pt.ua.EventManager.data

import com.google.firebase.firestore.DocumentId

data class Notification(
    @DocumentId 
    val id: String = "",
    val type: String = "", // "friend_request", "event_invite", etc.
    val senderUid: String = "",
    val senderName: String = "",
    val eventId: String? = null,
    val eventTitle: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
