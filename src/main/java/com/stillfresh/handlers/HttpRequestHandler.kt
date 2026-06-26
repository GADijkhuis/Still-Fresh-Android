package com.stillfresh.handlers

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

class HttpRequestHandler {
    companion object {
        suspend fun get(url: String, httpClient: HttpClient) : HttpResponse {
            return httpClient.get(url)
        }
    }
}