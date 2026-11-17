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

    /** Signs in with e-mail and password. Throws the Firebase exception when it fails. */
    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    /** Sends Firebase's "reset your password" e-mail. Firebase does not say if the address exists. */
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun logout() = auth.signOut()

    /** Firebase remembers the session on the device, so this is also true after a restart. */
    fun isLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Reads the role from users/{uid} of the signed-in user.
     * Returns null when nobody is signed in or the profile document is missing.
     */
    suspend fun currentRole(): UserRole? {
        val uid = auth.currentUser?.uid ?: return null
        val value = firestore.collection("users").document(uid).get().await().getString("role")
        return UserRole.entries.firstOrNull { it.firestoreValue == value }
    }
}
