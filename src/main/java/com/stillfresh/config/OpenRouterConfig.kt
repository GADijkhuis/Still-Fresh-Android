package com.stillfresh.config

import com.stillfresh.BuildConfig
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object OpenRouterConfig {
    
    private const val BASE_URL = "https://openrouter.ai/api/v1"
    private val API_KEY = BuildConfig.OPENROUTER_API_KEY
    
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 60000
        }
        
        defaultRequest {
            header("Authorization", "Bearer $API_KEY")
            header("HTTP-Referer", "https://stillfresh.app")
            header("X-Title", "StillFresh")
            contentType(ContentType.Application.Json)
        }
    }
    
    /**
     * Send a chat completion request to OpenRouter using Google Gemma model
     */
    suspend fun chatCompletion(
        prompt: String,
        model: String = "google/gemma-2-9b-it:free"
    ): Result<String> {
        return try {
            val response: HttpResponse = client.post("$BASE_URL/chat/completions") {
                setBody(ChatCompletionRequest(
                    model = model,
                    messages = listOf(
                        Message(role = "user", content = prompt)
                    )
                ))
            }
            
            val body = response.bodyAsText()
            val jsonResponse = Json.decodeFromString<ChatCompletionResponse>(body)
            
            val content = jsonResponse.choices.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("No response from AI"))
            
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get product expiration estimate
     * Returns the number of days until the product typically expires
     */
    suspend fun getExpirationEstimate(productName: String): Result<Int> {
        val prompt = """
            You are a food safety expert. Analyze this grocery item: "$productName"
            
            Task:
            1. Determine if this is a food/beverage item or not
            2. If NOT food (cleaning product, toiletry, paper good, etc.), respond: NOT_FOOD
            3. If it IS food, provide the realistic shelf life in days from purchase date
            
            Consider:
            - Proper storage conditions (refrigerated if needed)
            - Product is unopened
            - Use your knowledge of actual product shelf life
            - Provide exact days, not rounded to weeks
            
            Item: $productName
            Response (number only, or NOT_FOOD):
        """.trimIndent()
        
        return try {
            val result = chatCompletion(prompt)
            result.fold(
                onSuccess = { response ->
                    println("🤖 AI Response for '$productName': '$response'")
                    
                    val trimmed = response.trim().uppercase()
                    
                    // Check if it's not food
                    if (trimmed.contains("NOT_FOOD") || trimmed.contains("NOT FOOD")) {
                        println("⚠️ '$productName' is not food, skipping")
                        return Result.failure(Exception("Not a food item"))
                    }
                    
                    // Extract number from response
                    val days = response.trim()
                        .split(Regex("\\s+"))
                        .firstNotNullOfOrNull { word ->
                            word.replace(Regex("[^0-9]"), "").toIntOrNull()
                        }
                    
                    if (days != null && days > 0 && days <= 730) { // Max 2 years
                        println("✅ Parsed: $days days")
                        Result.success(days)
                    } else {
                        println("⚠️ Could not parse valid days, using fallback")
                        val fallback = getDefaultExpirationDays(productName)
                        Result.success(fallback)
                    }
                },
                onFailure = { error ->
                    println("❌ AI Error: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get expiration estimates for multiple products at once (more efficient)
     */
    suspend fun getBatchExpirationEstimates(productNames: List<String>): Result<Map<String, Int>> {
        if (productNames.isEmpty()) {
            return Result.success(emptyMap())
        }
        
        val productList = productNames.joinToString("\n") { "- $it" }
        
        val prompt = """
            You are a food safety expert analyzing grocery items from a receipt scan.
            
            $productList
            
            CRITICAL: Many items above are NOT actual products - they are receipt UI elements, buttons, or store text.
            
            For each item, determine:
            1. Is this an ACTUAL food/beverage product that someone would buy?
            2. Or is this receipt text, UI element, store info, or non-food item?
            
            ALWAYS mark as NOT_FOOD:
            - Receipt UI text: "opslaan" (save), "betalen" (pay), "pinnen" (card payment), "annuleren" (cancel)
            - Receipt totals: "totaal", "subtotaal", "saldo", "wisselgeld", "korting"
            - Store names alone: "AH", "Albert Heijn", "Jumbo", "Lidl", "Aldi"
            - Dates, times, or numbers alone
            - Barcodes, codes, or reference numbers
            - Cleaning products: "reiniger", "cleaner", "wasmiddel", "bleek"
            - Toiletries: "shampoo", "zeep", "soap", "tandpasta"
            - Paper goods: "papier", "tissues", "keukenpapier"
            - Any text that is clearly a button, label, or UI element
            
            ONLY mark as food if:
            - It's a recognizable food or beverage product
            - Someone would actually purchase and consume it
            - It has a realistic expiration timeline
            
            For FOOD items:
            - Provide realistic shelf life in days from purchase date
            - Consider proper storage (refrigerated if needed)
            - Assume products are unopened
            - Use exact days based on your knowledge
            
            Format (one per line):
            ItemName: days
            OR
            ItemName: NOT_FOOD
            
            Analyze each item:
        """.trimIndent()
        
        return try {
            val result = chatCompletion(prompt)
            result.fold(
                onSuccess = { response ->
                    println("🤖 AI Batch Response:\n$response")
                    val estimates = mutableMapOf<String, Int>()
                    val skippedNonFood = mutableListOf<String>()
                    
                    response.lines().forEach { line ->
                        val parts = line.split(":", limit = 2)
                        if (parts.size == 2) {
                            val productName = parts[0].trim()
                            val value = parts[1].trim().uppercase()
                            
                            // Check if it's marked as not food
                            if (value.contains("NOT_FOOD") || value.contains("NOT FOOD")) {
                                val matchedProduct = productNames.find { original ->
                                    original.equals(productName, ignoreCase = true) ||
                                    productName.contains(original, ignoreCase = true) ||
                                    original.contains(productName, ignoreCase = true)
                                }
                                if (matchedProduct != null) {
                                    skippedNonFood.add(matchedProduct)
                                    println("⚠️ Skipping non-food item: '$matchedProduct'")
                                }
                            } else {
                                // Try to parse days
                                val days = value.replace(Regex("[^0-9]"), "").toIntOrNull()
                                
                                if (days != null && days > 0 && days <= 730) {
                                    val matchedProduct = productNames.find { original ->
                                        original.equals(productName, ignoreCase = true) ||
                                        productName.contains(original, ignoreCase = true) ||
                                        original.contains(productName, ignoreCase = true)
                                    }
                                    if (matchedProduct != null) {
                                        estimates[matchedProduct] = days
                                        println("✅ Matched '$matchedProduct': $days days")
                                    }
                                }
                            }
                        }
                    }
                    
                    // Fill in missing FOOD products with smart defaults (skip non-food)
                    productNames.forEach { product ->
                        if (!estimates.containsKey(product) && !skippedNonFood.contains(product)) {
                            val fallback = getDefaultExpirationDays(product)
                            if (fallback == -1) {
                                // It's a non-food item according to fallback
                                skippedNonFood.add(product)
                                println("⚠️ Skipping non-food item (fallback): '$product'")
                            } else {
                                estimates[product] = fallback
                                println("⚠️ Using fallback for '$product': $fallback days")
                            }
                        }
                    }
                    
                    Result.success(estimates)
                },
                onFailure = { error ->
                    println("❌ AI Batch Error: ${error.message}")
                    // Return defaults for all products
                    val defaults = productNames.associateWith { getDefaultExpirationDays(it) }
                    Result.success(defaults)
                }
            )
        } catch (e: Exception) {
            println("❌ Batch Exception: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get default expiration days based on product category (fallback)
     */
    private fun getDefaultExpirationDays(productName: String): Int {
        val name = productName.lowercase()
        
        // First check if it's NOT food (cleaning products, toiletries, etc.)
        if (isNonFoodItem(name)) {
            return -1 // Special marker for non-food
        }
        
        return when {
            // Packaged snacks (long shelf life)
            name.contains("chips") || name.contains("chip") -> 75
            name.contains("crackers") || name.contains("cracker") -> 90
            name.contains("cookies") || name.contains("cookie") || name.contains("koek") -> 60
            name.contains("biscuit") -> 60
            name.contains("chocolate") || name.contains("chocolade") -> 180
            name.contains("candy") || name.contains("snoep") -> 180
            
            // Nuts (very long shelf life)
            name.contains("nuts") || name.contains("noten") || name.contains("noot") -> 210
            name.contains("peanut") || name.contains("pinda") -> 210
            name.contains("cashew") || name.contains("almond") || name.contains("amandel") -> 210
            name.contains("borrel") -> 210 // Borrelnoten (cocktail nuts)
            
            // Canned goods (very long)
            name.contains("canned") || name.contains("blik") -> 365
            name.contains("soup") && name.contains("can") -> 365
            
            // Dairy (short shelf life)
            name.contains("milk") || name.contains("melk") -> 7
            name.contains("yogurt") || name.contains("yoghurt") -> 14
            name.contains("cream") || name.contains("room") -> 7
            name.contains("butter") || name.contains("boter") -> 30
            
            // Meat & Fish (very short)
            name.contains("chicken") || name.contains("kip") -> 2
            name.contains("beef") || name.contains("rund") -> 3
            name.contains("pork") || name.contains("varken") -> 3
            name.contains("fish") || name.contains("vis") -> 2
            name.contains("salmon") || name.contains("zalm") -> 2
            name.contains("meat") || name.contains("vlees") -> 3
            name.contains("mince") || name.contains("gehakt") -> 2
            
            // Produce (varies)
            name.contains("lettuce") || name.contains("sla") -> 5
            name.contains("tomato") || name.contains("tomaat") -> 7
            name.contains("banana") || name.contains("banaan") -> 5
            name.contains("apple") || name.contains("appel") -> 14
            name.contains("carrot") || name.contains("wortel") -> 21
            name.contains("potato") || name.contains("aardappel") -> 30
            name.contains("onion") || name.contains("ui") -> 30
            name.contains("pepper") || name.contains("paprika") && !name.contains("chips") -> 10
            
            // Bread & Bakery
            name.contains("bread") || name.contains("brood") || name.contains("worst") -> 5
            name.contains("croissant") -> 2
            name.contains("baguette") -> 2
            
            // Eggs
            name.contains("egg") || name.contains("ei") -> 21
            
            // Cheese (longer)
            name.contains("cheese") || name.contains("kaas") || name.contains("feta") -> 30
            
            // Pasta & Rice (dry, very long)
            name.contains("pasta") || name.contains("spaghetti") -> 365
            name.contains("rice") || name.contains("rijst") -> 365
            name.contains("noodle") -> 365
            
            // Condiments & Sauces (long)
            name.contains("sauce") || name.contains("saus") -> 180
            name.contains("ketchup") || name.contains("mustard") || name.contains("mosterd") -> 180
            name.contains("mayonnaise") || name.contains("mayo") -> 60
            name.contains("pesto") || name.contains("rasp") -> 180
            
            // Legumes
            name.contains("kikkererwten") || name.contains("chickpea") -> 365
            name.contains("bonen") || name.contains("beans") -> 365
            
            // Default
            else -> 14
        }
    }
    
    /**
     * Check if an item is not food
     */
    private fun isNonFoodItem(name: String): Boolean {
        val nonFoodKeywords = listOf(
            // Receipt UI elements (Dutch & English)
            "opslaan", "save", "betalen", "pay", "pinnen", "card",
            "annuleren", "cancel", "terug", "back",
            "totaal", "total", "subtotaal", "subtotal",
            "saldo", "balance", "wisselgeld", "change",
            "korting", "discount", "bonus",
            
            // Store names (when alone)
            "albert heijn", "jumbo", "lidl", "aldi", "plus",
            
            // Cleaning products (Dutch & English)
            "reiniger", "cleaner", "schoonmaak", "cleaning",
            "wasmiddel", "detergent", "bleek", "bleach",
            "afwasmiddel", "dish soap", "zeep", "soap",
            "ontsmetting", "disinfectant",
            
            // Paper products
            "papier", "paper", "tissue", "tissues",
            "keukenpapier", "paper towel", "toiletpapier", "toilet paper",
            
            // Toiletries
            "shampoo", "conditioner", "tandpasta", "toothpaste",
            "deodorant", "parfum", "perfume",
            
            // Other non-food
            "batterij", "battery", "lamp", "bulb",
            "plastic", "folie", "foil", "zakjes", "bags"
        )
        
        return nonFoodKeywords.any { keyword -> name.contains(keyword) }
    }
}

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message
)
