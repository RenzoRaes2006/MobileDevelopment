package be.ap.laatsteronde.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** The shop of a merchant. [address] is the text shown to customers, [latitude] and [longitude] the pin. */
data class Shop(
    val ownerId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
) {
    val location: Coordinates get() = Coordinates(latitude, longitude)
}

/**
 * What the ViewModels need from the shops. It is an interface so a unit test can use a fake
 * instead of Firebase. A merchant has one shop at most, stored in shops/{uid of the merchant}.
 */
interface ShopRepository {
    /** The shop of the signed-in merchant, or null when they did not create one yet. */
    suspend fun myShop(): Shop?

    /** Creates the shop of the signed-in merchant, or replaces it. Throws the Firebase exception when it fails. */
    suspend fun saveMyShop(name: String, address: String, location: Coordinates)
}

/** All Firebase code for shops lives here. */
class FirebaseShopRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ShopRepository {

    private fun signedInUid(): String =
        auth.currentUser?.uid ?: error("Nobody is signed in")

    override suspend fun myShop(): Shop? {
        val uid = signedInUid()
        val document = firestore.collection(SHOPS).document(uid).get().await()
        if (!document.exists()) return null
        return Shop(
            ownerId = uid,
            name = document.getString("name") ?: return null,
            address = document.getString("address") ?: return null,
            latitude = document.getDouble("latitude") ?: return null,
            longitude = document.getDouble("longitude") ?: return null
        )
    }

    override suspend fun saveMyShop(name: String, address: String, location: Coordinates) {
        val uid = signedInUid()
        val shop = mapOf(
            "ownerId" to uid,
            "name" to name,
            "address" to address,
            "latitude" to location.latitude,
            "longitude" to location.longitude
        )
        firestore.collection(SHOPS).document(uid).set(shop).await()
    }

    private companion object {
        const val SHOPS = "shops"
    }
}
