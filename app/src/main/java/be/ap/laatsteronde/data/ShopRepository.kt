package be.ap.laatsteronde.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** The kinds of shop in the dropdown "Categorie" of the Figma. [firestoreValue] is what we store. */
enum class ShopCategory(val firestoreValue: String) {
    BAKER("bakker"),
    SUPERMARKET("supermarkt"),
    RESTAURANT("restaurant")
}

/** What a merchant fills in about the shop. The pin is [location]; the address is in four parts, as in the form. */
data class ShopDetails(
    val name: String,
    val category: ShopCategory,
    val street: String,
    val houseNumber: String,
    val postcode: String,
    val municipality: String,
    val location: Coordinates
)

/** The shop of a merchant: the details and who owns it. */
data class Shop(val ownerId: String, val details: ShopDetails)

/**
 * What the ViewModels need from the shops. It is an interface so a unit test can use a fake
 * instead of Firebase. A merchant has one shop at most, stored in shops/{uid of the merchant}.
 */
interface ShopRepository {
    /** The shop of the signed-in merchant, or null when they did not create one yet. */
    suspend fun myShop(): Shop?

    /** All shops, for the customers: where the packages come from. Incomplete documents are left out. */
    suspend fun allShops(): List<Shop>

    /** Creates the shop of the signed-in merchant, or replaces it. Throws the Firebase exception when it fails. */
    suspend fun saveMyShop(details: ShopDetails)
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
        return document.toShop(ownerId = uid)
    }

    override suspend fun allShops(): List<Shop> =
        firestore.collection(SHOPS).get().await().documents.mapNotNull { it.toShop(ownerId = it.id) }

    /** A document that is not complete (for example from an older version) counts as "no shop". */
    private fun DocumentSnapshot.toShop(ownerId: String): Shop? {
        val category = ShopCategory.entries.firstOrNull { it.firestoreValue == getString("category") }
            ?: return null
        return Shop(
            ownerId = ownerId,
            details = ShopDetails(
                name = getString("name") ?: return null,
                category = category,
                street = getString("street") ?: return null,
                houseNumber = getString("houseNumber").orEmpty(),
                postcode = getString("postcode") ?: return null,
                municipality = getString("municipality") ?: return null,
                location = Coordinates(
                    latitude = getDouble("latitude") ?: return null,
                    longitude = getDouble("longitude") ?: return null
                )
            )
        )
    }

    override suspend fun saveMyShop(details: ShopDetails) {
        val uid = signedInUid()
        val shop = mapOf(
            "ownerId" to uid,
            "name" to details.name,
            "category" to details.category.firestoreValue,
            "street" to details.street,
            "houseNumber" to details.houseNumber,
            "postcode" to details.postcode,
            "municipality" to details.municipality,
            "latitude" to details.location.latitude,
            "longitude" to details.location.longitude
        )
        // A transaction needs the server: without internet it fails at once. A normal write would stay
        // in Firestore's queue and save the shop later, after the merchant was told "no internet".
        val reference = firestore.collection(SHOPS).document(uid)
        firestore.runTransaction { transaction ->
            transaction.set(reference, shop)
            null
        }.await()
    }

    private companion object {
        const val SHOPS = "shops"
    }
}
