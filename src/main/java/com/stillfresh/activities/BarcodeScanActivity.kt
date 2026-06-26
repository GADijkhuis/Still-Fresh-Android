package com.stillfresh.activities

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.stillfresh.config.SupabaseConfig
import com.stillfresh.dataclasses.Product
import com.stillfresh.handlers.NotificationHandler
import com.stillfresh.handlers.OpenFoodFactsHandler
import com.stillfresh.handlers.ProductHandler
import com.stillfresh.theme.StillFreshTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScanActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(this, "Camera permission is required to scan barcodes", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        val user = SupabaseConfig.client.auth.currentUserOrNull()
        if (user == null) {
            finish()
            return
        }
        val userId = user.id

        setContent {
            StillFreshTheme {
                val scope = rememberCoroutineScope()
                BarcodeScanScreen(
                    onConfirm = { name ->
                        scope.launch {
                            try {
                                val productToSave = Product(
                                    user_id = userId,
                                    name = name,
                                    quantity = 1,
                                    purchase_date = LocalDate.now().toString(),
                                    expiration_date = LocalDate.now().plusDays(7).toString()
                                )
                                ProductHandler.addProduct(productToSave)
                                NotificationHandler.scheduleExpirationNotification(this@BarcodeScanActivity, productToSave.name, productToSave.expiration_date)
                                Toast.makeText(this@BarcodeScanActivity, "$name added", Toast.LENGTH_SHORT).show()
                                finish()
                            } catch (e: Exception) {
                                Toast.makeText(this@BarcodeScanActivity, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                                // Allow re-scanning if error
                            }
                        }
                    },
                    onCancel = { finish() },
                    cameraExecutor = cameraExecutor
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

data class BarcodeProduct(
    val name: String,
    val quantity: Int,
    val expirationDate: String
)

@Composable
fun BarcodeScanScreen(
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    cameraExecutor: ExecutorService
) {
    val teal = Color(0xFF70B9BE)
    val context = LocalContext.current

    var scannedValue by remember { mutableStateOf<String?>(null) }
    var hasScanned by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
    ) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val barcodeScanner = BarcodeScanning.getClient()

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (hasScanned) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            barcodes.firstOrNull()?.rawValue?.let { value ->
                                                if (!hasScanned) {
                                                    hasScanned = true
                                                    scannedValue = value
                                                }
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            ctx as androidx.lifecycle.LifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        //ignored
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Dark overlay with scan window cutout effect
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f)
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            // Scan window row
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(220.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                )
                // Clear scan window
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .border(2.dp, teal, RoundedCornerShape(12.dp))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(220.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }

            // Bottom overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f)
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Text(
                text = "Scan Barcode",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Instruction text
        Text(
            text = "Point the camera at a barcode",
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 140.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = teal)
            }
        }

        // Scanned result handling
        scannedValue?.let { value ->
            LaunchedEffect(value) {
                isLoading = true
                val result = OpenFoodFactsHandler.getProductById(value)
                val name = result?.product?.product_name ?: "Unknown Product"
                isLoading = false
                onConfirm(name)
            }
        }
    }
}
