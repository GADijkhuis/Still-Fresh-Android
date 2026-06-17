package com.stillfresh.activities

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.stillfresh.components.StillFreshIconButton
import com.stillfresh.config.SupabaseConfig
import com.stillfresh.dataclasses.Product
import com.stillfresh.handlers.ProductHandler
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

object InventoryView: ViewModel() {
    @Composable
    fun InventoryView(onBack: () -> Unit) {
        val user = SupabaseConfig.client.auth.currentUserOrNull()
        if (user == null) {
            onBack()
            return
        }
        val userId = user.id

        val teal = Color(0xFF70B9BE)
        val scope = rememberCoroutineScope()

        var products by remember { mutableStateOf<List<Product>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }

        fun refreshProducts() {
            scope.launch {
                isLoading = true
                products = ProductHandler.getProducts(userId)
                isLoading = false
            }
        }

        LaunchedEffect(Unit) {
            refreshProducts()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(20.dp, 100.dp, 20.dp, 70.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = teal
                )
            } else if (products.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your inventory is empty.",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add some products to track them!",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products) { product ->
                        InventoryItem(
                            product = product,
                            onDelete = {
                                scope.launch {
                                    ProductHandler.deleteProduct(product.id!!)
                                    products = products.filter { it.id != product.id }
                                }
                            },
                            onUpdateExpiry = { newDate ->
                                scope.launch {
                                    val updatedProduct = product.copy(expiration_date = newDate)
                                    ProductHandler.updateProduct(updatedProduct)
                                    products = products.map { if (it.id == product.id) updatedProduct else it }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun InventoryItem(
        product: Product,
        onDelete: () -> Unit,
        onUpdateExpiry: (String) -> Unit
    ) {
        val darkText = Color(0xFF2D3436)
        val teal = Color(0xFF70B9BE)
        val context = LocalContext.current

        // Date picker dialog
        val calendar = Calendar.getInstance()
        // Parse current expiration date to set initial picker state if possible
        try {
            val currentExpiry = LocalDate.parse(product.expiration_date)
            calendar.set(currentExpiry.year, currentExpiry.monthValue - 1, currentExpiry.dayOfMonth)
        } catch (e: Exception) {}

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                onUpdateExpiry(selectedDate.toString())
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = darkText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Qty: ${product.quantity}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(16.dp))

                        val isExpired = isExpired(product.expiration_date)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { datePickerDialog.show() }
                        ) {
                            Text(
                                text = if (isExpired) "Expired" else "Expires: ${product.expiration_date}",
                                fontSize = 14.sp,
                                color = if (isExpired) Color(0xFFE57373) else Color.Gray,
                                fontWeight = if (isExpired) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = "Change date",
                                tint = teal.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                StillFreshIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete",
                    onClick = onDelete,
                    containerColor = Color(0xFFE57373).copy(alpha = 0.1f),
                    contentColor = Color(0xFFE57373)
                )
            }
        }
    }

    private fun isExpired(date: String): Boolean {
        return try {
            val expiryDate = LocalDate.parse(date)
            expiryDate.isBefore(LocalDate.now())
        } catch (e: Exception) {
            false
        }
    }

}
