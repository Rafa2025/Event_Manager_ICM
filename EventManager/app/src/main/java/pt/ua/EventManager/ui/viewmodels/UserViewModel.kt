package pt.ua.EventManager.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.ua.EventManager.data.User
import pt.ua.EventManager.data.Notification

class UserViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified

    private var userSnapshotListener: ListenerRegistration? = null

    val currentUserUid: String?
        get() = auth.currentUser?.uid

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                _isEmailVerified.value = firebaseUser.isEmailVerified
                fetchUserData(firebaseUser.uid)
            } else {
                userSnapshotListener?.remove()
                _currentUser.value = null
                _isEmailVerified.value = false
            }
        }
    }

    private fun fetchUserData(uid: String) {
        // Remove existing listener before creating a new one
        userSnapshotListener?.remove()
        
        userSnapshotListener = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("UserViewModel", "Error fetching user data", error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                // Double check that we are still listening to the same UID to prevent race conditions
                if (auth.currentUser?.uid == uid) {
                    _currentUser.value = user
                }
            }
        }
    }

    fun reloadUser(onComplete: (Boolean) -> Unit = {}) {
        auth.currentUser?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _isEmailVerified.value = auth.currentUser?.isEmailVerified == true
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun sendVerificationEmail(onComplete: (Boolean, String?) -> Unit) {
        auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(true, null)
            } else {
                onComplete(false, task.exception?.message)
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
        
        // 1. Update current user's document
        db.collection("users").document(currentUid)
            .update("friendRequestsSent", FieldValue.arrayUnion(targetUid))
            
        // 2. Update target user's document
        db.collection("users").document(targetUid)
            .update("friendRequestsReceived", FieldValue.arrayUnion(currentUid))

        // 3. Create Notification for target user
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

        // 1. Update current user: Remove from received, add to friends
        db.collection("users").document(currentUid).update(
            "friendRequestsReceived", FieldValue.arrayRemove(senderUid),
            "friends", FieldValue.arrayUnion(senderUid)
        )

        // 2. Update sender user: Remove from sent, add to friends
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
                val user = task.result?.user
                val uid = user?.uid ?: ""
                val newUser = User(uid = uid, name = name, email = email)
                db.collection("users").document(uid).set(newUser).addOnCompleteListener {
                    user?.sendEmailVerification()
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
                _isEmailVerified.value = auth.currentUser?.isEmailVerified == true
                onResult(true, null)
            } else {
                onResult(false, task.exception?.message)
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    override fun onCleared() {
        super.onCleared()
        userSnapshotListener?.remove()
    }
}
