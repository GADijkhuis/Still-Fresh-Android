package com.stillfresh.dataclasses.openfoodfacts

import kotlinx.serialization.Serializable

@Serializable
data class OpenFoodFactsProduct(
    val product_type: String? = null,
    val product_name: String? = null,
    val brands: String? = null,
    val packagings: Array<OpenFoodFactsPackaging>? = null,
    val image_url: String? = null,
    val ingredients: Array<OpenFoodFactsIngredients>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OpenFoodFactsProduct

        if (product_type != other.product_type) return false
        if (product_name != other.product_name) return false
        if (brands != other.brands) return false
        if (!packagings.contentEquals(other.packagings)) return false
        if (image_url != other.image_url) return false
        if (!ingredients.contentEquals(other.ingredients)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = product_type?.hashCode() ?: 0
        result = 31 * result + (product_name?.hashCode() ?: 0)
        result = 31 * result + (brands?.hashCode() ?: 0)
        result = 31 * result + (packagings?.hashCode() ?: 0)
        result = 31 * result + (image_url?.hashCode() ?: 0)
        result = 31 * result + (ingredients?.contentHashCode() ?: 0)
        return result
    }
}