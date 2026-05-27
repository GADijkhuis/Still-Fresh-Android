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
import com.stillfresh.theme.StillFreshTheme

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
                ManualEntryScreen(
                    onConfirm = { products ->
                        // TODO: save products to database
                        Toast.makeText(this, "${products.size} products added", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@Composable
fun ManualEntryScreen(
    onConfirm: (List<ManualProduct>) -> Unit,
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

        // Confirm button
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
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
