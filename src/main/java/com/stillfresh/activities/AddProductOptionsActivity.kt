package com.stillfresh.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.stillfresh.components.StillFreshButton
import com.stillfresh.handlers.CameraFileHandler
import com.stillfresh.theme.StillFreshTheme
import kotlinx.io.IOException
import java.io.File
import java.util.Calendar


class AddProductOptionsActivity : ComponentActivity() {

    private lateinit var photoUri: Uri

    val resultLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (!success) {
            return@registerForActivityResult
        }

        try {
            val scanReceiptIntent = Intent(this@AddProductOptionsActivity, ScanReceiptActivity::class.java)
            scanReceiptIntent.putExtra("imageURI", photoUri.toString())
            startActivity(scanReceiptIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
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
            photoUri = CameraFileHandler.getImageUri(applicationContext)
            resultLauncher.launch(photoUri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }
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