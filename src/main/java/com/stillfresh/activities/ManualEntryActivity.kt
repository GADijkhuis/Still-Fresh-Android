package com.stillfresh.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stillfresh.components.StillFreshButton
import com.stillfresh.config.SupabaseConfig
import com.stillfresh.dataclasses.Product
import com.stillfresh.handlers.NotificationHandler
import com.stillfresh.handlers.ProductHandler
import com.stillfresh.theme.StillFreshTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

data class ManualProduct(
    val name: String,
    val quantity: Int = 1,
    val expirationDate: String
)

class ManualEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val user = SupabaseConfig.client.auth.currentUserOrNull()
        if (user == null) {
            finish()
            return
        }
        val userId = user.id

        setContent {
            StillFreshTheme {
                val scope = rememberCoroutineScope()
                ManualEntryScreen(
                    onConfirm = { products ->
                        scope.launch {
                            try {
                                val productsToSave = products.map {
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
                                    NotificationHandler.scheduleExpirationNotification(this@ManualEntryActivity, product.name, product.expiration_date)
                                }
                                Toast.makeText(this@ManualEntryActivity, "${products.size} products added", Toast.LENGTH_SHORT).show()
                                finish()
                            } catch (e: Exception) {
                                Toast.makeText(this@ManualEntryActivity, "Error saving products: ${e.message}", Toast.LENGTH_LONG).show()
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
fun ManualEntryScreen(
    onConfirm: (List<ManualProduct>) -> Unit,
    onCancel: () -> Unit
) {
    val teal = Color(0xFF70B9BE)
    val darkText = Color(0xFF2D3436)
    val context = LocalContext.current

    var productName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var expirationDate by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var products by remember { mutableStateOf<List<ManualProduct>>(emptyList()) }

    // Date picker state
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

                // Expiration Date Field (Clickable)
                OutlinedTextField(
                    value = expirationDate,
                    onValueChange = { },
                    label = { Text("Expires") },
                    modifier = Modifier
                        .weight(1f)
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
                        disabledTextColor = darkText
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (productName.isNotBlank()) {
                        products = products + ManualProduct(
                            name = productName.trim(),
                            quantity = quantity.toIntOrNull() ?: 1,
                            expirationDate = expirationDate
                        )
                        productName = ""
                        quantity = "1"
                        // Keep current expiration date as default for next item
                    }
                },
                enabled = productName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = teal)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add to list")
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
