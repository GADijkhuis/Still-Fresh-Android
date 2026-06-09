package com.stillfresh.handlers

import android.util.Log
import com.stillfresh.BuildConfig
import com.stillfresh.dataclasses.openfoodfacts.OpenFoodFactsProductResult
import com.stillfresh.dataclasses.openfoodfacts.OpenFoodFactsSearchResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

object OpenFoodFactsHandler {
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

    private val PRODUCT_URL = BuildConfig.OPENFOODFACTS_API_URL + "product/"
    private val SEARCH_URL = BuildConfig.OPENFOODFACTS_SEARCH_URL + "?search_simple=1&json=1&page_size=20&search_terms="

    suspend fun getProductById(productId: String) : OpenFoodFactsProductResult? {
        try {
            val response = HttpRequestHandler.get("${PRODUCT_URL}${productId}.json?fields=product_type,product_name,brands,packagings,image_url,ingredients", httpClient)

            if (response.status.value !in 200..299) {
                //Response is invalid
                throw Exception(response.status.toString())
            }

            val productResult: OpenFoodFactsProductResult = response.body()
            
            return productResult
        } catch (e: Exception) {
            Log.e("Food Fetch Error: ", e.toString())
            return null
        }
    }

    suspend fun getSearchResultsByName(searchValue: String) : OpenFoodFactsSearchResult? {
        try {
            val response = HttpRequestHandler.get("${SEARCH_URL}${searchValue}", httpClient)

            if (response.status.value !in 200..299) {
                //Response is invalid
                throw Exception(response.status.toString())
            }

            val searchResult: OpenFoodFactsSearchResult = response.body();

            return searchResult
        } catch (e: Exception) {
            Log.e("Food Fetch Error: ", e.toString())
            return null
        }
    }

}