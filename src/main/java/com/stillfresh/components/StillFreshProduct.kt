package com.stillfresh.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.stillfresh.dataclasses.openfoodfacts.OpenFoodFactsProduct
import io.github.jan.supabase.realtime.Column

@Composable
fun StillFreshProduct(
    product: OpenFoodFactsProduct
) {
    val productName = product.product_name ?: product.product_type ?: "Unknown product"

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (product.image_url != null) {
                AsyncImage(
                    modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .height(60.dp)
                        .width(60.dp),
                    model = product.image_url,
                    contentDescription = productName,
                    contentScale = ContentScale.FillWidth
                )
            }

            Column (
                modifier = Modifier.weight(1f)
            ) {
                Text(productName)

                if (product.brands != null) {
                    Text(
                        text = product.brands,
                        fontSize = 12.sp
                    )
                }
            }

            StillFreshIconButton(
                icon = Icons.Filled.Add,
                contentDescription = productName,
                onClick = { /*Add to inventory*/ }
            )
        }
    }
}