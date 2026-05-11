package pt.ua.EventManager.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.ua.EventManager.data.User
import pt.ua.EventManager.data.Notification
import android.util.Log

class UserViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // 3. Tratamento de Estado: Iniciamos com null para representar "carregando" ou "não logado"
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    val currentUserUid: String?
        get() = auth.currentUser?.uid

    init {
        // 2. Otimização do init: Apenas monitora o estado sem reloads agressivos
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null && firebaseUser.isEmailVerified) {
                // Se o usuário já está logado e verificado (ex: abriu o app agora), carregamos os dados
                fetchUserData(firebaseUser.uid)
            } else {
                // Se saiu ou não está verificado, limpamos o estado
                _currentUser.value = null
            }
        }
    }

    // 1 & 3. fetchUserData agora aceita um callback opcional para sincronizar o login
    private fun fetchUserData(uid: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("UserViewModel", "Erro Firestore: ${error.message}")
                _currentUser.value = null
                onComplete?.invoke(false, error.message)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val userObj = snapshot.toObject(User::class.java)
                _currentUser.value = userObj
                onComplete?.invoke(true, null)
            } else {
                Log.w("UserViewModel", "Documento do usuário $uid não existe no Firestore")
                _currentUser.value = null
                onComplete?.invoke(false, "Documento não encontrado no banco de dados.")
            }
        }
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser

                if (user != null && user.isEmailVerified) {
                    // Chamamos o fetchUserData e passamos o erro real se falhar
                    fetchUserData(user.uid) { success, errorMessage ->
                        if (success) {
                            onResult(true, null)
                        } else {
                            // Se o documento não existe, talvez devêssemos criá-lo ou avisar
                            onResult(false, errorMessage ?: "Erro ao carregar perfil.")
                        }
                    }
                } else if (user != null && !user.isEmailVerified) {
                    auth.signOut()
                    onResult(false, "Por favor, verifique o seu email antes de entrar.")
                }
            } else {
                onResult(false, task.exception?.message ?: "Erro na autenticação.")
            }
        }
    }

    fun updateLocation(locationName: String, onResult: (Boolean) -> Unit) {
        val uid = currentUserUid ?: return
        db.collection("users").document(uid)
            .update("location", locationName)
            .addOnSuccessListener {
                // O addSnapshotListener (no init) detetará esta mudança 
                // e atualizará o _currentUser automaticamente.
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e("UserViewModel", "Erro ao atualizar localização: ${e.message}")
                onResult(false)
            }
    }

    // --- Outras funções mantidas para integridade do arquivo ---

    fun signUp(name: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseUser = task.result?.user
                if (firebaseUser != null) {
                    val newUser = User(uid = firebaseUser.uid, name = name, email = email)
                    db.collection("users").document(firebaseUser.uid).set(newUser).addOnCompleteListener {
                        firebaseUser.sendEmailVerification()
                        auth.signOut()
                        onResult(true, "Verifique o seu email para ativar a conta.")
                    }
                }
            } else {
                onResult(false, task.exception?.message)
            }
        }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
    }

    fun getUserName(uid: String, onResult: (String?) -> Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener { snapshot ->
            onResult(snapshot.toObject(User::class.java)?.name)
        }.addOnFailureListener { onResult(null) }
    }

    fun searchUsers(query: String, onResult: (List<User>) -> Unit) {
        if (query.isBlank()) { onResult(emptyList()); return }
        db.collection("users")
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .limit(20).get().addOnSuccessListener { snapshots ->
                val users = snapshots.toObjects(User::class.java).filter { it.uid != currentUserUid }
                onResult(users)
            }.addOnFailureListener { onResult(emptyList()) }
    }

    fun sendFriendRequest(targetUid: String) {
        val currentUid = currentUserUid ?: return
        val senderName = _currentUser.value?.name ?: "Alguém"
        db.collection("users").document(currentUid).update("friendRequestsSent", FieldValue.arrayUnion(targetUid))
        db.collection("users").document(targetUid).update("friendRequestsReceived", FieldValue.arrayUnion(currentUid))
        val notification = Notification(
            "friend_request",
            currentUid,senderName,
            System.currentTimeMillis().toString(), // Converte Long para String
            "false" // Converte Boolean para String
        )
        db.collection("users").document(targetUid).collection("notifications").add(notification)
    }

    fun acceptFriendRequest(senderUid: String) {
        val currentUid = currentUserUid ?: return
        db.collection("users").document(currentUid).update("friendRequestsReceived", FieldValue.arrayRemove(senderUid), "friends", FieldValue.arrayUnion(senderUid))
        db.collection("users").document(senderUid).update("friendRequestsSent", FieldValue.arrayRemove(currentUid), "friends", FieldValue.arrayUnion(currentUid))
    }

    fun declineFriendRequest(senderUid: String) {
        val currentUid = currentUserUid ?: return
        db.collection("users").document(currentUid).update("friendRequestsReceived", FieldValue.arrayRemove(senderUid))
        db.collection("users").document(senderUid).update("friendRequestsSent", FieldValue.arrayRemove(currentUid))
    }

    fun removeFriend(friendUid: String) {
        val currentUid = currentUserUid ?: return
        db.collection("users").document(currentUid).update("friends", FieldValue.arrayRemove(friendUid))
        db.collection("users").document(friendUid).update("friends", FieldValue.arrayRemove(currentUid))
    }
}
