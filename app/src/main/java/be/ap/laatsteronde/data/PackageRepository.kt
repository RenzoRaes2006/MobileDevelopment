package be.ap.laatsteronde.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * What a merchant fills in for a package (Figma "Pakket aanmaken"). Prices are in whole cents,
 * so there are no rounding errors. The pick-up window is on [pickupDate] ("yyyy-MM-dd"),
 * between [pickupFrom] and [pickupTo] ("HH:mm").
 */
data class PackageDetails(
    val title: String,
    val description: String,
    val priceCents: Int,
    val originalPriceCents: Int,
    val quantity: Int,
    val pickupDate: String,
    val pickupFrom: String,
    val pickupTo: String
)

/**
 * A package of a shop. [visible] is the switch "Zichtbaar voor klanten" on Vandaag: when it is off,
 * customers do not see the package ("Offline gehaald").
 */
data class FoodPackage(
    val id: String,
    val ownerId: String,
    val details: PackageDetails,
    val visible: Boolean
)

/**
 * What the ViewModels need from the packages. It is an interface so a unit test can use a fake
 * instead of Firebase. Packages are stored in packages/{id}, with the uid of the merchant as ownerId.
 */
interface PackageRepository {
    /** The packages of the signed-in merchant with a pick-up window on [date] ("yyyy-MM-dd"), oldest first. */
    suspend fun myPackagesOn(date: String): List<FoodPackage>

    /** Creates a package of the signed-in merchant, visible for customers. Throws the Firebase exception when it fails. */
    suspend fun createPackage(details: PackageDetails)

    /** Shows or hides one package of the signed-in merchant for customers. Throws the Firebase exception when it fails. */
    suspend fun setVisible(packageId: String, visible: Boolean)
}

/** All Firebase code for packages lives here. */
class FirebasePackageRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PackageRepository {

    private fun signedInUid(): String =
        auth.currentUser?.uid ?: error("Nobody is signed in")

    override suspend fun myPackagesOn(date: String): List<FoodPackage> {
        val uid = signedInUid()
        val snapshot = firestore.collection(PACKAGES)
            .whereEqualTo("ownerId", uid)
            .whereEqualTo("pickupDate", date)
            .get()
            .await()
        // Sorted here, not by Firestore: ordering on a second field would need an index.
        return snapshot.documents
            .sortedBy { it.getTimestamp("createdAt")?.seconds ?: Long.MAX_VALUE }
            .mapNotNull { it.toFoodPackage() }
    }

    override suspend fun createPackage(details: PackageDetails) {
        val uid = signedInUid()
        val data = mapOf(
            "ownerId" to uid,
            // The shop of a merchant is stored under the uid of the merchant (shops/{uid}).
            "shopId" to uid,
            "title" to details.title,
            "description" to details.description,
            "priceCents" to details.priceCents,
            "originalPriceCents" to details.originalPriceCents,
            "quantity" to details.quantity,
            "pickupDate" to details.pickupDate,
            "pickupFrom" to details.pickupFrom,
            "pickupTo" to details.pickupTo,
            "visible" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )
        // A transaction needs the server: without internet it fails at once. A normal write would stay in
        // Firestore's queue and be saved later, after the merchant was told "no internet" and tried again,
        // which gives the same package twice.
        val reference = firestore.collection(PACKAGES).document()
        firestore.runTransaction { transaction ->
            transaction.set(reference, data)
            null
        }.await()
    }

    override suspend fun setVisible(packageId: String, visible: Boolean) {
        // A transaction, so that without internet it fails at once and does not wait in the queue
        // to switch the package later, after the switch already went back.
        val reference = firestore.collection(PACKAGES).document(packageId)
        firestore.runTransaction { transaction ->
            transaction.update(reference, "visible", visible)
            null
        }.await()
    }

    /** A document that is not complete counts as "no package" instead of crashing the list. */
    private fun DocumentSnapshot.toFoodPackage(): FoodPackage? {
        return FoodPackage(
            id = id,
            ownerId = getString("ownerId") ?: return null,
            details = PackageDetails(
                title = getString("title") ?: return null,
                description = getString("description").orEmpty(),
                priceCents = getLong("priceCents")?.toInt() ?: return null,
                originalPriceCents = getLong("originalPriceCents")?.toInt() ?: return null,
                quantity = getLong("quantity")?.toInt() ?: return null,
                pickupDate = getString("pickupDate") ?: return null,
                pickupFrom = getString("pickupFrom") ?: return null,
                pickupTo = getString("pickupTo") ?: return null
            ),
            visible = getBoolean("visible") ?: false
        )
    }

    private companion object {
        const val PACKAGES = "packages"
    }
}
