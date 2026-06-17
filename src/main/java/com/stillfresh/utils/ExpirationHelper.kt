package com.stillfresh.utils

import com.stillfresh.config.OpenRouterConfig
import com.stillfresh.handlers.ScannedProduct
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ProductWithExpiration(
    val name: String,
    val quantity: Int,
    val expirationDate: LocalDate?,
    val daysUntilExpiration: Int?,
    val isFood: Boolean = true
) {
    val isExpiringSoon: Boolean
        get() = isFood && daysUntilExpiration != null && daysUntilExpiration <= 3
    
    val isExpired: Boolean
        get() = isFood && daysUntilExpiration != null && daysUntilExpiration < 0
    
    val expirationStatus: ExpirationStatus
        get() = when {
            !isFood -> ExpirationStatus.NOT_FOOD
            isExpired -> ExpirationStatus.EXPIRED
            daysUntilExpiration == null -> ExpirationStatus.UNKNOWN
            daysUntilExpiration <= 1 -> ExpirationStatus.EXPIRES_TODAY
            daysUntilExpiration <= 3 -> ExpirationStatus.EXPIRES_SOON
            daysUntilExpiration <= 7 -> ExpirationStatus.EXPIRES_THIS_WEEK
            else -> ExpirationStatus.FRESH
        }
    
    fun getFormattedExpirationDate(): String {
        return if (isFood && expirationDate != null) {
            expirationDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        } else {
            "N/A"
        }
    }
    
    fun getExpirationMessage(): String {
        return when {
            !isFood -> "Not a food item"
            daysUntilExpiration == null -> "Unknown expiration"
            isExpired -> "Expired ${-daysUntilExpiration} days ago"
            daysUntilExpiration == 0 -> "Expires today"
            daysUntilExpiration == 1 -> "Expires tomorrow"
            daysUntilExpiration <= 7 -> "Expires in $daysUntilExpiration days"
            else -> "Fresh for $daysUntilExpiration days"
        }
    }
}

enum class ExpirationStatus {
    NOT_FOOD,
    EXPIRED,
    EXPIRES_TODAY,
    EXPIRES_SOON,
    EXPIRES_THIS_WEEK,
    FRESH,
    UNKNOWN
}

object ExpirationHelper {
    
    /**
     * Add expiration dates to a single product using AI
     */
    suspend fun addExpirationDate(
        product: ScannedProduct,
        purchaseDate: LocalDate = LocalDate.now()
    ): ProductWithExpiration {
        val daysUntilExpiration = OpenRouterConfig.getExpirationEstimate(product.name)
            .getOrDefault(7) // Default to 7 days if AI fails
        
        val expirationDate = purchaseDate.plusDays(daysUntilExpiration.toLong())
        val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expirationDate).toInt()
        
        return ProductWithExpiration(
            name = product.name,
            quantity = product.quantity,
            expirationDate = expirationDate,
            daysUntilExpiration = daysRemaining
        )
    }
    
    /**
     * Add expiration dates to multiple products using batch AI request (more efficient)
     */
    suspend fun addExpirationDates(
        products: List<ScannedProduct>,
        purchaseDate: LocalDate = LocalDate.now()
    ): List<ProductWithExpiration> {
        if (products.isEmpty()) return emptyList()
        
        // Get batch estimates from AI
        val productNames = products.map { it.name }
        val estimates = OpenRouterConfig.getBatchExpirationEstimates(productNames)
            .getOrDefault(emptyMap())
        
        return products.map { product ->
            val daysUntilExpiration = estimates[product.name]
            
            if (daysUntilExpiration == null || daysUntilExpiration == -1) {
                // Not food or AI failed
                ProductWithExpiration(
                    name = product.name,
                    quantity = product.quantity,
                    expirationDate = null,
                    daysUntilExpiration = null,
                    isFood = false
                )
            } else {
                // Valid food item
                val expirationDate = purchaseDate.plusDays(daysUntilExpiration.toLong())
                val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expirationDate).toInt()
                
                ProductWithExpiration(
                    name = product.name,
                    quantity = product.quantity,
                    expirationDate = expirationDate,
                    daysUntilExpiration = daysRemaining,
                    isFood = true
                )
            }
        }
    }
    
    /**
     * Sort products by expiration date (soonest first)
     */
    fun sortByExpiration(products: List<ProductWithExpiration>): List<ProductWithExpiration> {
        return products.sortedBy { it.expirationDate }
    }
    
    /**
     * Filter products that are expiring soon (within X days)
     */
    fun getExpiringSoon(products: List<ProductWithExpiration>, withinDays: Int = 3): List<ProductWithExpiration> {
        return products.filter { it.daysUntilExpiration in 0..withinDays }
    }
    
    /**
     * Filter expired products
     */
    fun getExpired(products: List<ProductWithExpiration>): List<ProductWithExpiration> {
        return products.filter { it.isExpired }
    }
    
    /**
     * Get products grouped by expiration status
     */
    fun groupByStatus(products: List<ProductWithExpiration>): Map<ExpirationStatus, List<ProductWithExpiration>> {
        return products.groupBy { it.expirationStatus }
    }
}
