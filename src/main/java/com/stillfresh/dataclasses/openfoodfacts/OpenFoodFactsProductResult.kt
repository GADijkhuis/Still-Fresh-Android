package com.stillfresh.dataclasses.openfoodfacts

import kotlinx.serialization.Serializable

@Serializable
data class OpenFoodFactsProductResult(
    val code: String,
    val product: OpenFoodFactsProduct
)