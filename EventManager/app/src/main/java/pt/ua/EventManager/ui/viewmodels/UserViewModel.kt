package pt.ua.EventManager.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.ua.EventManager.data.User

class UserViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // Se não estiver verificado, limpamos logo o estado local
                if (!firebaseUser.isEmailVerified) {
                    _currentUser.value = null
                }
                
                // Forçamos um reload para verificar se o status mudou (clique no link)
                firebaseUser.reload().addOnCompleteListener {
                    val updatedUser = auth.currentUser
                    if (updatedUser != null && updatedUser.isEmailVerified) {
                        fetchUserData(updatedUser.uid)
                    } else {
                        _currentUser.value = null
                    }
                }
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

    fun signUp(name: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseUser = task.result?.user
                
                firebaseUser?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                    val newUser = User(uid = firebaseUser.uid, name = name, email = email)
                    db.collection("users").document(firebaseUser.uid).set(newUser).addOnCompleteListener {
                        // Logout imediato para que ele não entre direto sem verificar
                        auth.signOut()
                        if (emailTask.isSuccessful) {
                            onResult(true, "Conta criada! Verifique o seu email ($email) antes de fazer login.")
                        } else {
                            onResult(true, "Conta criada, mas erro ao enviar email. Tente fazer login para reenviar.")
                        }
                    }
                }
            } else {
                onResult(false, task.exception?.message)
            }
        }
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.reload()?.addOnCompleteListener {
                    val updatedUser = auth.currentUser
                    if (updatedUser != null && updatedUser.isEmailVerified) {
                        fetchUserData(updatedUser.uid)
                        onResult(true, null)
                    } else {
                        auth.signOut() 
                        onResult(false, "Por favor, confirme o seu email no link que enviámos.")
                    }
                }
            } else {
                onResult(false, task.exception?.message)
            }
        }
    }

    fun logout() {
        auth.signOut()
    }
}
