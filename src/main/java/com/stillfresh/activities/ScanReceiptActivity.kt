package com.stillfresh.activities

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.stillfresh.components.StillFreshButton
import com.stillfresh.components.StillFreshTextField
import com.stillfresh.config.SupabaseConfig
import com.stillfresh.dataclasses.Product
import com.stillfresh.handlers.NotificationHandler
import com.stillfresh.handlers.ProductHandler
import com.stillfresh.handlers.ScannedProduct
import com.stillfresh.handlers.VisionHandler
import com.stillfresh.theme.StillFreshTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class ScanReceiptActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bmpUriStr: String? = intent?.getStringExtra("imageURI")
        if (bmpUriStr == null) {
            Toast.makeText(this, "No image found", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val bmpUri = bmpUriStr.toUri()
        val source = ImageDecoder.createSource(contentResolver, bmpUri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = true
        }

        enableEdgeToEdge()
        setContent {
            StillFreshTheme {
                var isScanning by remember { mutableStateOf(true) }
                var products by remember { mutableStateOf<List<ScannedProduct>>(emptyList()) }
                var checkedProducts by remember { mutableStateOf<Set<String>>(emptySet()) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    lifecycleScope.launch {
                        try {
                            val rawText = VisionHandler.recognizeText(bitmap)
                            val parsed = VisionHandler.parseReceiptText(rawText)
                            products = parsed
                            checkedProducts = parsed.map { it.name }.toSet()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Failed to scan receipt"
                        } finally {
                            isScanning = false
                        }
                    }
                }

                val user = SupabaseConfig.client.auth.currentUserOrNull()
                val userId = user?.id ?: ""

                ScanReceiptScreen(
                    bitmap = bitmap,
                    isScanning = isScanning,
                    products = products,
                    checkedProducts = checkedProducts,
                    errorMessage = errorMessage,
                    onToggleProduct = { name ->
                        checkedProducts = if (checkedProducts.contains(name)) {
                            checkedProducts - name
                        } else {
                            checkedProducts + name
                        }
                    },
                    onEditProduct = { old, new ->
                        // Replace old product with edited one, update checked set too
                        products = products.map { if (it.name == old.name) new else it }
                        if (checkedProducts.contains(old.name)) {
                            checkedProducts = checkedProducts - old.name + new.name
                        }
                    },
                    onDeleteProduct = { product ->
                        products = products.filter { it.name != product.name }
                        checkedProducts = checkedProducts - product.name
                    },
                    onAddProduct = { new ->
                        products = products + new
                        checkedProducts = checkedProducts + new.name
                    },
                    onConfirm = {
                        val selected = products.filter { checkedProducts.contains(it.name) }
                        lifecycleScope.launch {
                            try {
                                val productsToSave = selected.map {
                                    Product(
                                        user_id = userId,
                                        name = it.name,
                                        quantity = it.quantity,
                                        purchase_date = LocalDate.now().toString(),
                                        expiration_date = it.expirationDate
                                    )
                                }
                                ProductHandler.addProducts(productsToSave)
                                productsToSave.forEach { product ->
                                    NotificationHandler.scheduleExpirationNotification(this@ScanReceiptActivity, product.name, product.expiration_date)
                                }
                                Toast.makeText(this@ScanReceiptActivity, "${selected.size} products added", Toast.LENGTH_SHORT).show()
                                finish()
                            } catch (e: Exception) {
                                Toast.makeText(this@ScanReceiptActivity, "Error saving products: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@Composable
fun ScanReceiptScreen(
    bitmap: Bitmap,
    isScanning: Boolean,
    products: List<ScannedProduct>,
    checkedProducts: Set<String>,
    errorMessage: String?,
    onToggleProduct: (String) -> Unit,
    onEditProduct: (ScannedProduct, ScannedProduct) -> Unit,
    onDeleteProduct: (ScannedProduct) -> Unit,
    onAddProduct: (ScannedProduct) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val teal = Color(0xFF70B9BE)
    val darkText = Color(0xFF2D3436)

    // Dialog state
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ScannedProduct?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = darkText)
            }
            Text(
                text = "Scan Receipt",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = darkText
            )
            // Add product button
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add product", tint = teal)
            }
        }

        // Scanned image preview
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Receipt",
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentScale = ContentScale.Crop
        )

        // Results area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            when {
                isScanning -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = teal)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Scanning receipt...", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                errorMessage != null -> {
                    Text(
                        text = "Could not scan receipt: $errorMessage",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (products.isEmpty()) "No products found" else "Found ${products.size} products",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkText
                            )
                        }
                    }

                    if (products.isEmpty()) {
                        Text(
                            text = "No food items were detected. Use the + button to add products manually.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    products.forEach { product ->
                        val isChecked = checkedProducts.contains(product.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .background(
                                    if (isChecked) teal.copy(alpha = 0.08f) else Color(0xFFF5F5F5),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onToggleProduct(product.name) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = teal,
                                    uncheckedColor = Color.Gray
                                )
                            )

                            // Product info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = darkText
                                )
                                Row {
                                    if (product.quantity > 1) {
                                        Text(
                                            text = "Qty: ${product.quantity}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Text(
                                        text = "Exp: ${product.expirationDate}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            // Edit button
                            IconButton(
                                onClick = {
                                    editingProduct = product
                                    showEditDialog = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = { onDeleteProduct(product) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFE57373),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Confirm button
        if (!isScanning) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                StillFreshButton(
                    text = if (checkedProducts.isEmpty()) "No products selected" else "Add ${checkedProducts.size} products",
                    onClick = onConfirm,
                    enabled = checkedProducts.isNotEmpty(),
                    containerColor = teal,
                    contentColor = Color.White
                )
            }
        }
    }

    // Edit dialog
    if (showEditDialog && editingProduct != null) {
        ProductEditDialog(
            product = editingProduct!!,
            title = "Edit Product",
            onConfirm = { edited ->
                onEditProduct(editingProduct!!, edited)
                showEditDialog = false
                editingProduct = null
            },
            onDismiss = {
                showEditDialog = false
                editingProduct = null
            }
        )
    }

    // Add dialog
    if (showAddDialog) {
        ProductEditDialog(
            product = ScannedProduct(name = "", quantity = 1),
            title = "Add Product",
            onConfirm = { new ->
                if (new.name.isNotBlank()) onAddProduct(new)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun ProductEditDialog(
    product: ScannedProduct,
    title: String,
    onConfirm: (ScannedProduct) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var quantity by remember { mutableStateOf(product.quantity.toString()) }
    var expirationDate by remember { mutableStateOf(product.expirationDate) }
    val teal = Color(0xFF70B9BE)
    val context = LocalContext.current

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            expirationDate = selectedDate.toString()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3436)
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = teal,
                    unfocusedBorderColor = Color(0xFFCCCCCC),
                    focusedLabelColor = teal,
                    cursorColor = teal
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = quantity,
                onValueChange = { if (it.all { c -> c.isDigit() }) quantity = it },
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = teal,
                    unfocusedBorderColor = Color(0xFFCCCCCC),
                    focusedLabelColor = teal,
                    cursorColor = teal
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = expirationDate,
                onValueChange = { },
                label = { Text("Expiration Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { datePickerDialog.show() },
                shape = RoundedCornerShape(12.dp),
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = teal)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Color(0xFFCCCCCC),
                    disabledLabelColor = teal,
                    disabledTextColor = Color(0xFF2D3436)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Cancel", color = Color.Gray)
                }
                Button(
                    onClick = {
                        onConfirm(ScannedProduct(
                            name = name.trim(),
                            quantity = quantity.toIntOrNull() ?: 1,
                            expirationDate = expirationDate
                        ))
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = teal)
                ) {
                    Text("Save", color = Color.White)
                }
            }
        }
    }
}
