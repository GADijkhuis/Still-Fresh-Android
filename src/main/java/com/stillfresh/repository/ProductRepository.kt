package com.stillfresh.repository

import com.stillfresh.config.SupabaseConfig
import com.stillfresh.models.Product
import com.stillfresh.models.ProductInsert
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.format.DateTimeFormatter

object ProductRepository {
    
    private val client = SupabaseConfig.client
    private const val TABLE_NAME = "products"
    
    /**
     * Insert a single product
     */
    suspend fun insertProduct(product: ProductInsert): Result<Product> {
        return try {
            val inserted = client.from(TABLE_NAME)
                .insert(product) {
                    select()
                }
                .decodeSingle<Product>()
            
            Result.success(inserted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Insert multiple products at once (batch insert)
     */
    suspend fun insertProducts(products: List<ProductInsert>): Result<List<Product>> {
        return try {
            val inserted = client.from(TABLE_NAME)
                .insert(products) {
                    select()
                }
                .decodeList<Product>()
            
            Result.success(inserted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all products for the current user
     */
    suspend fun getAllProducts(userId: String): Result<List<Product>> {
        return try {
            val products = client.from(TABLE_NAME)
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<Product>()
            
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get products expiring soon (within X days)
     */
    suspend fun getExpiringSoon(userId: String, withinDays: Int = 3): Result<List<Product>> {
        return try {
            val products = client.from(TABLE_NAME)
                .select {
                    filter {
                        eq("user_id", userId)
                        lte("days_until_expiration", withinDays)
                        gte("days_until_expiration", 0)
                    }
                }
                .decodeList<Product>()
            
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get expired products
     */
    suspend fun getExpiredProducts(userId: String): Result<List<Product>> {
        return try {
            val products = client.from(TABLE_NAME)
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_expired", true)
                    }
                }
                .decodeList<Product>()
            
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get fresh products (not expiring soon)
     */
    suspend fun getFreshProducts(userId: String, moreThanDays: Int = 3): Result<List<Product>> {
        return try {
            val products = client.from(TABLE_NAME)
                .select {
                    filter {
                        eq("user_id", userId)
                        gt("days_until_expiration", moreThanDays)
                    }
                }
                .decodeList<Product>()
            
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update product quantity
     */
    suspend fun updateQuantity(productId: String, newQuantity: Int): Result<Product> {
        return try {
            val updated = client.from(TABLE_NAME)
                .update({
                    set("quantity", newQuantity)
                }) {
                    filter {
                        eq("id", productId)
                    }
                    select()
                }
                .decodeSingle<Product>()
            
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a product
     */
    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            client.from(TABLE_NAME)
                .delete {
                    filter {
                        eq("id", productId)
                    }
                }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete multiple products
     */
    suspend fun deleteProducts(productIds: List<String>): Result<Unit> {
        return try {
            client.from(TABLE_NAME)
                .delete {
                    filter {
                        isIn("id", productIds)
                    }
                }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Helper: Batch convert and insert products with expiration
     * Only saves food items - filters out non-food items
     */
    suspend fun insertProductsWithExpiration(
        products: List<com.stillfresh.utils.ProductWithExpiration>,
        userId: String
    ): Result<List<Product>> {
        // Filter out non-food items
        val foodItems = products.filter { it.isFood }
        
        if (foodItems.isEmpty()) {
            return Result.success(emptyList())
        }
        
        val productInserts = foodItems.map { product ->
            val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
            ProductInsert(
                userId = userId,
                name = product.name,
                quantity = product.quantity,
                purchaseDate = java.time.LocalDate.now().format(dateFormatter),
                expirationDate = product.expirationDate!!.format(dateFormatter)
            )
        }
        return insertProducts(productInserts)
    }
}
