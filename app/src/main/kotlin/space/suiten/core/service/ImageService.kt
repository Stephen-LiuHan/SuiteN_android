package space.suiten.core.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Service for handling image operations
 * - Image selection from gallery/camera
 * - Image compression
 * - Image storage to app's internal storage
 */
class ImageService(private val context: Context) {

    companion object {
        private const val MAX_IMAGE_WIDTH = 1600
        private const val IMAGE_QUALITY = 75
        private const val IMAGES_DIR = "captures"
    }

    /**
     * Save and compress an image from Uri
     * @return Saved image path
     */
    suspend fun saveImage(imageUri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw IllegalArgumentException("Cannot open image")

        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val compressedBitmap = compressImage(bitmap)
        val fileName = "${UUID.randomUUID()}.jpg"
        val imagesDir = File(context.filesDir, IMAGES_DIR)

        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        val imageFile = File(imagesDir, fileName)
        FileOutputStream(imageFile).use { out ->
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out)
        }

        bitmap.recycle()
        compressedBitmap.recycle()

        return imageFile.absolutePath
    }

    /**
     * Compress image to max width while maintaining aspect ratio
     */
    private fun compressImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= MAX_IMAGE_WIDTH) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth = MAX_IMAGE_WIDTH
        val newHeight = (newWidth / ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Delete image file
     */
    fun deleteImage(imagePath: String) {
        val file = File(imagePath)
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * Load bitmap from file path
     */
    fun loadBitmap(imagePath: String): Bitmap? {
        val file = File(imagePath)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(imagePath)
    }
}
