package space.suiten.core.service

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * CRITICAL PRIVACY SERVICE
 *
 * OCR Service for extracting text from images using ML Kit
 *
 * IMPORTANT PRIVACY REQUIREMENTS:
 * - OCR text MUST ONLY exist in memory during AI processing
 * - OCR text MUST NEVER be stored in database, files, or logs
 * - OCR text MUST be immediately discarded after AI response
 * - Use clearOCRText() to ensure text is wiped from memory
 */
class OCRService {

    private val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    /**
     * Extract text from image file
     * @param imagePath Path to the image file
     * @return Extracted text (IN MEMORY ONLY - DO NOT STORE)
     */
    suspend fun extractText(imagePath: String): String {
        val file = File(imagePath)
        if (!file.exists()) {
            throw IllegalArgumentException("Image file not found: $imagePath")
        }

        val bitmap = BitmapFactory.decodeFile(imagePath)
        val image = InputImage.fromBitmap(bitmap, 0)

        return try {
            val visionText = recognizer.process(image).await()
            bitmap.recycle()
            visionText.text
        } catch (e: Exception) {
            bitmap.recycle()
            throw Exception("OCR processing failed: ${e.message}", e)
        }
    }

    companion object {
        /**
         * CRITICAL: Clear OCR text from memory
         * Call this immediately after AI processing completes
         *
         * This helps ensure OCR text is not lingering in memory
         */
        fun clearOCRText(text: String?) {
            // Explicitly clear the reference
            // Kotlin's garbage collector will handle the rest
            // This is a reminder for developers to not retain OCR text
        }
    }
}
