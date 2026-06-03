package com.stillfresh.dataclasses.openfoodfacts

import kotlinx.serialization.Serializable

@Serializable
data class OpenFoodFactsPackaging(
    val number_of_units: Int? = null,
    val quantity_per_unit: String? = null,
    val weight: Int? = null
)