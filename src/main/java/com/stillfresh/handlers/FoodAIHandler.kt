package com.stillfresh.handlers

import com.stillfresh.BuildConfig
import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class FoodClassification(
    val name: String,
    val isFood: Boolean,
    val expiryDays: Int
)

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
private data class ChatResponse(
    val choices: List<ChatChoice>
)

@Serializable
private data class ChatChoice(
    val message: ChatMessage
)

object FoodAIHandler {

    private val openRouterKey = BuildConfig.OPENROUTER_API_KEY.trim().removeSurrounding("\"")
    private val geminiKey = BuildConfig.GEMINI_API_KEY.trim().removeSurrounding("\"")

    private val openRouterClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 60000
        }
        defaultRequest {
            header("Authorization", "Bearer $openRouterKey")
            header("HTTP-Referer", "https://stillfresh.app")
            header("X-Title", "StillFresh")
            contentType(ContentType.Application.Json)
        }
    }

    private val geminiClient = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 30000
        }
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun classifyAndEstimateExpiry(items: List<String>): List<FoodClassification> {
        if (items.isEmpty()) return emptyList()

        val prompt = """You are a food expiration expert. I will give you numbered items from a Dutch grocery receipt. For each item respond with ONLY the number, a colon, and either the estimated days until expiration OR "NOT_FOOD".

IMPORTANT:
- Items are in DUTCH (e.g. "KIP" = chicken, "BROOD" = bread, "MELK" = milk, "KAAS" = cheese)
- Base expiry on YOUR knowledge of real food science
- Consider typical home storage (refrigerated for perishables, pantry for dry goods)
- Be specific per item
- Anything that is NOT edible food or drinkable beverage = NOT_FOOD
- Receipt text, store names, discounts, bags, codes, cleaning products, toiletries = NOT_FOOD
- Common Dutch non-food: "statiegeld", "tas", "bon", "korting", "totaal", "pin", "emballage"

Items:
${items.mapIndexed { i, name -> "${i + 1}. $name" }.joinToString("\n")}

Respond ONLY like this (no other text, no explanation):
1: 5
2: NOT_FOOD
3: 365"""

        // Try OpenRouter first (Owl Alpha), then Gemini, then fallback
        val openRouterResult = tryOpenRouter(prompt, items)
        if (openRouterResult != null) return openRouterResult

        val geminiResult = tryGemini(prompt, items)
        if (geminiResult != null) return geminiResult

        return fallbackClassification(items)
    }

    private suspend fun tryOpenRouter(prompt: String, items: List<String>): List<FoodClassification>? {
        val models = listOf(
            "tngtech/deepseek-r1t-chimera:free",
            "google/gemma-4-26b-a4b-it:free"
        )

        for (model in models) {
            try {
                val request = ChatRequest(
                    model = model,
                    messages = listOf(ChatMessage(role = "user", content = prompt))
                )

                val response: HttpResponse = openRouterClient.post("https://openrouter.ai/api/v1/chat/completions") {
                    setBody(request)
                }

                val body = response.bodyAsText()

                if (response.status.value !in 200..299) continue

                val chatResponse = json.decodeFromString<ChatResponse>(body)
                val content = chatResponse.choices.firstOrNull()?.message?.content ?: continue

                return parseAIResponse(content, items)
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private suspend fun tryGemini(prompt: String, items: List<String>): List<FoodClassification>? {
        if (geminiKey.isEmpty()) return null

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", prompt) }
                    }
                }
            }
        }

        return try {
            val response: HttpResponse = geminiClient.post(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$geminiKey"
            ) {
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }

            val body = response.bodyAsText()
            if (response.status.value !in 200..299) return null

            val responseJson = json.parseToJsonElement(body).jsonObject
            val content = responseJson["candidates"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")
                ?.jsonPrimitive?.content ?: return null

            parseAIResponse(content, items)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseAIResponse(response: String, originalItems: List<String>): List<FoodClassification> {
        val results = mutableListOf<FoodClassification>()

        val parsed = mutableMapOf<Int, String>()
        response.lines().forEach { line ->
            val cleaned = line.trim()
                .removePrefix("-")
                .removePrefix("*")
                .removePrefix("*")
                .trim()

            val match = Regex("""^[*\-\s]*(\d+)\s*[).:|\-]\s*(.+)$""").find(cleaned)
            if (match != null) {
                val index = match.groupValues[1].toIntOrNull()
                val value = match.groupValues[2].trim()
                if (index != null) {
                    parsed[index] = value
                }
            }
        }

        originalItems.forEachIndexed { index, originalName ->
            val value = parsed[index + 1]?.uppercase()

            if (value != null) {
                if (value.contains("NOT_FOOD") || value.contains("NOT FOOD") || value.contains("NOTFOOD")) {
                    results.add(FoodClassification(name = originalName, isFood = false, expiryDays = 0))
                } else {
                    val days = Regex("""\d+""").find(value)?.value?.toIntOrNull()
                    if (days != null && days in 1..730) {
                        results.add(FoodClassification(name = originalName, isFood = true, expiryDays = days))
                    } else {
                        if (isNonFood(originalName)) {
                            results.add(FoodClassification(name = originalName, isFood = false, expiryDays = 0))
                        } else {
                            results.add(FoodClassification(name = originalName, isFood = true, expiryDays = getDefaultDays(originalName)))
                        }
                    }
                }
            } else {
                if (isNonFood(originalName)) {
                    results.add(FoodClassification(name = originalName, isFood = false, expiryDays = 0))
                } else {
                    results.add(FoodClassification(name = originalName, isFood = true, expiryDays = getDefaultDays(originalName)))
                }
            }
        }

        return results
    }

    private fun fallbackClassification(items: List<String>): List<FoodClassification> {
        return items.map { name ->
            if (isNonFood(name)) {
                FoodClassification(name = name, isFood = false, expiryDays = 0)
            } else {
                FoodClassification(name = name, isFood = true, expiryDays = getDefaultDays(name))
            }
        }
    }

    private fun isNonFood(name: String): Boolean {
        val lower = name.lowercase()
        val nonFoodKeywords = listOf(
            "opslaan", "betalen", "pinnen", "annuleren", "terug",
            "totaal", "subtotaal", "saldo", "wisselgeld", "korting", "bonus",
            "statiegeld", "emballage", "tas", "tassen", "zak",
            "bon", "kassa", "pin", "contant", "betaling",
            "reiniger", "cleaner", "schoonmaak", "wasmiddel", "bleek",
            "afwasmiddel", "zeep", "soap", "ontsmetting",
            "papier", "tissue", "keukenpapier", "toiletpapier", "wcpapier",
            "shampoo", "conditioner", "tandpasta", "deodorant", "douchegel",
            "batterij", "lamp", "plastic", "folie", "zakjes", "bags",
            "albert heijn", "jumbo", "lidl", "aldi", "plus", "dirk",
            "boodschappen", "week", "euro", "sparen", "punten",
            "kassabon", "transactie", "bedankt", "welkom", "adres",
            "openingstijden", "www", "http", "tel", "kvk", "btw"
        )
        return nonFoodKeywords.any { lower.contains(it) }
    }

    private fun getDefaultDays(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower.contains("kip") || lower.contains("chicken") || lower.contains("filet") -> 3
            lower.contains("gehakt") || lower.contains("mince") -> 2
            lower.contains("vis") || lower.contains("fish") || lower.contains("zalm") || lower.contains("tonijn") -> 2
            lower.contains("worst") || lower.contains("sausage") || lower.contains("bacon") -> 5
            lower.contains("ham") || lower.contains("schnitzel") -> 4
            lower.contains("melk") || lower.contains("milk") -> 7
            lower.contains("yoghurt") || lower.contains("yogurt") || lower.contains("kwark") -> 14
            lower.contains("kaas") || lower.contains("cheese") || lower.contains("mozzarella") -> 30
            lower.contains("boter") || lower.contains("butter") || lower.contains("margarine") -> 30
            lower.contains("room") || lower.contains("cream") -> 7
            lower.contains("ei") || lower.contains("egg") || lower.contains("eieren") -> 21
            lower.contains("brood") || lower.contains("bread") || lower.contains("stokbrood") -> 5
            lower.contains("croissant") || lower.contains("baguette") -> 3
            lower.contains("appel") || lower.contains("apple") -> 14
            lower.contains("banaan") || lower.contains("banana") -> 5
            lower.contains("aardbei") || lower.contains("strawberry") -> 5
            lower.contains("druif") || lower.contains("grape") -> 7
            lower.contains("watermeloen") || lower.contains("meloen") -> 7
            lower.contains("sinaasappel") || lower.contains("orange") -> 14
            lower.contains("citroen") || lower.contains("lemon") -> 21
            lower.contains("avocado") -> 5
            lower.contains("mango") || lower.contains("kiwi") -> 7
            lower.contains("sla") || lower.contains("lettuce") || lower.contains("rucola") -> 5
            lower.contains("tomaat") || lower.contains("tomato") || lower.contains("cherry") -> 7
            lower.contains("komkommer") || lower.contains("cucumber") -> 7
            lower.contains("paprika") || lower.contains("pepper") -> 10
            lower.contains("wortel") || lower.contains("carrot") -> 21
            lower.contains("aardappel") || lower.contains("potato") -> 30
            lower.contains("ui") || lower.contains("onion") -> 30
            lower.contains("champignon") || lower.contains("mushroom") -> 5
            lower.contains("courgette") || lower.contains("broccoli") || lower.contains("bloemkool") -> 7
            lower.contains("spinazie") || lower.contains("prei") -> 7
            lower.contains("mango") || lower.contains("papaya") -> 7
            lower.contains("sap") || lower.contains("juice") -> 7
            lower.contains("cola") || lower.contains("fris") || lower.contains("soda") -> 180
            lower.contains("chips") -> 150
            lower.contains("pasta") || lower.contains("rijst") || lower.contains("rice") || lower.contains("noodle") -> 365
            lower.contains("blik") -> 365
            lower.contains("noten") || lower.contains("nuts") || lower.contains("pinda") -> 210
            lower.contains("chocola") || lower.contains("chocolate") -> 180
            lower.contains("koek") || lower.contains("cookie") -> 90
            lower.contains("saus") || lower.contains("sauce") || lower.contains("ketchup") -> 180
            lower.contains("mayonaise") || lower.contains("mayo") -> 60
            lower.contains("olie") || lower.contains("oil") || lower.contains("azijn") -> 365
            lower.contains("soep") || lower.contains("soup") -> 5
            lower.contains("diepvries") || lower.contains("frozen") -> 180
            else -> 0
        }
    }
}
