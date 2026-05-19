package com.stillfresh.dataclasses

data class OpenFoodFactsProduct(
    val productType: String?,
    val productName: String?,
    val brands: String?,
    val packagings: OpenFoodFactsPackaging?,
    val imageUrl: String?,
    val ingredients: Array<OpenFoodFactsIngredients>?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OpenFoodFactsProduct

        if (productType != other.productType) return false
        if (productName != other.productName) return false
        if (brands != other.brands) return false
        if (packagings != other.packagings) return false
        if (imageUrl != other.imageUrl) return false
        if (!ingredients.contentEquals(other.ingredients)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = productType?.hashCode() ?: 0
        result = 31 * result + (productName?.hashCode() ?: 0)
        result = 31 * result + (brands?.hashCode() ?: 0)
        result = 31 * result + (packagings?.hashCode() ?: 0)
        result = 31 * result + (imageUrl?.hashCode() ?: 0)
        result = 31 * result + (ingredients?.contentHashCode() ?: 0)
        return result
    }
}