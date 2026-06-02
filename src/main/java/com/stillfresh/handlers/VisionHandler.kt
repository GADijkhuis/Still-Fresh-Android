package com.stillfresh.handlers

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class ScannedProduct(
    val name: String,
    val quantity: Int = 1
)

object VisionHandler {

    // Common grocery food keywords — used to validate scanned lines are likely food
    private val foodKeywords = setOf(
        // Fruit
        "apple", "appel", "banana", "banaan", "orange", "sinaasappel", "lemon", "citroen",
        "lime", "grape", "druif", "strawberry", "aardbei", "blueberry", "bosbes",
        "raspberry", "framboos", "mango", "pineapple", "ananas", "watermelon", "meloen",
        "peach", "perzik", "pear", "peer", "cherry", "kers", "kiwi", "avocado",
        "tomato", "tomaat", "tomaten",
        // Vegetables
        "carrot", "wortel", "broccoli", "spinach", "spinazie", "lettuce", "sla",
        "cucumber", "komkommer", "pepper", "paprika", "onion", "ui", "garlic", "knoflook",
        "potato", "aardappel", "aardappelen", "sweet potato", "zoete aardappel",
        "mushroom", "champignon", "zucchini", "courgette", "eggplant", "aubergine",
        "cauliflower", "bloemkool", "cabbage", "kool", "celery", "selderij",
        "asparagus", "asperge", "leek", "prei", "radish", "radijs", "beetroot", "biet",
        // Meat & Fish
        "chicken", "kip", "beef", "rundvlees", "rund", "pork", "varken", "varkensvlees",
        "lamb", "lam", "turkey", "kalkoen", "salmon", "zalm", "tuna", "tonijn",
        "cod", "kabeljauw", "shrimp", "garnaal", "mince", "gehakt", "steak",
        "sausage", "worst", "bacon", "ham", "filet", "fillet", "schnitzel",
        // Dairy
        "milk", "melk", "cheese", "kaas", "butter", "boter", "yogurt", "yoghurt",
        "cream", "room", "egg", "ei", "eieren", "eggs", "quark", "kwark",
        "mozzarella", "cheddar", "gouda", "brie", "feta",
        // Bread & Bakery
        "bread", "brood", "baguette", "croissant", "roll", "bol", "bun",
        "toast", "wrap", "pita", "tortilla", "cracker", "beschuit",
        // Grains & Pasta
        "rice", "rijst", "pasta", "spaghetti", "noodle", "noodles", "macaroni",
        "flour", "meel", "oats", "haver", "granola", "muesli", "cereal",
        "quinoa", "couscous", "bulgur",
        // Canned & Packaged
        "soup", "soep", "beans", "bonen", "lentils", "linzen", "chickpeas", "kikkererwten",
        "corn", "mais", "peas", "erwten", "sauce", "saus", "salsa", "pesto",
        "jam", "honey", "honing", "peanut butter", "pindakaas", "nutella",
        // Drinks
        "juice", "sap", "water", "coffee", "koffie", "tea", "thee",
        "cola", "soda", "limonade", "smoothie",
        // Snacks & Other
        "chips", "chocolate", "chocolade", "cookie", "koek", "biscuit",
        "nuts", "noten", "almonds", "amandelen", "cashew", "walnut", "walnoot",
        "oil", "olie", "vinegar", "azijn", "sugar", "suiker", "salt", "zout",
        "pepper", "peper", "spice", "kruid", "herb", "mayonnaise", "mustard", "mosterd",
        "ketchup", "hummus", "tzatziki",
        // Frozen
        "frozen", "diepvries", "ice cream", "ijsje", "sorbet",
    )

    // Lines that are definitely not food
    private val nonFoodPatterns = listOf(
        Regex("""^\s*$"""),
        Regex("""[\d]+[.,][\d]{2}\s*[*€$]?\s*$"""),   // prices
        Regex("""^(total|subtotal|btw|tax|korting|discount|bon|receipt|kassabon|datum|date|tijd|time|tel|www|http|@|pin|cash|change|wisselgeld|bedankt|thank|welkom|welcome|kassa|store|winkel|adres|address|kvk|btw-nr|iban|nummer|number|member|lid|card|kaart|pas|transactie|transaction|ref|invoice|factuur)""", RegexOption.IGNORE_CASE),
        Regex("""^\*+$"""),
        Regex("""^-+$"""),
        Regex("""^=+$"""),
        Regex("""^\d+$"""),
        Regex("""^[A-Z0-9\s]{1,3}$"""),               // very short codes
        Regex("""^\d{2}[/-]\d{2}[/-]\d{2,4}"""),      // dates
        Regex("""^\d{2}:\d{2}"""),                     // times
    )

    /**
     * Runs ML Kit OCR on the given bitmap and returns the raw text.
     */
    suspend fun recognizeText(bitmap: Bitmap): String = suspendCoroutine { continuation ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                continuation.resume(visionText.text)
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }

    /**
     * Parses raw OCR text from a grocery receipt into a list of food products.
     * Uses a food keyword list to filter out non-food lines.
     */
    fun parseReceiptText(rawText: String): List<ScannedProduct> {
        val lines = rawText.lines()
        val products = mutableListOf<ScannedProduct>()

        for (line in lines) {
            val trimmed = line.trim()

            // Skip obvious non-food lines
            if (nonFoodPatterns.any { it.containsMatchIn(trimmed) }) continue
            if (trimmed.length < 3) continue

            // Strip trailing price
            val withoutPrice = trimmed
                .replace(Regex("""\s+[\d]+[.,][\d]{2}\s*[*€$]?\s*$"""), "")
                .trim()

            if (withoutPrice.length < 3) continue

            // Check for quantity prefix like "2x Product" or "2 x Product"
            val quantityMatch = Regex("""^(\d+)\s*[xX]\s*(.+)$""").find(withoutPrice)
            val (qty, productName) = if (quantityMatch != null) {
                val q = quantityMatch.groupValues[1].toIntOrNull() ?: 1
                val n = quantityMatch.groupValues[2].trim()
                Pair(q, n)
            } else {
                Pair(1, withoutPrice)
            }

            if (productName.length < 3) continue

            // Only add if it looks like a food item
            if (looksLikeFood(productName)) {
                products.add(ScannedProduct(
                    name = cleanProductName(productName),
                    quantity = qty
                ))
            }
        }

        return products.distinctBy { it.name.lowercase() }
    }

    /**
     * Returns true if the line likely refers to a food product.
     * Checks against the food keyword list.
     */
    private fun looksLikeFood(text: String): Boolean {
        val lower = text.lowercase()
        return foodKeywords.any { keyword -> lower.contains(keyword) }
    }

    private fun cleanProductName(name: String): String {
        return name
            .replace(Regex("""\s{2,}"""), " ")
            .replace(Regex("""[*|#]"""), "")
            .trim()
            .replaceFirstChar { it.uppercase() }
    }
}
