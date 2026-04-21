package pt.ua.EventManager.data

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId 
    val uid: String = "", 
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val registeredEvents: List<String> = emptyList(),
    val createdEvents: List<String> = emptyList()
)
