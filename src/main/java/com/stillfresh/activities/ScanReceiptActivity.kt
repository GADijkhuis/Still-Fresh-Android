package com.stillfresh.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import com.stillfresh.components.StillFreshButton
import com.stillfresh.theme.StillFreshTheme

class ScanReceiptActivity : ComponentActivity() {
    private val REQUEST_IMAGE_CAPTURE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StillFreshTheme {
                ScanReceiptScreen(
                    onTakePicture = { dispatchTakePictureIntent() }
                )
            }
        }


    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            ActivityCompat.startActivityForResult(this, takePictureIntent, REQUEST_IMAGE_CAPTURE, null)
        } catch (e: ActivityNotFoundException) {
            Log.e("Camera Exception", e.toString())
        }
    }
}

@Composable
fun ScanReceiptScreen(
    onTakePicture: () -> Unit
) {
    StillFreshButton(
        text = "Scan Receipt",
        onClick = onTakePicture
    )
}