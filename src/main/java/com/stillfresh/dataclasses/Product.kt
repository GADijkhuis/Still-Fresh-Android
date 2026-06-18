package com.stillfresh.dataclasses

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String? = null,
    val user_id: String,
    val name: String,
    val quantity: Int = 1,
    val purchase_date: String,
    val expiration_date: String,
    val created_at: String? = null,
    val updated_at: String? = null
)
