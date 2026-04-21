package pt.ua.EventManager.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.ua.EventManager.data.Event
import pt.ua.EventManager.data.User

class EventViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    val currentUserUid: String? get() = auth.currentUser?.uid

    init {
        fetchEvents()
    }

    private fun fetchEvents() {
        db.collection("events")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _events.value = snapshot.toObjects(Event::class.java)
                }
            }
    }
    
    fun createEvent(event: Event, organizerName: String) {
        val eventWithHost = event.copy(
            organizerUid = currentUserUid ?: "anonymous",
            organizerName = organizerName
        )
        db.collection("events").add(eventWithHost)
    }

    fun getHostingEvents(allEvents: List<Event>): List<Event> {
        return allEvents.filter { it.organizerUid == currentUserUid }
    }

    fun getAttendingEvents(allEvents: List<Event>): List<Event> {
        return allEvents.filter { it.participantsUids.contains(currentUserUid) }
    }
}
