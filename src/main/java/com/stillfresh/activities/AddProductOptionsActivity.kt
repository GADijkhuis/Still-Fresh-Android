package com.stillfresh.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stillfresh.components.StillFreshButton
import com.stillfresh.theme.StillFreshTheme
import java.io.ByteArrayOutputStream


class AddProductOptionsActivity : ComponentActivity() {

//    private var photoUri: Uri? = null
//    private var scannedImage: Bitmap? = null

    val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                Log.i("Camera Result Type: ", imageBitmap.height.toString())

                val stream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

                val byteArray = stream.toByteArray()
                imageBitmap.recycle()

                val scanReceiptIntent = Intent(this@AddProductOptionsActivity, ScanReceiptActivity::class.java)
                scanReceiptIntent.putExtra("scannedImage", byteArray)

                startActivity(scanReceiptIntent)
            }
            else -> {
                Log.e("Camera Result Type: ", "NO RESULT: ${result.resultCode}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StillFreshTheme {
                AddProductOptionsActivity(
                    onTakePicture = { dispatchTakePictureIntent() },
                )
            }
        }


    }

    private fun dispatchTakePictureIntent() {

        try {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

//            val photoFile = createImageFile();
//            photoUri = FileProvider.getUriForFile(
//                this,
//                "$packageName.fileprovider", // FileProvider authority (defined in manifest)
//                photoFile
//            );

            resultLauncher.launch(takePictureIntent)
        } catch (e: ActivityNotFoundException) {
            Log.e("Camera Exception", e.toString())
        }
    }
//
//    @Throws(IOException::class)
//    private fun createImageFile(): File {
//        // Create a directory inside cache to store images (if not already created)
//        val imageDir = File(cacheDir, "images")
//        if (!imageDir.exists()) {
//            imageDir.mkdirs()
//        }
//        // Create a temp file with prefix "captured_" and suffix ".jpg"
//        return File.createTempFile("captured_", ".jpg", imageDir)
//    }

}

@Composable
fun AddProductOptionsActivity(
    onTakePicture: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StillFreshButton(
                text = "Scan Receipt",
                onClick = onTakePicture
            )
        }
    }
}