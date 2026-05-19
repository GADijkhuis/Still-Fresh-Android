package com.stillfresh.handlers

import android.util.Log
import com.stillfresh.BuildConfig
import com.stillfresh.dataclasses.openfoodfacts.OpenFoodFactsProductResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

class OpenFoodFactsHandler {
    private val httpClient = HttpClient() {
        headers {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            append(HttpHeaders.UserAgent, "StillFresh?/1.0 (${BuildConfig.OPENFOODFACTS_API_MAIL})")

            val credentials = "${BuildConfig.OPENFOODFACTS_API_USER}:${BuildConfig.OPENFOODFACTS_API_PASS}"
            val encodedCredentials = Base64.encode(credentials.toByteArray())

            append(HttpHeaders.Authorization, "Basic $encodedCredentials")
        }
    }

    private val productUrl = BuildConfig.OPENFOODFACTS_API_URL + "product/"
    private val searchUrl = BuildConfig.OPENFOODFACTS_API_URL + "search/"

    suspend fun getProductById(productId: String) {
        try {
            val response = HttpRequestHandler.get("${productUrl}${productId}.json?fields=product_type,product_name,brands,packagings,image_url,ingredients", httpClient)

            if (response.status.value !in 200..299) {
                //Response is invalid
                throw Exception(response.status.toString())
            }

            val responseClass: OpenFoodFactsProductResult = response.body()

            Log.d("Food Fetch: ", responseClass.toString())

        } catch (e: Exception) {
            Log.d("Food Fetch Error: ", e.toString())
        }
    }

    suspend fun getSearchResultsByName(searchValue: String) {

    }

}