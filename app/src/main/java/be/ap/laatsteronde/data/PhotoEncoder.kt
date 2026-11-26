package be.ap.laatsteronde.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** The longest side of a stored photo, in pixels (assignment: max. 800 px). */
const val PHOTO_MAX_SIDE = 800

/** JPEG quality of a stored photo (assignment: about 70). */
const val PHOTO_JPEG_QUALITY = 70

/**
 * The longest Base64 text we store, the same limit as in the Firestore rules. A Firestore document can hold
 * at most 1 MiB; a photo of 800 px is usually 100 000 to 200 000 characters, so this leaves room.
 */
const val PHOTO_BASE64_MAX_LENGTH = 500_000

/** A photo the merchant took or chose. The [PhotoEncoder] turns it into Base64 text. */
interface PickedPhoto

/** A photo of the camera (TakePicturePreview gives a small bitmap). */
class CameraPhoto(val bitmap: Bitmap) : PickedPhoto

/** A photo from the gallery (PickVisualMedia gives the address of the file). */
class GalleryPhoto(val uri: Uri) : PickedPhoto

/**
 * Turns a photo into Base64 text for the field photoBase64. It is an interface so a unit test can use a
 * fake: a Bitmap cannot be made in a plain JVM test.
 */
interface PhotoEncoder {
    /** The photo made smaller, as JPEG in Base64, or null when it cannot be read. */
    suspend fun toBase64(photo: PickedPhoto): String?
}

/** The real encoder. All the heavy work (reading, scaling, compressing) runs on Dispatchers.Default. */
class AndroidPhotoEncoder(context: Context) : PhotoEncoder {
    private val contentResolver = context.applicationContext.contentResolver

    override suspend fun toBase64(photo: PickedPhoto): String? = withContext(Dispatchers.Default) {
        val bitmap = when (photo) {
            is CameraPhoto -> photo.bitmap
            is GalleryPhoto -> readFromGallery(photo.uri)
            else -> null
        } ?: return@withContext null

        val (width, height) = scaledSize(bitmap.width, bitmap.height, PHOTO_MAX_SIDE)
        val small = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val bytes = ByteArrayOutputStream()
        small.compress(Bitmap.CompressFormat.JPEG, PHOTO_JPEG_QUALITY, bytes)
        // NO_WRAP: one long line, without the line breaks that Base64 adds by default.
        Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Reads a photo of the gallery. A photo of a phone can be 4000 px or more: reading it whole could run out
     * of memory, so we first read only its size and then read it already a few times smaller.
     */
    private fun readFromGallery(uri: Uri): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, PHOTO_MAX_SIDE)
        }
        val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        bitmap?.let { rotateUpright(it, uri) }
    } catch (e: Exception) {
        null
    }

    /** A phone often stores a photo on its side, with a note (EXIF) on how to turn it. We turn it upright. */
    private fun rotateUpright(bitmap: Bitmap, uri: Uri): Bitmap {
        val orientation = contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

/**
 * Turns the Base64 text of a photo back into a picture, or null when the text is not a photo.
 * Call it on Dispatchers.Default and only once per photo: decoding is heavy.
 *
 * Anyone with a merchant account can write photoBase64 without our app. A small file can hold a picture of
 * for example 30 000 x 30 000 px, which would not fit in the memory of the phone and crash the app of every
 * customer. So we first read only the size and read the picture already smaller, as for the gallery.
 */
fun decodePhoto(base64: String): Bitmap? = try {
    val bytes = Base64.decode(base64, Base64.NO_WRAP)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        null // not a picture
    } else {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, PHOTO_MAX_SIDE)
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
} catch (e: IllegalArgumentException) {
    null // not valid Base64
}

/** The size of a photo with its longest side at most [maxSide], in the same proportions. A small photo stays as it is. */
fun scaledSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
    val longest = max(width, height)
    if (longest <= maxSide) return width to height
    val factor = maxSide.toDouble() / longest
    return max(1, (width * factor).roundToInt()) to max(1, (height * factor).roundToInt())
}

/**
 * How many times smaller Android may read a photo (1, 2, 4, 8 …) so that it is still at least [maxSide]
 * on its longest side. Reading it smaller saves memory; [scaledSize] then makes it exactly the right size.
 */
fun sampleSizeFor(width: Int, height: Int, maxSide: Int): Int {
    var sampleSize = 1
    while (max(width, height) / (sampleSize * 2) >= maxSide) {
        sampleSize *= 2
    }
    return sampleSize
}
