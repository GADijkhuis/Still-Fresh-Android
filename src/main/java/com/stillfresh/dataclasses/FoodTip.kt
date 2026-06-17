package com.stillfresh.dataclasses

import kotlinx.serialization.Serializable

@Serializable
data class FoodTip(
    val title: String,
    val category: String
)