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
    val quantity: Int = 1,
    val isFood: Boolean = true,
    val expiryDays: Int = 7,
    val expirationDate: String = java.time.LocalDate.now().plusDays(7).toString()
)

object VisionHandler {

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
     * Parses raw OCR text from a receipt into a list of potential product lines.
     * Does basic filtering of prices, dates, and metadata — AI handles food classification.
     */
    fun parseReceiptText(rawText: String): List<ScannedProduct> {
        val lines = rawText.lines()
        val products = mutableListOf<ScannedProduct>()

        for (line in lines) {
            val trimmed = line.trim()

            if (nonFoodPatterns.any { it.containsMatchIn(trimmed) }) continue
            if (trimmed.length < 3) continue

            val withoutPrice = trimmed
                .replace(Regex("""\s+[\d]+[.,][\d]{2}\s*[*€$]?\s*$"""), "")
                .trim()

            if (withoutPrice.length < 3) continue

            val quantityMatch = Regex("""^(\d+)\s*[xX]\s*(.+)$""").find(withoutPrice)
            val (qty, productName) = if (quantityMatch != null) {
                val q = quantityMatch.groupValues[1].toIntOrNull() ?: 1
                val n = quantityMatch.groupValues[2].trim()
                Pair(q, n)
            } else {
                Pair(1, withoutPrice)
            }

            if (productName.length < 3) continue
            if (productName.matches(Regex("""[\d\s.,]+"""))) continue

            products.add(ScannedProduct(
                name = cleanProductName(productName),
                quantity = qty
            ))
        }

        return products.distinctBy { it.name.lowercase() }
    }

    /**
     * Applies AI classification results to scanned products.
     * Updates isFood, expiryDays, and expirationDate for each item.
     */
    fun applyAIClassification(
        products: List<ScannedProduct>,
        classifications: List<FoodClassification>
    ): List<ScannedProduct> {
        return products.mapIndexed { index, product ->
            val classification = classifications.getOrNull(index)
            if (classification != null) {
                product.copy(
                    name = classification.name.ifEmpty { product.name },
                    isFood = classification.isFood,
                    expiryDays = classification.expiryDays,
                    expirationDate = if (classification.isFood) {
                        java.time.LocalDate.now().plusDays(classification.expiryDays.toLong()).toString()
                    } else {
                        ""
                    }
                )
            } else {
                product
            }
        }
    }

    private fun cleanProductName(name: String): String {
        return name
            .replace(Regex("""\s{2,}"""), " ")
            .replace(Regex("""[*|#]"""), "")
            .trim()
            .replaceFirstChar { it.uppercase() }
    }
}
