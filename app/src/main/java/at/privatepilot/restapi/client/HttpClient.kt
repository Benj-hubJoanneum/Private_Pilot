package at.privatepilot.restapi.client

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.BufferedReader
import java.io.InputStreamReader

class HttpClient {

    private val client: OkHttpClient = OkHttpClient()

    fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()

        return response.body?.string() ?: ""
    }

    fun getWithBufferedReader(url: String): BufferedReader {
        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()

        val inputStream = response.body?.byteStream()
        return BufferedReader(InputStreamReader(inputStream))
    }

    fun post(url: String, header: String, requestBody: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, requestBody)

        val request = Request.Builder()
            .url(url)
            .addHeader("name", header)
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        return response.body?.string() ?: ""
    }
}