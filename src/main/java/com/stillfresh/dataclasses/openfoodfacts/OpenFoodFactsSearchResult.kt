package com.stillfresh.dataclasses.openfoodfacts

import kotlinx.serialization.Serializable

@Serializable
data class OpenFoodFactsSearchResult(
    val count: Int,
    val products: Array<OpenFoodFactsProduct>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OpenFoodFactsSearchResult

        if (count != other.count) return false
        if (!products.contentEquals(other.products)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = count
        result = 31 * result + products.contentHashCode()
        return result
    }
}
