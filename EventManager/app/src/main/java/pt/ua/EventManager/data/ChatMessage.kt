package pt.ua.EventManager.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ChatMessage(
    val senderUid: String = "",
    val senderName: String = "",
    val message: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)
