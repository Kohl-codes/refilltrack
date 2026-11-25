package com.orador.refilltrack.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.orador.refilltrack.model.User
import com.orador.refilltrack.model.Role

// Phase 2: login with Firebase Authentication, role from Cloud Firestore
object AuthRepository {

    private const val USERS = "users"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Step 1: check email + password with Firebase Authentication
    // Step 2: read users/{uid} from Firestore to learn the name and the role
    fun login(
        email: String,
        password: String,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    onError("Login failed. Please try again.")
                } else {
                    loadProfile(uid, onSuccess, onError)
                }
            }
            .addOnFailureListener { e ->
                if (e is FirebaseNetworkException) {
                    onError("No internet connection")
                } else {
                    onError("Invalid email or password")
                }
            }
    }

    private fun loadProfile(
        uid: String,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection(USERS).document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role")
                if (!doc.exists() || role == null) {
                    auth.signOut()
                    onError("This account has no role assigned")
                } else {
                    onSuccess(
                        User(
                            uid = uid,
                            name = doc.getString("name") ?: "",
                            email = doc.getString("email") ?: "",
                            role = role
                        )
                    )
                }
            }
            .addOnFailureListener { e ->
                auth.signOut()
                onError(e.message ?: "Could not load your profile")
            }
    }

    // Used by Order Details to show the rider's name
    fun getUserName(uid: String, onResult: (String) -> Unit) {
        db.collection(USERS).document(uid).get()
            .addOnSuccessListener { doc -> onResult(doc.getString("name") ?: "Unknown rider") }
            .addOnFailureListener { onResult("Unknown rider") }
    }

    fun logout() {
        auth.signOut()
    }

    // READ: all users whose role is "rider" (used by the Assign Rider spinner)
    fun loadRiders(onResult: (List<User>) -> Unit, onError: (String) -> Unit) {
        db.collection(USERS)
            .whereEqualTo("role", Role.RIDER)
            .get()
            .addOnSuccessListener { snapshot ->
                val riders = snapshot.documents.map { doc ->
                    User(
                        uid = doc.id,
                        name = doc.getString("name") ?: "Unnamed rider",
                        email = doc.getString("email") ?: "",
                        role = Role.RIDER
                    )
                }.sortedBy { it.name }
                onResult(riders)
            }
            .addOnFailureListener { e -> onError(e.message ?: "Could not load riders") }
    }



}
