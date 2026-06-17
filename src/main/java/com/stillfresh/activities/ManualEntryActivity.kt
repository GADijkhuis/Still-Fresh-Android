package com.stillfresh.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stillfresh.components.StillFreshButton
import com.stillfresh.config.SupabaseConfig
import com.stillfresh.handlers.ScannedProduct
import com.stillfresh.repository.ProductRepository
import com.stillfresh.theme.StillFreshTheme
import com.stillfresh.utils.ExpirationHelper
import com.stillfresh.utils.ProductWithExpiration
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

data class ManualProduct(
    val name: String,
    val quantity: Int = 1
)

class ManualEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StillFreshTheme {
                var isProcessingAI by remember { mutableStateOf(false) }
                var productsWithExpiration by remember { mutableStateOf<List<ProductWithExpiration>?>(null) }
                
                ManualEntryScreen(
                    isProcessingAI = isProcessingAI,
                    productsWithExpiration = productsWithExpiration,
                    onConfirm = { products ->
                        lifecycleScope.launch {
                            try {
                                isProcessingAI = true
                                
                                // Convert ManualProduct to ScannedProduct
                                val scannedProducts = products.map { 
                                    ScannedProduct(it.name, it.quantity) 
                                }
                                
                                // Call AI to get expiration dates
                                val withExpiration = ExpirationHelper.addExpirationDates(scannedProducts)
                                productsWithExpiration = withExpiration
                                
                                // Save to database
                                val user = SupabaseConfig.client.auth.currentUserOrNull()
                                if (user != null) {
                                    val result = ProductRepository.insertProductsWithExpiration(
                                        withExpiration,
                                        user.id
                                    )
                                    
                                    result.fold(
                                        onSuccess = { savedProducts ->
                                            println("✅ Saved ${savedProducts.size} products to database")
                                        },
                                        onFailure = { error ->
                                            println("⚠️ Database save failed: ${error.message}")
                                            error.printStackTrace()
                                        }
                                    )
                                }
                                
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@ManualEntryActivity,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                e.printStackTrace()
                            } finally {
                                isProcessingAI = false
                            }
                        }
                    },
                    onFinish = { finish() },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@Composable
fun ManualEntryScreen(
    isProcessingAI: Boolean,
    productsWithExpiration: List<ProductWithExpiration>?,
    onConfirm: (List<ManualProduct>) -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    val teal = Color(0xFF70B9BE)
    val darkText = Color(0xFF2D3436)

    var productName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var products by remember { mutableStateOf<List<ManualProduct>>(emptyList()) }

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
                text = "Add Products",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = darkText
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        HorizontalDivider(color = Color(0xFFF0F0F0))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            when {
                isProcessingAI -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = teal)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Getting expiration dates from AI...", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
                
                productsWithExpiration != null -> {
                    // Show results with expiration dates
                    val foodItems = productsWithExpiration.filter { it.isFood }
                    val nonFoodItems = productsWithExpiration.filter { !it.isFood }
                    
                    Text(
                        text = "✅ Analysis Complete!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = teal,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (foodItems.isNotEmpty()) {
                        Text(
                            text = "${foodItems.size} food items saved with expiration dates:",
                            fontSize = 14.sp,
                            color = darkText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    
                    if (nonFoodItems.isNotEmpty()) {
                        Text(
                            text = "${nonFoodItems.size} non-food items detected (not saved):",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    
                    productsWithExpiration.forEach { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    !product.isFood -> Color(0xFFF5F5F5)
                                    product.isExpired -> Color(0xFFFFEBEE)
                                    product.isExpiringSoon -> Color(0xFFFFF3E0)
                                    else -> Color(0xFFE8F5E9)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = darkText
                                        )
                                        if (product.quantity > 1) {
                                            Text(
                                                text = "Qty: ${product.quantity}",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    
                                    // Status badge
                                    Surface(
                                        color = when {
                                            !product.isFood -> Color(0xFF9E9E9E)
                                            product.isExpired -> Color(0xFFEF5350)
                                            product.isExpiringSoon -> Color(0xFFFF9800)
                                            else -> Color(0xFF66BB6A)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                !product.isFood -> "NOT FOOD"
                                                product.isExpired -> "EXPIRED"
                                                product.daysUntilExpiration == 0 -> "TODAY"
                                                product.daysUntilExpiration!! <= 3 -> "${product.daysUntilExpiration}d"
                                                else -> "${product.daysUntilExpiration}d"
                                            },
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                
                                if (product.isFood) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "📅 ${product.getFormattedExpirationDate()}",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = product.getExpirationMessage(),
                                        fontSize = 13.sp,
                                        color = when {
                                            product.isExpired -> Color(0xFFD32F2F)
                                            product.isExpiringSoon -> Color(0xFFF57C00)
                                            else -> Color(0xFF388E3C)
                                        },
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "This item was not saved to your pantry",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (nonFoodItems.isNotEmpty()) {
                        Text(
                            text = "💡 Tip: Non-food items are automatically filtered out",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                
                else -> {
                    // Input section
                    Text(
                        text = "Add a product",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = darkText
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) quantity = it },
                            label = { Text("Qty") },
                            modifier = Modifier.width(90.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = teal,
                                unfocusedBorderColor = Color(0xFFCCCCCC),
                                focusedLabelColor = teal,
                                cursorColor = teal
                            )
                        )

                        Button(
                            onClick = {
                                if (productName.isNotBlank()) {
                                    products = products + ManualProduct(
                                        name = productName.trim(),
                                        quantity = quantity.toIntOrNull() ?: 1
                                    )
                                    productName = ""
                                    quantity = "1"
                                }
                            },
                            enabled = productName.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = teal)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add to list")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Product list
                    if (products.isNotEmpty()) {
                        Text(
                            text = "Products to add (${products.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = darkText,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        products.forEach { product ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .background(teal.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = darkText
                                    )
                                    if (product.quantity > 1) {
                                        Text(
                                            text = "Qty: ${product.quantity}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { products = products.filter { it != product } },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Remove",
                                        tint = Color(0xFFE57373),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No products added yet.\nType a name and tap Add to list.",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Confirm button
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            if (productsWithExpiration != null) {
                StillFreshButton(
                    text = "Done",
                    onClick = onFinish,
                    containerColor = teal,
                    contentColor = Color.White
                )
            } else if (!isProcessingAI) {
                StillFreshButton(
                    text = if (products.isEmpty()) "Add products to continue" else "Save ${products.size} products",
                    onClick = { onConfirm(products) },
                    enabled = products.isNotEmpty(),
                    containerColor = teal,
                    contentColor = Color.White
                )
            }
        }
    }
}
