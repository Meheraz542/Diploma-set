package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageManager {
    private const val TAG = "ImageStorageManager"
    private const val DIRECTORY_NAME = "marketplace_images"
    private const val AVATARS_DIR = "marketplace_avatars"

    /**
     * Persistently saves an image from a temporary content Uri to app internal storage.
     * Prevents security permission revocation when the app process is restarted.
     */
    suspend fun saveImageLocally(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, DIRECTORY_NAME)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val fileName = "book_${UUID.randomUUID()}.jpg"
            val targetFile = File(dir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                // Decode bounds first to prevent OutOfMemory on huge camera pictures
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val tempBytes = inputStream.readBytes()
                BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, options)

                val sampleSize = calculateInSampleSize(options, reqWidth = 1200, reqHeight = 1200)
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }

                val bitmap = BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.size, decodeOptions)
                    ?: throw IllegalStateException("Failed to decode bitmap from stream")

                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()
            }

            val persistentUri = Uri.fromFile(targetFile).toString()
            Log.d(TAG, "Successfully persisted image to $persistentUri")
            persistentUri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local image, falling back to original uri", e)
            sourceUri.toString()
        }
    }

    /**
     * Persistently saves a user avatar image.
     */
    suspend fun saveAvatarLocally(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, AVATARS_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val fileName = "avatar_${UUID.randomUUID()}.jpg"
            val targetFile = File(dir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                val sampleSize = calculateInSampleSize(options, reqWidth = 512, reqHeight = 512)
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                    ?: throw IllegalStateException("Could not decode avatar")

                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bitmap.recycle()
            }

            Uri.fromFile(targetFile).toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local avatar", e)
            sourceUri.toString()
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
