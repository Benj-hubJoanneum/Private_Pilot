package at.privatepilot.client.restapi.client

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class HttpClient() {

    private val client: OkHttpClient = OkHttpClient()

    fun get(url: String): String {
        try {
            val request = Request.Builder()
                .url(url)
                .build()

            val clientWithTimeout = client.newBuilder()
                .callTimeout(1, TimeUnit.SECONDS)
                .build()

            val response = clientWithTimeout.newCall(request).execute()

            return response.body?.string() ?: ""
        } catch (e: Exception) {
            return ""
        }
    }

    fun post(url: String, header_name: String, header_IP: String, requestBody: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = requestBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("name", header_name)
            .addHeader("ip", header_IP)
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        return response.body?.string() ?: ""
    }
}