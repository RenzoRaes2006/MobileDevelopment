package be.ap.laatsteronde.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** The two kinds of users. [firestoreValue] is what we store in users/{uid}.role. */
enum class UserRole(val firestoreValue: String) {
    CUSTOMER("customer"),
    MERCHANT("merchant")
}

/**
 * All Firebase code for user accounts lives here, so ViewModels and screens
 * never talk to Firebase directly.
 */
class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Creates a Firebase Auth account and a profile document users/{uid}
     * with name, email and role. Throws the Firebase exception when something fails.
     */
    suspend fun register(name: String, email: String, password: String, role: UserRole) {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: error("Firebase returned no user after registration")

        val profile = mapOf(
            "name" to name,
            "email" to email,
            "role" to role.firestoreValue
        )
        try {
            firestore.collection("users").document(user.uid).set(profile).await()
        } catch (e: Exception) {
            // Without a profile the account has no role, so remove it again
            // and let the user try once more.
            runCatching { user.delete().await() }
            throw e
        }
    }
}
