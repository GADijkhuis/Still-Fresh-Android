package com.stillfresh.components

import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.stillfresh.dataclasses.openfoodfacts.OpenFoodFactsProduct

@Composable
fun StillFreshProduct(
    product: OpenFoodFactsProduct
) {
    Card() {
        Text(product.product_name ?: product.product_type ?: "Unknown product")
    }
}