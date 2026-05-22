package com.stillfresh.activities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.stillfresh.theme.StillFreshTheme

class ScanReceiptActivity : ComponentActivity() {
    lateinit var scannedImage: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bmpByteArray: ByteArray? = intent?.getByteArrayExtra("scannedImage")

        if (bmpByteArray != null) {
            scannedImage = BitmapFactory.decodeByteArray(bmpByteArray, 0, bmpByteArray.count())
        } else {
            Toast.makeText(this@ScanReceiptActivity, "No image found", Toast.LENGTH_LONG).show()
            finish()
        }

        enableEdgeToEdge()
        setContent {
            StillFreshTheme {
                ScanReceiptScreen(
                    scannedImage
                )
            }
        }
    }
}

@Composable
fun ScanReceiptScreen(
    scannedImage: Bitmap
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
            Image(
                bitmap = scannedImage.asImageBitmap(),
                modifier = Modifier.fillMaxSize().fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                contentDescription = null
            )
        }
    }
}