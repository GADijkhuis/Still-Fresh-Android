package com.stillfresh.handlers

import com.stillfresh.config.SupabaseConfig
import com.stillfresh.dataclasses.Product
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ProductHandler {
    suspend fun getProducts(userId: String): List<Product> = withContext(Dispatchers.IO) {
        SupabaseConfig.client.postgrest["products"].select {
            filter {
                eq("user_id", userId)
            }
        }.decodeList<Product>()
    }

    suspend fun addProduct(product: Product) = withContext(Dispatchers.IO) {
        SupabaseConfig.client.postgrest["products"].insert(product)
    }

    suspend fun addProducts(products: List<Product>) = withContext(Dispatchers.IO) {
        SupabaseConfig.client.postgrest["products"].insert(products)
    }

    suspend fun deleteProduct(id: String) = withContext(Dispatchers.IO) {
        SupabaseConfig.client.postgrest["products"].delete {
            filter {
                eq("id", id)
            }
        }
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        SupabaseConfig.client.postgrest["products"].update(product) {
            filter {
                eq("id", product.id ?: "")
            }
        }
    }
}
