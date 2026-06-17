package com.stillfresh.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val name: String,
    val quantity: Int = 1,
    @SerialName("purchase_date")
    val purchaseDate: String, // Format: "2024-01-15"
    @SerialName("expiration_date")
    val expirationDate: String, // Format: "2024-01-22"
    @SerialName("is_expired")
    val isExpired: Boolean? = null, // Computed by database
    @SerialName("days_until_expiration")
    val daysUntilExpiration: Int? = null, // Computed by database
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class ProductInsert(
    @SerialName("user_id")
    val userId: String,
    val name: String,
    val quantity: Int = 1,
    @SerialName("purchase_date")
    val purchaseDate: String,
    @SerialName("expiration_date")
    val expirationDate: String
)
