package com.stillfresh.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stillfresh.handlers.CameraFileHandler
import com.stillfresh.theme.StillFreshTheme

class AddProductOptionsActivity : ComponentActivity() {

    private lateinit var photoUri: Uri

    val resultLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (!success) return@registerForActivityResult
        try {
            val intent = Intent(this, ScanReceiptActivity::class.java)
            intent.putExtra("imageURI", photoUri.toString())
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StillFreshTheme {
                AddProductOptionsScreen(
                    onScanReceipt = { dispatchTakePictureIntent() },
                    onManualEntry = { /* TODO: navigate to manual entry */ },
                    onScanBarcode = { /* TODO: navigate to barcode scanner */ },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        try {
            photoUri = CameraFileHandler.getImageUri(applicationContext)
            resultLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductOptionsScreen(
    onScanReceipt: () -> Unit,
    onManualEntry: () -> Unit,
    onScanBarcode: () -> Unit,
    onDismiss: () -> Unit
) {
    val teal = Color(0xFF70B9BE)
    val darkText = Color(0xFF2D3436)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFE0E0E0), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add Product",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = darkText
            )

            Text(
                text = "How would you like to add a product?",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
            )

            // Options row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AddOptionTile(
                    icon = Icons.Outlined.CameraAlt,
                    label = "Scan Receipt",
                    teal = teal,
                    onClick = onScanReceipt
                )
                AddOptionTile(
                    icon = Icons.Outlined.QrCodeScanner,
                    label = "Scan Barcode",
                    teal = teal,
                    onClick = onScanBarcode
                )
                AddOptionTile(
                    icon = Icons.Outlined.Edit,
                    label = "Manual Entry",
                    teal = teal,
                    onClick = onManualEntry
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun AddOptionTile(
    icon: ImageVector,
    label: String,
    teal: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(teal.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = teal,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2D3436),
            textAlign = TextAlign.Center
        )
    }
}
