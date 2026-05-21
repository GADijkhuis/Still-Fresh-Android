package com.stillfresh.dataclasses.openfoodfacts

import kotlinx.serialization.Serializable

@Serializable
data class OpenFoodFactsIngredients(
    val text: String? = null,
    val vegan: String? = null,
    val vegetarian: String? = null
)