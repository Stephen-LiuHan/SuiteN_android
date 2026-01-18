package space.suiten.core.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.UUID

/**
 * Contract for launching UCrop image cropper
 */
class CropImageContract : ActivityResultContract<Uri, Uri?>() {

    override fun createIntent(context: Context, input: Uri): Intent {
        // Create output file
        val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
        val outputFile = File(outputDir, "cropped_${UUID.randomUUID()}.jpg")
        val outputUri = Uri.fromFile(outputFile)

        return UCrop.of(input, outputUri)
            .withAspectRatio(3f, 4f) // Default aspect ratio for book pages
            .withMaxResultSize(1920, 2560) // Max resolution for good OCR quality
            .withOptions(UCrop.Options().apply {
                setCompressionQuality(90)
                setFreeStyleCropEnabled(true) // Allow free-form cropping
                setShowCropFrame(true)
                setShowCropGrid(true)
                setToolbarTitle("テキスト部分をトリミング")
                setToolbarCancelDrawable(android.R.drawable.ic_menu_close_clear_cancel)
            })
            .getIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode == Activity.RESULT_OK && intent != null) {
            return UCrop.getOutput(intent)
        }
        return null
    }
}
