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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.stillfresh.components.ExpiringFoodCard
import com.stillfresh.components.FreshHackCard
import com.stillfresh.components.HomeBottomBar
import com.stillfresh.components.HomeHeader
import com.stillfresh.components.SectionHeader
import com.stillfresh.config.SupabaseConfig
import com.stillfresh.dataclasses.FoodTip
import com.stillfresh.dataclasses.Product
import com.stillfresh.handlers.CameraFileHandler
import com.stillfresh.handlers.NotificationHandler
import com.stillfresh.handlers.ProductHandler
import com.stillfresh.theme.StillFreshTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.foundation.lazy.items
import com.stillfresh.handlers.FoodTipHandler
import kotlin.random.Random
import java.time.LocalDate

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
        
        // Schedule notifications for all products when home starts
        user?.id?.let { userId ->
            lifecycleScope.launch {
                val products = ProductHandler.getProducts(userId)
                products.forEach { product ->
                    NotificationHandler.scheduleExpirationNotification(this@HomeActivity, product.name, product.expiration_date)
                }
            }
        }

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

    val user = SupabaseConfig.client.auth.currentUserOrNull()
    val userId = user?.id ?: ""

    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
    }

    LaunchedEffect(Unit) {
        if (userId.isNotEmpty()) {
            products = ProductHandler.getProducts(userId)
        }
    }

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
                HomeContent(
                    modifier = Modifier.padding(paddingValues),
                    products = products
                )
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

@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    products: List<Product>
) {
    val context = LocalContext.current
    var tips by remember {
        mutableStateOf<List<FoodTip>>(emptyList())
    }

    LaunchedEffect(Unit) {
        tips = FoodTipHandler
            .getTips(context)
            .shuffled(Random(LocalDate.now().dayOfYear))
            .take(5)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Good Morning",
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "StillFresh",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Text(
                text = "Fresh Hacks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tips) { tip ->
                    FreshHackCard(
                        tip = tip
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "Use It or Lose It"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                products
                    .sortedBy { it.expiration_date }
                    .take(3)
                    .forEach { product ->

                        ExpiringFoodCard(
                            product = product
                        )
                    }
            }
        }
    }
}
