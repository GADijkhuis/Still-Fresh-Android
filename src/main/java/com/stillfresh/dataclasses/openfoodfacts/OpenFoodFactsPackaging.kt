package com.stillfresh.dataclasses.openfoodfacts

import kotlinx.serialization.Serializable

@Serializable
data class OpenFoodFactsPackaging(
    val numberOfUnits: Int? = null,
    val quantityPerUnit: String? = null,
    val weight: Int? = null
)