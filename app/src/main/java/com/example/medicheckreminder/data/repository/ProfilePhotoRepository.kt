package com.example.medicheckreminder.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.medicheckreminder.R
import com.example.medicheckreminder.util.ImageScaling
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Profile picture flow (Part 3 blob-storage outcome):
 *  1. shrink the picked photo on-device,
 *  2. upload the JPEG to Firebase Storage at profile_pictures/{uid}.jpg,
 *  3. save the download URL on the user's Firestore document (users/{uid}),
 *  4. cache the URL locally so the avatar still shows when offline.
 */
class ProfilePhotoRepository(
    private val context: Context,
    private val accountStore: AccountStore
) {

    /** Uploads [source]; never throws, failures come back as [PhotoResult.Failure]. */
    suspend fun upload(source: Uri): PhotoResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Upload started for $source")

        val bytes = try {
            shrinkToJpeg(source)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Could not read or shrink the picked image", e)
            return@withContext PhotoResult.Failure(R.string.error_photo_read)
        }
        Log.d(TAG, "Shrunk image to ${bytes.size / 1024} KB")

        try {
            val uid = requireUid()
            val ref = FirebaseStorage.getInstance().reference.child("$STORAGE_FOLDER/$uid.jpg")
            val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
            ref.putBytes(bytes, metadata).awaitResult()
            val url = ref.downloadUrl.awaitResult().toString()
            Log.d(TAG, "Storage upload finished for uid=$uid")

            // The image is already safe in Storage, so a failed Firestore write is logged but
            // does not fail the upload; the next successful upload rewrites the document.
            try {
                FirebaseFirestore.getInstance().collection(USERS_COLLECTION).document(uid)
                    .set(
                        mapOf(
                            FIELD_PHOTO_URL to url,
                            FIELD_PHOTO_UPDATED_AT to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    .awaitResult()
                Log.d(TAG, "Saved photo URL on users/$uid")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Photo is in Storage but the Firestore write failed", e)
            }

            accountStore.setProfilePhoto(url)
            PhotoResult.Success(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: NotSignedInException) {
            Log.e(TAG, "No Firebase user, so the upload was not attempted", e)
            PhotoResult.Failure(R.string.error_photo_sign_in)
        } catch (e: IllegalStateException) {
            // FirebaseApp is not initialised (no google-services.json in this build)
            Log.e(TAG, "Firebase is not configured", e)
            PhotoResult.Failure(R.string.error_photo_backend)
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            PhotoResult.Failure(R.string.error_photo_upload)
        }
    }

    /**
     * Reads users/{uid}.photoUrl from Firestore so a photo uploaded on another device appears here.
     * Returns true when the cached URL changed. Silent on failure (offline, not signed in, no Firebase).
     */
    suspend fun refreshFromCloud(): Boolean = withContext(Dispatchers.IO) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext false
            val snapshot = FirebaseFirestore.getInstance()
                .collection(USERS_COLLECTION).document(uid).get().awaitResult()
            val url = snapshot.getString(FIELD_PHOTO_URL)
            if (!url.isNullOrBlank() && url != accountStore.profilePhotoUrl()) {
                accountStore.setProfilePhoto(url)
                Log.d(TAG, "Picked up a newer profile photo from Firestore")
                true
            } else {
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Profile photo refresh skipped: ${e.message}")
            false
        }
    }

    /**
     * Storage rules need a signed-in Firebase user. Email and Google sign-in both create one, so the
     * photo is stored under the same uid as the users/{uid} record. A session that only exists on
     * this phone (for example a login made while Firebase was unreachable) has no Firebase user, so
     * the person is asked to sign in again rather than being given an anonymous account.
     */
    private fun requireUid(): String =
        FirebaseAuth.getInstance().currentUser?.uid ?: throw NotSignedInException()

    private class NotSignedInException : Exception("No Firebase user is signed in")

    // ---------------------------------------------------------------------------------------
    // Image reading
    // ---------------------------------------------------------------------------------------

    private fun shrinkToJpeg(uri: Uri): ByteArray {
        val flattened = flattenOnWhite(decodeScaled(uri))
        return ByteArrayOutputStream().use { out ->
            flattened.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }

    /** Android 9+ uses ImageDecoder (handles HEIC, rotation, big photos); older phones use BitmapFactory. */
    private fun decodeScaled(uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val (w, h) = ImageScaling.fitWithin(info.size.width, info.size.height, MAX_EDGE_PX)
                    decoder.setTargetSize(w, h)
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (e: Exception) {
                Log.w(TAG, "ImageDecoder failed, falling back to BitmapFactory", e)
            }
        }
        return decodeWithBitmapFactory(uri)
    }

    private fun decodeWithBitmapFactory(uri: Uri): Bitmap {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
        // decodeStream returns null on purpose when inJustDecodeBounds is true; the size lands in `bounds`
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Not a readable image")

        val options = BitmapFactory.Options().apply {
            inSampleSize = ImageScaling.sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE_PX)
        }
        val pixelStream = resolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")
        var bitmap: Bitmap = pixelStream.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Could not decode $uri")

        val (targetW, targetH) = ImageScaling.fitWithin(bitmap.width, bitmap.height, MAX_EDGE_PX)
        if (targetW != bitmap.width || targetH != bitmap.height) {
            bitmap = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        }

        val rotation = readExifRotation(uri)
        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap
    }

    /** A failed EXIF read must never block the upload, so any error just means "no rotation". */
    private fun readExifRotation(uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            when (
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (e: Exception) {
        Log.w(TAG, "Could not read EXIF orientation", e)
        0
    }

    /** JPEG has no transparency, so draw on white (otherwise transparent PNGs turn black). */
    private fun flattenOnWhite(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(result).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        return result
    }

    /** Minimal Task -> suspend bridge so we don't need the play-services coroutines artifact. */
    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }

    private companion object {
        const val TAG = "ProfilePhoto"
        const val STORAGE_FOLDER = "profile_pictures"
        const val USERS_COLLECTION = "users"
        const val FIELD_PHOTO_URL = "photoUrl"
        const val FIELD_PHOTO_UPDATED_AT = "photoUpdatedAt"
        const val MAX_EDGE_PX = 512
        const val JPEG_QUALITY = 85
    }
}

sealed class PhotoResult {
    data class Success(val url: String) : PhotoResult()
    data class Failure(val messageRes: Int) : PhotoResult()
}