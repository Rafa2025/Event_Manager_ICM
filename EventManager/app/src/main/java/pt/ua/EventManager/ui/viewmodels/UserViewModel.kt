package pt.ua.EventManager.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.ua.EventManager.data.User
import pt.ua.EventManager.data.Notification

class UserViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    val currentUserUid: String?
        get() = auth.currentUser?.uid

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                fetchUserData(firebaseUser.uid)
            } else {
                _currentUser.value = null
            }
        }
    }

    private fun fetchUserData(uid: String) {
        db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                _currentUser.value = snapshot.toObject(User::class.java)
            }
        }
    }

    fun searchUsers(query: String, onResult: (List<User>) -> Unit) {
        if (query.isBlank()) {
            onResult(emptyList())
            return
        }
        db.collection("users")
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .limit(20)
            .get()
            .addOnSuccessListener { snapshots ->
                val users = snapshots.toObjects(User::class.java).filter { it.uid != currentUserUid }
                onResult(users)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun sendFriendRequest(targetUid: String) {
        val currentUid = currentUserUid ?: return
        val senderName = _currentUser.value?.name ?: "Someone"
        
        // 1. Update lists
        db.collection("users").document(currentUid)
            .update("friendRequestsSent", FieldValue.arrayUnion(targetUid))
            
        db.collection("users").document(targetUid)
            .update("friendRequestsReceived", FieldValue.arrayUnion(currentUid))

        // 2. Create Notification
        val notification = Notification(
            type = "friend_request",
            senderUid = currentUid,
            senderName = senderName,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        
        db.collection("users").document(targetUid)
            .collection("notifications")
            .add(notification)
    }

    fun acceptFriendRequest(senderUid: String) {
        val currentUid = currentUserUid ?: return

        // 1. Remove from received and add to friends for current user
        db.collection("users").document(currentUid).update(
            "friendRequestsReceived", FieldValue.arrayRemove(senderUid),
            "friends", FieldValue.arrayUnion(senderUid)
        )

        // 2. Remove from sent and add to friends for sender user
        db.collection("users").document(senderUid).update(
            "friendRequestsSent", FieldValue.arrayRemove(currentUid),
            "friends", FieldValue.arrayUnion(currentUid)
        )
    }

    fun declineFriendRequest(senderUid: String) {
        val currentUid = currentUserUid ?: return

        db.collection("users").document(currentUid)
            .update("friendRequestsReceived", FieldValue.arrayRemove(senderUid))

        db.collection("users").document(senderUid)
            .update("friendRequestsSent", FieldValue.arrayRemove(currentUid))
    }

    fun removeFriend(friendUid: String) {
        val currentUid = currentUserUid ?: return

        // Remove friendUid from current user's friends list
        db.collection("users").document(currentUid)
            .update("friends", FieldValue.arrayRemove(friendUid))

        // Remove currentUid from friend's friends list
        db.collection("users").document(friendUid)
            .update("friends", FieldValue.arrayRemove(currentUid))
    }

    fun getUserName(uid: String, onResult: (String) -> Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener { snapshot ->
            val name = snapshot.getString("name") ?: "Unknown User"
            onResult(name)
        }.addOnFailureListener {
            onResult("Error loading name")
        }
    }

    fun signUp(name: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = task.result?.user?.uid ?: ""
                val newUser = User(uid = uid, name = name, email = email)
                db.collection("users").document(uid).set(newUser).addOnCompleteListener {
                    onResult(true, null)
                }
            } else {
                onResult(false, task.exception?.message)
            }
        }
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onResult(true, null)
            } else {
                onResult(false, task.exception?.message)
            }
        }
    }

    fun logout() {
        auth.signOut()
    }
}
