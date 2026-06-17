package com.stillfresh.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.stillfresh.components.HomeBottomBar
import com.stillfresh.components.HomeHeader
import com.stillfresh.config.SupabaseConfig
import com.stillfresh.handlers.CameraFileHandler
import com.stillfresh.theme.StillFreshTheme
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.json.jsonPrimitive

class HomeActivity : ComponentActivity() {

    private lateinit var photoUri: Uri

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to scan receipts", Toast.LENGTH_LONG).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val intent = Intent(this, ScanReceiptActivity::class.java)
            intent.putExtra("imageURI", photoUri.toString())
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val user = SupabaseConfig.client.auth.currentUserOrNull()
        val username = user?.userMetadata?.get("username")?.jsonPrimitive?.content ?: "User"

        setContent {
            StillFreshTheme {
                HomeScreen(
                    username = username,
                    onScanReceipt = {
                        checkCameraPermissionAndLaunch()
                    },
                    onManualEntry = {
                        startActivity(Intent(this@HomeActivity, ManualEntryActivity::class.java))
                    },
                    onScanBarcode = {
                        startActivity(Intent(this@HomeActivity, BarcodeScanActivity::class.java))
                    },
                    onProfileClick = {
                        startActivity(Intent(this@HomeActivity, AccountActivity::class.java))
                    }
                )
            }
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        photoUri = CameraFileHandler.getImageUri(applicationContext)
        cameraLauncher.launch(photoUri)
    }
}

@Composable
fun HomeScreen(
    username: String,
    onScanReceipt: () -> Unit = {},
    onManualEntry: () -> Unit = {},
    onScanBarcode: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            HomeHeader(
                username = username,
                onProfileClick = onProfileClick
            )
        },
        bottomBar = {
            HomeBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                },
                onAddClick = { showAddSheet = true },
                username = username
            )
        }
    ) { paddingValues ->
        when (selectedTab) {
            1 -> {
                SearchActivity.SearchView()
            }
            2 -> {
                InventoryView.InventoryView(onBack = { selectedTab = 0 })
            }
            3 -> {
                onProfileClick()
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFFF2F2F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Content coming soon",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
        }


    }

    if (showAddSheet) {
        AddProductOptionsScreen(
            onScanReceipt = {
                showAddSheet = false
                onScanReceipt()
            },
            onManualEntry = {
                showAddSheet = false
                onManualEntry()
            },
            onScanBarcode = {
                showAddSheet = false
                onScanBarcode()
            },
            onDismiss = { showAddSheet = false }
        )
    }
}
