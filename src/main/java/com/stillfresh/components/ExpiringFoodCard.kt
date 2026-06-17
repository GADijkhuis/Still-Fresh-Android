package com.stillfresh.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stillfresh.dataclasses.Product
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun ExpiringFoodCard(
    product: Product
) {

    val daysLeft = try {
        ChronoUnit.DAYS.between(
            LocalDate.now(),
            LocalDate.parse(product.expiration_date)
        )
    } catch (e: Exception) {
        0
    }

    val expirationText = when {
        daysLeft < 0 -> "Expired"
        daysLeft == 0L -> "Expires today"
        daysLeft == 1L -> "Expires tomorrow"
        else -> "Expires in $daysLeft days"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        Color(0xFF5F9966),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = product.name.first().uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                // Productnaam uit database
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold
                )

                // Aantal
                Text(
                    text = "Quantity: ${product.quantity}",
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Dagen tot vervaldatum
                Text(
                    text = expirationText,
                    color = if (daysLeft < 0)
                        Color.Red
                    else
                        Color(0xFFE57373)
                )
            }

            FilledIconButton(
                onClick = {}
            ) {
                Text("→")
            }
        }
    }
}